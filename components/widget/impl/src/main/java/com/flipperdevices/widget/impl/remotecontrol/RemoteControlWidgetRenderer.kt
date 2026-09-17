package com.flipperdevices.widget.impl.remotecontrol

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.flipperdevices.widget.impl.R

object RemoteControlWidgetRenderer {
    // Kept only for as long as the process lives: lets a periodic/reboot onUpdate()
    // keep showing the last frame instead of flashing back to a blank screen.
    private val lastBitmapByWidgetId = mutableMapOf<Int, Bitmap>()

    fun update(context: Context, widgetId: Int, bitmap: Bitmap?) {
        if (bitmap != null) {
            lastBitmapByWidgetId[widgetId] = bitmap
        }
        val resolvedBitmap = bitmap
            ?: lastBitmapByWidgetId[widgetId]
            ?: RemoteControlFrameDecoder.emptyBitmap()

        val remoteViews = RemoteViews(context.packageName, R.layout.widget_layout_remote_control)
        remoteViews.setImageViewBitmap(R.id.remote_screen, resolvedBitmap)
        RemoteControlDirection.entries.forEach { direction ->
            remoteViews.setOnClickPendingIntent(
                direction.viewId,
                RemoteControlWidgetReceiver.buildPendingIntent(context, widgetId, direction)
            )
        }
        AppWidgetManager.getInstance(context).updateAppWidget(widgetId, remoteViews)
    }

    fun onWidgetsRemoved(widgetIds: IntArray) {
        widgetIds.forEach { lastBitmapByWidgetId.remove(it) }
    }
}
