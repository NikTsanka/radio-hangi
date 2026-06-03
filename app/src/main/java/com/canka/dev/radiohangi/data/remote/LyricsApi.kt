package com.canka.dev.radiohangi.data.remote

import com.canka.dev.radiohangi.data.remote.dto.LyricsResponse
import retrofit2.http.GET
import retrofit2.http.Path

/** lyrics.ovh — best-effort lyrics for the current song on Screen A. */
interface LyricsApi {
    @GET("{artist}/{song}")
    suspend fun getLyrics(
        @Path("artist") artist: String,
        @Path("song") song: String,
    ): LyricsResponse
}
