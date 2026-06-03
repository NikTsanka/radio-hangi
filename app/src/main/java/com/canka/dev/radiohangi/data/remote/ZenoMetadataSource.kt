package com.canka.dev.radiohangi.data.remote

import com.canka.dev.radiohangi.data.AppConfig
import com.canka.dev.radiohangi.data.remote.dto.ZenoMetadataDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException

/**
 * Android replacement for the web version's browser `EventSource`: subscribes to the Zeno
 * now-playing SSE endpoint via OkHttp and emits each event's raw `streamTitle`.
 *
 * Emitted as a cold [Flow]; the caller is responsible for reconnect (retryWhen) and parsing.
 * The event source is cancelled automatically when the flow collector goes away.
 */
class ZenoMetadataSource(
    private val client: OkHttpClient,
    private val json: Json,
) {
    fun streamTitles(): Flow<String> = callbackFlow {
        val request = Request.Builder()
            .url(AppConfig.ZENO_METADATA_SSE_URL)
            .header("Accept", "text/event-stream")
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String,
            ) {
                val title = runCatching {
                    json.decodeFromString<ZenoMetadataDto>(data).streamTitle
                }.getOrNull()
                if (!title.isNullOrBlank()) trySend(title)
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?,
            ) {
                // Surface the failure so the upstream Flow can reconnect after a delay.
                close(t ?: IOException("SSE connection failed (HTTP ${response?.code})"))
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = EventSources.createFactory(client).newEventSource(request, listener)
        awaitClose { eventSource.cancel() }
    }
}
