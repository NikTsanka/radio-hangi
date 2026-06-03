package com.canka.dev.radiohangi.ui.world

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.canka.dev.radiohangi.domain.model.Station
import com.canka.dev.radiohangi.domain.model.Tag
import com.canka.dev.radiohangi.domain.model.isoFlag

/** Horizontally scrolling, multi-select genre chips. */
@Composable
fun GenreChipsRow(
    tags: List<Tag>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tags.isEmpty()) return
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tags, key = { it.name }) { tag ->
            FilterChip(
                selected = tag.name in selected,
                onClick = { onToggle(tag.name) },
                label = { Text(tag.name) },
            )
        }
    }
}

/**
 * A single station row: favicon (radio-icon fallback), name + meta, and a favorite toggle.
 * Tapping plays the station; a long-press surfaces its details via [onLongPress].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationRow(
    station: Station,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val radioFallback = rememberVectorPainter(Icons.Filled.Radio)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onPlay, onLongClick = onLongPress)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = station.favicon,
            contentDescription = null,
            error = radioFallback,
            fallback = radioFallback,
            placeholder = radioFallback,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            Text(
                text = stationMeta(station),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
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

private fun stationMeta(station: Station): String {
    val parts = buildList {
        station.country?.let { add(it) }
        station.codec?.takeIf { it.isNotBlank() }?.let { codec ->
            add(if (station.bitrate > 0) "$codec ${station.bitrate}kbps" else codec)
        }
    }
    return parts.joinToString(" • ").ifEmpty { "Station" }
}

/**
 * Bottom sheet with a station's details, shown on a long-press in the list. Offers Play,
 * favorite toggle, and a link to the station's homepage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationInfoSheet(
    station: Station,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val radioFallback = rememberVectorPainter(Icons.Filled.Radio)
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = station.favicon,
                    contentDescription = null,
                    error = radioFallback,
                    fallback = radioFallback,
                    placeholder = radioFallback,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val flag = isoFlag(station.countryCode)
                    station.country?.takeIf { it.isNotBlank() }?.let { country ->
                        Text(
                            text = if (flag.isEmpty()) country else "$flag  $country",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            station.codec?.takeIf { it.isNotBlank() }?.let { codec ->
                val quality = if (station.bitrate > 0) "$codec • ${station.bitrate} kbps" else codec
                InfoLine(label = "Quality", value = quality)
            }
            if (station.votes > 0) InfoLine(label = "Votes", value = station.votes.toString())
            if (station.tags.isNotEmpty()) {
                InfoLine(label = "Tags", value = station.tags.joinToString(", "))
            }

            station.homepage?.takeIf { it.isNotBlank() }?.let { url ->
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                        } catch (_: ActivityNotFoundException) {
                            // No browser available — ignore.
                        }
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Visit website")
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onPlay(); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Play")
            }
        }
    }
}

/** A label/value pair line used inside [StationInfoSheet]. */
@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
