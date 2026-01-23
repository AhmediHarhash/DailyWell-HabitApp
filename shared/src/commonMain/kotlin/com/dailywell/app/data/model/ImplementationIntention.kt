package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

/**
 * Implementation Intentions Model
 * Based on Peter Gollwitzer's research - specific "if-then" planning doubles success rate
 * Formula: "When [SITUATION], I will [ACTION]"
 *
 * Research shows implementation intentions improve goal achievement by 91%
 */
@Serializable
data class ImplementationIntention(
    val id: String,
    val habitId: String,
    val situation: IntentionSituation,
    val action: String,                    // What the user will do
    val location: String? = null,          // Where they'll do it
    val time: String? = null,              // When specifically (e.g., "7:00 AM")
    val obstacle: String? = null,          // Anticipated obstacle
    val obstacleResponse: String? = null,  // How to handle the obstacle
    val isEnabled: Boolean = true,
    val createdAt: String,
    val completionCount: Int = 0,
    val lastTriggeredAt: String? = null
) {
    fun getIntentionStatement(): String {
        val timeLocation = buildString {
            if (time != null) append("at $time ")
            if (location != null) append("in $location")
        }.trim()

        return "When ${situation.description}, I will $action" +
                if (timeLocation.isNotEmpty()) " ($timeLocation)" else ""
    }

    fun getObstacleStatement(): String? {
        if (obstacle == null || obstacleResponse == null) return null
        return "If $obstacle, I will $obstacleResponse"
    }
}

/**
 * Pre-defined situations that trigger habits
 */
@Serializable
enum class IntentionSituation(val description: String, val emoji: String) {
    // Time-based triggers
    WAKE_UP("I wake up", "🌅"),
    AFTER_BREAKFAST("I finish breakfast", "🍳"),
    LUNCH_BREAK("my lunch break starts", "🥗"),
    AFTER_WORK("I finish work", "💼"),
    BEFORE_DINNER("I'm about to eat dinner", "🍽️"),
    BEFORE_BED("I'm getting ready for bed", "🌙"),

    // Emotional triggers
    FEELING_STRESSED("I feel stressed", "😰"),
    FEELING_TIRED("I feel tired", "😴"),
    FEELING_BORED("I feel bored", "😐"),
    FEELING_ANXIOUS("I feel anxious", "😟"),
    FEELING_ENERGETIC("I feel energetic", "⚡"),

    // Location-based triggers
    ARRIVE_HOME("I arrive home", "🏠"),
    ARRIVE_WORK("I arrive at work", "🏢"),
    IN_KITCHEN("I'm in the kitchen", "🍳"),
    AT_GYM("I'm at the gym", "🏋️"),

    // Event-based triggers
    PHONE_RINGS("my phone buzzes", "📱"),
    MEETING_ENDS("a meeting ends", "📅"),
    COFFEE_READY("my coffee is ready", "☕"),

    // Custom
    CUSTOM("(custom situation)", "✏️")
}

/**
 * Templates for common implementation intentions based on habit types
 */
object IntentionTemplates {

    data class IntentionTemplate(
        val habitId: String,
        val situation: IntentionSituation,
        val suggestedAction: String,
        val suggestedLocation: String? = null,
        val suggestedTime: String? = null,
        val commonObstacle: String? = null,
        val obstacleResponse: String? = null,
        val scienceNote: String
    )

    val sleepIntentions = listOf(
        IntentionTemplate(
            habitId = "sleep",
            situation = IntentionSituation.BEFORE_BED,
            suggestedAction = "put my phone in another room",
            suggestedLocation = "bedroom",
            suggestedTime = "10:00 PM",
            commonObstacle = "I want to check social media",
            obstacleResponse = "read a physical book instead",
            scienceNote = "Blue light exposure reduces melatonin by 50%"
        ),
        IntentionTemplate(
            habitId = "sleep",
            situation = IntentionSituation.AFTER_WORK,
            suggestedAction = "stop drinking caffeine",
            suggestedTime = "2:00 PM",
            commonObstacle = "I feel tired and need a pick-me-up",
            obstacleResponse = "take a 10-minute walk instead",
            scienceNote = "Caffeine has a 6-hour half-life affecting sleep quality"
        )
    )

    val waterIntentions = listOf(
        IntentionTemplate(
            habitId = "water",
            situation = IntentionSituation.WAKE_UP,
            suggestedAction = "drink a full glass of water",
            suggestedLocation = "kitchen",
            commonObstacle = "I reach for coffee first",
            obstacleResponse = "keep a water glass on my nightstand",
            scienceNote = "Morning hydration increases metabolism by 24%"
        ),
        IntentionTemplate(
            habitId = "water",
            situation = IntentionSituation.BEFORE_DINNER,
            suggestedAction = "drink a glass of water",
            commonObstacle = "I forget before meals",
            obstacleResponse = "set a reminder 30 minutes before usual dinner time",
            scienceNote = "Pre-meal hydration aids digestion and portion control"
        )
    )

    val moveIntentions = listOf(
        IntentionTemplate(
            habitId = "move",
            situation = IntentionSituation.WAKE_UP,
            suggestedAction = "do 10 minutes of stretching",
            suggestedLocation = "living room",
            suggestedTime = "6:30 AM",
            commonObstacle = "I feel too tired",
            obstacleResponse = "commit to just 2 minutes, then decide",
            scienceNote = "Morning movement increases energy for 4+ hours"
        ),
        IntentionTemplate(
            habitId = "move",
            situation = IntentionSituation.LUNCH_BREAK,
            suggestedAction = "take a 15-minute walk",
            commonObstacle = "I have too much work",
            obstacleResponse = "walk while listening to a work podcast",
            scienceNote = "Midday walks improve afternoon productivity by 23%"
        )
    )

    val vegetablesIntentions = listOf(
        IntentionTemplate(
            habitId = "vegetables",
            situation = IntentionSituation.IN_KITCHEN,
            suggestedAction = "prepare vegetables first before cooking",
            commonObstacle = "vegetables go bad before I use them",
            obstacleResponse = "prep vegetables on Sunday for the week",
            scienceNote = "Visual priority increases vegetable consumption by 40%"
        )
    )

    val calmIntentions = listOf(
        IntentionTemplate(
            habitId = "calm",
            situation = IntentionSituation.FEELING_STRESSED,
            suggestedAction = "take 5 deep breaths",
            commonObstacle = "I'm too busy to pause",
            obstacleResponse = "do just one breath, then assess",
            scienceNote = "6 deep breaths activate parasympathetic response in 30 seconds"
        ),
        IntentionTemplate(
            habitId = "calm",
            situation = IntentionSituation.BEFORE_BED,
            suggestedAction = "do 5 minutes of meditation",
            suggestedLocation = "bedroom",
            suggestedTime = "9:30 PM",
            commonObstacle = "my mind is racing",
            obstacleResponse = "use a guided meditation app",
            scienceNote = "Pre-sleep meditation improves sleep onset by 47%"
        )
    )

    val connectIntentions = listOf(
        IntentionTemplate(
            habitId = "connect",
            situation = IntentionSituation.LUNCH_BREAK,
            suggestedAction = "text or call one friend",
            commonObstacle = "I don't know what to say",
            obstacleResponse = "send a simple 'thinking of you' message",
            scienceNote = "Brief social contact triggers oxytocin release"
        ),
        IntentionTemplate(
            habitId = "connect",
            situation = IntentionSituation.ARRIVE_HOME,
            suggestedAction = "have a device-free conversation with family",
            suggestedLocation = "living room",
            commonObstacle = "everyone is busy",
            obstacleResponse = "start with 'how was the best part of your day?'",
            scienceNote = "Quality conversation predicts relationship satisfaction"
        )
    )

    val unplugIntentions = listOf(
        IntentionTemplate(
            habitId = "unplug",
            situation = IntentionSituation.BEFORE_BED,
            suggestedAction = "put all devices in another room",
            suggestedTime = "9:00 PM",
            commonObstacle = "I use my phone as an alarm",
            obstacleResponse = "buy a simple alarm clock",
            scienceNote = "Device-free bedroom improves sleep quality by 58%"
        ),
        IntentionTemplate(
            habitId = "unplug",
            situation = IntentionSituation.AFTER_WORK,
            suggestedAction = "check notifications once, then turn off work apps",
            commonObstacle = "I might miss something important",
            obstacleResponse = "set emergency contact exceptions only",
            scienceNote = "Post-work unplugging reduces cortisol by 23%"
        )
    )

    fun getTemplatesForHabit(habitId: String): List<IntentionTemplate> {
        return when (habitId) {
            "sleep" -> sleepIntentions
            "water" -> waterIntentions
            "move" -> moveIntentions
            "vegetables" -> vegetablesIntentions
            "calm" -> calmIntentions
            "connect" -> connectIntentions
            "unplug" -> unplugIntentions
            else -> emptyList()
        }
    }

    fun getAllTemplates(): List<IntentionTemplate> {
        return sleepIntentions + waterIntentions + moveIntentions +
                vegetablesIntentions + calmIntentions + connectIntentions + unplugIntentions
    }
}

/**
 * User's saved implementation intentions
 */
@Serializable
data class UserIntentions(
    val intentions: List<ImplementationIntention> = emptyList()
) {
    fun getIntentionsForHabit(habitId: String): List<ImplementationIntention> {
        return intentions.filter { it.habitId == habitId && it.isEnabled }
    }

    fun withIntentionAdded(intention: ImplementationIntention): UserIntentions {
        return copy(intentions = intentions + intention)
    }

    fun withIntentionRemoved(intentionId: String): UserIntentions {
        return copy(intentions = intentions.filter { it.id != intentionId })
    }
}
