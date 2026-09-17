package com.flipperdevices.widget.impl.remotecontrol

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class RemoteControlWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            RemoteControlWidgetRenderer.update(context, widgetId, bitmap = null)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        RemoteControlWidgetRenderer.onWidgetsRemoved(appWidgetIds)
    }
}
