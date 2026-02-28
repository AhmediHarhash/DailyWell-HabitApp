package com.dailywell.app.ui.screens.scan

import androidx.compose.runtime.Composable

/**
 * Food Scan Screen - expect declaration for commonMain
 * The actual implementation is in androidMain with CameraX and ML Kit
 */
@Composable
expect fun FoodScanScreen(
    viewModel: FoodScanViewModel,
    onNavigateBack: () -> Unit
)
