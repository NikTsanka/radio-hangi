package com.canka.dev.radiohangi

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.canka.dev.radiohangi.data.AppContainer

/**
 * Application entry point. Owns the [AppContainer] (HTTP stack + repositories) and configures
 * Coil's singleton image loader to fetch over the shared OkHttp client.
 */
class RadioHangiApplication : Application(), SingletonImageLoader.Factory {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Subscribe to the Zeno now-playing SSE on launch (mirrors the web version).
        container.radioRepository.start()
    }

    /** Route Coil image loads through our OkHttp client; enable crossfade transitions. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { container.okHttpClient })) }
            .crossfade(true)
            .build()
}
