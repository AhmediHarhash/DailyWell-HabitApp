package com.dailywell.app.data

import kotlin.random.Random

object CelebrationMessages {

    // ============================================================
    // MICRO-CELEBRATION MESSAGES (shown between habit completions)
    // These create momentum and engagement during the check-in flow
    // ============================================================

    // Shown when user reaches 50% of habits completed
    private val midwayMessages = listOf(
        "Halfway there! Momentum is building.",
        "50% done. Keep this energy going.",
        "The hard part is starting. You did that.",
        "Half your habits checked. Nice rhythm.",
        "You're in the flow now.",
        "Middle of the pack. Keep pushing through."
    )

    // Shown when user has just ONE habit left
    private val almostThereMessages = listOf(
        "One more. You've got this.",
        "Last one! Close it out strong.",
        "Final habit. Make it count.",
        "Just one left. Finish what you started.",
        "Almost there. One tap to go.",
        "The finish line is right there."
    )

    // Shown when user checks their HARDEST habit (lowest historical completion rate)
    private val hardHabitMessages = listOf(
        "That one's tough for you. Respect.",
        "Your growth edge. Nice work tackling it.",
        "This is where change happens.",
        "The hard ones count the most.",
        "You showed up for the challenge."
    )

    // Shown on FIRST habit of the day (starting momentum)
    private val firstHabitMessages = listOf(
        "First one down. The rest will follow.",
        "You started. That's the hardest part.",
        "Day officially in motion.",
        "One check, infinite possibilities.",
        "The journey of a day begins with one habit."
    )

    // ============================================================
    // COMPLETION MESSAGES (shown at end of check-in)
    // ============================================================

    // When all habits are completed
    private val allCompleteMessages = listOf(
        "All done! You're unstoppable.",
        "Perfect day! But remember, consistency > perfection.",
        "Nailed it! See you tomorrow for more.",
        "100%! Your future self thanks you.",
        "Full house! This is what progress looks like.",
        "You showed up for yourself today.",
        "Every habit checked. Well done.",
        "That's how it's done.",
        "Clean sweep! Take a moment to appreciate this.",
        "All green. All good."
    )

    // When some habits are completed
    private val partialCompleteMessages = listOf(
        "%d out of %d is still progress!",
        "Good effort today. Tomorrow is fresh.",
        "Some days are harder. You still showed up.",
        "Not perfect, but you tried. That counts.",
        "Partial wins are still wins.",
        "Progress, not perfection.",
        "You did what you could. That matters.",
        "Better than zero. Keep going.",
        "Every check mark counts.",
        "Consistency isn't about perfect days."
    )

    // When no habits are completed
    private val noCompleteMessages = listOf(
        "Tomorrow is a new day.",
        "One day doesn't define you.",
        "Rest days happen. Don't let one become two.",
        "Reset and restart. That's the skill.",
        "It's okay. Show up tomorrow.",
        "Forgive today. Plan for tomorrow.",
        "Everyone misses sometimes. Champions restart.",
        "Not your day. That's human.",
        "Acknowledge it, then move on."
    )

    // Streak celebration messages
    private val streakMessages = mapOf(
        1 to listOf(
            "Day 1! You started. That's the hardest part.",
            "First step taken!",
            "Journey of a thousand miles begins with one step."
        ),
        7 to listOf(
            "Day 7! One full week of showing up!",
            "A whole week! You're building momentum.",
            "7 days strong! Keep it rolling."
        ),
        14 to listOf(
            "Two weeks! This is becoming routine.",
            "14 days of consistency!",
            "Half a month of dedication."
        ),
        21 to listOf(
            "Day 21! Habit forming in progress...",
            "3 weeks! They say this is when habits form.",
            "21 days. You're rewiring your brain."
        ),
        30 to listOf(
            "Day 30! This is becoming who you are.",
            "A full month! Incredible dedication.",
            "30 days. This isn't luck anymore."
        ),
        66 to listOf(
            "Day 66! Scientists say it's official now.",
            "66 days. The habit is part of you.",
            "Research says 66 days makes it automatic. You did it."
        ),
        100 to listOf(
            "Day 100! You've built something lasting.",
            "Triple digits! This is a lifestyle now.",
            "100 days of showing up. Remarkable."
        )
    )

    // Streak broken messages
    private val streakBrokenMessages = listOf(
        "Streak broken, but progress isn't lost.",
        "One day doesn't undo weeks of effort.",
        "Reset and restart. That's the real skill.",
        "Even the best miss days. What matters is starting again.",
        "The streak reset. Your habits didn't.",
        "A pause, not an ending.",
        "The counter resets. Your growth doesn't.",
        "Streaks are nice. Consistency over time matters more."
    )

    // Weekly summary messages
    private val weeklyCompleteMessages = mapOf(
        7 to listOf(
            "Perfect week! 7/7 days. Incredible!",
            "All 7 days! You're on fire!",
            "100% this week. Don't expect this every time - but enjoy it."
        ),
        6 to listOf(
            "6 out of 7 days! That's 86%. Excellent.",
            "Almost perfect week. This is real consistency.",
            "6/7 is outstanding. Keep it up."
        ),
        5 to listOf(
            "5 out of 7 days. That's a WIN.",
            "71% this week. Solid performance.",
            "5/7 - more good days than not. That's the goal."
        ),
        4 to listOf(
            "4 out of 7 days. More wins than losses.",
            "Over half the week! Progress is happening.",
            "4/7 - you're building momentum."
        ),
        3 to listOf(
            "3 out of 7 days. Room to grow, but you showed up.",
            "Nearly half the week. Keep pushing.",
            "3/7 - every day you show up matters."
        ),
        2 to listOf(
            "2 out of 7 days. Let's make next week stronger.",
            "Tough week. But 2 days is better than zero.",
            "2/7 - what made those days work? Do more of that."
        ),
        1 to listOf(
            "1 out of 7 days. One is better than none.",
            "Challenging week. You still showed up once.",
            "1/7 - start there. Build from it."
        ),
        0 to listOf(
            "No completions this week. Fresh start tomorrow.",
            "Tough week. Everyone has them. Reset and go.",
            "0/7 happens. What matters is next week."
        )
    )

    fun getCompletionMessage(completed: Int, total: Int): String {
        return when {
            completed == total && total > 0 -> allCompleteMessages.random()
            completed == 0 -> noCompleteMessages.random()
            else -> {
                val template = partialCompleteMessages.random()
                if (template.contains("%d")) {
                    template.format(completed, total)
                } else {
                    template
                }
            }
        }
    }

    fun getStreakMessage(days: Int): String {
        // Find the closest milestone
        val milestones = streakMessages.keys.sortedDescending()
        val milestone = milestones.find { days >= it } ?: return "Day $days! Keep going!"

        return if (days == milestone) {
            streakMessages[milestone]?.random() ?: "Day $days!"
        } else {
            val nextMilestone = milestones.filter { it > days }.minOrNull()
            if (nextMilestone != null) {
                "Day $days! ${nextMilestone - days} days to your next milestone."
            } else {
                "Day $days! Legendary consistency!"
            }
        }
    }

    fun getStreakBrokenMessage(): String = streakBrokenMessages.random()

    fun getWeeklySummaryMessage(daysCompleted: Int): String {
        return weeklyCompleteMessages[daysCompleted.coerceIn(0, 7)]?.random()
            ?: "Keep building your habits!"
    }

    fun getEncouragementForHabit(habitName: String): String {
        val encouragements = listOf(
            "How about $habitName today?",
            "Don't forget $habitName!",
            "$habitName is waiting for you.",
            "Ready for $habitName?",
            "Your $habitName habit misses you."
        )
        return encouragements.random()
    }

    // ============================================================
    // MICRO-CELEBRATION ACCESSORS
    // ============================================================

    /**
     * Returns a message when user completes their FIRST habit of the day.
     * Psychology: Validates the hardest step (starting) and builds momentum.
     */
    fun getFirstHabitMessage(): String = firstHabitMessages.random()

    /**
     * Returns a message when user reaches 50% completion.
     * Psychology: Acknowledges progress, creates mid-point energy boost.
     */
    fun getMidwayMessage(): String = midwayMessages.random()

    /**
     * Returns a message when user has just ONE habit remaining.
     * Psychology: Creates anticipation and urgency to finish.
     */
    fun getAlmostThereMessage(): String = almostThereMessages.random()

    /**
     * Returns a message when user completes their historically hardest habit.
     * Psychology: Recognizes effort on challenging behaviors, builds self-efficacy.
     */
    fun getHardHabitMessage(): String = hardHabitMessages.random()

    /**
     * Determines which micro-message to show based on progress.
     * @param completedCount Number of habits completed
     * @param totalCount Total number of habits to complete
     * @param isFirstHabit Whether this is the first habit checked today
     * @param isHardHabit Whether this habit has the lowest historical completion rate
     * @return A contextual encouragement message, or null if no message needed
     */
    fun getMicroCelebration(
        completedCount: Int,
        totalCount: Int,
        isFirstHabit: Boolean = false,
        isHardHabit: Boolean = false
    ): String? {
        return when {
            // First habit gets special recognition
            isFirstHabit && completedCount == 1 -> getFirstHabitMessage()
            // Hard habit gets special recognition (takes priority over position-based)
            isHardHabit -> getHardHabitMessage()
            // Exactly at midpoint (e.g., 2/4, 3/6)
            totalCount > 2 && completedCount == totalCount / 2 -> getMidwayMessage()
            // One habit remaining
            totalCount > 1 && completedCount == totalCount - 1 -> getAlmostThereMessage()
            // No micro-celebration needed
            else -> null
        }
    }
}
