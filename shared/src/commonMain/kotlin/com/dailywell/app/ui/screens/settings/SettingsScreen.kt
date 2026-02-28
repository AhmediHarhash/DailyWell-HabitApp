package com.dailywell.app.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.LocalDailyWellColors
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.core.theme.Success
import com.dailywell.app.core.theme.Error
import com.dailywell.app.data.model.AIGovernancePolicy
import com.dailywell.app.data.model.AIPlanType
import com.dailywell.app.data.model.Habit
import com.dailywell.app.data.model.UserAIUsage
import com.dailywell.app.data.model.UserSettings
import com.dailywell.app.data.repository.AICoachingRepository
import com.dailywell.app.data.repository.HabitRepository
import com.dailywell.app.data.repository.SettingsRepository
import com.dailywell.app.domain.model.HabitType
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.ElevationLevel
import com.dailywell.app.ui.components.GlassCard
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import com.dailywell.app.ui.components.StaggeredItem
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToHabitStacking: (() -> Unit)? = null,
    onNavigateToIntentions: (() -> Unit)? = null,
    onNavigateToSmartReminders: (() -> Unit)? = null,
    onNavigateToAIUsageDetails: (() -> Unit)? = null,
    onNavigateToAIInsights: (() -> Unit)? = null,
    onNavigateToAudioCoaching: (() -> Unit)? = null,
    onNavigateToBiometric: (() -> Unit)? = null,
    onNavigateToAICoaching: (() -> Unit)? = null,
    onNavigateToGamification: (() -> Unit)? = null,
    onNavigateToSmartNotifications: (() -> Unit)? = null,
    onNavigateToCalendarIntegration: (() -> Unit)? = null,
    onNavigateToAtRiskAnalysis: (() -> Unit)? = null,
    // Account management
    onNavigateToAuth: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
    onDeleteAccount: (suspend () -> Result<Unit>)? = null,
    onChangePassword: (suspend (String, String) -> Result<Unit>)? = null,
    onUpdateDisplayName: (suspend (String) -> Result<Unit>)? = null,
    isSignedIn: Boolean = false,
    accountEmail: String? = null,
    accountDisplayName: String? = null,
    isEmailVerified: Boolean = false,
    authProvider: String = "none",
    isPremiumOverride: Boolean? = null,
    aiCoachingRepository: AICoachingRepository = koinInject(),
    settingsRepository: SettingsRepository = koinInject(),
    habitRepository: HabitRepository = koinInject()
) {
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(UserSettings()) }
    var enabledHabits by remember { mutableStateOf<Set<String>>(emptySet()) }
    var habits by remember { mutableStateOf<List<Habit>>(emptyList()) }
    var showCustomizeDialog by remember { mutableStateOf<HabitType?>(null) }
    var showAddCustomHabitDialog by remember { mutableStateOf(false) }

    // Debug mode: tap version 7 times to toggle premium
    var versionTapCount by remember { mutableStateOf(0) }
    var showDebugMessage by remember { mutableStateOf<String?>(null) }

    // Account management dialogs
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showEditDisplayNameDialog by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    val isPremiumUser = isPremiumOverride ?: settings.isPremium
    val aiUsage by aiCoachingRepository.getAIUsage().collectAsState(
        initial = UserAIUsage(
            userId = "local",
            planType = AIPlanType.FREE,
            resetDate = "next cycle",
            lastUpdated = ""
        )
    )

    // Collapsible section states for 2026 modern UX
    var isAdvancedFeaturesExpanded by remember { mutableStateOf(false) }
    var isAICoachExpanded by remember { mutableStateOf(true) }  // Expanded by default
    var isRemindersExpanded by remember { mutableStateOf(true) }  // Expanded by default

    // Get theme colors
    val dailyWellColors = LocalDailyWellColors.current

    LaunchedEffect(Unit) {
        settingsRepository.getSettings().collect {
            settings = it
            enabledHabits = it.enabledHabitIds.toSet()
        }
    }

    LaunchedEffect(Unit) {
        habitRepository.getAllHabits().collect {
            habits = it
        }
    }

    // Clear debug message after delay
    LaunchedEffect(showDebugMessage) {
        if (showDebugMessage != null) {
            kotlinx.coroutines.delay(2000)
            showDebugMessage = null
        }
    }

    // Customize habit dialog
    showCustomizeDialog?.let { habitType ->
        val habit = habits.find { it.id == habitType.id }
        val currentThreshold = settings.customThresholds[habitType.id] ?: habitType.defaultThreshold

        CustomizeHabitDialog(
            habitType = habitType,
            currentThreshold = currentThreshold,
            onDismiss = { showCustomizeDialog = null },
            onSave = { newThreshold ->
                scope.launch {
                    val updatedThresholds = settings.customThresholds + (habitType.id to newThreshold)
                    settingsRepository.updateSettings(settings.copy(customThresholds = updatedThresholds))
                }
                showCustomizeDialog = null
            }
        )
    }

    // Add custom habit dialog
    if (showAddCustomHabitDialog) {
        AddCustomHabitDialog(
            onDismiss = { showAddCustomHabitDialog = false },
            onSave = { name, emoji, threshold, question ->
                scope.launch {
                    habitRepository.createCustomHabit(name, emoji, threshold, question)
                }
                showAddCustomHabitDialog = false
            }
        )
    }

    // Change Password Dialog
    if (showChangePasswordDialog && onChangePassword != null) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onChangePassword = onChangePassword
        )
    }

    // Delete Account Dialog
    if (showDeleteAccountDialog && onDeleteAccount != null) {
        DeleteAccountDialog(
            onDismiss = { showDeleteAccountDialog = false },
            onDeleteAccount = {
                scope.launch {
                    val result = onDeleteAccount()
                    if (result.isSuccess) {
                        showDeleteAccountDialog = false
                    }
                }
            }
        )
    }

    // Edit Display Name Dialog
    if (showEditDisplayNameDialog && onUpdateDisplayName != null) {
        EditDisplayNameDialog(
            currentName = accountDisplayName ?: "",
            onDismiss = { showEditDisplayNameDialog = false },
            onSave = { newName ->
                scope.launch {
                    val result = onUpdateDisplayName(newName)
                    if (result.isSuccess) {
                        showEditDisplayNameDialog = false
                    }
                }
            }
        )
    }

    // Sign Out Confirmation
    if (showSignOutConfirm && onSignOut != null) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out? Your data will remain saved.") },
            confirmButton = {
                TextButton(onClick = {
                    onSignOut()
                    showSignOutConfirm = false
                }) {
                    Text("Sign Out", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Profile & Settings",
                    subtitle = "Profile, preferences, and account controls",
                    onNavigationClick = onBack
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Profile + AI wallet hub
            item {
                ProfileUsageHubCard(
                    isSignedIn = isSignedIn,
                    accountDisplayName = accountDisplayName,
                    accountEmail = accountEmail,
                    isEmailVerified = isEmailVerified,
                    isPremium = isPremiumUser,
                    aiUsage = aiUsage,
                    onPrimaryAction = when {
                        !isSignedIn -> onNavigateToAuth
                        isPremiumUser -> onNavigateToAIUsageDetails ?: onNavigateToAIInsights ?: onNavigateToAICoaching ?: onNavigateToPaywall
                        else -> onNavigateToPaywall
                    },
                    onPrimaryActionLabel = if (isSignedIn) {
                        if (isPremiumUser) "Usage Insights" else "Upgrade now"
                    } else {
                        "Sign in to sync"
                    }
                )
            }

                // Account section
                item {
                    PremiumSectionChip(
                        text = "Account Controls",
                        icon = DailyWellIcons.Auth.EditProfile
                    )
                }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = ElevationLevel.Subtle,
                    cornerRadius = 20.dp
                ) {
                    if (isSignedIn) {
                        // Signed in: show account actions
                        Column {
                            // Edit Profile
                            if (onUpdateDisplayName != null) {
                                FeatureRow(
                                    icon = DailyWellIcons.Auth.EditProfile,
                                    title = "Edit profile",
                                    subtitle = "Update display name and identity",
                                    onClick = { showEditDisplayNameDialog = true }
                                )
                            }

                            // Change Password (email provider only)
                            if (onChangePassword != null && authProvider == "email") {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                FeatureRow(
                                    icon = DailyWellIcons.Auth.PasswordChange,
                                    title = "Change password",
                                    subtitle = "Update your login credential",
                                    onClick = { showChangePasswordDialog = true }
                                )
                            }

                            // Sign Out
                            if (onSignOut != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showSignOutConfirm = true }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = DailyWellIcons.Auth.SignOut,
                                        contentDescription = "Sign Out",
                                        modifier = Modifier.size(24.dp),
                                        tint = Error
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Sign out",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Error
                                    )
                                }
                            }

                            // Delete Account
                            if (onDeleteAccount != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showDeleteAccountDialog = true }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = DailyWellIcons.Auth.Delete,
                                        contentDescription = "Delete Account",
                                        modifier = Modifier.size(24.dp),
                                        tint = Error
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Delete account",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Error
                                        )
                                        Text(
                                            text = "Permanently delete your account and data",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Not signed in: show sign-in prompt
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = onNavigateToAuth != null) {
                                    onNavigateToAuth?.invoke()
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = DailyWellIcons.Social.Person,
                                    contentDescription = "Sign In",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sign in",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Create account to sync coach + scan usage",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (onNavigateToAuth != null) {
                                Icon(
                                    imageVector = DailyWellIcons.Nav.ChevronRight,
                                    contentDescription = "Go",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Premium banner
            if (!isPremiumUser) {
                item {
                    PremiumBanner(onClick = onNavigateToPaywall)
                }
            }

                // Habits section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PremiumSectionChip(
                            text = "Habits",
                            icon = DailyWellIcons.Habits.HabitStacking
                        )
                        if (isPremiumUser) {
                            val customCount = habits.count { it.isCustom }
                            if (customCount < 3) {
                                TextButton(onClick = { showAddCustomHabitDialog = true }) {
                                    Text("+ Add Custom")
                                }
                            }
                        }
                    }
                }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = ElevationLevel.Subtle,
                    cornerRadius = 20.dp
                ) {
                    Column {
                        HabitType.entries.forEachIndexed { index, habitType ->
                            val isEnabled = enabledHabits.contains(habitType.id)
                            val maxHabitLimit = HabitType.entries.size
                            val canEnable = isEnabled || enabledHabits.size < maxHabitLimit
                            val customThreshold = settings.customThresholds[habitType.id]

                            HabitSettingItem(
                                habit = habitType,
                                isEnabled = isEnabled,
                                canToggle = canEnable,
                                isPremium = isPremiumUser,
                                customThreshold = customThreshold,
                                onToggle = {
                                    scope.launch {
                                        if (isEnabled) {
                                            settingsRepository.disableHabit(habitType.id)
                                            habitRepository.setHabitEnabled(habitType.id, false)
                                        } else if (canEnable) {
                                            settingsRepository.enableHabit(habitType.id)
                                            habitRepository.setHabitEnabled(habitType.id, true)
                                        }
                                    }
                                },
                                onCustomize = if (isPremiumUser) {
                                    { showCustomizeDialog = habitType }
                                } else null
                            )
                            if (index < HabitType.entries.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }
                        }

                        // Custom habits
                        val customHabits = habits.filter { it.isCustom }
                        if (customHabits.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 2.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                            customHabits.forEachIndexed { index, habit ->
                                CustomHabitSettingItem(
                                    habit = habit,
                                    onDelete = {
                                        scope.launch {
                                            habitRepository.deleteHabit(habit.id)
                                        }
                                    }
                                )
                                if (index < customHabits.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!isPremiumUser) {
                item {
                    Text(
                        text = "Core habits: ${enabledHabits.size}/${HabitType.entries.size} enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Premium Features section (Phase 2) - Collapsible
            if (isPremiumUser) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Animated chevron rotation
                    val chevronRotation by animateFloatAsState(
                        targetValue = if (isAdvancedFeaturesExpanded) 180f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "chevron"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isAdvancedFeaturesExpanded) {
                                    dailyWellColors.timeGradientStart.copy(alpha = 0.1f)
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                }
                            )
                            .clickable { isAdvancedFeaturesExpanded = !isAdvancedFeaturesExpanded }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = DailyWellIcons.Onboarding.Ready,
                                contentDescription = "Advanced Features",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Advanced Features",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = DailyWellIcons.Nav.ExpandMore,
                            contentDescription = "Expand",
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = chevronRotation },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = isAdvancedFeaturesExpanded,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn(),
                        exit = shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeOut()
                    ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = ElevationLevel.Subtle,
                        cornerRadius = 20.dp
                    ) {
                        Column {
                            // Habit Stacking
                            if (onNavigateToHabitStacking != null) {
                                FeatureRow(
                                    icon = DailyWellIcons.Habits.HabitStacking,
                                    title = "Habit Stacking",
                                    subtitle = "Chain habits together for 3.2x success",
                                    onClick = onNavigateToHabitStacking
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }

                            // Implementation Intentions
                            if (onNavigateToIntentions != null) {
                                FeatureRow(
                                    icon = DailyWellIcons.Habits.Intentions,
                                    title = "If-Then Plans",
                                    subtitle = "\"When X, I will Y\" planning",
                                    onClick = onNavigateToIntentions
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }

                            // Smart Reminders
                            if (onNavigateToSmartReminders != null) {
                                FeatureRow(
                                    icon = DailyWellIcons.Habits.SmartReminders,
                                    title = "Smart Reminders",
                                    subtitle = "AI-optimized timing for nudges",
                                    onClick = onNavigateToSmartReminders
                                )
                            }

                            // Phase 3 Features
                            // AI Insights
                            if (onNavigateToAIInsights != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                FeatureRow(
                                    icon = DailyWellIcons.Misc.Sparkle,
                                    title = "AI Insights",
                                    subtitle = "Pattern recognition & predictions",
                                    onClick = onNavigateToAIInsights
                                )
                            }

                            // Audio Coaching
                            if (onNavigateToAudioCoaching != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                FeatureRow(
                                    icon = DailyWellIcons.Coaching.Audio,
                                    title = "Audio Coaching",
                                    subtitle = "2-3 min micro-lessons",
                                    onClick = onNavigateToAudioCoaching
                                )
                            }

                            // Phase 4 Features
                            // Biometric Dashboard
                            if (onNavigateToBiometric != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                FeatureRow(
                                    icon = DailyWellIcons.Health.Biometric,
                                    title = "Biometrics",
                                    subtitle = "Sleep, HRV & recovery insights",
                                    onClick = onNavigateToBiometric
                                )
                            }

                            // AI Coaching
                            if (onNavigateToAICoaching != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                FeatureRow(
                                    icon = DailyWellIcons.Coaching.AICoach,
                                    title = "AI Coach",
                                    subtitle = "Personalized coaching sessions",
                                    onClick = onNavigateToAICoaching
                                )
                            }

                            // Phase 5 - Gamification
                            if (onNavigateToGamification != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                FeatureRow(
                                    icon = DailyWellIcons.Gamification.Trophy,
                                    title = "Rewards & Progress",
                                    subtitle = "XP, badges, daily rewards & more",
                                    onClick = onNavigateToGamification
                                )
                            }

                        }
                    }
                    }  // End AnimatedVisibility
                }
            }

                // Smart Notifications section (available to all users)
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    PremiumSectionChip(
                        text = "AI Coach",
                        icon = DailyWellIcons.Coaching.AICoach
                    )
                }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = ElevationLevel.Subtle,
                    cornerRadius = 20.dp
                ) {
                    Column {
                        // Smart Notifications
                        if (onNavigateToSmartNotifications != null) {
                            FeatureRow(
                                icon = DailyWellIcons.Coaching.SmartNotification,
                                title = "Smart Notifications",
                                subtitle = "AI-powered proactive nudges and check-ins",
                                onClick = onNavigateToSmartNotifications
                            )
                        }

                        // Calendar Integration
                        if (onNavigateToCalendarIntegration != null) {
                            HorizontalDivider()
                            FeatureRow(
                                icon = DailyWellIcons.Analytics.Calendar,
                                title = "Calendar Integration",
                                subtitle = "Sync with Google/Outlook for smart scheduling",
                                onClick = onNavigateToCalendarIntegration
                            )
                        }

                        // At-Risk Analysis
                        if (onNavigateToAtRiskAnalysis != null) {
                            HorizontalDivider()
                            FeatureRow(
                                icon = DailyWellIcons.Analytics.AtRisk,
                                title = "Risk Analysis",
                                subtitle = "Predict & prevent habit failures before they happen",
                                onClick = onNavigateToAtRiskAnalysis
                            )
                        }
                    }
                }
            }

                // Reminders section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    PremiumSectionChip(
                        text = "Reminders",
                        icon = DailyWellIcons.Habits.SmartReminders
                    )
                }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = ElevationLevel.Subtle,
                    cornerRadius = 20.dp
                ) {
                    Column {
                        // Reminder enabled toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Daily Reminder",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Get reminded to check in",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.reminderEnabled,
                                onCheckedChange = {
                                    scope.launch {
                                        settingsRepository.setReminderEnabled(it)
                                    }
                                }
                            )
                        }

                        if (settings.reminderEnabled) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )

                            // Reminder time
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reminder Time",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                val hour = settings.reminderHour
                                val period = if (hour < 12) "AM" else "PM"
                                val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                                Text(
                                    text = "$displayHour:${String.format("%02d", settings.reminderMinute)} $period",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

                // About section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    PremiumSectionChip(
                        text = "About",
                        icon = DailyWellIcons.Misc.Sparkle
                    )
                }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = ElevationLevel.Subtle,
                    cornerRadius = 20.dp
                ) {
                    Column {
                        // Version row - tap 7 times to toggle premium (debug feature)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    versionTapCount++
                                    if (versionTapCount >= 7) {
                                        versionTapCount = 0
                                        scope.launch {
                                            val newPremiumState = !settings.isPremium
                                            settingsRepository.setPremium(newPremiumState)
                                            showDebugMessage = if (newPremiumState) {
                                                "Premium ENABLED for testing"
                                            } else {
                                                "Premium DISABLED"
                                            }
                                        }
                                    } else if (versionTapCount >= 4) {
                                        showDebugMessage = "${7 - versionTapCount} more taps..."
                                    }
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Version",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = showDebugMessage ?: "1.0.0",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (showDebugMessage != null) Success else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        SettingsRow(
                            title = "Philosophy",
                            subtitle = "Consistency > Perfection"
                        )
                    }
                }
            }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileUsageHubCard(
    isSignedIn: Boolean,
    accountDisplayName: String?,
    accountEmail: String?,
    isEmailVerified: Boolean,
    isPremium: Boolean,
    aiUsage: UserAIUsage,
    onPrimaryAction: (() -> Unit)?,
    onPrimaryActionLabel: String
) {
    val displayName = if (isSignedIn) {
        accountDisplayName?.takeIf { it.isNotBlank() }
            ?: accountEmail?.substringBefore("@")
            ?: "You"
    } else {
        "Guest"
    }
    val subtitle = if (isSignedIn) {
        accountEmail ?: "Signed in"
    } else {
        "Sign in to sync your profile, coach sessions, and AI usage."
    }

    val usagePercent = max(aiUsage.percentUsed, aiUsage.costPercentUsed).coerceIn(0f, 100f)
    val progress = (usagePercent / 100f).coerceIn(0f, 1f)
    val usedPercent = usagePercent.roundToInt().coerceIn(0, 100)
    val availablePercent = (100 - usedPercent).coerceAtLeast(0)
    val localChat = aiUsage.localMessagesCount
    val cloudChat = aiUsage.cloudChatCalls
    val scanCalls = aiUsage.cloudScanCalls
    val reportCalls = aiUsage.cloudReportCalls
    val totalCloud = aiUsage.cloudTotalCalls
    val budgetBadgeText = if (isPremium) "PREMIUM" else "FREE"
    val planText = if (isPremium && aiUsage.planType == AIPlanType.FREE) {
        "Premium mode"
    } else {
        aiUsage.planType.displayName
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = displayName.firstOrNull()?.uppercase() ?: "Y"
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isEmailVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = DailyWellIcons.Auth.Verified,
                                contentDescription = "Verified",
                                modifier = Modifier.size(14.dp),
                                tint = Success
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isPremium) Success.copy(alpha = 0.2f) else Primary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = budgetBadgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPremium) Success else Primary
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI usage",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = planText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = if (progress >= 0.8f) Error else Success,
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
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UsageStatChip(
                    modifier = Modifier.weight(1f),
                    label = "Local chat",
                    value = localChat.toString(),
                    tint = Success
                )
                UsageStatChip(
                    modifier = Modifier.weight(1f),
                    label = "Cloud chat",
                    value = cloudChat.toString(),
                    tint = Primary
                )
                UsageStatChip(
                    modifier = Modifier.weight(1f),
                    label = "Scans",
                    value = scanCalls.toString(),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UsageStatChip(
                    modifier = Modifier.weight(1f),
                    label = "Reports",
                    value = reportCalls.toString(),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                UsageStatChip(
                    modifier = Modifier.weight(1f),
                    label = "Cloud total",
                    value = totalCloud.toString(),
                    tint = Primary
                )
                UsageStatChip(
                    modifier = Modifier.weight(1f),
                    label = "Reset",
                    value = aiUsage.resetDate,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            Text(
                text = if (isPremium) {
                    "One AI wallet across chat, scan, and reports."
                } else {
                    "Free scan cap: ${AIGovernancePolicy.FREE_SCAN_LIMIT_PER_MONTH}/month. Trial cap: ${AIGovernancePolicy.TRIAL_SCAN_LIMIT_TOTAL} total."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (onPrimaryAction != null) {
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPremium) Primary.copy(alpha = 0.9f) else Success
                    )
                ) {
                    Text(onPrimaryActionLabel)
                }
            }
        }
    }
}

@Composable
private fun UsageStatChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    tint: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
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

@Composable
private fun PremiumBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Success.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = DailyWellIcons.Status.Premium,
                contentDescription = "Premium",
                modifier = Modifier.size(32.dp),
                tint = Success
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upgrade to Premium",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Success
                )
                Text(
                    text = "All ${HabitType.entries.size} habits • Full history • Insights",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = DailyWellIcons.Nav.ChevronRight,
                contentDescription = "Go",
                modifier = Modifier.size(24.dp),
                tint = Success
            )
        }
    }
}

@Composable
private fun HabitSettingItem(
    habit: HabitType,
    isEnabled: Boolean,
    canToggle: Boolean,
    isPremium: Boolean,
    customThreshold: String?,
    onToggle: () -> Unit,
    onCustomize: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canToggle) { onToggle() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = DailyWellIcons.getHabitIcon(habit.id),
            contentDescription = habit.displayName,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = if (canToggle) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = customThreshold ?: habit.defaultThreshold,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (customThreshold != null) Success else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onCustomize != null && isEnabled) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onCustomize() }
                    )
                }
            }
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = { if (canToggle) onToggle() },
            enabled = canToggle
        )
    }
}

@Composable
private fun CustomHabitSettingItem(
    habit: Habit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = DailyWellIcons.Habits.Custom,
            contentDescription = habit.name,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = habit.threshold,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = DailyWellIcons.Actions.Delete,
                contentDescription = "Delete habit",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun CustomizeHabitDialog(
    habitType: HabitType,
    currentThreshold: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var threshold by remember { mutableStateOf(currentThreshold) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DailyWellIcons.getHabitIcon(habitType.id),
                    contentDescription = habitType.displayName,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Customize ${habitType.displayName}")
            }
        },
        text = {
            Column {
                Text(
                    text = "What counts as \"done\" for you?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = threshold,
                    onValueChange = { threshold = it },
                    label = { Text("Your goal") },
                    placeholder = { Text(habitType.defaultThreshold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Default: ${habitType.defaultThreshold}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(threshold) },
                enabled = threshold.isNotBlank()
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
private fun AddCustomHabitDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, threshold: String, question: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("✨") }
    var threshold by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { if (it.length <= 2) emoji = it },
                        label = { Text("Emoji") },
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("e.g., Read") }
                    )
                }
                OutlinedTextField(
                    value = threshold,
                    onValueChange = { threshold = it },
                    label = { Text("Goal") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g., 20+ minutes") }
                )
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Check-in question") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g., Did you read today?") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name,
                        emoji.ifBlank { "✨" },
                        threshold.ifBlank { "Daily" },
                        question.ifBlank { "Did you complete $name?" }
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
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
private fun SettingsRow(
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = DailyWellIcons.Nav.ChevronRight,
            contentDescription = "Navigate",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== ACCOUNT MANAGEMENT DIALOGS ====================

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onChangePassword: suspend (String, String) -> Result<Unit>
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DailyWellIcons.Auth.PasswordChange,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Password")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it; errorMessage = null },
                    label = { Text("Current Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMessage = null },
                    label = { Text("New Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
                errorMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        currentPassword.isBlank() -> errorMessage = "Enter current password"
                        newPassword.length < 6 -> errorMessage = "New password must be at least 6 characters"
                        newPassword != confirmPassword -> errorMessage = "Passwords don't match"
                        else -> {
                            isLoading = true
                            scope.launch {
                                val result = onChangePassword(currentPassword, newPassword)
                                isLoading = false
                                result.fold(
                                    onSuccess = { onDismiss() },
                                    onFailure = { errorMessage = it.message }
                                )
                            }
                        }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Update")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    var confirmText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DailyWellIcons.Auth.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Account", color = Error)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "This will permanently delete your account and all associated data. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Type DELETE to confirm:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("DELETE") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDeleteAccount,
                enabled = confirmText == "DELETE"
            ) {
                Text("Delete Account", color = if (confirmText == "DELETE") Error else Error.copy(alpha = 0.4f))
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
private fun EditDisplayNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DailyWellIcons.Auth.EditProfile,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Display Name")
            }
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name) },
                enabled = name.isNotBlank() && name != currentName
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
