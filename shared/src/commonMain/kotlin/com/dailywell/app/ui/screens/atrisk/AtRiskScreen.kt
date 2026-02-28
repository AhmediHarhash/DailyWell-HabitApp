package com.dailywell.app.ui.screens.atrisk

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import com.dailywell.app.data.model.*
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.core.theme.Success
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtRiskScreen(
    viewModel: AtRiskViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showHabitDetails by remember { mutableStateOf(false) }

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Risk Analysis",
                    subtitle = "Detect streak breaks early",
                    onNavigationClick = onNavigateBack,
                    trailingActions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                        IconButton(onClick = { viewModel.refreshRiskData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Daily Summary Card
                    uiState.dailySummary?.let { summary ->
                        item {
                            DailySummaryCard(summary = summary)
                        }
                    }

                    // Weather Impact
                    uiState.currentWeather?.let { weather ->
                        if (!weather.isOutdoorFriendly) {
                            item {
                                WeatherAlertCard(weather = weather)
                            }
                        }
                    }

                    // Active Alerts
                    if (uiState.activeAlerts.isNotEmpty()) {
                        item {
                            PremiumSectionChip(
                                text = "Active alerts",
                                icon = DailyWellIcons.Status.Warning
                            )
                        }

                        items(uiState.activeAlerts) { alert ->
                            AlertCard(
                                alert = alert,
                                onDismiss = { viewModel.dismissAlert(alert.id) }
                            )
                        }
                    }

                    // High Risk Habits
                    val highRiskHabits = viewModel.getHighRiskHabits()
                    if (highRiskHabits.isNotEmpty()) {
                        item {
                            PremiumSectionChip(
                                text = "High risk",
                                icon = DailyWellIcons.Status.Error
                            )
                        }

                        items(highRiskHabits) { assessment ->
                            RiskAssessmentCard(
                                assessment = assessment,
                                onClick = {
                                    viewModel.selectHabitForDetails(assessment.habitId)
                                    showHabitDetails = true
                                }
                            )
                        }
                    }

                    // Medium Risk Habits
                    val mediumRiskHabits = viewModel.getMediumRiskHabits()
                    if (mediumRiskHabits.isNotEmpty()) {
                        item {
                            PremiumSectionChip(
                                text = "Medium risk",
                                icon = DailyWellIcons.Analytics.AtRisk
                            )
                        }

                        items(mediumRiskHabits) { assessment ->
                            RiskAssessmentCard(
                                assessment = assessment,
                                onClick = {
                                    viewModel.selectHabitForDetails(assessment.habitId)
                                    showHabitDetails = true
                                }
                            )
                        }
                    }

                    // Habit Health Overview
                    if (uiState.habitHealthList.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            PremiumSectionChip(
                                text = "Habit health",
                                icon = DailyWellIcons.Analytics.BarChart
                            )
                        }

                        items(uiState.habitHealthList) { health ->
                            HabitHealthCard(
                                health = health,
                                assessment = uiState.riskAssessments.find { it.habitId == health.habitId },
                                onClick = {
                                    viewModel.selectHabitForDetails(health.habitId)
                                    showHabitDetails = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Settings Dialog
    if (showSettings) {
        AtRiskSettingsDialog(
            settings = uiState.notificationSettings,
            onDismiss = { showSettings = false },
            onSave = { settings ->
                viewModel.updateNotificationSettings(settings)
                showSettings = false
            }
        )
    }

    // Habit Details Dialog
    if (showHabitDetails && uiState.selectedHabitHealth != null) {
        HabitDetailsDialog(
            health = uiState.selectedHabitHealth!!,
            pattern = uiState.selectedHabitPattern,
            assessment = uiState.riskAssessments.find { it.habitId == uiState.selectedHabitHealth?.habitId },
            onDismiss = {
                showHabitDetails = false
                viewModel.clearSelectedHabit()
            }
        )
    }
}

@Composable
private fun DailySummaryCard(summary: DailyRiskSummary) {
    val backgroundColor = when (summary.overallRiskLevel) {
        RiskLevel.CRITICAL, RiskLevel.HIGH -> MaterialTheme.colorScheme.errorContainer
        RiskLevel.MEDIUM -> Color(0xFFFFF3E0)
        RiskLevel.LOW -> Color(0xFFE8F5E9)
    }

    val riskIcon = when (summary.overallRiskLevel) {
        RiskLevel.CRITICAL -> DailyWellIcons.Status.Error
        RiskLevel.HIGH -> DailyWellIcons.Status.Warning
        RiskLevel.MEDIUM -> DailyWellIcons.Analytics.BarChart
        RiskLevel.LOW -> DailyWellIcons.Status.Success
    }

    val riskIconTint = when (summary.overallRiskLevel) {
        RiskLevel.CRITICAL, RiskLevel.HIGH -> MaterialTheme.colorScheme.error
        RiskLevel.MEDIUM -> Color(0xFFFFA000)
        RiskLevel.LOW -> Success
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = riskIcon,
                    contentDescription = summary.overallRiskLevel.name,
                    modifier = Modifier.size(32.dp),
                    tint = riskIconTint
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Today's Risk Level",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = summary.overallRiskLevel.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Total Habits",
                    value = summary.totalHabits.toString()
                )
                StatItem(
                    label = "At Risk",
                    value = (summary.highRiskCount + summary.mediumRiskCount).toString(),
                    color = if (summary.highRiskCount > 0) MaterialTheme.colorScheme.error else Color.Unspecified
                )
                StatItem(
                    label = "High Risk",
                    value = summary.highRiskCount.toString(),
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (summary.recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                summary.recommendations.forEach { recommendation ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = DailyWellIcons.Onboarding.Philosophy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = recommendation,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color = Color.Unspecified
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeatherAlertCard(weather: WeatherCondition) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (weather.condition) {
                    WeatherType.RAINY -> DailyWellIcons.Weather.Rainy
                    WeatherType.STORMY -> DailyWellIcons.Weather.Storm
                    WeatherType.SNOWY -> DailyWellIcons.Weather.Snowy
                    WeatherType.EXTREME_HEAT -> DailyWellIcons.Weather.Hot
                    WeatherType.EXTREME_COLD -> DailyWellIcons.Weather.Cold
                    else -> DailyWellIcons.Weather.Sunny
                },
                contentDescription = weather.description,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Weather Alert",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${weather.description} (${weather.temperature.toInt()}°C)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Outdoor activities may be affected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: AtRiskAlert,
    onDismiss: () -> Unit
) {
    val backgroundColor = when (alert.riskLevel) {
        RiskLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
        RiskLevel.HIGH -> Color(0xFFFFEBEE)
        RiskLevel.MEDIUM -> Color(0xFFFFF8E1)
        RiskLevel.LOW -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = DailyWellIcons.getHabitIcon(alert.habitId),
                        contentDescription = alert.habitName,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = alert.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = alert.message,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            alert.actionSuggestion?.let { suggestion ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = DailyWellIcons.Onboarding.Philosophy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary
                    )
                }
            }

            alert.suggestedTime?.let { time ->
                val formattedTime = Instant.ofEpochMilli(time)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("h:mm a"))
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = DailyWellIcons.Misc.Time,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Suggested time: $formattedTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RiskAssessmentCard(
    assessment: HabitRiskAssessment,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = DailyWellIcons.getHabitIcon(assessment.habitId),
                        contentDescription = assessment.habitName,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = assessment.habitName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        RiskLevelBadge(riskLevel = assessment.riskLevel)
                    }
                }

                RiskScoreIndicator(score = assessment.riskScore)
            }

            if (assessment.riskFactors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                assessment.riskFactors.take(2).forEach { factor ->
                    RiskFactorRow(factor = factor)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (assessment.riskFactors.size > 2) {
                    Text(
                        text = "+${assessment.riskFactors.size - 2} more factors",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            assessment.preemptiveSuggestion?.let { suggestion ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = DailyWellIcons.Onboarding.Philosophy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RiskLevelBadge(riskLevel: RiskLevel) {
    val (color, text) = when (riskLevel) {
        RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error to "Critical"
        RiskLevel.HIGH -> Color(0xFFD32F2F) to "High Risk"
        RiskLevel.MEDIUM -> Color(0xFFFFA000) to "Medium"
        RiskLevel.LOW -> Success to "Low Risk"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun RiskScoreIndicator(score: Float) {
    val color = when {
        score >= 0.7f -> MaterialTheme.colorScheme.error
        score >= 0.4f -> Color(0xFFFFA000)
        else -> Success
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${(score * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun RiskFactorRow(factor: AtRiskFactor) {
    val factorIcon = when (factor.type) {
        RiskFactorType.DAY_OF_WEEK -> DailyWellIcons.Analytics.Calendar
        RiskFactorType.TIME_PRESSURE -> DailyWellIcons.Misc.Time
        RiskFactorType.WEATHER -> DailyWellIcons.Weather.Sunny
        RiskFactorType.STREAK_FATIGUE -> DailyWellIcons.Analytics.TrendDown
        RiskFactorType.RECENT_MISSES -> DailyWellIcons.Analytics.TrendDown
        RiskFactorType.CALENDAR_CONFLICT -> DailyWellIcons.Analytics.Calendar
        RiskFactorType.LATE_IN_DAY -> DailyWellIcons.Misc.Night
        RiskFactorType.WEEKEND_PATTERN -> DailyWellIcons.Social.Cheer
        RiskFactorType.RECOVERY_MODE -> DailyWellIcons.Habits.Recovery
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = factorIcon,
            contentDescription = factor.type.name,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = factor.description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HabitHealthCard(
    health: HabitHealth,
    assessment: HabitRiskAssessment?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Health Score Circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                health.healthScore >= 70 -> Success.copy(alpha = 0.2f)
                                health.healthScore >= 40 -> Color(0xFFFFA000).copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = health.healthScore.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            health.healthScore >= 70 -> Success
                            health.healthScore >= 40 -> Color(0xFFFFA000)
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (assessment != null) DailyWellIcons.getHabitIcon(assessment.habitId) else DailyWellIcons.Analytics.BarChart,
                            contentDescription = assessment?.habitName ?: "Habit",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = assessment?.habitName ?: "Habit",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val healthTrendIcon = when (health.trend) {
                            HealthTrend.IMPROVING -> DailyWellIcons.Analytics.TrendUp
                            HealthTrend.STABLE -> DailyWellIcons.Analytics.TrendFlat
                            HealthTrend.DECLINING -> DailyWellIcons.Analytics.TrendDown
                            HealthTrend.NEW -> DailyWellIcons.Status.New
                        }
                        Icon(
                            imageVector = healthTrendIcon,
                            contentDescription = health.trend.name,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = health.trend.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = DailyWellIcons.Analytics.Streak,
                            contentDescription = "Streak",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${health.currentStreak} day streak",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Icon(
                imageVector = DailyWellIcons.Nav.ChevronRight,
                contentDescription = "Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AtRiskSettingsDialog(
    settings: AtRiskNotificationSettings,
    onDismiss: () -> Unit,
    onSave: (AtRiskNotificationSettings) -> Unit
) {
    var enabled by remember { mutableStateOf(settings.enabled) }
    var notifyHighRisk by remember { mutableStateOf(settings.notifyOnHighRisk) }
    var notifyMediumRisk by remember { mutableStateOf(settings.notifyOnMediumRisk) }
    var includeWeather by remember { mutableStateOf(settings.includeWeatherAlerts) }
    var includeCalendar by remember { mutableStateOf(settings.includeCalendarAlerts) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Risk Alert Settings") },
        text = {
            Column {
                SettingSwitch(
                    title = "Enable Risk Alerts",
                    checked = enabled,
                    onCheckedChange = { enabled = it }
                )

                if (enabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingSwitch(
                        title = "High Risk Alerts",
                        subtitle = "Notify when habits are at high risk",
                        checked = notifyHighRisk,
                        onCheckedChange = { notifyHighRisk = it }
                    )

                    SettingSwitch(
                        title = "Medium Risk Alerts",
                        subtitle = "Notify for medium risk habits too",
                        checked = notifyMediumRisk,
                        onCheckedChange = { notifyMediumRisk = it }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingSwitch(
                        title = "Weather Alerts",
                        subtitle = "Include weather impact warnings",
                        checked = includeWeather,
                        onCheckedChange = { includeWeather = it }
                    )

                    SettingSwitch(
                        title = "Calendar Alerts",
                        subtitle = "Include calendar conflict warnings",
                        checked = includeCalendar,
                        onCheckedChange = { includeCalendar = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        settings.copy(
                            enabled = enabled,
                            notifyOnHighRisk = notifyHighRisk,
                            notifyOnMediumRisk = notifyMediumRisk,
                            includeWeatherAlerts = includeWeather,
                            includeCalendarAlerts = includeCalendar
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun HabitDetailsDialog(
    health: HabitHealth,
    pattern: HabitPattern?,
    assessment: HabitRiskAssessment?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (assessment != null) DailyWellIcons.getHabitIcon(assessment.habitId) else DailyWellIcons.Analytics.BarChart,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("${assessment?.habitName ?: "Habit"} Details")
            }
        },
        text = {
            LazyColumn {
                // Health Score Section
                item {
                    Text(
                        text = "Health Score: ${health.healthScore}/100",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Stats
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            label = "Current Streak",
                            value = "${health.currentStreak}"
                        )
                        StatItem(
                            label = "Best Streak",
                            value = "${health.longestStreak}"
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Completion Rates
                item {
                    Text(
                        text = "Completion Rates",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Last 7 days: ${(health.completionRateLast7Days * 100).toInt()}%")
                    Text("Last 30 days: ${(health.completionRateLast30Days * 100).toInt()}%")
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Patterns
                item {
                    Text(
                        text = "Patterns",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    health.bestDay?.let { Text("Best day: $it") }
                    health.worstDay?.let { Text("Challenging day: $it") }
                    health.bestTimeOfDay?.let { Text("Best time: $it") }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Day of Week Analysis
                pattern?.let { p ->
                    item {
                        Text(
                            text = "Day-by-Day Analysis",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    items(p.dayOfWeekStats) { stat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stat.dayName.take(3))
                            Text("${(stat.completionRate * 100).toInt()}%")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}



