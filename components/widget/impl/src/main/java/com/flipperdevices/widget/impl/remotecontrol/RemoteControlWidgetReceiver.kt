package com.flipperdevices.widget.impl.remotecontrol

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.flipperdevices.core.log.LogTagProvider
import com.flipperdevices.core.log.info

private const val DEFAULT_WIDGET_APP_ID = -1

class RemoteControlWidgetReceiver : BroadcastReceiver(), LogTagProvider {
    override val TAG = "RemoteControlWidgetReceiver"

    override fun onReceive(context: Context, intent: Intent?) {
        val direction = intent?.getStringExtra(EXTRA_DIRECTION) ?: return
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, DEFAULT_WIDGET_APP_ID)
        if (widgetId == DEFAULT_WIDGET_APP_ID) return
        info { "#onReceive press $direction for widget $widgetId" }

        val request = OneTimeWorkRequestBuilder<RemoteControlPressWorker>()
            .setInputData(
                Data.Builder()
                    .putInt(EXTRA_WIDGET_ID, widgetId)
                    .putString(EXTRA_DIRECTION, direction)
                    .build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "remote_control_press_$widgetId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        fun buildPendingIntent(
            context: Context,
            widgetId: Int,
            direction: RemoteControlDirection
        ): PendingIntent {
            val intent = Intent(context, RemoteControlWidgetReceiver::class.java).apply {
                action = ACTION_PRESS_PREFIX + direction.name
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(EXTRA_DIRECTION, direction.name)
            }
            val requestCode = widgetId * RemoteControlDirection.entries.size + direction.ordinal
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(context, requestCode, intent, flags)
        }
    }
}
