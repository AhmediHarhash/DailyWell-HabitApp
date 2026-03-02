package com.dailywell.app.ui.screens.you

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailywell.app.core.theme.AccentIndigo
import com.dailywell.app.core.theme.AccentSky
import com.dailywell.app.data.model.ThemeMode
import com.dailywell.app.data.model.UserSettings
import com.dailywell.app.data.repository.EntryRepository
import com.dailywell.app.data.repository.HabitRepository
import com.dailywell.app.data.repository.SettingsRepository
import com.dailywell.app.ui.components.DailyWellIcons
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun YouScreen(
    isPremium: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToMilestones: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToUsageDetails: () -> Unit,
    onNavigateToCustomHabit: () -> Unit
) {
    val settingsRepository: SettingsRepository = koinInject()
    val habitRepository: HabitRepository = koinInject()
    val entryRepository: EntryRepository = koinInject()
    val scope = rememberCoroutineScope()

    val settings by settingsRepository.getSettings().collectAsState(initial = UserSettings())
    val habits by habitRepository.getEnabledHabits().collectAsState(initial = emptyList())
    val streakInfo by entryRepository.getStreakInfo().collectAsState(initial = com.dailywell.app.data.model.StreakInfo())

    val displayName = settings.displayName
        ?.takeIf { it.isNotBlank() }
        ?: settings.userName?.takeIf { it.isNotBlank() }
        ?: "there"

    val themeIsDark = settings.themeMode == ThemeMode.DARK

    val toggleTheme: () -> Unit = {
        scope.launch {
            val snapshot = settingsRepository.getSettingsSnapshot()
            val target = if (snapshot.themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
            settingsRepository.updateSettings(snapshot.copy(themeMode = target))
        }
        Unit
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF2F4F7),
                        Color(0xFFECEFF4)
                    )
                )
            )
    ) {
        val wideLayout = maxWidth >= 760.dp

        if (wideLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardRail(
                    modifier = Modifier
                        .width(70.dp)
                        .fillMaxHeight(),
                    themeIsDark = themeIsDark,
                    onToggleTheme = toggleTheme,
                    onSettingsClick = onNavigateToSettings
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFF7F8FA),
                    tonalElevation = 0.dp,
                    shadowElevation = 4.dp
                ) {
                    DashboardContent(
                        displayName = displayName,
                        streakDays = streakInfo.currentStreak,
                        habitsCount = habits.size,
                        onNavigateToMilestones = onNavigateToMilestones,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToCustomHabit = onNavigateToCustomHabit
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RailIconButton(
                        icon = if (themeIsDark) DailyWellIcons.Misc.Brightness else DailyWellIcons.Misc.DarkMode,
                        contentDescription = "Change theme",
                        onClick = toggleTheme
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RailIconButton(
                        icon = DailyWellIcons.Nav.Settings,
                        contentDescription = "Settings",
                        onClick = onNavigateToSettings
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFFF7F8FA),
                    tonalElevation = 0.dp,
                    shadowElevation = 3.dp
                ) {
                    DashboardContent(
                        displayName = displayName,
                        streakDays = streakInfo.currentStreak,
                        habitsCount = habits.size,
                        onNavigateToMilestones = onNavigateToMilestones,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToCustomHabit = onNavigateToCustomHabit
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardRail(
    modifier: Modifier,
    themeIsDark: Boolean,
    onToggleTheme: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFF7F8FA),
        tonalElevation = 0.dp,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                AccentSky,
                                AccentIndigo
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = DailyWellIcons.Onboarding.Welcome,
                    contentDescription = "DailyWell",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            RailIconButton(
                icon = if (themeIsDark) DailyWellIcons.Misc.Brightness else DailyWellIcons.Misc.DarkMode,
                contentDescription = "Change theme",
                onClick = onToggleTheme
            )
            Spacer(modifier = Modifier.height(8.dp))
            RailIconButton(
                icon = DailyWellIcons.Nav.Settings,
                contentDescription = "Settings",
                onClick = onSettingsClick
            )
        }
    }
}

@Composable
private fun RailIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFDDE2EB), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFF495164),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun DashboardContent(
    displayName: String,
    streakDays: Int,
    habitsCount: Int,
    onNavigateToMilestones: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCustomHabit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Welcome back, $displayName",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF171C26)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatPill(
                icon = DailyWellIcons.Analytics.Streak,
                label = "${streakDays.coerceAtLeast(0)} day streak"
            )
            StatPill(
                icon = DailyWellIcons.Nav.Settings,
                label = "Settings hub"
            )
            StatPill(
                icon = DailyWellIcons.Habits.Custom,
                label = "$habitsCount habits"
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF3F2DF),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Make DailyWell work like you",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF192033),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Tune coaching style, tracker defaults, and report depth for your real routine.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5A6172)
                )
                Button(
                    onClick = onNavigateToSettings,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF151A24),
                        contentColor = Color.White
                    )
                ) {
                    Text("Start now", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Text(
            text = "TODAY",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF8B92A0),
            fontWeight = FontWeight.Bold
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "What are your next steps?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF252B38)
                )
                TextButton(onClick = onNavigateToCustomHabit) {
                    Text("Add", color = AccentSky, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onNavigateToMilestones),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Milestones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2432)
                    )
                    Text(
                        text = "Review streak highlights",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6E7586)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onNavigateToSettings),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2432)
                    )
                    Text(
                        text = "Account, AI usage, and personalization",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6E7586)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    icon: ImageVector,
    label: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFEDEFF3))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF6A7283),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF50586B),
            fontWeight = FontWeight.SemiBold
        )
    }
}
