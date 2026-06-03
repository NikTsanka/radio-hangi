package com.canka.dev.radiohangi.data

import android.content.Context
import com.canka.dev.radiohangi.data.remote.DeezerApi
import com.canka.dev.radiohangi.data.remote.LyricsApi
import com.canka.dev.radiohangi.data.remote.RadioBrowserApi
import com.canka.dev.radiohangi.data.remote.ZenoMetadataSource
import com.canka.dev.radiohangi.data.repository.FavoritesRepository
import com.canka.dev.radiohangi.data.repository.RadioRepository
import com.canka.dev.radiohangi.data.repository.RecentsRepository
import com.canka.dev.radiohangi.data.repository.WorldRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Lightweight manual dependency container (no DI framework). Holds the shared HTTP/JSON
 * stack and the application-scoped repositories. Created once in
 * [com.canka.dev.radiohangi.RadioHangiApplication].
 */
class AppContainer(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true // APIs return many fields we don't model
        isLenient = true
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        // Radio Browser asks clients to send a descriptive User-Agent.
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "RadioHangi/1.0 (Android; canka.dev)")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
        )
        .build()

    private val jsonConverter = json.asConverterFactory("application/json".toMediaType())

    private fun <T> retrofit(baseUrl: String, service: Class<T>): T = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(jsonConverter)
        .build()
        .create(service)

    private val deezerApi = retrofit("https://api.deezer.com/", DeezerApi::class.java)
    private val lyricsApi = retrofit(AppConfig.LYRICS_BASE_URL, LyricsApi::class.java)
    private val radioBrowserApi = retrofit(AppConfig.RADIO_BROWSER_BASE_URL, RadioBrowserApi::class.java)

    private val zenoMetadataSource = ZenoMetadataSource(okHttpClient, json)

    // Screen A
    val radioRepository: RadioRepository =
        RadioRepository(zenoMetadataSource, deezerApi, lyricsApi)

    // Screen B + favorites
    val worldRepository: WorldRepository = WorldRepository(radioBrowserApi)
    val favoritesRepository: FavoritesRepository = FavoritesRepository(context.applicationContext, json)
    val recentsRepository: RecentsRepository = RecentsRepository(context.applicationContext, json)
}
