package com.canka.dev.radiohangi.player

import android.content.Intent
import android.media.AudioManager
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.metadata.icy.IcyInfo
import com.canka.dev.radiohangi.widget.updateRadioWidget
import com.canka.dev.radiohangi.widget.updateWidgetQuickStations
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.canka.dev.radiohangi.RadioHangiApplication
import com.canka.dev.radiohangi.data.AppConfig
import com.canka.dev.radiohangi.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The playback core: a Media3 [MediaSessionService] that owns a single [ExoPlayer] and
 * exposes it through a [MediaSession].
 *
 * The [MediaSession] gives us — for free — the foreground media notification, lock-screen
 * transport controls, and headset / Bluetooth media-button handling. The service keeps
 * running (foreground) while playing, so audio continues with the screen off.
 *
 * It also mirrors the live now-playing [Track] from the repository into the player's
 * MediaMetadata, so the notification/lock screen show the current song + album art.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    // Player ops must happen on the app's main thread.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val radioRepository by lazy {
        (application as RadioHangiApplication).container.radioRepository
    }
    private val equalizerController by lazy {
        (application as RadioHangiApplication).container.equalizerController
    }

    // Current World station's favicon (art fallback) + last applied ICY title; tracked so we can
    // reset the art when the song changes and skip duplicate metadata events.
    private var stationFaviconUri: String? = null
    private var stationStreamTitle: String? = null
    private var stationCoverJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        // Many Radio Browser stream URLs respond with a 301/302 redirect — frequently
        // cross-protocol (HTTPS→HTTP) via a CDN. ExoPlayer's default data source rejects
        // those, so the station fails with "Source error". Allow cross-protocol redirects
        // (and send a real User-Agent — some hosts 403 the default one).
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("RadioHangi/1.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
        val mediaSourceFactory = DefaultMediaSourceFactory(
            DefaultDataSource.Factory(this, httpDataSourceFactory),
        )

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        // Fixed audio session id so the equalizer can attach to the player's output.
        val audioSessionId = getSystemService(AudioManager::class.java).generateAudioSessionId()
        player.audioSessionId = audioSessionId
        equalizerController.bind(audioSessionId)

        // Preload the Main Radio (Zeno) stream so pressing play works immediately.
        player.setMediaItem(buildZenoMediaItem())
        player.prepare()

        mediaSession = MediaSession.Builder(this, player).build()

        observeNowPlaying()
        attachWidgetSync(player)
        attachStationMetadataSync(player)
        observeQuickStations()
    }

    /**
     * Keep the widget's quick-play row in sync with favorites + recents (recents first, deduped).
     * Lives here because the service outlives the UI, so the widget updates even with no Activity.
     */
    private fun observeQuickStations() {
        val container = (application as RadioHangiApplication).container
        scope.launch {
            combine(
                container.recentsRepository.recents,
                container.favoritesRepository.favorites,
            ) { recents, favorites ->
                (recents + favorites.filterNot { fav -> recents.any { it.uuid == fav.uuid } })
                    .take(AppConfig.WIDGET_QUICK_SIZE)
            }.collectLatest { quick ->
                updateWidgetQuickStations(this@PlaybackService, quick)
            }
        }
    }

    /**
     * Surface a World station's *current song* (title + album art) from its ICY in-stream
     * metadata, writing it into the player's MediaMetadata so every surface — Screen A, the
     * mini-player, the notification, the widget — reads one source of truth.
     *
     * Icecast/Shoutcast stations broadcast the playing song as an ICY `StreamTitle`
     * ("Artist - Title"), delivered via [Player.Listener.onMetadata]. We can't rely on the
     * player's combined [Player.getMediaMetadata] title for this: the station's static MediaItem
     * title (its name) is merged *over* the dynamic ICY title and wins. So we read the raw ICY
     * title here, write it into the MediaItem title, then resolve a Deezer cover for it and write
     * that into the artwork — both via metadata-only [Player.replaceMediaItem] calls that don't
     * interrupt playback. The Zeno home stream gets its now-playing from SSE (see [observeNowPlaying]).
     */
    private fun attachStationMetadataSync(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // A new station loaded: remember its favicon (art fallback) and reset song state.
                stationFaviconUri = mediaItem?.mediaMetadata?.artworkUri?.toString()
                stationStreamTitle = mediaItem?.mediaMetadata?.title?.toString()
                stationCoverJob?.cancel()
            }

            override fun onMetadata(metadata: Metadata) {
                val item = player.currentMediaItem ?: return
                if (item.mediaId == ZENO_MEDIA_ID) return // home stream uses SSE, not ICY

                val streamTitle = (0 until metadata.length())
                    .asSequence()
                    .map { metadata.get(it) }
                    .filterIsInstance<IcyInfo>()
                    .firstNotNullOfOrNull { it.title?.trim()?.takeIf(String::isNotEmpty) }
                    ?: return
                if (streamTitle == stationStreamTitle) return // same song — nothing to do
                stationStreamTitle = streamTitle

                // Show the new song immediately, resetting art to the station favicon until (and
                // only if) a Deezer cover resolves for it.
                applyStationMetadata(player, streamTitle, stationFaviconUri)

                val track = Track.fromStreamTitle(streamTitle)
                stationCoverJob?.cancel()
                if (track.hasArtist) {
                    stationCoverJob = scope.launch {
                        val cover = radioRepository.lookupCover(track.artist, track.song)
                            ?: return@launch
                        // Apply only if still the current song on the same station.
                        if (stationStreamTitle == streamTitle &&
                            player.currentMediaItem?.mediaId == item.mediaId
                        ) {
                            applyStationMetadata(player, streamTitle, cover)
                        }
                    }
                }
            }
        })
    }

    /** Metadata-only update of the current station item's title + artwork (no playback interruption). */
    private fun applyStationMetadata(player: Player, title: String, artUri: String?) {
        val item = player.currentMediaItem ?: return
        val current = item.mediaMetadata
        if (current.title?.toString() == title && current.artworkUri?.toString() == artUri) return
        val updated = current.buildUpon()
            .setTitle(title)
            .setArtworkUri(artUri?.toUri())
            .build()
        player.replaceMediaItem(
            player.currentMediaItemIndex,
            item.buildUpon().setMediaMetadata(updated).build(),
        )
    }

    /** Mirror the player's metadata + play state to the home-screen widget on every change. */
    private fun attachWidgetSync(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onEvents(p: Player, events: Player.Events) {
                if (events.containsAny(
                        Player.EVENT_MEDIA_METADATA_CHANGED,
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_PLAYBACK_STATE_CHANGED,
                    )
                ) {
                    val md = p.mediaMetadata
                    scope.launch {
                        updateRadioWidget(
                            context = this@PlaybackService,
                            title = md.title?.toString() ?: "Radio Hangi",
                            artist = md.artist?.toString() ?: "",
                            artUri = md.artworkUri?.toString(),
                            isPlaying = p.isPlaying,
                        )
                    }
                }
            }
        })
    }

    /**
     * Keep the notification/lock-screen metadata in sync with the live now-playing track.
     * Uses metadata-only [androidx.media3.common.Player.replaceMediaItem], which Media3
     * applies without interrupting playback of the live stream.
     */
    private fun observeNowPlaying() {
        val repository = (application as RadioHangiApplication).container.radioRepository
        repository.start() // idempotent — ensures SSE is running even if launched headless
        scope.launch {
            repository.currentTrack.collectLatest { track ->
                track ?: return@collectLatest
                val player = mediaSession?.player ?: return@collectLatest
                val item = player.currentMediaItem ?: return@collectLatest
                // Only the Zeno main stream carries live now-playing metadata; never
                // overwrite a World Radio station's own metadata.
                if (item.mediaId != ZENO_MEDIA_ID) return@collectLatest

                val metadata = MediaMetadata.Builder()
                    .setTitle(track.song)
                    .setArtist(track.artist.ifBlank { "Radio Hangi" })
                    .setIsPlayable(true)
                    .apply { track.coverUrl?.let { setArtworkUri(it.toUri()) } }
                    .build()

                player.replaceMediaItem(
                    player.currentMediaItemIndex,
                    item.buildUpon().setMediaMetadata(metadata).build(),
                )
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        equalizerController.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
