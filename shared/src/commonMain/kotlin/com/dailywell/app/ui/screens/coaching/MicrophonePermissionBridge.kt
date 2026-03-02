package com.dailywell.app.ui.screens.coaching

import androidx.compose.runtime.Composable

data class MicrophonePermissionRequester(
    val isGranted: () -> Boolean,
    val requestPermission: () -> Unit
)

@Composable
expect fun rememberMicrophonePermissionRequester(
    onPermissionResult: (Boolean) -> Unit
): MicrophonePermissionRequester
