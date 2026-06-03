package com.canka.dev.radiohangi.player

import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.compose.ui.platform.LocalContext

/**
 * Connects the Compose UI to [PlaybackService] and returns the live [MediaController] as
 * Compose [State] (null until the async connection completes, and again after disposal).
 *
 * The controller is the UI's remote handle to the player: play/pause, volume, current
 * metadata, etc. Building it starts/binds the session service as needed.
 */
@Composable
fun rememberManagedMediaController(): State<MediaController?> {
    val context = LocalContext.current
    val controllerState = remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(context) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                // get() is safe here — the future has completed. Guard against connection failure.
                controllerState.value = runCatching { future.get() }.getOrNull()
            },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            MediaController.releaseFuture(future)
            controllerState.value = null
        }
    }

    return controllerState
}
