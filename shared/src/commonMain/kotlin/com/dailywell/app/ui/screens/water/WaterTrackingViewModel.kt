package com.dailywell.app.ui.screens.water

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for Water Tracking ViewModel
 * Allows commonMain code to reference the ViewModel without Android dependencies
 */
interface WaterTrackingViewModel {
    val uiState: StateFlow<WaterTrackingUiState>

    fun logGlass(size: GlassSize)
    fun logCustomAmount(amountMl: Int)
    fun quickAdd()
    fun removeEntry(entryId: String)
    fun selectSource(source: WaterSource)
    fun updateCustomAmount(amount: Int?)
    fun updateDailyGoal(goalMl: Int)
    fun updateSettings(settings: WaterSettings)
    fun setShowAddDialog(show: Boolean)
    fun setShowSettingsSheet(show: Boolean)
}
