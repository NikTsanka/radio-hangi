package com.canka.dev.radiohangi.data.repository

import com.canka.dev.radiohangi.data.AppConfig
import com.canka.dev.radiohangi.data.remote.RadioBrowserApi
import com.canka.dev.radiohangi.data.remote.dto.RadioBrowserStationDto
import com.canka.dev.radiohangi.domain.model.Country
import com.canka.dev.radiohangi.domain.model.SortOrder
import com.canka.dev.radiohangi.domain.model.Station
import com.canka.dev.radiohangi.domain.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * World Radio data access (Radio Browser). Maps network DTOs to domain models and applies
 * the same filters as the web version (e.g. countries with ≥5 stations). All calls run on IO.
 */
class WorldRepository(private val api: RadioBrowserApi) {

    suspend fun countries(): List<Country> = withContext(Dispatchers.IO) {
        api.countries()
            .filter { it.code.isNotBlank() && it.stationcount >= AppConfig.MIN_STATIONS_PER_COUNTRY }
            .map { Country(name = it.name, code = it.code, stationCount = it.stationcount) }
    }

    suspend fun tags(): List<Tag> = withContext(Dispatchers.IO) {
        api.tags(AppConfig.TAGS_LIMIT)
            .filter { it.name.isNotBlank() }
            .map { Tag(name = it.name, stationCount = it.stationcount) }
    }

    suspend fun searchStations(
        name: String,
        countryCode: String?,
        tags: List<String>,
        sort: SortOrder,
        offset: Int,
    ): List<Station> = withContext(Dispatchers.IO) {
        api.searchStations(
            order = sort.apiValue,
            limit = AppConfig.STATIONS_PAGE_SIZE,
            offset = offset,
            name = name.ifBlank { null },
            countryCode = countryCode,
            tagList = tags.takeIf { it.isNotEmpty() }?.joinToString(","),
        ).map(::toStation)
    }

    /** Fire-and-forget play-click registration; never throws. */
    suspend fun registerClick(uuid: String) {
        withContext(Dispatchers.IO) { runCatching { api.registerClick(uuid) } }
    }

    /** Votes ("likes") for a station. Returns true when the vote was accepted. Never throws. */
    suspend fun voteStation(uuid: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { api.vote(uuid).ok }.getOrDefault(false)
    }

    private fun toStation(dto: RadioBrowserStationDto): Station = Station(
        uuid = dto.stationuuid,
        name = dto.name.ifBlank { "Unknown station" },
        streamUrl = dto.urlResolved.ifBlank { dto.url },
        favicon = dto.favicon.ifBlank { null },
        country = dto.country.ifBlank { null },
        countryCode = dto.countrycode.ifBlank { null },
        tags = dto.tags.split(",").map(String::trim).filter(String::isNotEmpty),
        codec = dto.codec.ifBlank { null },
        bitrate = dto.bitrate,
        votes = dto.votes,
        homepage = dto.homepage.ifBlank { null },
    )
}
