package com.dailywell.app.ui.screens.biometric

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.dailywell.app.data.model.*
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.core.theme.PrimaryLight
import com.dailywell.app.core.theme.Secondary
import com.dailywell.app.core.theme.SecondaryLight
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricScreen(
    onBack: () -> Unit,
    isPremium: Boolean = true,
    viewModel: BiometricViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biometric Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "←", fontSize = 24.sp)
                    }
                },
                actions = {
                    if (uiState.connectedDevices.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.syncAllDevices() },
                            enabled = !uiState.isSyncing
                        ) {
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Secondary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("🔄", fontSize = 20.sp)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Secondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tab selector
                TabRow(
                    selectedTabIndex = BiometricTab.entries.indexOf(uiState.selectedTab),
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    BiometricTab.entries.forEach { tab ->
                        Tab(
                            selected = uiState.selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(tab.emoji)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tab.title, fontSize = 12.sp)
                                }
                            }
                        )
                    }
                }

                // Content based on selected tab
                AnimatedContent(
                    targetState = uiState.selectedTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    }
                ) { tab ->
                    when (tab) {
                        BiometricTab.OVERVIEW -> OverviewTab(
                            uiState = uiState,
                            viewModel = viewModel,
                            onConnectDevice = { viewModel.showDeviceConnectDialog(true) }
                        )
                        BiometricTab.SLEEP -> SleepTab(
                            sleepRecords = uiState.sleepRecords,
                            viewModel = viewModel
                        )
                        BiometricTab.HRV -> HrvTab(
                            hrvRecords = uiState.hrvRecords,
                            viewModel = viewModel
                        )
                        BiometricTab.CORRELATIONS -> CorrelationsTab(
                            correlations = uiState.correlations,
                            insights = uiState.insights,
                            onDismissInsight = { viewModel.dismissInsight(it) }
                        )
                    }
                }
            }
        }

        // Device connect dialog
        if (uiState.showDeviceConnectDialog) {
            DeviceConnectDialog(
                connectedDevices = uiState.connectedDevices,
                onConnect = { viewModel.connectDevice(it) },
                onDismiss = { viewModel.showDeviceConnectDialog(false) }
            )
        }
    }
}

@Composable
private fun OverviewTab(
    uiState: BiometricUiState,
    viewModel: BiometricViewModel,
    onConnectDevice: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connect devices prompt if none connected
        if (uiState.connectedDevices.isEmpty()) {
            item {
                ConnectDevicesCard(onConnect = onConnectDevice)
            }
        }

        // Recovery Score Card
        item {
            RecoveryScoreCard(
                score = uiState.dashboardSummary.todayRecoveryScore,
                viewModel = viewModel
            )
        }

        // Quick Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickStatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "😴",
                    title = "Sleep",
                    value = "${"%.1f".format(uiState.dashboardSummary.avgSleepDuration)}h",
                    subtitle = "7-day avg"
                )
                QuickStatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "💗",
                    title = "HRV Trend",
                    value = viewModel.formatHrvTrend(uiState.dashboardSummary.weeklyHrvTrend),
                    subtitle = "vs last week",
                    valueColor = if (uiState.dashboardSummary.weeklyHrvTrend >= 0) Secondary else Color(0xFFE57373)
                )
            }
        }

        // Latest insights
        if (uiState.insights.isNotEmpty()) {
            item {
                Text(
                    text = "Latest Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(uiState.insights.take(3)) { insight ->
                BiometricInsightCard(
                    insight = insight,
                    onDismiss = { viewModel.dismissInsight(insight.id) }
                )
            }
        }

        // Top correlation
        uiState.dashboardSummary.topCorrelation?.let { correlation ->
            item {
                Text(
                    text = "Top Correlation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                CorrelationCard(correlation = correlation)
            }
        }

        // Connected devices
        if (uiState.connectedDevices.isNotEmpty()) {
            item {
                Text(
                    text = "Connected Devices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(uiState.connectedDevices) { device ->
                ConnectedDeviceCard(
                    device = device,
                    onDisconnect = { viewModel.disconnectDevice(device.source) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SleepTab(
    sleepRecords: List<SleepBiometricRecord>,
    viewModel: BiometricViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (sleepRecords.isEmpty()) {
            item {
                EmptyDataCard(
                    emoji = "😴",
                    title = "No Sleep Data",
                    description = "Connect a device or log your sleep manually to see insights."
                )
            }
        } else {
            // Sleep summary
            item {
                val avgDuration = sleepRecords.map { it.durationMinutes }.average().toFloat() / 60f
                val avgDeepSleep = sleepRecords.map { it.deepSleepMinutes }.average().toFloat() / 60f
                val avgEfficiency = sleepRecords.map { it.efficiency }.average().toFloat()

                SleepSummaryCard(
                    avgDuration = avgDuration,
                    avgDeepSleep = avgDeepSleep,
                    avgEfficiency = avgEfficiency
                )
            }

            item {
                Text(
                    text = "Recent Sleep",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(sleepRecords.take(7)) { record ->
                SleepRecordCard(record = record, viewModel = viewModel)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HrvTab(
    hrvRecords: List<HrvRecord>,
    viewModel: BiometricViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (hrvRecords.isEmpty()) {
            item {
                EmptyDataCard(
                    emoji = "💗",
                    title = "No HRV Data",
                    description = "Connect a wearable device to track your heart rate variability."
                )
            }
        } else {
            // HRV explanation
            item {
                HrvExplanationCard()
            }

            // HRV summary
            item {
                val avgHrv = hrvRecords.map { it.avgHrv }.average().toFloat()
                val avgRhr = hrvRecords.map { it.restingHeartRate }.average().toInt()

                HrvSummaryCard(
                    avgHrv = avgHrv,
                    avgRhr = avgRhr,
                    recordCount = hrvRecords.size
                )
            }

            item {
                Text(
                    text = "Recent Readings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(hrvRecords.take(7)) { record ->
                HrvRecordCard(record = record)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CorrelationsTab(
    correlations: List<BiometricCorrelation>,
    insights: List<BiometricInsight>,
    onDismissInsight: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PrimaryLight.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔬", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "How Correlations Work",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "We analyze your biometric data alongside your habit completions to discover hidden patterns. More data = better insights!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (correlations.isEmpty()) {
            item {
                EmptyDataCard(
                    emoji = "📊",
                    title = "Building Correlations",
                    description = "Keep tracking your habits and biometrics. We need at least 7 days of data to find meaningful patterns."
                )
            }
        } else {
            item {
                Text(
                    text = "Discovered Correlations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(correlations) { correlation ->
                CorrelationCard(correlation = correlation)
            }
        }

        // Related insights
        val correlationInsights = insights.filter {
            it.type in listOf(
                BiometricInsightType.SLEEP_IMPACT,
                BiometricInsightType.HABIT_BENEFIT
            )
        }

        if (correlationInsights.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Related Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(correlationInsights) { insight ->
                BiometricInsightCard(
                    insight = insight,
                    onDismiss = { onDismissInsight(insight.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Component Cards

@Composable
private fun ConnectDevicesCard(onConnect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnect() },
        colors = CardDefaults.cardColors(containerColor = Secondary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📱", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Connect Your Devices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sync with Oura Ring, WHOOP, Apple Health, or Google Fit to unlock personalized biometric insights.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(containerColor = Secondary)
            ) {
                Text("Connect Device")
            }
        }
    }
}

@Composable
private fun RecoveryScoreCard(
    score: Int,
    viewModel: BiometricViewModel
) {
    val scoreColor = when (viewModel.getRecoveryScoreColor(score)) {
        "green" -> Secondary
        "yellow" -> Color(0xFFFFB74D)
        "orange" -> Color(0xFFFF8A65)
        else -> Color(0xFFE57373)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = scoreColor.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Today's Recovery",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Score circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(scoreColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = "/100",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    score >= 80 -> "🟢 Excellent! You're ready to push hard today."
                    score >= 60 -> "🟡 Good. Moderate intensity recommended."
                    score >= 40 -> "🟠 Fair. Consider lighter activities."
                    else -> "🔴 Low. Focus on rest and recovery."
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QuickStatCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BiometricInsightCard(
    insight: BiometricInsight,
    onDismiss: () -> Unit
) {
    val severityColor = when (insight.severity) {
        InsightSeverity.SUCCESS -> Secondary
        InsightSeverity.WARNING -> Color(0xFFFFB74D)
        InsightSeverity.ALERT -> Color(0xFFE57373)
        InsightSeverity.INFO -> Primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = severityColor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(insight.emoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "✕",
                    modifier = Modifier.clickable { onDismiss() },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodyMedium
            )

            insight.recommendation?.let { rec ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 $rec",
                    style = MaterialTheme.typography.bodySmall,
                    color = severityColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CorrelationCard(correlation: BiometricCorrelation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(correlation.biometricType.emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = correlation.habitName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (correlation.direction) {
                        CorrelationDirection.POSITIVE -> "↑"
                        CorrelationDirection.NEGATIVE -> "↓"
                        CorrelationDirection.NEUTRAL -> "↔"
                    },
                    fontSize = 16.sp,
                    color = when (correlation.direction) {
                        CorrelationDirection.POSITIVE -> Secondary
                        CorrelationDirection.NEGATIVE -> Color(0xFFE57373)
                        CorrelationDirection.NEUTRAL -> Color.Gray
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = correlation.biometricType.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = correlation.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Confidence: ${(correlation.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "${correlation.dataPoints} data points",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "💡 ${correlation.recommendation}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Secondary
            )
        }
    }
}

@Composable
private fun ConnectedDeviceCard(
    device: ConnectedDevice,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (device.source) {
                        BiometricSource.OURA -> "💍"
                        BiometricSource.WHOOP -> "⌚"
                        BiometricSource.APPLE_HEALTH -> "❤️"
                        BiometricSource.GOOGLE_FIT -> "💚"
                        else -> "📱"
                    },
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = device.deviceName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Last synced: ${device.lastSyncedAt.substringBefore("T")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TextButton(onClick = onDisconnect) {
                Text("Disconnect", color = Color(0xFFE57373))
            }
        }
    }
}

@Composable
private fun SleepSummaryCard(
    avgDuration: Float,
    avgDeepSleep: Float,
    avgEfficiency: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "7-Day Sleep Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("😴", fontSize = 24.sp)
                    Text(
                        text = "${"%.1f".format(avgDuration)}h",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Avg Duration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌙", fontSize = 24.sp)
                    Text(
                        text = "${"%.1f".format(avgDeepSleep)}h",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Deep Sleep",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✨", fontSize = 24.sp)
                    Text(
                        text = "${avgEfficiency.toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Efficiency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepRecordCard(
    record: SleepBiometricRecord,
    viewModel: BiometricViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = record.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = viewModel.formatSleepDuration(record.durationMinutes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Deep", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${record.deepSleepMinutes}m",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("REM", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${record.remSleepMinutes}m",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Eff", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${record.efficiency.toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (record.efficiency >= 85) Secondary else Color(0xFFFFB74D)
                    )
                }
            }
        }
    }
}

@Composable
private fun HrvExplanationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💗", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "What is HRV?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Heart Rate Variability measures the variation in time between heartbeats. Higher HRV generally indicates better cardiovascular fitness, resilience to stress, and overall recovery. Tracking HRV helps optimize your training and habit timing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HrvSummaryCard(
    avgHrv: Float,
    avgRhr: Int,
    recordCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "HRV Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💗", fontSize = 24.sp)
                    Text(
                        text = "${avgHrv.toInt()}ms",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Avg HRV",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("❤️", fontSize = 24.sp)
                    Text(
                        text = "$avgRhr bpm",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Resting HR",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", fontSize = 24.sp)
                    Text(
                        text = "$recordCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Readings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HrvRecordCard(record: HrvRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = record.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${record.avgHrv.toInt()}ms HRV",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Min", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${record.minHrv.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Max", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${record.maxHrv.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RHR", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${record.restingHeartRate}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (record.restingHeartRate <= 60) Secondary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDataCard(
    emoji: String,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeviceConnectDialog(
    connectedDevices: List<ConnectedDevice>,
    onConnect: (BiometricSource) -> Unit,
    onDismiss: () -> Unit
) {
    val connectedSources = connectedDevices.map { it.source }.toSet()
    val availableSources = listOf(
        BiometricSource.APPLE_HEALTH,
        BiometricSource.GOOGLE_FIT,
        BiometricSource.OURA,
        BiometricSource.WHOOP,
        BiometricSource.FITBIT,
        BiometricSource.GARMIN
    ).filter { it !in connectedSources }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect Device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select a device or health platform to connect:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                availableSources.forEach { source ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConnect(source) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (source) {
                                    BiometricSource.OURA -> "💍"
                                    BiometricSource.WHOOP -> "⌚"
                                    BiometricSource.APPLE_HEALTH -> "❤️"
                                    BiometricSource.GOOGLE_FIT -> "💚"
                                    BiometricSource.FITBIT -> "🏃"
                                    BiometricSource.GARMIN -> "🎯"
                                    else -> "📱"
                                },
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = source.displayName,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                if (availableSources.isEmpty()) {
                    Text(
                        text = "All supported devices are already connected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
