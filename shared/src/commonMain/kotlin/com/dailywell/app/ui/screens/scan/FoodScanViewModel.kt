package com.dailywell.app.ui.screens.scan

import com.dailywell.app.data.model.ScannedFood
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for Food Scan ViewModel
 * Allows commonMain code to reference the ViewModel without Android dependencies
 */
interface FoodScanViewModel {
    val uiState: StateFlow<FoodScanUiState>

    fun onBarcodeDetected(barcode: String)
    fun switchToPhotoMode()
    fun switchToCameraMode()
    fun closeResults()
    fun dismissError()
    fun setCameraPermission(granted: Boolean)
    fun onPhotoCaptured(imageBase64: String)
    fun viewHistoricalScan(food: ScannedFood)
}
