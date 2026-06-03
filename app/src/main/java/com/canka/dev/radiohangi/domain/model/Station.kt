package com.canka.dev.radiohangi.domain.model

import kotlinx.serialization.Serializable

/**
 * A World Radio station from the Radio Browser API.
 *
 * This is the *domain* model (already normalized: tags as a list, a single resolved
 * stream URL). The raw network DTO + mapping live in the data layer (Milestone 5).
 *
 * Serializable because favorites are persisted as JSON in DataStore
 * (mirrors the web localStorage keys `wr-favs` / `wr-favs-data`).
 *
 * NOTE: Radio Browser stations have NO now-playing / album art / lyrics — only [favicon].
 */
@Serializable
data class Station(
    val uuid: String,
    val name: String,
    /** Playable stream URL (url_resolved, falling back to url). */
    val streamUrl: String,
    val favicon: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val tags: List<String> = emptyList(),
    val codec: String? = null,
    val bitrate: Int = 0,
    val votes: Int = 0,
    val homepage: String? = null,
)
