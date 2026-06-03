package com.canka.dev.radiohangi.domain.model

/**
 * Sort options for the World Radio station search.
 * [apiValue] maps to the Radio Browser `order` query param (always with reverse=true).
 */
enum class SortOrder(val apiValue: String, val label: String) {
    Votes("votes", "Most voted"),
    Clicks("clickcount", "Most played"),
    Bitrate("bitrate", "Bitrate"),
    Name("name", "Name"),
}
