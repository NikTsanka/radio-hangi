package com.canka.dev.radiohangi.ui

import android.app.Application
import android.content.ComponentName
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.canka.dev.radiohangi.RadioHangiApplication
import com.canka.dev.radiohangi.domain.model.Track
import com.canka.dev.radiohangi.player.PlaybackService
import com.canka.dev.radiohangi.player.ZENO_MEDIA_ID
import com.canka.dev.radiohangi.ui.radio.AlbumArt
import com.canka.dev.radiohangi.ui.radio.MarqueeText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Compact now-playing for the persistent mini-player. `null` = nothing to show. */
data class MiniPlayerState(
    val title: String,
    val subtitle: String,
    val artworkUri: String?,
    val isPlaying: Boolean,
    val isBuffering: Boolean,
)

/**
 * App-scoped ViewModel backing the persistent mini-player. It owns its own [MediaController]
 * connected to the shared playback session and mirrors the now-playing into [state], so the
 * control stays live no matter which tab is on screen.
 */
class MiniPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<MiniPlayerState?>(null)
    val state = _state.asStateFlow()

    private var controller: MediaController? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)
    }

    init {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val c = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = c
                c.addListener(listener)
                sync(c)
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private fun sync(player: Player) {
        val item = player.currentMediaItem
        if (item == null || player.playbackState == Player.STATE_IDLE) {
            _state.value = null
            return
        }
        val md = player.mediaMetadata
        val title: String
        val subtitle: String
        if (item.mediaId == ZENO_MEDIA_ID) {
            // Home stream: SSE already split the song/artist into the metadata fields.
            title = md.title?.toString() ?: "Radio Hangi"
            subtitle = md.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Live"
        } else {
            // World station: parse its "Artist - Title" ICY title the same way Screen A does.
            val parsed = md.title?.toString()?.let(Track::fromStreamTitle)
            if (parsed != null && parsed.hasArtist) {
                title = parsed.song
                subtitle = parsed.artist
            } else {
                title = md.title?.toString() ?: "World Radio"
                subtitle = md.artist?.toString().orEmpty()
            }
        }
        _state.value = MiniPlayerState(
            title = title,
            subtitle = subtitle,
            artworkUri = md.artworkUri?.toString(),
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
        )
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        super.onCleared()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RadioHangiApplication
                MiniPlayerViewModel(app)
            }
        }
    }
}

/**
 * Persistent mini-player bar. Shows the current art + title/subtitle and a play/pause toggle;
 * tapping the bar (anywhere but the button) invokes [onOpen] to jump to the full Radio screen.
 * Renders nothing when there's no active media.
 */
@Composable
fun MiniPlayer(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MiniPlayerViewModel = viewModel(factory = MiniPlayerViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state ?: return

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArt(coverUrl = s.artworkUri, modifier = Modifier.size(44.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                MarqueeText(
                    text = s.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (s.subtitle.isNotBlank()) {
                    Text(
                        text = s.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(contentAlignment = Alignment.Center) {
                IconButton(onClick = viewModel::togglePlayPause) {
                    Icon(
                        imageVector = if (s.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (s.isPlaying) "Pause" else "Play",
                    )
                }
                if (s.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
