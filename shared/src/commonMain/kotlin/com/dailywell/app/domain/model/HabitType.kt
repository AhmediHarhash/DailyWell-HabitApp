package com.dailywell.app.domain.model

import androidx.compose.ui.graphics.Color
import com.dailywell.app.core.theme.*

/**
 * Time of day for habit grouping - 2026 UX standard
 * Competitors like Habitify, Streaks group habits by time
 */
enum class TimeOfDay(
    val displayName: String,
    val emoji: String,
    val order: Int
) {
    MORNING("Morning", "🌅", 0),
    AFTERNOON("Afternoon", "☀️", 1),
    EVENING("Evening", "🌙", 2),
    ANYTIME("Anytime", "⏰", 3)
}

enum class HabitType(
    val id: String,
    val displayName: String,
    val emoji: String,
    val defaultThreshold: String,
    val description: String,
    val color: Color,
    val question: String,
    val preferredTime: TimeOfDay
) {
    // "Rest" feels restorative and warm vs clinical "Sleep"
    SLEEP(
        id = "sleep",
        displayName = "Rest",
        emoji = "😴",
        defaultThreshold = "7+ hours",
        description = "Quality rest for recovery",
        color = SleepColor,
        question = "Did you get 7+ hours of rest?",
        preferredTime = TimeOfDay.EVENING  // Log before bed
    ),
    // "Hydrate" is an action verb - empowers the user
    WATER(
        id = "water",
        displayName = "Hydrate",
        emoji = "💧",
        defaultThreshold = "8+ glasses",
        description = "Stay hydrated throughout the day",
        color = WaterColor,
        question = "Did you stay hydrated today?",
        preferredTime = TimeOfDay.ANYTIME  // Throughout day
    ),
    // "Move" already perfect - action verb
    MOVE(
        id = "move",
        displayName = "Move",
        emoji = "🏃",
        defaultThreshold = "30+ minutes",
        description = "Any physical activity counts",
        color = MoveColor,
        question = "Did you move for 30+ minutes?",
        preferredTime = TimeOfDay.MORNING  // Exercise in morning
    ),
    // "Nourish" is aspirational vs restrictive "Vegetables"
    VEGETABLES(
        id = "vegetables",
        displayName = "Nourish",
        emoji = "🥬",
        defaultThreshold = "Ate greens",
        description = "Fuel your body with good food",
        color = VegetablesColor,
        question = "Did you nourish yourself with greens today?",
        preferredTime = TimeOfDay.AFTERNOON  // Lunch/dinner
    ),
    // "Calm" already perfect - emotional state
    CALM(
        id = "calm",
        displayName = "Calm",
        emoji = "🧘",
        defaultThreshold = "Any practice",
        description = "A moment of peace for yourself",
        color = CalmColor,
        question = "Did you take time to feel calm today?",
        preferredTime = TimeOfDay.EVENING  // Wind down
    ),
    // "Connect" already perfect - action verb
    CONNECT(
        id = "connect",
        displayName = "Connect",
        emoji = "💬",
        defaultThreshold = "Talked to someone",
        description = "Meaningful human connection",
        color = ConnectColor,
        question = "Did you connect with someone today?",
        preferredTime = TimeOfDay.AFTERNOON  // Social time
    ),
    // "Unplug" already perfect - action verb
    UNPLUG(
        id = "unplug",
        displayName = "Unplug",
        emoji = "📵",
        defaultThreshold = "Screen-free before bed",
        description = "Give your mind a break",
        color = UnplugColor,
        question = "Did you unplug before bed?",
        preferredTime = TimeOfDay.EVENING  // Before sleep
    ),
    // New habits for goal-based onboarding
    FOCUS(
        id = "focus",
        displayName = "Focus",
        emoji = "🎯",
        defaultThreshold = "25+ min deep work",
        description = "Sharpen your concentration",
        color = FocusColor,
        question = "Did you do focused deep work today?",
        preferredTime = TimeOfDay.MORNING
    ),
    LEARN(
        id = "learn",
        displayName = "Learn",
        emoji = "📖",
        defaultThreshold = "Read or study",
        description = "Feed your curiosity daily",
        color = LearnColor,
        question = "Did you learn something new today?",
        preferredTime = TimeOfDay.EVENING
    ),
    GRATITUDE(
        id = "gratitude",
        displayName = "Gratitude",
        emoji = "🙏",
        defaultThreshold = "3 things grateful for",
        description = "Notice what's already good",
        color = GratitudeColor,
        question = "Did you practice gratitude today?",
        preferredTime = TimeOfDay.EVENING
    ),
    NATURE(
        id = "nature",
        displayName = "Nature",
        emoji = "🌿",
        defaultThreshold = "Time outdoors",
        description = "Reconnect with the natural world",
        color = NatureColor,
        question = "Did you spend time in nature today?",
        preferredTime = TimeOfDay.AFTERNOON
    ),
    BREATHE(
        id = "breathe",
        displayName = "Breathe",
        emoji = "🌬️",
        defaultThreshold = "Breathing exercise",
        description = "Calm your nervous system",
        color = BreatheColor,
        question = "Did you do a breathing exercise today?",
        preferredTime = TimeOfDay.ANYTIME
    );

    companion object {
        fun fromId(id: String): HabitType? = entries.find { it.id == id }

        val allHabits: List<HabitType> = entries.toList()
    }
}
