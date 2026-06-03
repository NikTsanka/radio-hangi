package com.canka.dev.radiohangi.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---- Zeno SSE now-playing metadata ----
/** Each SSE message's JSON payload, e.g. {"streamTitle":"Artist - Title"}. */
@Serializable
data class ZenoMetadataDto(
    val streamTitle: String? = null,
)

// ---- Deezer search (album art) ----
/** GET https://api.deezer.com/search?q=... — cover read from data[0].album.cover_big. */
@Serializable
data class DeezerSearchResponse(
    val data: List<DeezerTrackDto> = emptyList(),
)

@Serializable
data class DeezerTrackDto(
    val album: DeezerAlbumDto? = null,
)

@Serializable
data class DeezerAlbumDto(
    @SerialName("cover_big") val coverBig: String? = null,
)

// ---- lyrics.ovh ----
/** GET https://api.lyrics.ovh/v1/{artist}/{song} — JSON { "lyrics": "..." }. */
@Serializable
data class LyricsResponse(
    val lyrics: String? = null,
)
