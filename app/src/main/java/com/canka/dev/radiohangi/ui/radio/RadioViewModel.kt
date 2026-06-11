package com.canka.dev.radiohangi.ui.radio

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.canka.dev.radiohangi.RadioHangiApplication
import com.canka.dev.radiohangi.data.AppConfig
import com.canka.dev.radiohangi.data.repository.FavoritesRepository
import com.canka.dev.radiohangi.data.repository.PlayerPrefsRepository
import com.canka.dev.radiohangi.data.repository.RadioRepository
import com.canka.dev.radiohangi.data.repository.RecentsRepository
import com.canka.dev.radiohangi.domain.model.ConnectionStatus
import com.canka.dev.radiohangi.domain.model.LyricsState
import com.canka.dev.radiohangi.domain.model.PlaybackState
import com.canka.dev.radiohangi.domain.model.SleepTimerOption
import com.canka.dev.radiohangi.domain.model.Station
import com.canka.dev.radiohangi.domain.model.Track
import com.canka.dev.radiohangi.player.PlaybackService
import com.canka.dev.radiohangi.player.ZENO_MEDIA_ID
import com.canka.dev.radiohangi.player.buildStationMediaItem
import com.canka.dev.radiohangi.player.buildZenoMediaItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Now-playing data for Screen A (sourced from [RadioRepository]). */
data class RadioUiState(
    val current: Track? = null,
    val lyrics: LyricsState = LyricsState.Idle,
    val connection: ConnectionStatus = ConnectionStatus.Idle,
    /** True when a World Radio station is playing instead of the Zeno home stream. */
    val isStation: Boolean = false,
)

/** Snapshot of the player's current media item (mirrored from the [MediaController]). */
private data class CurrentItem(
    val mediaId: String,
    val title: String?,
    val artist: String?,
    val artworkUri: String?,
)

/**
 * Lyrics for a World station's current song (resolved from its ICY stream title). Album art is
 * resolved in [PlaybackService] and read back from the player metadata, so it isn't duplicated here.
 */
private data class StationNowPlaying(
    val lyrics: LyricsState = LyricsState.Idle,
)

/** Live player/transport state (sourced from the MediaController). */
data class PlaybackUiState(
    val state: PlaybackState = PlaybackState.Idle,
    val isPlaying: Boolean = false,
    val volume: Float = AppConfig.DEFAULT_VOLUME,
    val isMuted: Boolean = false,
)

/** Sleep-timer state. [remainingMs] counts down while [option] is active. */
data class SleepUiState(
    val option: SleepTimerOption = SleepTimerOption.Off,
    val remainingMs: Long = 0L,
)

/**
 * ViewModel for Screen A. Bridges the [RadioRepository] (now-playing data) and a
 * [MediaController] (transport/volume), and owns the sleep-timer logic. State is exposed as
 * [StateFlow]s and collected by the Compose UI.
 */
class RadioViewModel(
    application: Application,
    private val repository: RadioRepository,
    private val favoritesRepository: FavoritesRepository,
    private val recentsRepository: RecentsRepository,
    private val playerPrefsRepository: PlayerPrefsRepository,
) : AndroidViewModel(application) {

    /** Quick-play stations for the Radio screen: recents first, then favorites not already shown. */
    val quickStations: StateFlow<List<Station>> =
        combine(recentsRepository.recents, favoritesRepository.favorites) { recents, favorites ->
            (recents + favorites.filterNot { fav -> recents.any { it.uuid == fav.uuid } })
                .take(AppConfig.QUICK_ROW_SIZE)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** UUIDs of favorited stations, so the quick row can flag them. */
    val favoriteUuids: StateFlow<Set<String>> =
        favoritesRepository.favorites
            .map { favorites -> favorites.mapTo(HashSet()) { it.uuid } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Mirrors the player's current media item so the screen can reflect what's actually playing. */
    private val _currentItem = MutableStateFlow<CurrentItem?>(null)

    /** Art + lyrics resolved from a World station's ICY stream title (when it broadcasts one). */
    private val _stationNp = MutableStateFlow(StationNowPlaying())

    /**
     * The full [Station] currently playing, resolved by media id from recents/favorites (every
     * played station is recorded in recents, so its complete object is retrievable here). Null
     * for the Zeno home stream, which isn't a favoritable World station.
     */
    val currentStation: StateFlow<Station?> =
        combine(
            _currentItem,
            recentsRepository.recents,
            favoritesRepository.favorites,
        ) { item, recents, favorites ->
            if (item == null || item.mediaId == ZENO_MEDIA_ID) null
            else (recents + favorites).firstOrNull { it.uuid == item.mediaId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Zeno home-stream now-playing (SSE song + Deezer art + lyrics). */
    private val zenoState: Flow<RadioUiState> =
        combine(
            repository.currentTrack,
            repository.lyrics,
            repository.connection,
        ) { current, lyrics, connection ->
            RadioUiState(current, lyrics, connection, isStation = false)
        }

    val uiState: StateFlow<RadioUiState> =
        combine(zenoState, _currentItem, _stationNp) { zeno, item, station ->
            if (item == null || item.mediaId == ZENO_MEDIA_ID) {
                zeno // the Zeno home stream
            } else {
                // A World station: when it broadcasts an "Artist - Title" ICY stream title we
                // surface that song (+ resolved art/lyrics); otherwise just the station favicon.
                val parsed = item.title?.let(Track::fromStreamTitle)
                val hasSong = parsed != null && parsed.hasArtist
                RadioUiState(
                    current = Track(
                        artist = if (hasSong) parsed.artist else item.artist.orEmpty(),
                        song = if (hasSong) parsed.song else (item.title ?: "World Radio"),
                        // Art comes from the player metadata (Deezer cover resolved by the service,
                        // or the station favicon as a fallback).
                        coverUrl = item.artworkUri,
                    ),
                    lyrics = station.lyrics,
                    connection = ConnectionStatus.Idle,
                    isStation = true,
                )
            }
        }.distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RadioUiState())

    private val _playback = MutableStateFlow(PlaybackUiState())
    val playback: StateFlow<PlaybackUiState> = _playback.asStateFlow()

    private val _sleep = MutableStateFlow(SleepUiState())
    val sleep: StateFlow<SleepUiState> = _sleep.asStateFlow()

    private var controller: MediaController? = null
    private var sleepJob: Job? = null
    private var volumeBeforeMute: Float = AppConfig.DEFAULT_VOLUME

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncPlaybackState(player)
            syncCurrentItem(player)
        }
    }

    init {
        repository.start()
        connectController()
        observeStationMetadata()
    }

    /**
     * For World stations, resolve lyrics from the ICY stream title the station broadcasts
     * (ExoPlayer surfaces it as the media title; album art is handled by the service). Re-runs
     * whenever the song changes; [collectLatest] cancels an in-flight lookup when the next arrives.
     */
    private fun observeStationMetadata() {
        viewModelScope.launch {
            _currentItem
                .map { item -> if (item == null || item.mediaId == ZENO_MEDIA_ID) null else item.title }
                .distinctUntilChanged()
                .collectLatest { title ->
                    val track = title?.let(Track::fromStreamTitle)
                    if (track == null || !track.hasArtist) {
                        _stationNp.value = StationNowPlaying() // station name only — no song info
                        return@collectLatest
                    }
                    _stationNp.value = StationNowPlaying(lyrics = LyricsState.Loading)
                    // Some stations broadcast "Title - Artist" reversed — if the direct lookup
                    // misses, retry with the halves swapped before giving up.
                    val direct = repository.lookupLyrics(track.artist, track.song)
                    val lyrics = if (direct is LyricsState.Found) direct
                    else repository.lookupLyrics(track.song, track.artist)
                    _stationNp.value = StationNowPlaying(lyrics = lyrics)
                }
        }
    }

    private fun connectController() {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val c = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = c
                c.addListener(playerListener)
                syncPlaybackState(c)
                syncCurrentItem(c)
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private fun syncPlaybackState(player: Player) {
        val state = when {
            player.playerError != null -> PlaybackState.Error
            player.playbackState == Player.STATE_BUFFERING -> PlaybackState.Buffering
            player.isPlaying -> PlaybackState.Playing
            player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED ->
                PlaybackState.Idle
            else -> PlaybackState.Paused
        }
        val volume = player.volume
        _playback.value = _playback.value.copy(
            state = state,
            isPlaying = player.isPlaying,
            volume = volume,
            isMuted = volume <= 0f,
        )
    }

    /** Capture the player's current media item so the UI reflects what's actually playing. */
    private fun syncCurrentItem(player: Player) {
        val item = player.currentMediaItem
        _currentItem.value = item?.let {
            val md = player.mediaMetadata
            CurrentItem(
                mediaId = it.mediaId,
                title = md.title?.toString(),
                artist = md.artist?.toString(),
                artworkUri = md.artworkUri?.toString(),
            )
        }
    }

    // ---- Transport controls ----

    /** Play/pause whatever is currently loaded (home stream or a World station). */
    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    /** Re-prepare and resume after a playback error (e.g. a dropped/blocked stream). */
    fun retry() {
        val c = controller ?: return
        c.prepare()
        c.play()
    }

    /** Play a World station from the quick-play row (and record it as recently played). */
    fun playStation(station: Station) {
        val c = controller ?: return
        if (station.streamUrl.isBlank()) return
        c.setMediaItem(buildStationMediaItem(station))
        c.prepare()
        c.play()
        viewModelScope.launch { recentsRepository.add(station) }
    }

    /** Toggle the currently-playing World station in favorites (no-op for the Zeno home stream). */
    fun toggleFavoriteCurrent() {
        val station = currentStation.value ?: return
        viewModelScope.launch { favoritesRepository.toggle(station) }
    }

    /** Switch back to the Zeno "home" radio and play it (used by the Home Radio button). */
    fun playHomeRadio() {
        val c = controller ?: return
        if (c.currentMediaItem?.mediaId != ZENO_MEDIA_ID) {
            c.setMediaItem(buildZenoMediaItem())
            c.prepare()
        } else if (c.playbackState == Player.STATE_IDLE) {
            c.prepare()
        }
        c.play()
    }

    fun setVolume(value: Float) {
        val v = value.coerceIn(0f, 1f)
        controller?.volume = v
        if (v > 0f) volumeBeforeMute = v
        persistVolume(v)
    }

    fun toggleMute() {
        val c = controller ?: return
        if (c.volume > 0f) {
            volumeBeforeMute = c.volume
            c.volume = 0f
        } else {
            c.volume = volumeBeforeMute.takeIf { it > 0f } ?: AppConfig.DEFAULT_VOLUME
        }
        persistVolume(c.volume)
    }

    private fun persistVolume(value: Float) {
        viewModelScope.launch { playerPrefsRepository.saveVolume(value) }
    }

    // ---- Sleep timer ----

    fun setSleepTimer(option: SleepTimerOption) {
        sleepJob?.cancel()
        if (option == SleepTimerOption.Off) {
            _sleep.value = SleepUiState()
            return
        }
        sleepJob = viewModelScope.launch {
            var remaining = option.minutes * 60_000L
            while (remaining > 0) {
                _sleep.value = SleepUiState(option, remaining)
                delay(1_000)
                remaining -= 1_000
            }
            _sleep.value = SleepUiState()
            controller?.pause()
        }
    }

    override fun onCleared() {
        sleepJob?.cancel()
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        super.onCleared()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RadioHangiApplication
                RadioViewModel(
                    app,
                    app.container.radioRepository,
                    app.container.favoritesRepository,
                    app.container.recentsRepository,
                    app.container.playerPrefsRepository,
                )
            }
        }
    }
}
