package com.flipperdevices.screenstreaming.impl.viewmodel.repository

import com.flipperdevices.bridge.connection.feature.provider.api.FFeatureProvider
import com.flipperdevices.bridge.connection.feature.provider.api.getSync
import com.flipperdevices.bridge.connection.feature.screenstreaming.api.FScreenStreamingFeatureApi
import com.flipperdevices.core.ktx.jre.FlipperDispatchers
import com.flipperdevices.core.log.LogTagProvider
import com.flipperdevices.core.log.error
import com.flipperdevices.protobuf.screen.InputKey
import com.flipperdevices.protobuf.screen.InputType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import dev.zacsweers.metro.Inject

class FlipperButtonRepository @Inject constructor(
    private val fFeatureProvider: FFeatureProvider
) : LogTagProvider {
    override val TAG: String = "FlipperButtonRequest"

    // Only guards the order in which PRESS/SHORT/LONG commands are sent on the
    // wire. Waiting for the device's RELEASE ack (below) must NOT be covered by
    // this lock, otherwise a queue of rapid taps serializes on a full BLE round
    // trip per tap instead of just on the (near-instant) local send.
    private val sendMutex = Mutex()

    fun pressOnButton(
        viewModelScope: CoroutineScope,
        key: InputKey,
        type: InputType,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch(FlipperDispatchers.workStealingDispatcher) {
            val fScreenStreamingFeatureApi = fFeatureProvider.getSync<FScreenStreamingFeatureApi>()
            if (fScreenStreamingFeatureApi == null) {
                error { "#pressOnButton FScreenStreamingFeatureApi not found!" }
                return@launch
            }

            sendMutex.withLock {
                fScreenStreamingFeatureApi.sendInputAndForget(key, InputType.PRESS)
                fScreenStreamingFeatureApi.sendInputAndForget(key, type)
            }

            fScreenStreamingFeatureApi.awaitInput(key, InputType.RELEASE)
                .onEach { result ->
                    result.onFailure { error(it) { "#pressOnButton InputType.RELEASE failed" } }
                }
                .onEach { onComplete.invoke() }
                .collect()
        }
    }
}
