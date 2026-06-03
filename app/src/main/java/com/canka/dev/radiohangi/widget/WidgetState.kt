package com.canka.dev.radiohangi.widget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.canka.dev.radiohangi.domain.model.Station
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Preference keys for the data the [RadioWidget] renders (held in Glance widget state). */
object WidgetKeys {
    val TITLE = stringPreferencesKey("widget_title")
    val ARTIST = stringPreferencesKey("widget_artist")
    val ART_URI = stringPreferencesKey("widget_art_uri")
    val IS_PLAYING = booleanPreferencesKey("widget_is_playing")

    /** JSON-encoded list of favorite/recent stations for the widget's quick-play row. */
    val QUICK_STATIONS = stringPreferencesKey("widget_quick_stations")
}

internal val widgetJson = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Pushes the current now-playing snapshot to every placed [RadioWidget] instance and
 * triggers a redraw. Called by the playback service whenever the player's metadata or
 * play state changes, so the widget stays in sync with the session.
 */
suspend fun updateRadioWidget(
    context: Context,
    title: String,
    artist: String,
    artUri: String?,
    isPlaying: Boolean,
) {
    val ids = GlanceAppWidgetManager(context).getGlanceIds(RadioWidget::class.java)
    ids.forEach { id ->
        updateAppWidgetState(context, id) { prefs ->
            prefs[WidgetKeys.TITLE] = title
            prefs[WidgetKeys.ARTIST] = artist
            if (artUri != null) prefs[WidgetKeys.ART_URI] = artUri else prefs.remove(WidgetKeys.ART_URI)
            prefs[WidgetKeys.IS_PLAYING] = isPlaying
        }
    }
    RadioWidget().updateAll(context)
}

/** Pushes the favorite/recent quick-play stations to every placed widget and redraws. */
suspend fun updateWidgetQuickStations(context: Context, stations: List<Station>) {
    val encoded = widgetJson.encodeToString(stations)
    val ids = GlanceAppWidgetManager(context).getGlanceIds(RadioWidget::class.java)
    ids.forEach { id ->
        updateAppWidgetState(context, id) { prefs -> prefs[WidgetKeys.QUICK_STATIONS] = encoded }
    }
    RadioWidget().updateAll(context)
}
