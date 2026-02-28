package com.dailywell.app.ui.screens.wellness

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.dailywell.app.data.model.*
import com.dailywell.app.ui.components.*
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessScoreScreen(
    onBackClick: () -> Unit = {},
    onShareScore: (String, String) -> Unit = { _, _ -> }
) {
    val viewModel: WellnessScoreViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val score = uiState.score ?: return

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Weekly Wellness Score",
                    subtitle = "Your weekly habit health snapshot",
                    onNavigationClick = onBackClick,
                    trailingActions = {
                        IconButton(onClick = {
                            val card = ShareableScoreCard.from(score)
                            onShareScore(card.shareText, card.imageDescription)
                        }) {
                            Icon(DailyWellIcons.Social.Share, "Share")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StaggeredItem(index = 0) {
                        PremiumSectionChip(
                            text = "Weekly report",
                            icon = DailyWellIcons.Analytics.BarChart
                        )
                    }
                }

                // Main score card
                item {
                    StaggeredItem(index = 1) {
                        MainScoreCard(score = score)
                    }
                }

                // Score breakdown
                item {
                    StaggeredItem(index = 2) {
                        ScoreBreakdownCard(breakdown = score.scoreBreakdown)
                    }
                }

                // Week summary
                item {
                    StaggeredItem(index = 3) {
                        WeekSummaryCard(summary = score.weekSummary)
                    }
                }

                // Insights
                if (score.insights.isNotEmpty()) {
                    item {
                        StaggeredItem(index = 4) {
                            InsightsCard(insights = score.insights)
                        }
                    }
                }

                // Recommendations
                if (score.recommendations.isNotEmpty()) {
                    item {
                        StaggeredItem(index = 5) {
                            RecommendationsCard(recommendations = score.recommendations)
                        }
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun MainScoreCard(score: WeeklyWellnessScore) {
    val gradient = when (score.rank) {
        ScoreRank.CHAMPION -> Brush.linearGradient(
            colors = listOf(Color(0xFFFFD700), Color(0xFFFF8C00))
        )
        ScoreRank.EXCELLENT -> Brush.linearGradient(
            colors = listOf(Color(0xFF9C27B0), Color(0xFF2196F3))
        )
        ScoreRank.THRIVING -> Brush.linearGradient(
            colors = listOf(Color(0xFF4CAF50), Color(0xFF00BCD4))
        )
        ScoreRank.BUILDING -> Brush.linearGradient(
            colors = listOf(Color(0xFF2196F3), Color(0xFF3F51B5))
        )
        ScoreRank.STRUGGLING -> Brush.linearGradient(
            colors = listOf(Color(0xFF90A4AE), Color(0xFF64B5F6))
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rank badge
                Text(
                    text = score.rank.displayName().uppercase(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                // Score
                Text(
                    text = "${score.overallScore}",
                    color = Color.White,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "out of 100",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp
                )

                // Score change
                if (score.scoreChange != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (score.scoreChange > 0) DailyWellIcons.Analytics.TrendUp else DailyWellIcons.Analytics.TrendDown,
                            contentDescription = if (score.scoreChange > 0) "Up" else "Down",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Text(
                            text = "${kotlin.math.abs(score.scoreChange)} from last week",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                // Week range
                Text(
                    text = "${score.weekStart} - ${score.weekEnd}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun ScoreBreakdownCard(breakdown: ScoreBreakdown) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Score Breakdown",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            ScoreBar(
                label = "Completion Rate",
                score = breakdown.completionScore,
                maxScore = 40,
                color = Color(0xFF4CAF50)
            )

            ScoreBar(
                label = "Streak Maintenance",
                score = breakdown.streakScore,
                maxScore = 20,
                color = Color(0xFFFF9800)
            )

            ScoreBar(
                label = "Consistency",
                score = breakdown.consistencyScore,
                maxScore = 20,
                color = Color(0xFF2196F3)
            )

            ScoreBar(
                label = "Improvement",
                score = breakdown.improvementScore,
                maxScore = 20,
                color = Color(0xFF9C27B0)
            )
        }
    }
}

@Composable
fun ScoreBar(
    label: String,
    score: Int,
    maxScore: Int,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$score / $maxScore",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        LinearProgressIndicator(
            progress = { score.toFloat() / maxScore },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun WeekSummaryCard(summary: WeekSummary) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Week Summary",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStatItem(
                    value = "${summary.daysActive}/7",
                    label = "Days Active",
                    icon = DailyWellIcons.Analytics.Calendar
                )
                SummaryStatItem(
                    value = "${(summary.completionRate * 100).toInt()}%",
                    label = "Completion",
                    icon = DailyWellIcons.Actions.CheckCircle
                )
                SummaryStatItem(
                    value = "${summary.perfectDays}",
                    label = "Perfect Days",
                    icon = DailyWellIcons.Status.Star
                )
                SummaryStatItem(
                    value = "${summary.currentStreak}",
                    label = "Streak",
                    icon = DailyWellIcons.Analytics.Streak
                )
            }
        }
    }
}

@Composable
fun SummaryStatItem(
    value: String,
    label: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InsightsCard(insights: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DailyWellIcons.Analytics.Insights,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Insights",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            insights.forEach { insight ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = insight,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendationsCard(recommendations: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DailyWellIcons.Onboarding.Philosophy,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recommendations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            recommendations.forEach { recommendation ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Nav.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = recommendation,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
