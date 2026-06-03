package com.canka.dev.radiohangi.widget

import android.content.ComponentName
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.canka.dev.radiohangi.RadioHangiApplication
import com.canka.dev.radiohangi.domain.model.Station
import com.canka.dev.radiohangi.player.PlaybackService
import com.canka.dev.radiohangi.player.buildStationMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Plays a specific World station tapped in the widget's quick-play row. The station's fields are
 * passed as [ActionParameters]; we reconstruct a [Station], play it through the shared
 * [PlaybackService] session via a short-lived [MediaController], and record it as recently played.
 */
class PlayStationAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val url = parameters[URL]?.takeIf { it.isNotBlank() } ?: return
        val station = Station(
            uuid = parameters[UUID] ?: return,
            name = parameters[NAME] ?: "",
            streamUrl = url,
            favicon = parameters[FAVICON]?.takeIf { it.isNotBlank() },
            country = parameters[COUNTRY]?.takeIf { it.isNotBlank() },
        )

        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        // MediaController must be built/used on the main thread.
        withContext(Dispatchers.Main) {
            val controller = MediaController.Builder(context, token).buildAsync().await(context)
            controller.setMediaItem(buildStationMediaItem(station))
            controller.prepare()
            controller.play()
            controller.release()
        }

        (context.applicationContext as RadioHangiApplication).container.recentsRepository.add(station)
    }

    companion object {
        val UUID = ActionParameters.Key<String>("station_uuid")
        val NAME = ActionParameters.Key<String>("station_name")
        val URL = ActionParameters.Key<String>("station_url")
        val FAVICON = ActionParameters.Key<String>("station_favicon")
        val COUNTRY = ActionParameters.Key<String>("station_country")
    }
}
