package com.dailywell.app.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.dailywell.app.ui.components.DailyWellIcons
import org.jetbrains.compose.resources.DrawableResource

/**
 * All screens in the app using sealed class for type safety.
 * 5 bottom nav tabs: Today, Insights, Track, Coach, You
 */
sealed class Screen(val route: String) {
    // ============== BOTTOM NAV TABS (5 tabs) ==============
    data object Today : Screen("today")           // Daily hub: habits, mood, quick actions
    data object Insights : Screen("insights")     // Analytics, patterns, AI insights
    data object Track : Screen("track")           // Food scan, nutrition, workouts, body, water
    data object Coach : Screen("coach")           // AI chat, audio, recovery
    data object You : Screen("you")               // Profile, achievements, rewards, settings

    // ============== FEATURE SCREENS ==============
    // Build Better Habits
    data object MyHabits : Screen("my_habits")
    data object HabitStacking : Screen("habit_stacking")
    data object Intentions : Screen("intentions")
    data object SmartReminders : Screen("smart_reminders")
    data object Recovery : Screen("recovery")
    data object CustomHabit : Screen("custom_habit")

    // Analytics & Insights
    data object Patterns : Screen("patterns")      // Insights/patterns
    data object AIInsights : Screen("ai_insights")
    data object AIUsageDetails : Screen("ai_usage_details")
    data object AIObservability : Screen("ai_observability")
    data object Calendar : Screen("calendar")
    data object AtRisk : Screen("at_risk")
    data object Reflections : Screen("reflections")

    // AI & Coaching
    data object AICoaching : Screen("ai_coaching")
    data object AudioCoaching : Screen("audio_coaching")
    data object SmartNotifications : Screen("smart_notifications")

    // Rewards
    data object Gamification : Screen("gamification")

    // Health & Body
    data object Biometric : Screen("biometric")
    data object WaterTracking : Screen("water_tracking")
    data object HealthConnect : Screen("health_connect")

    // Nutrition & Fitness
    data object FoodScanning : Screen("food_scanning")      // AI Photo Scanner
    data object Nutrition : Screen("nutrition")              // Meal Tracking
    data object WorkoutLog : Screen("workout_log")           // Exercise Logging
    data object WorkoutHistory : Screen("workout_history")
    data object BodyMetrics : Screen("body_metrics")         // Weight & Measurements
    data object Measurements : Screen("measurements")
    data object ProgressPhotos : Screen("progress_photos")

    // Other
    data object Onboarding : Screen("onboarding")
    data object Auth : Screen("auth")
    data object Milestones : Screen("milestones")
    data object Premium : Screen("premium")
    data object Settings : Screen("settings")

    companion object {
        val bottomNavScreens = listOf(Today, Insights, Track, Coach, You)
    }
}

/**
 * Bottom navigation item definition
 * Uses Material Icon references instead of emoji strings
 */
data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val iconResource: DrawableResource? = null,
    val isPremium: Boolean = false
)

/**
 * 5-tab bottom navigation
 * [Today] [Insights] [Track] [Coach] [You]
 */
val bottomNavItems = listOf(
    BottomNavItem(Screen.Today, "Today", DailyWellIcons.Nav.Today),
    BottomNavItem(Screen.Insights, "Insights", DailyWellIcons.Nav.Insights),
    BottomNavItem(Screen.Track, "Track", DailyWellIcons.Nav.Track),
    BottomNavItem(Screen.Coach, "Coach", DailyWellIcons.Nav.Coach),
    BottomNavItem(Screen.You, "You", DailyWellIcons.Nav.You)
)

/**
 * Feature definitions organized by the 5 tabs
 */
enum class FeatureId(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val screen: Screen,
    val isPremium: Boolean = true,
    val category: FeatureCategory
) {
    // Build Better Habits (Today tab features)
    HABIT_STACKING(
        "Habit Stacking",
        "Chain habits together",
        DailyWellIcons.Habits.HabitStacking,
        Screen.HabitStacking,
        true,
        FeatureCategory.BUILD_HABITS
    ),
    INTENTIONS(
        "Daily Intentions",
        "Set your focus",
        DailyWellIcons.Habits.Intentions,
        Screen.Intentions,
        true,
        FeatureCategory.BUILD_HABITS
    ),
    SMART_REMINDERS(
        "Smart Reminders",
        "AI-timed notifications",
        DailyWellIcons.Habits.SmartReminders,
        Screen.SmartReminders,
        true,
        FeatureCategory.BUILD_HABITS
    ),
    RECOVERY(
        "Recovery Mode",
        "Get back on track",
        DailyWellIcons.Habits.Recovery,
        Screen.Recovery,
        true,
        FeatureCategory.BUILD_HABITS
    ),

    // Insights tab features
    PATTERNS(
        "Pattern Analysis",
        "Discover your trends",
        DailyWellIcons.Analytics.Pattern,
        Screen.Patterns,
        true,
        FeatureCategory.INSIGHTS
    ),
    AI_INSIGHTS(
        "AI Insights",
        "Personalized discoveries",
        DailyWellIcons.Coaching.AICoach,
        Screen.AIInsights,
        true,
        FeatureCategory.INSIGHTS
    ),
    CALENDAR(
        "Calendar Sync",
        "See habits in calendar",
        DailyWellIcons.Analytics.Calendar,
        Screen.Calendar,
        true,
        FeatureCategory.INSIGHTS
    ),
    AT_RISK(
        "At-Risk Alerts",
        "Prevent habit breaks",
        DailyWellIcons.Analytics.AtRisk,
        Screen.AtRisk,
        true,
        FeatureCategory.INSIGHTS
    ),

    // Coach tab features
    AI_COACHING(
        "AI Coach",
        "Personal habit advisor",
        DailyWellIcons.Coaching.AICoach,
        Screen.AICoaching,
        true,
        FeatureCategory.COACHING
    ),
    AUDIO_COACHING(
        "Audio Sessions",
        "Guided meditations",
        DailyWellIcons.Coaching.Audio,
        Screen.AudioCoaching,
        true,
        FeatureCategory.COACHING
    ),
    SMART_NOTIFICATIONS(
        "Smart Nudges",
        "Proactive reminders",
        DailyWellIcons.Coaching.SmartNotification,
        Screen.SmartNotifications,
        false,
        FeatureCategory.COACHING
    ),

    // Track tab features
    FOOD_SCANNING(
        "AI Food Scanner",
        "Snap & log meals instantly",
        DailyWellIcons.Health.FoodScan,
        Screen.FoodScanning,
        true,
        FeatureCategory.TRACKING
    ),
    NUTRITION(
        "Nutrition Tracker",
        "Track calories & macros",
        DailyWellIcons.Health.Nutrition,
        Screen.Nutrition,
        true,
        FeatureCategory.TRACKING
    ),
    WORKOUT_LOG(
        "Workout Log",
        "Log exercises & track PRs",
        DailyWellIcons.Health.Workout,
        Screen.WorkoutLog,
        true,
        FeatureCategory.TRACKING
    ),
    BODY_METRICS(
        "Body Metrics",
        "Weight, measurements & photos",
        DailyWellIcons.Health.Weight,
        Screen.BodyMetrics,
        true,
        FeatureCategory.TRACKING
    ),
    WATER_TRACKING(
        "Water Tracker",
        "Stay hydrated daily",
        DailyWellIcons.Health.WaterDrop,
        Screen.WaterTracking,
        false,
        FeatureCategory.TRACKING
    ),
    BIOMETRIC(
        "Biometrics",
        "Sleep, HRV & recovery",
        DailyWellIcons.Health.Biometric,
        Screen.Biometric,
        true,
        FeatureCategory.TRACKING
    ),

    // You tab / personal motivation only
    GAMIFICATION(
        "Rewards",
        "Points, streaks, and badges",
        DailyWellIcons.Gamification.Reward,
        Screen.Gamification,
        true,
        FeatureCategory.SOCIAL
    )
}

enum class FeatureCategory(val title: String) {
    BUILD_HABITS("Build Better Habits"),
    INSIGHTS("Insights & Analytics"),
    COACHING("AI & Coaching"),
    TRACKING("Health & Tracking"),
    SOCIAL("Rewards & Progress")
}
