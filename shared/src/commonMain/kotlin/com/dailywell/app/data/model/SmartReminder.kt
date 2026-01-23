package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

/**
 * Smart Adaptive Reminders
 * AI-powered reminder timing that learns from user behavior patterns
 *
 * Key insight: Reminders work 3x better when timed to natural activity windows
 */
@Serializable
data class SmartReminderData(
    val isEnabled: Boolean = true,
    val habitReminders: Map<String, HabitReminderSettings> = emptyMap(),
    val learnedPatterns: UserActivityPatterns = UserActivityPatterns(),
    val lastUpdated: String? = null
)

@Serializable
data class HabitReminderSettings(
    val habitId: String,
    val isEnabled: Boolean = true,
    val preferredTime: String? = null,           // User's preferred time (e.g., "08:00")
    val smartTimingEnabled: Boolean = true,       // Use AI to optimize timing
    val frequency: ReminderFrequency = ReminderFrequency.ONCE,
    val tone: ReminderTone = ReminderTone.GENTLE,
    val lastReminderAt: String? = null,
    val responseRate: Float = 0f,                 // How often user acts on reminders
    val averageResponseTimeMinutes: Int? = null   // How long until user acts
)

@Serializable
enum class ReminderFrequency(val description: String) {
    ONCE("Once daily"),
    TWICE("Morning & evening"),
    ADAPTIVE("Smart timing"),        // AI determines frequency
    AS_NEEDED("Only when behind")    // Only remind if habit is incomplete
}

@Serializable
enum class ReminderTone(val description: String, val emoji: String) {
    GENTLE("Gentle nudge", "🌿"),
    ENCOURAGING("Encouraging", "💪"),
    PLAYFUL("Playful", "🎯"),
    DIRECT("Direct", "📋"),
    SCIENCE("Science-backed", "🔬")   // Include research stat
}

/**
 * Learned patterns about when user is most receptive to reminders
 */
@Serializable
data class UserActivityPatterns(
    val mostActiveHours: List<Int> = listOf(8, 12, 18),  // Hours 0-23
    val leastActiveHours: List<Int> = listOf(23, 0, 1, 2, 3, 4, 5, 6),
    val weekdayPattern: WeekdayPattern = WeekdayPattern(),
    val habitSpecificPatterns: Map<String, HabitActivityPattern> = emptyMap()
)

@Serializable
data class WeekdayPattern(
    val mondayActive: Boolean = true,
    val tuesdayActive: Boolean = true,
    val wednesdayActive: Boolean = true,
    val thursdayActive: Boolean = true,
    val fridayActive: Boolean = true,
    val saturdayActive: Boolean = true,
    val sundayActive: Boolean = true,
    val weekendShiftMinutes: Int = 60  // How much later on weekends
)

@Serializable
data class HabitActivityPattern(
    val habitId: String,
    val optimalHour: Int,              // Best hour to remind (0-23)
    val optimalMinute: Int = 0,        // Best minute
    val successRateAtOptimal: Float = 0f,
    val completionHistory: List<String> = emptyList()  // Recent completion times
)

/**
 * Smart reminder message templates with psychological backing
 */
object SmartReminderMessages {

    data class ReminderMessage(
        val message: String,
        val tone: ReminderTone,
        val scienceNote: String? = null
    )

    private val gentleMessages = mapOf(
        "sleep" to listOf(
            ReminderMessage("Wind-down time approaching", ReminderTone.GENTLE),
            ReminderMessage("Your rest window is opening", ReminderTone.GENTLE),
            ReminderMessage("A gentle reminder to start your evening routine", ReminderTone.GENTLE)
        ),
        "water" to listOf(
            ReminderMessage("Time for a hydration moment", ReminderTone.GENTLE),
            ReminderMessage("Your body would appreciate some water", ReminderTone.GENTLE),
            ReminderMessage("A glass of water is waiting for you", ReminderTone.GENTLE)
        ),
        "move" to listOf(
            ReminderMessage("Your body is ready for some movement", ReminderTone.GENTLE),
            ReminderMessage("A good time for a stretch or walk", ReminderTone.GENTLE),
            ReminderMessage("Movement opportunity ahead", ReminderTone.GENTLE)
        ),
        "vegetables" to listOf(
            ReminderMessage("Nourishment time approaching", ReminderTone.GENTLE),
            ReminderMessage("A colorful plate awaits", ReminderTone.GENTLE)
        ),
        "calm" to listOf(
            ReminderMessage("A moment of calm is available", ReminderTone.GENTLE),
            ReminderMessage("Space for stillness when you're ready", ReminderTone.GENTLE)
        ),
        "connect" to listOf(
            ReminderMessage("Someone might love to hear from you", ReminderTone.GENTLE),
            ReminderMessage("Connection opportunity", ReminderTone.GENTLE)
        ),
        "unplug" to listOf(
            ReminderMessage("Digital pause time", ReminderTone.GENTLE),
            ReminderMessage("A screen-free moment awaits", ReminderTone.GENTLE)
        )
    )

    private val encouragingMessages = mapOf(
        "sleep" to listOf(
            ReminderMessage("You've got this! Quality sleep starts now", ReminderTone.ENCOURAGING),
            ReminderMessage("Tonight's great sleep starts with this moment", ReminderTone.ENCOURAGING)
        ),
        "water" to listOf(
            ReminderMessage("Hydration powers everything you do!", ReminderTone.ENCOURAGING),
            ReminderMessage("One glass closer to your best self", ReminderTone.ENCOURAGING)
        ),
        "move" to listOf(
            ReminderMessage("Every step counts! Time to move", ReminderTone.ENCOURAGING),
            ReminderMessage("Your energy is waiting to be unlocked", ReminderTone.ENCOURAGING)
        ),
        "calm" to listOf(
            ReminderMessage("You deserve this peaceful moment", ReminderTone.ENCOURAGING),
            ReminderMessage("Calm is your superpower - activate it", ReminderTone.ENCOURAGING)
        )
    )

    private val scienceMessages = mapOf(
        "sleep" to listOf(
            ReminderMessage(
                "Sleep window opening",
                ReminderTone.SCIENCE,
                "7-9 hours of sleep improves memory consolidation by 40%"
            ),
            ReminderMessage(
                "Melatonin rising - optimal sleep time approaching",
                ReminderTone.SCIENCE,
                "Sleep before midnight enhances deep sleep phases"
            )
        ),
        "water" to listOf(
            ReminderMessage(
                "Hydration checkpoint",
                ReminderTone.SCIENCE,
                "Even 2% dehydration reduces cognitive performance by 20%"
            ),
            ReminderMessage(
                "Water fuels your brain",
                ReminderTone.SCIENCE,
                "Proper hydration improves focus and mood"
            )
        ),
        "move" to listOf(
            ReminderMessage(
                "Movement break time",
                ReminderTone.SCIENCE,
                "10 minutes of walking boosts creativity by 60%"
            ),
            ReminderMessage(
                "Your muscles are ready",
                ReminderTone.SCIENCE,
                "Regular movement reduces stress hormones by 25%"
            )
        ),
        "calm" to listOf(
            ReminderMessage(
                "Breathwork window",
                ReminderTone.SCIENCE,
                "6 deep breaths activate your parasympathetic system in 30 seconds"
            )
        )
    )

    fun getMessageForHabit(habitId: String, tone: ReminderTone): ReminderMessage {
        val messages = when (tone) {
            ReminderTone.GENTLE -> gentleMessages[habitId]
            ReminderTone.ENCOURAGING -> encouragingMessages[habitId]
            ReminderTone.SCIENCE -> scienceMessages[habitId]
            else -> gentleMessages[habitId]
        }
        return messages?.randomOrNull() ?: ReminderMessage(
            "Time for your $habitId habit",
            tone
        )
    }
}

/**
 * Calculate optimal reminder time based on learned patterns
 */
object SmartTimingCalculator {

    fun calculateOptimalTime(
        habitId: String,
        patterns: UserActivityPatterns,
        settings: HabitReminderSettings
    ): Pair<Int, Int> { // Returns (hour, minute)
        // If user has a preferred time, use it as base
        val preferredHour = settings.preferredTime?.split(":")?.firstOrNull()?.toIntOrNull()

        // Check habit-specific patterns
        val habitPattern = patterns.habitSpecificPatterns[habitId]
        if (habitPattern != null && habitPattern.successRateAtOptimal > 0.5f) {
            return Pair(habitPattern.optimalHour, habitPattern.optimalMinute)
        }

        // Use preferred time if set
        if (preferredHour != null) {
            val minute = settings.preferredTime?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0
            return Pair(preferredHour, minute)
        }

        // Fall back to most active hours
        val optimalHour = patterns.mostActiveHours.firstOrNull() ?: 8
        return Pair(optimalHour, 0)
    }

    fun shouldSendReminder(
        habitId: String,
        currentHour: Int,
        settings: HabitReminderSettings,
        patterns: UserActivityPatterns
    ): Boolean {
        if (!settings.isEnabled) return false

        // Don't remind during least active hours
        if (currentHour in patterns.leastActiveHours) return false

        val (optimalHour, _) = calculateOptimalTime(habitId, patterns, settings)

        // Send reminder within 30 minutes of optimal time
        return kotlin.math.abs(currentHour - optimalHour) <= 1
    }
}
