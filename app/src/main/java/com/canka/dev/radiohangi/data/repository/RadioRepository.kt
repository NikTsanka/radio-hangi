package com.canka.dev.radiohangi.data.repository

import com.canka.dev.radiohangi.data.AppConfig
import com.canka.dev.radiohangi.data.remote.DeezerApi
import com.canka.dev.radiohangi.data.remote.LyricsApi
import com.canka.dev.radiohangi.data.remote.ZenoMetadataSource
import com.canka.dev.radiohangi.domain.model.ConnectionStatus
import com.canka.dev.radiohangi.domain.model.LyricsState
import com.canka.dev.radiohangi.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Source of truth for Screen A (Main Radio). Subscribes to the Zeno SSE metadata, parses
 * the now-playing [Track], resolves album art from Deezer, and fetches lyrics. Both the UI
 * ([com.canka.dev.radiohangi.ui.radio.RadioViewModel]) and the playback service observe
 * these flows.
 *
 * Application-scoped singleton; [start] is idempotent and mirrors the web version's
 * "subscribe on load" behavior. All network work runs on [Dispatchers.IO].
 */
class RadioRepository(
    private val metadataSource: ZenoMetadataSource,
    private val deezerApi: DeezerApi,
    private val lyricsApi: LyricsApi,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _lyrics = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val lyrics: StateFlow<LyricsState> = _lyrics.asStateFlow()

    private val _connection = MutableStateFlow(ConnectionStatus.Idle)
    val connection: StateFlow<ConnectionStatus> = _connection.asStateFlow()

    private val started = AtomicBoolean(false)

    /** Begins the SSE pipeline. Safe to call multiple times — only the first call has effect. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            metadataSource.streamTitles()
                .onStart { _connection.value = ConnectionStatus.Connecting }
                .onEach { _connection.value = ConnectionStatus.Connected }
                .map(Track::fromStreamTitle)
                // Ignore repeated identical now-playing events.
                .distinctUntilChanged { a, b -> a.key == b.key }
                .retryWhen { _, _ ->
                    // Auto-reconnect after ~5s on any SSE error (mirrors the web version).
                    _connection.value = ConnectionStatus.Error
                    delay(AppConfig.SSE_RECONNECT_DELAY_MS)
                    _connection.value = ConnectionStatus.Connecting
                    true
                }
                .collect(::onNewTrack)
        }
    }

    private fun onNewTrack(track: Track) {
        _currentTrack.value = track
        _lyrics.value = LyricsState.Loading

        // Resolve art + lyrics concurrently; failures degrade gracefully (fallbacks in UI).
        scope.launch { resolveCover(track) }
        scope.launch { resolveLyrics(track) }
    }

    /** Album-art chain: Deezer cover_big → (null → UI falls back to bundled cover.png). */
    private suspend fun resolveCover(track: Track) {
        val cover = lookupCover(track.artist, track.song)
        // Only apply if this is still the current track (ignore stale responses).
        if (cover != null && _currentTrack.value?.key == track.key) {
            _currentTrack.update { it?.copy(coverUrl = cover) }
        }
    }

    private suspend fun resolveLyrics(track: Track) {
        val state = lookupLyrics(track.artist, track.song)
        if (_currentTrack.value?.key == track.key) _lyrics.value = state // ignore stale
    }

    /**
     * One-shot album-art lookup (Deezer `cover_big`) for an arbitrary song. Returns null when
     * nothing matches. Reusable: the Zeno pipeline and World-station now-playing both use it.
     */
    suspend fun lookupCover(artist: String, song: String): String? {
        val query = "$artist $song".trim()
        if (query.isBlank()) return null
        return runCatching {
            deezerApi.search(query).data.firstOrNull()?.album?.coverBig
        }.getOrNull()
    }

    /** One-shot lyrics lookup (lyrics.ovh) for an arbitrary song. */
    suspend fun lookupLyrics(artist: String, song: String): LyricsState {
        if (artist.isBlank()) return LyricsState.NotFound
        val text = runCatching { lyricsApi.getLyrics(artist, song).lyrics }.getOrNull()
        return if (text.isNullOrBlank()) LyricsState.NotFound else LyricsState.Found(text.trim())
    }
}
