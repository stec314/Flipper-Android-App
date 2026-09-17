package com.flipperdevices.bottombar.impl.model

import com.flipperdevices.core.preference.pb.SelectedTab
import com.flipperdevices.deeplink.model.DeeplinkBottomBarTab
import kotlinx.serialization.Serializable

@Serializable
enum class BottomBarTabEnum(val protobufRepresentation: SelectedTab) {
    DEVICE(SelectedTab.DEVICE),
    ARCHIVE(SelectedTab.ARCHIVE),
    APPS(SelectedTab.APPS),
    TOOLS(SelectedTab.TOOLS),
    FILE_MANAGER(SelectedTab.FILE_MANAGER)
}

fun DeeplinkBottomBarTab.toBottomBarTabEnum(): BottomBarTabEnum {
    return when (this) {
        DeeplinkBottomBarTab.DEVICE -> BottomBarTabEnum.DEVICE
        DeeplinkBottomBarTab.ARCHIVE -> BottomBarTabEnum.ARCHIVE
        DeeplinkBottomBarTab.APPS -> BottomBarTabEnum.APPS
        DeeplinkBottomBarTab.TOOLS -> BottomBarTabEnum.TOOLS
        DeeplinkBottomBarTab.FILE_MANAGER -> BottomBarTabEnum.FILE_MANAGER
    }
}
