package com.canka.dev.radiohangi.player

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.canka.dev.radiohangi.data.AppConfig
import com.canka.dev.radiohangi.domain.model.Station

/**
 * Stable media IDs let the playback service tell *what* is playing. Only the main Zeno
 * stream gets live now-playing metadata; World stations keep their own (static) metadata.
 */
const val ZENO_MEDIA_ID = "zeno_main"

/** The fixed Main Radio (Zeno) stream. */
fun buildZenoMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(ZENO_MEDIA_ID)
    .setUri(AppConfig.ZENO_STREAM_URL)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle("Radio Hangi")
            .setArtist("Live")
            .setIsPlayable(true)
            .build(),
    )
    .build()

/** A World Radio station (no now-playing data — favicon used as artwork). */
fun buildStationMediaItem(station: Station): MediaItem = MediaItem.Builder()
    .setMediaId(station.uuid)
    .setUri(station.streamUrl)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(station.name)
            .setArtist(station.country ?: "World Radio")
            .setIsPlayable(true)
            .apply { station.favicon?.let { setArtworkUri(it.toUri()) } }
            .build(),
    )
    .build()
