package com.dailywell.app.ui.screens.scan

import com.dailywell.app.data.model.ScannedFood

/**
 * UI state for the Food Scan screen
 * Shared data classes used by both commonMain navigation and androidMain implementation
 */
data class FoodScanUiState(
    val isLoading: Boolean = false,
    val scanMode: ScanMode = ScanMode.CAMERA,
    val scannedFood: ScannedFood? = null,
    val error: String? = null,
    val scanHistory: List<ScannedFood> = emptyList(),
    val showResults: Boolean = false,
    val lastBarcode: String? = null,
    val cameraPermissionGranted: Boolean = false
)

enum class ScanMode {
    CAMERA,    // Camera viewfinder for barcode scanning
    PHOTO,     // Take photo for AI analysis
    RESULTS    // Show scan results
}
