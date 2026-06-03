package com.canka.dev.radiohangi.widget

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.canka.dev.radiohangi.player.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Play/Pause handler for the widget button. Connects a short-lived [MediaController] to the
 * same [PlaybackService] session, toggles playback, then releases the controller.
 */
class ToggleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        // MediaController must be built/used on the main thread.
        withContext(Dispatchers.Main) {
            val controller = MediaController.Builder(context, token).buildAsync().await(context)
            if (controller.isPlaying) {
                controller.pause()
            } else {
                if (controller.playbackState == Player.STATE_IDLE) controller.prepare()
                controller.play()
            }
            controller.release()
        }
    }
}

/** Suspends until the MediaController future completes (resolved on the main executor). */
internal suspend fun com.google.common.util.concurrent.ListenableFuture<MediaController>.await(
    context: Context,
): MediaController = suspendCancellableCoroutine { cont ->
    addListener({ cont.resume(get()) }, ContextCompat.getMainExecutor(context))
}
