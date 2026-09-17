package com.flipperdevices.widget.impl.remotecontrol

import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flipperdevices.bridge.connection.feature.provider.api.FFeatureProvider
import com.flipperdevices.bridge.connection.feature.provider.api.getSync
import com.flipperdevices.bridge.connection.feature.screenstreaming.api.FScreenStreamingFeatureApi
import com.flipperdevices.bridge.connection.orchestrator.api.FDeviceOrchestrator
import com.flipperdevices.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import com.flipperdevices.bridge.connection.service.api.FConnectionService
import com.flipperdevices.core.di.ComponentHolder
import com.flipperdevices.core.ktx.android.getBluetoothAdapter
import com.flipperdevices.core.log.LogTagProvider
import com.flipperdevices.core.log.error
import com.flipperdevices.core.log.info
import com.flipperdevices.protobuf.screen.InputType
import com.flipperdevices.widget.impl.di.WidgetComponent
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import dev.zacsweers.metro.Inject

private const val CONNECT_TIMEOUT_MS = 5 * 1000L
private const val FRAME_COLLECT_MS = 700L

class RemoteControlPressWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), LogTagProvider {
    override val TAG = "RemoteControlPressWorker"

    @Inject
    lateinit var fFeatureProvider: FFeatureProvider

    @Inject
    lateinit var fConnectionService: FConnectionService

    @Inject
    lateinit var fDeviceOrchestrator: FDeviceOrchestrator

    init {
        ComponentHolder.component<WidgetComponent>().inject(this)
    }

    override suspend fun doWork(): Result = coroutineScope {
        val widgetId = inputData.getInt(EXTRA_WIDGET_ID, -1)
        val direction = inputData.getString(EXTRA_DIRECTION)
            ?.let(RemoteControlDirection::valueOf)
        if (widgetId < 0 || direction == null) {
            error { "#doWork missing widgetId/direction" }
            return@coroutineScope Result.failure()
        }

        if (!isBluetoothEnabled()) {
            info { "#doWork bluetooth is disabled" }
            RemoteControlWidgetRenderer.update(applicationContext, widgetId, bitmap = null)
            return@coroutineScope Result.failure()
        }

        try {
            withTimeout(CONNECT_TIMEOUT_MS) {
                fConnectionService.connectIfNotForceDisconnect()
                fDeviceOrchestrator.getState()
                    .filterIsInstance<FDeviceConnectStatus.Connected>()
                    .first()
            }
        } catch (timeout: TimeoutCancellationException) {
            error(timeout) { "#doWork could not connect within $CONNECT_TIMEOUT_MS ms" }
            RemoteControlWidgetRenderer.update(applicationContext, widgetId, bitmap = null)
            return@coroutineScope Result.failure()
        }

        val screenApi = fFeatureProvider.getSync<FScreenStreamingFeatureApi>()
        if (screenApi == null) {
            error { "#doWork FScreenStreamingFeatureApi not found" }
            RemoteControlWidgetRenderer.update(applicationContext, widgetId, bitmap = null)
            return@coroutineScope Result.failure()
        }

        var latestBitmap: Bitmap? = null
        val collectJob = launch {
            screenApi.guiScreenFrameFlow().collect { frame ->
                RemoteControlFrameDecoder.decode(frame)?.let { latestBitmap = it }
            }
        }

        screenApi.sendInputAndForget(direction.inputKey, InputType.PRESS)
        screenApi.sendInputAndForget(direction.inputKey, InputType.SHORT)

        delay(FRAME_COLLECT_MS)
        collectJob.cancelAndJoin()
        screenApi.stop()

        RemoteControlWidgetRenderer.update(applicationContext, widgetId, latestBitmap)
        Result.success()
    }

    private fun isBluetoothEnabled(): Boolean {
        val bluetoothManager = ContextCompat.getSystemService(
            applicationContext,
            BluetoothManager::class.java
        )
        return bluetoothManager?.getBluetoothAdapter()?.isEnabled ?: false
    }
}
