package com.dailywell.app.domain.model

import androidx.compose.ui.graphics.Color
import com.dailywell.app.core.theme.*

enum class HabitType(
    val id: String,
    val displayName: String,
    val emoji: String,
    val defaultThreshold: String,
    val description: String,
    val color: Color,
    val question: String
) {
    // "Rest" feels restorative and warm vs clinical "Sleep"
    SLEEP(
        id = "sleep",
        displayName = "Rest",
        emoji = "😴",
        defaultThreshold = "7+ hours",
        description = "Quality rest for recovery",
        color = SleepColor,
        question = "Did you get 7+ hours of rest?"
    ),
    // "Hydrate" is an action verb - empowers the user
    WATER(
        id = "water",
        displayName = "Hydrate",
        emoji = "💧",
        defaultThreshold = "8+ glasses",
        description = "Stay hydrated throughout the day",
        color = WaterColor,
        question = "Did you stay hydrated today?"
    ),
    // "Move" already perfect - action verb
    MOVE(
        id = "move",
        displayName = "Move",
        emoji = "🏃",
        defaultThreshold = "30+ minutes",
        description = "Any physical activity counts",
        color = MoveColor,
        question = "Did you move for 30+ minutes?"
    ),
    // "Nourish" is aspirational vs restrictive "Vegetables"
    VEGETABLES(
        id = "vegetables",
        displayName = "Nourish",
        emoji = "🥬",
        defaultThreshold = "Ate greens",
        description = "Fuel your body with good food",
        color = VegetablesColor,
        question = "Did you nourish yourself with greens today?"
    ),
    // "Calm" already perfect - emotional state
    CALM(
        id = "calm",
        displayName = "Calm",
        emoji = "🧘",
        defaultThreshold = "Any practice",
        description = "A moment of peace for yourself",
        color = CalmColor,
        question = "Did you take time to feel calm today?"
    ),
    // "Connect" already perfect - action verb
    CONNECT(
        id = "connect",
        displayName = "Connect",
        emoji = "💬",
        defaultThreshold = "Talked to someone",
        description = "Meaningful human connection",
        color = ConnectColor,
        question = "Did you connect with someone today?"
    ),
    // "Unplug" already perfect - action verb
    UNPLUG(
        id = "unplug",
        displayName = "Unplug",
        emoji = "📵",
        defaultThreshold = "Screen-free before bed",
        description = "Give your mind a break",
        color = UnplugColor,
        question = "Did you unplug before bed?"
    );

    companion object {
        fun fromId(id: String): HabitType? = entries.find { it.id == id }

        val allHabits: List<HabitType> = entries.toList()
    }
}
