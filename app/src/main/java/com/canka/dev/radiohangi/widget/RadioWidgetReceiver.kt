package com.canka.dev.radiohangi.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** Broadcast receiver that hosts the [RadioWidget] (entry point for the home screen). */
class RadioWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RadioWidget()
}
