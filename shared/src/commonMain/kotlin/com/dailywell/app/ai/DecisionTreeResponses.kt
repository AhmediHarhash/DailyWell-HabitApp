package com.dailywell.app.ai

import com.dailywell.app.data.model.CoachingMessage
import com.dailywell.app.data.model.MessageRole
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * Decision Tree Response System
 *
 * Handles 70% of user messages for FREE (no Claude API cost)
 * Only routes complex/personal questions to Claude Haiku
 *
 * Cost savings: ~$0.25-0.35 per user per month
 */
object DecisionTreeResponses {

    // ==========================================
    // GREETINGS - No AI needed (FREE)
    // ==========================================

    private val greetingPatterns = listOf(
        "hi", "hello", "hey", "good morning", "good afternoon",
        "good evening", "good night", "morning", "evening",
        "what's up", "whats up", "sup", "howdy", "hola",
        "yo", "hiya", "heya"
    )

    private val greetingResponses = listOf(
        "Hey there! Ready to crush some habits today?",
        "Hello! Great to see you! How can I help?",
        "Hi! What would you like to work on today?",
        "Hey! Your wellness journey continues. What's on your mind?",
        "Hello! I'm here to help you build better habits!",
        "Hi there! Let's make today count!"
    )

    // ==========================================
    // HOW ARE YOU - No AI needed (FREE)
    // ==========================================

    private val howAreYouPatterns = listOf(
        "how are you", "how r u", "how're you", "how you doing",
        "how's it going", "hows it going", "what's going on",
        "how do you feel", "you good", "you ok"
    )

    private val howAreYouResponses = listOf(
        "I'm here and ready to help you! More importantly, how are YOU feeling today?",
        "I'm great! Thanks for asking. How are you doing with your habits?",
        "Doing well! Let's focus on you - how's your day going?",
        "All good on my end! Tell me about your day.",
        "I'm ready to support you! What's on your mind?"
    )

    // ==========================================
    // THANK YOU - No AI needed (FREE)
    // ==========================================

    private val thankYouPatterns = listOf(
        "thank you", "thanks", "thx", "ty", "appreciate it",
        "thank u", "thankyou", "thanks a lot", "much appreciated"
    )

    private val thankYouResponses = listOf(
        "You're welcome! Keep up the great work!",
        "Anytime! That's what I'm here for.",
        "Happy to help! You've got this!",
        "No problem! Remember, consistency is key!",
        "You're welcome! Let me know if you need anything else."
    )

    // ==========================================
    // MOTIVATION REQUESTS - No AI needed (FREE)
    // ==========================================

    private val motivationPatterns = listOf(
        "motivate me", "i need motivation", "feeling unmotivated",
        "not feeling it", "don't feel like it", "lazy today",
        "need a push", "give me motivation", "inspire me",
        "feeling down", "feeling low", "need encouragement"
    )

    private val motivationResponses = listOf(
        "Remember: You don't have to be perfect, you just have to show up. One small action today beats zero actions!",
        "The person who moves a mountain begins by carrying away small stones. Start tiny, finish big!",
        "Your future self is watching. Make them proud with just ONE habit today.",
        "Motivation follows action, not the other way around. Just start - even for 2 minutes!",
        "You've done hard things before. This is no different. You've got this!",
        "Every expert was once a beginner. Every champion was once a contender. Keep going!",
        "The only bad workout is the one that didn't happen. Same goes for habits!",
        "Progress, not perfection. Even 1% better is still better!",
        "You're not starting from zero - you're starting from experience. Use it!"
    )

    // ==========================================
    // STREAK CELEBRATIONS - No AI needed (FREE)
    // ==========================================

    fun getStreakCelebration(days: Int): String = when {
        days == 1 -> "First day complete! The journey of a thousand miles begins with a single step!"
        days == 3 -> "3 days in a row! You're building momentum!"
        days == 7 -> "ONE WEEK! A new habit is starting to form!"
        days == 14 -> "Two weeks strong! This is becoming part of who you are!"
        days == 21 -> "21 DAYS! Scientists say this is when habits start to stick!"
        days == 30 -> "30 DAYS! You've officially built a habit. Incredible!"
        days == 50 -> "50 days! Half a century of consistency. You're unstoppable!"
        days == 100 -> "100 DAYS! LEGENDARY! You've achieved what most only dream of!"
        days == 365 -> "ONE YEAR! This is no longer a habit - it's who you are!"
        days % 100 == 0 -> "$days days! Another century of consistency. You're a machine!"
        days % 30 == 0 -> "$days days! Another month conquered. Keep this fire burning!"
        days % 7 == 0 -> "Week ${days / 7} complete! $days days and counting!"
        else -> "Day $days complete! Every day counts. Keep going!"
    }

    // ==========================================
    // HABIT-SPECIFIC TIPS - No AI needed (FREE)
    // ==========================================

    private val habitTips = mapOf(
        "sleep" to listOf(
            "Tip: Try to go to bed at the same time every night, even on weekends.",
            "Sleep hack: Avoid screens 1 hour before bed. Blue light disrupts melatonin.",
            "Pro tip: Keep your bedroom cool (65-68F) for optimal sleep.",
            "Did you know? Caffeine has a half-life of 5-6 hours. Cut off after 2pm!",
            "Try this: Write down tomorrow's tasks before bed to clear your mind."
        ),
        "rest" to listOf(
            "Tip: Try to go to bed at the same time every night, even on weekends.",
            "Sleep hack: Avoid screens 1 hour before bed. Blue light disrupts melatonin.",
            "Pro tip: Keep your bedroom cool (65-68F) for optimal sleep."
        ),
        "water" to listOf(
            "Tip: Keep a water bottle at your desk. Visual cue = more drinking!",
            "Hydration hack: Drink a full glass right when you wake up.",
            "Pro tip: Set a timer every hour as a water reminder.",
            "Did you know? Feeling tired? You might just be dehydrated!",
            "Try this: Add lemon or cucumber for flavor without calories."
        ),
        "hydrate" to listOf(
            "Tip: Keep a water bottle at your desk. Visual cue = more drinking!",
            "Hydration hack: Drink a full glass right when you wake up.",
            "Pro tip: Set a timer every hour as a water reminder."
        ),
        "move" to listOf(
            "Tip: A 10-minute walk after meals aids digestion and mood!",
            "Exercise hack: Lay out workout clothes the night before.",
            "Pro tip: Can't do 30 minutes? Three 10-minute sessions work too!",
            "Did you know? Just 7 minutes of exercise can boost mood for hours.",
            "Try this: Take calls while walking. Multitask your movement!"
        ),
        "exercise" to listOf(
            "Tip: A 10-minute walk after meals aids digestion and mood!",
            "Exercise hack: Lay out workout clothes the night before.",
            "Pro tip: Can't do 30 minutes? Three 10-minute sessions work too!"
        ),
        "nourish" to listOf(
            "Tip: Eat the rainbow - different colors = different nutrients!",
            "Nutrition hack: Prep veggies on Sunday for easy weekday eating.",
            "Pro tip: Add spinach to smoothies - you won't taste it!",
            "Did you know? Frozen vegetables are just as nutritious as fresh.",
            "Try this: Replace one snack with a fruit or veggie today."
        ),
        "vegetables" to listOf(
            "Tip: Eat the rainbow - different colors = different nutrients!",
            "Nutrition hack: Prep veggies on Sunday for easy weekday eating.",
            "Pro tip: Add spinach to smoothies - you won't taste it!"
        ),
        "calm" to listOf(
            "Tip: Even 5 minutes of meditation can reduce stress hormones.",
            "Mindfulness hack: Try box breathing - 4 in, 4 hold, 4 out, 4 hold.",
            "Pro tip: Meditate at the same time daily to build the habit.",
            "Did you know? Meditation physically changes brain structure over time!",
            "Try this: Use the 5-4-3-2-1 grounding technique when stressed."
        ),
        "meditate" to listOf(
            "Tip: Even 5 minutes of meditation can reduce stress hormones.",
            "Mindfulness hack: Try box breathing - 4 in, 4 hold, 4 out, 4 hold.",
            "Pro tip: Meditate at the same time daily to build the habit."
        )
    )

    // ==========================================
    // GOODBYE MESSAGES - No AI needed (FREE)
    // ==========================================

    private val goodbyePatterns = listOf(
        "bye", "goodbye", "see you", "later", "gotta go",
        "cya", "ttyl", "talk later", "night", "goodnight"
    )

    private val goodbyeResponses = listOf(
        "See you later! Keep crushing those habits!",
        "Bye for now! Remember: progress over perfection!",
        "Take care! You're doing amazing!",
        "Until next time! Every day you show up is a win!",
        "Goodbye! Can't wait to celebrate your next streak milestone!"
    )

    // ==========================================
    // NEGATIVE/STRUGGLING MESSAGES - Handle with care (FREE)
    // ==========================================

    private val strugglingPatterns = listOf(
        "i failed", "i messed up", "i broke my streak",
        "feeling bad", "feeling terrible", "i suck",
        "can't do this", "giving up", "too hard",
        "missed my habit", "forgot", "skipped"
    )

    private val strugglingResponses = listOf(
        "Hey, it's okay. One missed day doesn't erase all your progress. What matters is you're here now. Ready to start fresh?",
        "Everyone stumbles. The difference between success and failure isn't never falling - it's getting back up. And here you are!",
        "I hear you. Habits are hard. But you know what? You're still trying. That counts for so much more than you realize.",
        "Missing a day doesn't make you a failure. It makes you human. Let's focus on what you CAN do today.",
        "Be kind to yourself. If a friend missed one day, would you tell them to give up? Of course not. Give yourself that same grace.",
        "Progress isn't linear. Some days are harder than others. The fact that you're here means you haven't given up. That's everything."
    )

    private fun normalizeForPatternMatching(message: String): String {
        return message
            .lowercase()
            .replace(Regex("[^a-z0-9\\s']"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun matchesPattern(normalizedMessage: String, pattern: String): Boolean {
        val normalizedPattern = normalizeForPatternMatching(pattern)
        if (normalizedPattern.isEmpty()) return false

        return if (normalizedPattern.contains(" ")) {
            normalizedMessage.contains(normalizedPattern)
        } else {
            Regex("\\b${Regex.escape(normalizedPattern)}\\b").containsMatchIn(normalizedMessage)
        }
    }

    private fun matchesAnyPattern(normalizedMessage: String, patterns: List<String>): Boolean {
        return patterns.any { matchesPattern(normalizedMessage, it) }
    }

    // ==========================================
    // MAIN DECISION ENGINE
    // ==========================================

    /**
     * Try to get a free response from decision tree
     * Returns null if message requires Claude AI
     */
    fun tryGetFreeResponse(userMessage: String, context: UserContext? = null): FreeResponseResult {
        val normalizedMessage = normalizeForPatternMatching(userMessage)

        // Check greetings
        if (matchesAnyPattern(normalizedMessage, greetingPatterns)) {
            return FreeResponseResult(
                response = greetingResponses.random(),
                category = ResponseCategory.GREETING,
                usedAI = false
            )
        }

        // Check how are you
        if (matchesAnyPattern(normalizedMessage, howAreYouPatterns)) {
            return FreeResponseResult(
                response = howAreYouResponses.random(),
                category = ResponseCategory.HOW_ARE_YOU,
                usedAI = false
            )
        }

        // Check thank you
        if (matchesAnyPattern(normalizedMessage, thankYouPatterns)) {
            return FreeResponseResult(
                response = thankYouResponses.random(),
                category = ResponseCategory.THANK_YOU,
                usedAI = false
            )
        }

        // Check goodbye
        if (matchesAnyPattern(normalizedMessage, goodbyePatterns)) {
            return FreeResponseResult(
                response = goodbyeResponses.random(),
                category = ResponseCategory.GOODBYE,
                usedAI = false
            )
        }

        // Check motivation requests
        if (matchesAnyPattern(normalizedMessage, motivationPatterns)) {
            return FreeResponseResult(
                response = motivationResponses.random(),
                category = ResponseCategory.MOTIVATION,
                usedAI = false
            )
        }

        // Check struggling/failed messages
        if (matchesAnyPattern(normalizedMessage, strugglingPatterns)) {
            return FreeResponseResult(
                response = strugglingResponses.random(),
                category = ResponseCategory.RECOVERY,
                usedAI = false
            )
        }

        // Check habit-specific tips
        habitTips.forEach { (habit, tips) ->
            if (matchesPattern(normalizedMessage, habit) &&
                (matchesPattern(normalizedMessage, "tip") ||
                 matchesPattern(normalizedMessage, "advice") ||
                 matchesPattern(normalizedMessage, "help"))) {
                return FreeResponseResult(
                    response = tips.random(),
                    category = ResponseCategory.HABIT_TIP,
                    usedAI = false
                )
            }
        }

        // No match - needs Claude AI
        return FreeResponseResult(
            response = null,
            category = ResponseCategory.NEEDS_AI,
            usedAI = false
        )
    }

    /**
     * Get a tip for a specific habit (FREE)
     */
    fun getHabitTip(habitType: String): String {
        return habitTips[habitType.lowercase()]?.random()
            ?: "Keep up the great work with your habits! Consistency is the key to lasting change."
    }

    /**
     * Get a general coaching response (FREE)
     * Used as fallback when SLM is not available
     */
    fun getCoachingResponse(message: String): String {
        val normalizedMessage = message.lowercase()

        return when {
            normalizedMessage.contains("help") ->
                "I'm here to support you! Whether you need motivation, tips, or just someone to check in with, I've got you covered."

            normalizedMessage.contains("how") && normalizedMessage.contains("start") ->
                "Starting is the hardest part, but you've already begun by being here. Pick ONE small habit, make it tiny enough you can't fail, and do it at the same time every day."

            normalizedMessage.contains("habit") && normalizedMessage.contains("track") ->
                "Tracking your habits is powerful! Research shows that people who track are 40% more likely to reach their goals. Keep it simple: done or not done."

            normalizedMessage.contains("feel") && (normalizedMessage.contains("good") || normalizedMessage.contains("great")) ->
                "That's wonderful! Positive feelings are fuel for habits. Capture this energy and channel it into completing your habits today!"

            normalizedMessage.contains("struggling") || normalizedMessage.contains("hard") ->
                "I hear you. Building habits IS hard - anyone who says otherwise hasn't tried. But you're here, which means you haven't given up. That's what matters."

            normalizedMessage.contains("advice") || normalizedMessage.contains("suggest") ->
                "My best advice: start smaller than you think. Instead of '30 minutes of exercise,' try '2 pushups.' Once the habit sticks, you can scale up."

            normalizedMessage.contains("goal") ->
                "Goals are great for direction, but habits are what get you there. Focus less on the destination and more on the daily actions that lead to it."

            else ->
                "I'm here to help you build better habits. Would you like tips for a specific habit, some motivation, or to celebrate a recent win?"
        }
    }

    /**
     * Get morning greeting based on time and streak (FREE)
     */
    fun getMorningGreeting(hour: Int, streak: Int, userName: String?): String {
        val timeGreeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }

        val name = userName?.let { ", $it" } ?: ""

        val streakNote = when {
            streak >= 100 -> "Your $streak-day streak is LEGENDARY!"
            streak >= 30 -> "Wow, $streak days! You're a habit master!"
            streak >= 14 -> "$streak-day streak - two weeks strong!"
            streak >= 7 -> "A $streak-day streak! One week of power!"
            streak >= 3 -> "$streak days and growing!"
            else -> "Let's make today count!"
        }

        return "$timeGreeting$name! $streakNote"
    }
}

/**
 * Result from decision tree check
 */
data class FreeResponseResult(
    val response: String?,
    val category: ResponseCategory,
    val usedAI: Boolean
)

/**
 * Categories for analytics
 */
enum class ResponseCategory {
    GREETING,
    HOW_ARE_YOU,
    THANK_YOU,
    GOODBYE,
    MOTIVATION,
    RECOVERY,
    HABIT_TIP,
    STREAK_CELEBRATION,
    NEEDS_AI  // Requires Claude
}

/**
 * User context for personalized free responses
 */
data class UserContext(
    val userName: String? = null,
    val currentStreak: Int = 0,
    val habits: List<String> = emptyList(),
    val lastCompletedHabit: String? = null,
    val missedDays: Int = 0,
    val isPremium: Boolean = false
)
