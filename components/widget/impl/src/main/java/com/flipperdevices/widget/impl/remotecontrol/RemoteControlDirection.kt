package com.flipperdevices.widget.impl.remotecontrol

import com.flipperdevices.protobuf.screen.InputKey
import com.flipperdevices.widget.impl.R

enum class RemoteControlDirection(val inputKey: InputKey, val viewId: Int) {
    UP(InputKey.UP, R.id.remote_btn_up),
    DOWN(InputKey.DOWN, R.id.remote_btn_down),
    LEFT(InputKey.LEFT, R.id.remote_btn_left),
    RIGHT(InputKey.RIGHT, R.id.remote_btn_right),
    OK(InputKey.OK, R.id.remote_btn_ok),
    BACK(InputKey.BACK, R.id.remote_btn_back)
}
