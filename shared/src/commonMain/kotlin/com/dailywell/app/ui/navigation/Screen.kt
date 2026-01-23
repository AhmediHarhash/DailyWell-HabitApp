package com.dailywell.app.ui.navigation

sealed class Screen(val route: String) {
    // Main screens (bottom nav)
    // "Check-In" - action-oriented, present-focused
    data object Today : Screen("today")
    // "Journey" - progress narrative, growth-focused
    data object Week : Screen("week")
    // "Patterns" - less clinical than "Insights", discovery-focused
    data object Patterns : Screen("patterns")

    // Other screens
    data object Onboarding : Screen("onboarding")
    // "My Habits" - personal ownership language
    data object MyHabits : Screen("my_habits")
    data object Premium : Screen("premium") // Renamed from "paywall" - more aspirational
    data object Milestones : Screen("milestones") // Renamed from "achievements" - journey language
    data object WeeklyReflection : Screen("weekly_reflection")
    data object CustomHabit : Screen("custom_habit")

    companion object {
        val bottomNavScreens = listOf(Today, Week, Patterns)
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: String, // We'll use text emoji for simplicity, can be replaced with icons
    val isPremium: Boolean = false
)

val bottomNavItems = listOf(
    // "Check-In" with checkmark - action-oriented, invites completion
    BottomNavItem(Screen.Today, "Check-In", "✓"),
    // "Journey" with growth chart - progress narrative, forward-looking
    BottomNavItem(Screen.Week, "Journey", "📈"),
    // "Patterns" with crystal ball - discovery, self-knowledge (less clinical than "Insights")
    BottomNavItem(Screen.Patterns, "Patterns", "🔮", isPremium = true)
)
