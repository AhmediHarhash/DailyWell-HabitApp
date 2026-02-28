package com.dailywell.app.ui.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.api.ClaudeApiClient
import com.dailywell.app.api.OpenFoodFactsClient
import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.dailywell.app.ui.screens.scan.FoodScanViewModel as FoodScanViewModelInterface

/**
 * Android implementation of FoodScanViewModel
 * Uses AndroidX ViewModel for lifecycle management
 */
class FoodScanViewModelImpl(
    private val openFoodFactsClient: OpenFoodFactsClient,
    private val claudeApiClient: ClaudeApiClient
) : ViewModel(), FoodScanViewModelInterface {

    private val _uiState = MutableStateFlow(FoodScanUiState())
    override val uiState: StateFlow<FoodScanUiState> = _uiState.asStateFlow()

    /**
     * Called when ML Kit detects a barcode
     */
    override fun onBarcodeDetected(barcode: String) {
        // Avoid duplicate scans
        if (barcode == _uiState.value.lastBarcode && _uiState.value.isLoading) {
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                lastBarcode = barcode,
                error = null
            )
        }

        viewModelScope.launch {
            val result = openFoodFactsClient.lookupBarcode(barcode)

            result.fold(
                onSuccess = { food ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            scannedFood = food,
                            scanMode = ScanMode.RESULTS,
                            showResults = true,
                            scanHistory = listOf(food) + it.scanHistory.take(19) // Keep last 20
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Product not found. Try scanning again or take a photo."
                        )
                    }
                }
            )
        }
    }

    /**
     * Switch to photo mode for AI analysis
     */
    override fun switchToPhotoMode() {
        _uiState.update {
            it.copy(scanMode = ScanMode.PHOTO)
        }
    }

    /**
     * Switch back to barcode camera mode
     */
    override fun switchToCameraMode() {
        _uiState.update {
            it.copy(
                scanMode = ScanMode.CAMERA,
                showResults = false,
                scannedFood = null,
                error = null
            )
        }
    }

    /**
     * Close results and go back to camera
     */
    override fun closeResults() {
        _uiState.update {
            it.copy(
                showResults = false,
                scannedFood = null,
                scanMode = ScanMode.CAMERA,
                lastBarcode = null,
                error = null
            )
        }
    }

    /**
     * Dismiss error message
     */
    override fun dismissError() {
        _uiState.update {
            it.copy(error = null)
        }
    }

    /**
     * Set camera permission status
     */
    override fun setCameraPermission(granted: Boolean) {
        _uiState.update {
            it.copy(cameraPermissionGranted = granted)
        }
    }

    /**
     * Called when photo is captured for AI analysis
     * Uses Claude Haiku 4.5 Vision (~$0.006 per image)
     */
    override fun onPhotoCaptured(imageBase64: String) {
        _uiState.update {
            it.copy(isLoading = true, error = null)
        }

        viewModelScope.launch {
            val result = claudeApiClient.analyzeFoodPhoto(imageBase64)

            result.fold(
                onSuccess = { analysis ->
                    // Convert photo analysis to ScannedFood model
                    val scannedFood = ScannedFood(
                        id = "photo_${System.currentTimeMillis()}",
                        barcode = "",
                        name = analysis.foodName,
                        brand = "",
                        imageUrl = "",
                        quantity = analysis.portionSize,
                        healthScore = analysis.healthScore,
                        healthGradeStr = analysis.healthGrade,
                        novaGroup = analysis.novaGroup,
                        nutrients = NutrientInfo(
                            calories = analysis.estimatedCalories,
                            fat = analysis.nutrients.fat,
                            saturatedFat = 0f,
                            carbohydrates = analysis.nutrients.carbs,
                            sugars = analysis.nutrients.sugar,
                            fiber = analysis.nutrients.fiber,
                            protein = analysis.nutrients.protein,
                            sodium = analysis.nutrients.sodium,
                            salt = 0f,
                            servingSize = analysis.portionSize
                        ),
                        ingredients = "",
                        additives = emptyList(),
                        alternatives = emptyList(),
                        friendlyMessage = analysis.friendlyTip,
                        tips = analysis.positiveAspects + analysis.considerations,
                        ecoScore = "",
                        scannedAt = System.currentTimeMillis(),
                        scanSource = "photo"
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            scannedFood = scannedFood,
                            scanMode = ScanMode.RESULTS,
                            showResults = true,
                            scanHistory = listOf(scannedFood) + it.scanHistory.take(19)
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Couldn't analyze photo. Try a clearer image or use barcode scanning."
                        )
                    }
                }
            )
        }
    }

    /**
     * View a historical scan
     */
    override fun viewHistoricalScan(food: ScannedFood) {
        _uiState.update {
            it.copy(
                scannedFood = food,
                scanMode = ScanMode.RESULTS,
                showResults = true
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        openFoodFactsClient.close()
    }
}
