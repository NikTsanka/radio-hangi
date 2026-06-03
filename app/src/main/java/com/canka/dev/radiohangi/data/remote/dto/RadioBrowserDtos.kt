package com.canka.dev.radiohangi.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Radio Browser /countries item. */
@Serializable
data class RadioBrowserCountryDto(
    val name: String = "",
    @SerialName("iso_3166_1") val code: String = "",
    val stationcount: Int = 0,
)

/** Radio Browser /tags item (genre). */
@Serializable
data class RadioBrowserTagDto(
    val name: String = "",
    val stationcount: Int = 0,
)

/** Radio Browser /stations/search item. */
@Serializable
data class RadioBrowserStationDto(
    val stationuuid: String = "",
    val name: String = "",
    val url: String = "",
    @SerialName("url_resolved") val urlResolved: String = "",
    val favicon: String = "",
    val country: String = "",
    val countrycode: String = "",
    /** Comma-separated genre tags. */
    val tags: String = "",
    val codec: String = "",
    val bitrate: Int = 0,
    val votes: Int = 0,
    val homepage: String = "",
)
