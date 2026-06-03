package com.canka.dev.radiohangi.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.canka.dev.radiohangi.R
import com.canka.dev.radiohangi.data.AppConfig
import com.canka.dev.radiohangi.domain.model.Station
import kotlinx.serialization.decodeFromString

/**
 * Home-screen widget (Jetpack Glance — no RemoteViews/XML). Shows the current album art,
 * song title/artist and a Play/Pause button bound to the same Media3 session via
 * [ToggleAction], plus a quick-play row of favorite/recent stations ([PlayStationAction]).
 * Data is read from Glance state, kept fresh by the playback service.
 */
class RadioWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Read the snapshot + load artwork BEFORE composing (Glance composition can't suspend).
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val title = prefs[WidgetKeys.TITLE] ?: "Radio Hangi"
        val artist = prefs[WidgetKeys.ARTIST] ?: "Tap to play"
        val isPlaying = prefs[WidgetKeys.IS_PLAYING] == true
        val art = loadArtBitmap(context, prefs[WidgetKeys.ART_URI])

        val quickStations = prefs[WidgetKeys.QUICK_STATIONS]
            ?.let { runCatching { widgetJson.decodeFromString<List<Station>>(it) }.getOrDefault(emptyList()) }
            ?.take(AppConfig.WIDGET_QUICK_SIZE)
            .orEmpty()
        val quickArt = quickStations.map { loadArtBitmap(context, it.favicon) }

        provideContent {
            WidgetContent(title, artist, isPlaying, art, quickStations, quickArt)
        }
    }

    private suspend fun loadArtBitmap(context: Context, data: String?): Bitmap? {
        if (data.isNullOrBlank()) return null
        val request = ImageRequest.Builder(context).data(data).build()
        val result = SingletonImageLoader.get(context).execute(request)
        return (result as? SuccessResult)?.image?.toBitmap()
    }
}

@Composable
private fun WidgetContent(
    title: String,
    artist: String,
    isPlaying: Boolean,
    art: Bitmap?,
    quickStations: List<Station>,
    quickArt: List<Bitmap?>,
) {
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(12.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = if (art != null) ImageProvider(art) else ImageProvider(R.drawable.cover),
                    contentDescription = null,
                    modifier = GlanceModifier.size(56.dp).cornerRadius(8.dp),
                )
                Spacer(GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = title,
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        text = artist,
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                    )
                }
                Spacer(GlanceModifier.width(8.dp))
                CircleIconButton(
                    imageProvider = ImageProvider(
                        if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    onClick = actionRunCallback<ToggleAction>(),
                )
            }

            if (quickStations.isNotEmpty()) {
                Spacer(GlanceModifier.height(10.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    quickStations.forEachIndexed { i, station ->
                        Box(
                            modifier = GlanceModifier
                                .size(40.dp)
                                .cornerRadius(8.dp)
                                .clickable(
                                    actionRunCallback<PlayStationAction>(
                                        actionParametersOf(
                                            PlayStationAction.UUID to station.uuid,
                                            PlayStationAction.NAME to station.name,
                                            PlayStationAction.URL to station.streamUrl,
                                            PlayStationAction.FAVICON to station.favicon.orEmpty(),
                                            PlayStationAction.COUNTRY to station.country.orEmpty(),
                                        ),
                                    ),
                                ),
                        ) {
                            Image(
                                provider = quickArt.getOrNull(i)?.let { ImageProvider(it) }
                                    ?: ImageProvider(R.drawable.cover),
                                contentDescription = station.name,
                                modifier = GlanceModifier.size(40.dp).cornerRadius(8.dp),
                            )
                        }
                        if (i < quickStations.lastIndex) Spacer(GlanceModifier.width(8.dp))
                    }
                }
            }
        }
    }
}
