package com.canka.dev.radiohangi.data.remote

import com.canka.dev.radiohangi.data.remote.dto.RadioBrowserCountryDto
import com.canka.dev.radiohangi.data.remote.dto.RadioBrowserStationDto
import com.canka.dev.radiohangi.data.remote.dto.RadioBrowserTagDto
import com.canka.dev.radiohangi.data.remote.dto.RadioBrowserVoteDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Radio Browser API (Screen B — World Radio). Endpoints/params mirror the web module.
 * Base URL ends in `/json/`.
 */
interface RadioBrowserApi {

    @GET("countries?order=name&hidebroken=true")
    suspend fun countries(): List<RadioBrowserCountryDto>

    @GET("tags?order=stationcount&reverse=true&hidebroken=true")
    suspend fun tags(@Query("limit") limit: Int): List<RadioBrowserTagDto>

    @GET("stations/search")
    suspend fun searchStations(
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String,
        @Query("reverse") reverse: Boolean = true,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("name") name: String? = null,
        @Query("countrycode") countryCode: String? = null,
        @Query("tagList") tagList: String? = null,
    ): List<RadioBrowserStationDto>

    /** Registers a play click; fire-and-forget (errors ignored by the caller). */
    @GET("url/{stationuuid}")
    suspend fun registerClick(@Path("stationuuid") uuid: String): Response<ResponseBody>

    /** Casts a vote ("like") for a station. Radio Browser rate-limits repeat votes per IP. */
    @GET("vote/{stationuuid}")
    suspend fun vote(@Path("stationuuid") uuid: String): RadioBrowserVoteDto
}
