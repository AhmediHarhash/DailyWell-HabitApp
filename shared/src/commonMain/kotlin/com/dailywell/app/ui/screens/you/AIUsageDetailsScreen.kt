package com.dailywell.app.ui.screens.you

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.core.theme.Success
import com.dailywell.app.data.model.AIInteraction
import com.dailywell.app.data.model.AIPlanType
import com.dailywell.app.data.model.AIRoutingIntentStat
import com.dailywell.app.data.model.MonthlyAIUsageReport
import com.dailywell.app.data.model.UserAIUsage
import com.dailywell.app.data.repository.AICoachingRepository
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import org.koin.compose.koinInject
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun AIUsageDetailsScreen(
    onBack: () -> Unit,
    onOpenObservability: () -> Unit,
    aiCoachingRepository: AICoachingRepository = koinInject()
) {
    val aiUsage by aiCoachingRepository.getAIUsage().collectAsState(
        initial = UserAIUsage(
            userId = "local",
            planType = AIPlanType.FREE,
            resetDate = "next cycle",
            lastUpdated = ""
        )
    )

    var isLoading by remember { mutableStateOf(true) }
    var monthlyReport by remember { mutableStateOf<MonthlyAIUsageReport?>(null) }
    var recentInteractions by remember { mutableStateOf<List<AIInteraction>>(emptyList()) }

    LaunchedEffect(aiUsage.lastUpdated) {
        isLoading = true
        monthlyReport = aiCoachingRepository.getMonthlyUsageReport()
        recentInteractions = aiCoachingRepository.getRecentAIInteractions(limit = 8)
        isLoading = false
    }

    val usagePercent = max(aiUsage.percentUsed, aiUsage.costPercentUsed).coerceIn(0f, 100f)
    val progress = (usagePercent / 100f).coerceIn(0f, 1f)
    val usedPercent = usagePercent.roundToInt().coerceIn(0, 100)
    val availablePercent = (100 - usedPercent).coerceAtLeast(0)
    val localChat = aiUsage.localMessagesCount
    val cloudChat = aiUsage.cloudChatCalls
    val scanCalls = aiUsage.cloudScanCalls
    val reportCalls = aiUsage.cloudReportCalls

    GlassScreenWrapper {
        androidx.compose.material3.Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "AI Usage",
                    subtitle = "Wallet, limits, and activity details",
                    onNavigationClick = onBack
                )
            }
        ) { paddingValues ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Primary.copy(alpha = 0.10f),
                                                MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    )
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Shared AI wallet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = if (progress >= 0.8f) MaterialTheme.colorScheme.error else Success,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Used $usedPercent%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Available $availablePercent%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    UsageStatPill("Local chat", localChat.toString(), Success, Modifier.weight(1f))
                                    UsageStatPill("Cloud chat", cloudChat.toString(), Primary, Modifier.weight(1f))
                                    UsageStatPill("Scans", scanCalls.toString(), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    UsageStatPill("Reports", reportCalls.toString(), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                                    UsageStatPill("Cloud total", aiUsage.cloudTotalCalls.toString(), Primary, Modifier.weight(1f))
                                    UsageStatPill("Reset", aiUsage.resetDate, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                                }
                                Text(
                                    text = "One AI wallet across chat, scan, and reports.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onOpenObservability,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Open Observability")
                                }
                            }
                        }
                    }

                    monthlyReport?.let { report ->
                        item {
                            PremiumSectionChip(
                                text = "Monthly summary",
                                icon = DailyWellIcons.Analytics.BarChart
                            )
                        }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    UsageStatPill(
                                        label = "Total ops",
                                        value = report.totalMessages.toString(),
                                        tint = Primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    UsageStatPill(
                                        label = "Local %",
                                        value = "${report.efficiencyPercent.roundToInt()}%",
                                        tint = Success,
                                        modifier = Modifier.weight(1f)
                                    )
                                    UsageStatPill(
                                        label = "Tokens",
                                        value = report.totalTokens.toString(),
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    if (recentInteractions.isNotEmpty()) {
                        item {
                            PremiumSectionChip(
                                text = "Recent activity",
                                icon = DailyWellIcons.Analytics.Pattern
                            )
                        }
                        items(recentInteractions) { interaction ->
                            InteractionRow(interaction = interaction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AIObservabilityScreen(
    onBack: () -> Unit,
    aiCoachingRepository: AICoachingRepository = koinInject()
) {
    var isLoading by remember { mutableStateOf(true) }
    var intentStats by remember { mutableStateOf<List<AIRoutingIntentStat>>(emptyList()) }
    var recommendations by remember { mutableStateOf<List<String>>(emptyList()) }
    var recentInteractions by remember { mutableStateOf<List<AIInteraction>>(emptyList()) }

    LaunchedEffect(Unit) {
        isLoading = true
        intentStats = aiCoachingRepository.getRoutingIntentStats(limit = 8)
        recommendations = aiCoachingRepository.getRoutingRecommendations()
        recentInteractions = aiCoachingRepository.getRecentAIInteractions(limit = 20)
        isLoading = false
    }

    GlassScreenWrapper {
        androidx.compose.material3.Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "AI Observability",
                    subtitle = "Model routing and cost telemetry",
                    onNavigationClick = onBack
                )
            }
        ) { paddingValues ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (intentStats.isNotEmpty()) {
                        item {
                            PremiumSectionChip(
                                text = "Intent routing stats",
                                icon = DailyWellIcons.Analytics.Score
                            )
                        }
                        items(intentStats) { stat ->
                            IntentStatRow(stat = stat)
                        }
                    }

                    if (recommendations.isNotEmpty()) {
                        item {
                            PremiumSectionChip(
                                text = "Router recommendations",
                                icon = DailyWellIcons.Misc.Sparkle
                            )
                        }
                        items(recommendations) { recommendation ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = recommendation,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    if (recentInteractions.isNotEmpty()) {
                        item {
                            PremiumSectionChip(
                                text = "Recent model events",
                                icon = DailyWellIcons.Analytics.Pattern
                            )
                        }
                        items(recentInteractions) { interaction ->
                            InteractionRow(interaction = interaction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntentStatRow(stat: AIRoutingIntentStat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stat.intent,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UsageStatPill("Calls", stat.callCount.toString(), Primary, Modifier.weight(1f))
                UsageStatPill("Avg ms", stat.avgResponseTimeMs.toString(), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                UsageStatPill("Avg cost", formatUsd(stat.avgCostUsd), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InteractionRow(interaction: AIInteraction) {
    val timestamp = interaction.timestamp
        .substringAfter("T", interaction.timestamp)
        .substringBefore(".")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = interaction.modelUsed.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = interaction.responseCategory,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UsageStatPill("Tokens", interaction.totalTokens.toString(), Primary, Modifier.weight(1f))
                UsageStatPill("Cost", formatUsd(interaction.estimatedCostUsd), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun UsageStatPill(
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatUsd(amount: Float): String {
    val normalized = (amount * 1000f).roundToInt() / 1000f
    return "$$normalized"
}
