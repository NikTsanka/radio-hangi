package com.canka.dev.radiohangi.data.remote

import com.canka.dev.radiohangi.data.remote.dto.DeezerSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/** Deezer search — used only to resolve album art for Screen A (Main Radio). */
interface DeezerApi {
    /** Plain JSON GET (no jsonp/callback). [query] is "artist song". */
    @GET("search")
    suspend fun search(@Query("q") query: String): DeezerSearchResponse
}
