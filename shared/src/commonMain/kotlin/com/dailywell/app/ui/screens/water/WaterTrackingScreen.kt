package com.dailywell.app.ui.screens.water

import androidx.compose.runtime.Composable

/**
 * Water Tracking Screen - expect declaration for commonMain
 * The actual implementation is in androidMain
 */
@Composable
expect fun WaterTrackingScreen(
    viewModel: WaterTrackingViewModel,
    onNavigateBack: () -> Unit
)
