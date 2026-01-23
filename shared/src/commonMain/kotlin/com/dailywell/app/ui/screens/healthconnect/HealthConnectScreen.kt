package com.dailywell.app.ui.screens.healthconnect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.Success

/**
 * Health Connect setup and status screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectScreen(
    connectionState: HealthConnectUiState,
    onRequestPermissions: () -> Unit,
    onOpenHealthConnect: () -> Unit,
    onInstallHealthConnect: () -> Unit,
    onSyncNow: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Connect") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "<-", fontSize = 20.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Health Connect Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Success.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "❤️", fontSize = 40.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Auto-Track Your Habits",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Connect with Health Connect to automatically track your sleep, exercise, and water intake.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (connectionState.status) {
                        ConnectionStatus.CONNECTED -> Success.copy(alpha = 0.1f)
                        ConnectionStatus.PERMISSIONS_REQUIRED -> Color(0xFFFFF3E0)
                        ConnectionStatus.NOT_INSTALLED -> Color(0xFFFFEBEE)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (connectionState.status) {
                            ConnectionStatus.CONNECTED -> "✓"
                            ConnectionStatus.PERMISSIONS_REQUIRED -> "⚠️"
                            ConnectionStatus.NOT_INSTALLED -> "❌"
                            else -> "⏳"
                        },
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = when (connectionState.status) {
                                ConnectionStatus.CONNECTED -> "Connected"
                                ConnectionStatus.PERMISSIONS_REQUIRED -> "Permissions Needed"
                                ConnectionStatus.NOT_INSTALLED -> "Not Installed"
                                ConnectionStatus.NOT_SUPPORTED -> "Not Supported"
                                else -> "Checking..."
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when (connectionState.status) {
                                ConnectionStatus.CONNECTED -> "Your health data syncs automatically"
                                ConnectionStatus.PERMISSIONS_REQUIRED -> "Grant access to sync health data"
                                ConnectionStatus.NOT_INSTALLED -> "Install Health Connect to continue"
                                ConnectionStatus.NOT_SUPPORTED -> "Your device doesn't support Health Connect"
                                else -> "Please wait..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button based on status
            when (connectionState.status) {
                ConnectionStatus.NOT_INSTALLED -> {
                    Button(
                        onClick = onInstallHealthConnect,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Success
                        )
                    ) {
                        Text("Install Health Connect")
                    }
                }
                ConnectionStatus.PERMISSIONS_REQUIRED -> {
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Success
                        )
                    ) {
                        Text("Grant Permissions")
                    }
                }
                ConnectionStatus.CONNECTED -> {
                    Button(
                        onClick = onSyncNow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Success
                        ),
                        enabled = !connectionState.isSyncing
                    ) {
                        if (connectionState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing...")
                        } else {
                            Text("Sync Now")
                        }
                    }
                }
                else -> {
                    // Loading or Not Supported - no action button
                }
            }

            if (connectionState.status == ConnectionStatus.CONNECTED) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenHealthConnect,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage in Health Connect")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Features list
            Text(
                text = "WHAT SYNCS AUTOMATICALLY",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            HealthFeatureItem(
                emoji = "😴",
                title = "Rest",
                description = "Auto-complete when you sleep 7+ hours",
                isEnabled = connectionState.status == ConnectionStatus.CONNECTED
            )

            HealthFeatureItem(
                emoji = "🏃",
                title = "Move",
                description = "Auto-complete when you exercise 30+ minutes",
                isEnabled = connectionState.status == ConnectionStatus.CONNECTED
            )

            HealthFeatureItem(
                emoji = "💧",
                title = "Hydrate",
                description = "Auto-complete when you drink 8+ glasses",
                isEnabled = connectionState.status == ConnectionStatus.CONNECTED
            )

            // Health Data Preview (if connected)
            if (connectionState.status == ConnectionStatus.CONNECTED && connectionState.healthData != null) {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "TODAY'S DATA",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        HealthDataRow("Sleep", connectionState.healthData.sleepDisplay)
                        HealthDataRow("Steps", "${connectionState.healthData.steps}")
                        HealthDataRow("Exercise", connectionState.healthData.exerciseDisplay)
                        HealthDataRow("Water", "${connectionState.healthData.waterGlasses} glasses")
                        if (connectionState.healthData.heartRate > 0) {
                            HealthDataRow("Avg Heart Rate", "${connectionState.healthData.heartRate.toInt()} bpm")
                        }
                    }
                }

                if (connectionState.lastSyncTime.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Last synced: ${connectionState.lastSyncTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthFeatureItem(
    emoji: String,
    title: String,
    description: String,
    isEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            modifier = Modifier.width(40.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = if (isEnabled) "✓" else "○",
            fontSize = 20.sp,
            color = if (isEnabled) Success else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HealthDataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * UI State for Health Connect screen
 */
data class HealthConnectUiState(
    val status: ConnectionStatus = ConnectionStatus.CHECKING,
    val isSyncing: Boolean = false,
    val healthData: HealthDataUi? = null,
    val lastSyncTime: String = "",
    val autoCompleteSuggestions: Map<String, Boolean> = emptyMap()
)

enum class ConnectionStatus {
    CHECKING,
    NOT_SUPPORTED,
    NOT_INSTALLED,
    PERMISSIONS_REQUIRED,
    CONNECTED
}

data class HealthDataUi(
    val sleepMinutes: Int = 0,
    val steps: Long = 0,
    val exerciseMinutes: Int = 0,
    val waterGlasses: Int = 0,
    val heartRate: Double = 0.0
) {
    val sleepDisplay: String get() {
        val hours = sleepMinutes / 60
        val mins = sleepMinutes % 60
        return if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
    }

    val exerciseDisplay: String get() {
        return if (exerciseMinutes >= 60) {
            val hours = exerciseMinutes / 60
            val mins = exerciseMinutes % 60
            if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
        } else {
            "${exerciseMinutes}m"
        }
    }
}
