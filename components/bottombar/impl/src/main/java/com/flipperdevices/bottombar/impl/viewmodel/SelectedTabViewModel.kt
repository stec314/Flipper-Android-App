package com.flipperdevices.bottombar.impl.viewmodel

import androidx.datastore.core.DataStore
import com.flipperdevices.bottombar.impl.model.BottomBarTabConfig
import com.flipperdevices.bottombar.impl.model.BottomBarTabConfig.RemoteControl
import com.flipperdevices.core.preference.pb.Settings
import com.flipperdevices.core.ui.lifecycle.DecomposeViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import dev.zacsweers.metro.Inject

/**
 * The app always opens on the Remote Control tab.
 */
class SelectedTabViewModel @Inject constructor(
    private val settingsDataStore: DataStore<Settings>,
) : DecomposeViewModel() {

    private fun setRemoteFeaturePromoted() {
        viewModelScope.launch {
            settingsDataStore.updateData { it.copy(infrared_remotes_tab_shown = true) }
        }
    }

    fun getSelectedTab(): BottomBarTabConfig {
        val settings = runBlocking { settingsDataStore.data.first() }
        if (!settings.infrared_remotes_tab_shown) {
            setRemoteFeaturePromoted()
        }
        return RemoteControl
    }
}
