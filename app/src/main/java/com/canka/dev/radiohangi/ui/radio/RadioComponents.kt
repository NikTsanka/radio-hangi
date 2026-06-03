package com.canka.dev.radiohangi.ui.radio

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.canka.dev.radiohangi.R
import com.canka.dev.radiohangi.domain.model.ConnectionStatus
import com.canka.dev.radiohangi.domain.model.LyricsState
import com.canka.dev.radiohangi.domain.model.Station
import com.canka.dev.radiohangi.domain.model.Track

/**
 * Full-bleed blurred album art used as an ambient background (mirrors the web version).
 * A dark scrim keeps foreground text readable. Blur is a no-op below API 31 — the dimmed
 * image still reads fine.
 */
@Composable
fun AmbientBackground(coverUrl: String?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = coverUrl,
            contentDescription = null,
            error = painterResource(R.drawable.cover),
            fallback = painterResource(R.drawable.cover),
            placeholder = painterResource(R.drawable.cover),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(48.dp),
        )
        // Scrim: darken toward the bottom for contrast with controls.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f)),
        )
    }
}

/** Square album art with rounded corners; falls back to the bundled cover.png. */
@Composable
fun AlbumArt(coverUrl: String?, modifier: Modifier = Modifier) {
    AsyncImage(
        model = coverUrl,
        contentDescription = "Album art",
        error = painterResource(R.drawable.cover),
        fallback = painterResource(R.drawable.cover),
        placeholder = painterResource(R.drawable.cover),
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
    )
}

/**
 * Vinyl-style round disc showing the album art at its center. Spins continuously while
 * [isPlaying] and freezes (keeping its current angle) when paused. Falls back to the
 * bundled cover.png. One full turn every 12s — a calm, record-player feel.
 */
@Composable
fun SpinningDisc(coverUrl: String?, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            // Loops from the current angle to +360°; visually seamless since 360° ≡ 0°.
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 12_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            )
        } else {
            rotation.stop() // Freeze at the current angle.
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // The record: black vinyl body that carries the spin + the album art as its label.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation.value)
                .clip(CircleShape)
                .background(Color(0xFF101010)),
            contentAlignment = Alignment.Center,
        ) {
            // Album art forms the center label (~62% of the disc).
            AsyncImage(
                model = coverUrl,
                contentDescription = "Album art",
                error = painterResource(R.drawable.cover),
                fallback = painterResource(R.drawable.cover),
                placeholder = painterResource(R.drawable.cover),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize(0.62f)
                    .clip(CircleShape)
                    .border(2.dp, Color(0x33FFFFFF), CircleShape),
            )
            // Subtle glossy sheen across the vinyl grooves.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0x22FFFFFF), Color.Transparent, Color(0x14FFFFFF)),
                        ),
                    ),
            )
        }
        // Spindle hole in the center (does not spin).
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
        )
    }
}

/** Single-line text that scrolls horizontally when it overflows (long song titles). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = TextAlign.Center,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineSmall,
) {
    Text(
        text = text,
        style = style,
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = 1,
        modifier = modifier.basicMarquee(),
    )
}

/** Animated equalizer bars; only animates while [playing] (no transition runs when paused). */
@Composable
fun EqualizerBars(
    playing: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 5,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    // Only spin up the infinite transition while playing — when paused, the bars are static
    // (avoids burning frames animating invisible motion behind the paused UI).
    val transition = if (playing) rememberInfiniteTransition(label = "equalizer") else null
    Row(
        modifier = modifier.height(24.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(barCount) { i ->
            val h = if (transition != null) {
                val fraction by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 350 + i * 90),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "bar$i",
                )
                fraction
            } else {
                0.25f
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
    }
}

/** Colored dot + label reflecting the SSE/stream connection state. */
@Composable
fun ConnectionIndicator(status: ConnectionStatus, modifier: Modifier = Modifier) {
    // Green for the live "on air" dot; other states keep their semantic colors.
    val liveGreen = Color(0xFF22C55E)
    val (color, label) = when (status) {
        ConnectionStatus.Connected -> liveGreen to "Live"
        ConnectionStatus.Connecting -> MaterialTheme.colorScheme.tertiary to "Connecting…"
        ConnectionStatus.Error -> MaterialTheme.colorScheme.error to "Reconnecting…"
        ConnectionStatus.Idle -> MaterialTheme.colorScheme.onSurfaceVariant to "Idle"
    }
    // Pulsing blink for the dot — emphasizes that the stream is live.
    val transition = rememberInfiniteTransition(label = "live-dot")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "live-dot-alpha",
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha)),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/**
 * Quick-play row of favorite + recently-played stations. Tapping a chip plays that station on
 * Screen A without leaving the screen. Favorited stations carry a small heart badge. Renders
 * nothing when there are no stations to show.
 */
@Composable
fun StationQuickRow(
    stations: List<Station>,
    favoriteUuids: Set<String>,
    onPlay: (Station) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (stations.isEmpty()) return
    val radioFallback = rememberVectorPainter(Icons.Filled.Radio)
    // Compact mode is a tight single row of favicon circles (no header, no name labels) meant
    // to sit pinned near the top of the screen; the full mode adds a header and station names.
    val chipSize = if (compact) 44.dp else 56.dp
    val badgeSize = if (compact) 14.dp else 16.dp
    Column(modifier = modifier) {
        if (!compact) {
            Text(
                text = "Quick play",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp)) {
            items(stations, key = { it.uuid }) { station ->
                Column(
                    modifier = Modifier
                        .then(if (compact) Modifier else Modifier.width(72.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPlay(station) }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(
                            model = station.favicon,
                            contentDescription = station.name,
                            error = radioFallback,
                            fallback = radioFallback,
                            placeholder = radioFallback,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(chipSize).clip(CircleShape),
                        )
                        if (station.uuid in favoriteUuids) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Favorite",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(badgeSize)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(1.dp),
                            )
                        }
                    }
                    if (!compact) {
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/** Lyrics panel handling all four states gracefully (never blank, never crashes). */
@Composable
fun LyricsPanel(state: LyricsState, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.TopStart) {
        when (state) {
            LyricsState.Idle -> Unit
            LyricsState.Loading -> Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

            LyricsState.NotFound -> Text(
                text = "Lyrics not found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            is LyricsState.Found -> Text(
                text = state.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            )
        }
    }
}
