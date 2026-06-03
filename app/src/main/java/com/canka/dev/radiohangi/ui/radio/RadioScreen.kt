package com.canka.dev.radiohangi.ui.radio

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.canka.dev.radiohangi.data.AppConfig
import com.canka.dev.radiohangi.domain.model.PlaybackState
import com.canka.dev.radiohangi.domain.model.SleepTimerOption
import com.canka.dev.radiohangi.domain.model.Track

/**
 * Screen A — Main Radio. Now-playing (SSE) + Deezer album art + blurred ambient background +
 * animated equalizer + lyrics panel + recent-songs history + play/pause, volume/mute,
 * sleep timer, and share.
 */
@Composable
fun RadioScreen(viewModel: RadioViewModel = viewModel(factory = RadioViewModel.Factory)) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val sleep by viewModel.sleep.collectAsStateWithLifecycle()
    val quickStations by viewModel.quickStations.collectAsStateWithLifecycle()
    val favoriteUuids by viewModel.favoriteUuids.collectAsStateWithLifecycle()
    val currentStation by viewModel.currentStation.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    val current = ui.current

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground(coverUrl = current?.coverUrl)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ---- FIXED TOP: quick play, now-playing art + title ----

            // Compact quick-play of favorites/recents pinned at the very top. A "World Radio"
            // hint shows while a station is playing. (Home + Lyrics now live in the app bar.)
            if (quickStations.isNotEmpty()) {
                StationQuickRow(
                    stations = quickStations,
                    favoriteUuids = favoriteUuids,
                    onPlay = viewModel::playStation,
                    compact = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (ui.isStation) {
                Text(
                    text = "World Radio",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(12.dp))

            SpinningDisc(
                coverUrl = current?.coverUrl,
                isPlaying = playback.isPlaying,
                modifier = Modifier.fillMaxWidth(0.82f).aspectRatio(1f),
            )

            Spacer(Modifier.height(16.dp))

            MarqueeText(
                text = current?.song ?: "Radio Hangi",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = current?.artist?.takeIf { it.isNotBlank() } ?: "Live stream",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )

            // Breathing room below the band name so the equalizer doesn't feel glued to it.
            Spacer(Modifier.height(24.dp))
            EqualizerBars(playing = playback.isPlaying)

            // Flexible spacer pushes the transport controls to the bottom.
            Spacer(Modifier.weight(1f))

            // ---- FIXED BOTTOM: transport + volume, sleep + share ----
            PlayerControls(
                isPlaying = playback.isPlaying,
                isBuffering = playback.state == PlaybackState.Buffering,
                hasError = playback.state == PlaybackState.Error,
                volume = playback.volume,
                isMuted = playback.isMuted,
                // Favorite the currently-playing World station (hidden for the Zeno home stream).
                showFavorite = currentStation != null,
                isFavorite = currentStation?.uuid in favoriteUuids,
                onPlayPause = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.togglePlayPause()
                },
                onRetry = viewModel::retry,
                onVolumeChange = viewModel::setVolume,
                onToggleMute = viewModel::toggleMute,
                onToggleFavorite = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleFavoriteCurrent()
                },
            )

            Spacer(Modifier.height(4.dp))

            SecondaryActions(
                sleepOption = sleep.option,
                sleepRemainingMs = sleep.remainingMs,
                onSelectSleep = viewModel::setSleepTimer,
                currentTrack = current,
            )
        }
    }
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    hasError: Boolean,
    volume: Float,
    isMuted: Boolean,
    showFavorite: Boolean,
    isFavorite: Boolean,
    onPlayPause: () -> Unit,
    onRetry: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            FilledIconButton(
                onClick = onPlayPause,
                shape = CircleShape,
                enabled = !isBuffering,
                modifier = Modifier.size(88.dp),
            ) {
                // Crossfade the transport icon for a smoother play/pause toggle.
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "play-pause",
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
            // Buffering ring around the transport button while the stream is connecting.
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(88.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Surface a stream failure with a one-tap retry (e.g. a station that dropped).
        if (hasError) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Couldn't play this stream.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onToggleMute) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff
                    else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                )
            }
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
            )
            // Favorite toggle for the playing World station, to the right of the volume slider.
            if (showFavorite) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SecondaryActions(
    sleepOption: SleepTimerOption,
    sleepRemainingMs: Long,
    onSelectSleep: (SleepTimerOption) -> Unit,
    currentTrack: Track?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Sleep timer with a dropdown of Off / 15 / 30 / 60.
        Box {
            TextButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.Bedtime, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(sleepTimerLabel(sleepOption, sleepRemainingMs))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                SleepTimerOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onSelectSleep(option)
                            menuOpen = false
                        },
                    )
                }
            }
        }

        // Find the current song on YouTube.
        TextButton(
            onClick = { findCurrentSong(context, currentTrack) },
            enabled = currentTrack?.song?.isNotBlank() == true,
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Find")
        }

        // Share the current song.
        TextButton(
            onClick = { shareCurrentSong(context, currentTrack) },
            enabled = currentTrack != null,
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Share")
        }
    }
}

private fun sleepTimerLabel(option: SleepTimerOption, remainingMs: Long): String {
    if (option == SleepTimerOption.Off) return "Sleep"
    val totalSeconds = (remainingMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/** Opens a YouTube search for the current song (falls back to whatever handles the web URL). */
private fun findCurrentSong(context: android.content.Context, track: Track?) {
    track ?: return
    val query = listOf(track.artist, track.song)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { return }
    val url = "https://www.youtube.com/results?search_query=" + android.net.Uri.encode(query)
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    }
}

private fun shareCurrentSong(context: android.content.Context, track: Track?) {
    track ?: return
    val artist = track.artist.ifBlank { "Radio Hangi" }
    val text = AppConfig.SHARE_TEMPLATE.format(track.song, artist)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}
