package com.dailywell.app.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Feature #13: Psychology Curriculum (Inspired by Noom)
 * 180 days of CBT-based lessons on behavior change, emotional eating, and mindset
 * This is what justifies Noom's $70/month pricing - we're adding it for free
 */

@Serializable
data class DailyLesson(
    val id: String,
    val day: Int,                           // 1-180
    val title: String,
    val category: LessonCategory,
    val readTime: Int,                      // Minutes
    val content: String,                    // Full lesson text
    val keyTakeaways: List<String>,         // 3-5 bullet points
    val actionItem: String,                 // What to do today
    val quiz: List<QuizQuestion>? = null,   // Optional comprehension quiz
    val reflection: String? = null          // Journal prompt
)

@Serializable
enum class LessonCategory {
    PSYCHOLOGY_BASICS,      // Why we overeat, habits, triggers
    EMOTIONAL_EATING,       // Stress, boredom, sadness eating
    MINDSET,               // Growth mindset, self-compassion
    NUTRITION_SCIENCE,     // Calorie density, macros, metabolism
    BEHAVIOR_CHANGE,       // Building habits, overcoming obstacles
    SOCIAL_SITUATIONS,     // Eating out, parties, peer pressure
    STRESS_MANAGEMENT,     // Coping without food
    GOAL_SETTING,          // SMART goals, tracking progress
    MAINTENANCE,           // Keeping weight off long-term
    SELF_LOVE              // Body image, confidence
}

@Serializable
data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,         // Index of correct option
    val explanation: String
)

@Serializable
data class UserLessonProgress(
    val userId: String,
    val currentDay: Int = 1,
    val completedLessons: List<CompletedLesson> = emptyList(),
    val streak: Int = 0,
    val totalReadTime: Int = 0,     // Total minutes spent reading
    val quizScore: Int = 0          // Total correct answers
) {
    val completionRate: Float get() = completedLessons.size / 180f * 100
}

@Serializable
data class CompletedLesson(
    val lessonId: String,
    val completedOn: String,
    val readTime: Int,              // Actual time spent
    val quizScore: Int? = null,     // How many questions correct
    val reflection: String? = null,  // User's journal entry
    val rating: Int? = null         // 1-5 stars
)

@Serializable
data class LessonReminder(
    val userId: String,
    val enabled: Boolean = true,
    val timeOfDay: String = "09:00",  // When to send notification
    val message: String = "Your daily lesson is ready!"
)

// Psychology curriculum database
object PsychologyCurriculum {

    fun getLesson(day: Int): DailyLesson? {
        return lessons.find { it.day == day }
    }

    fun getLessonsByCategory(category: LessonCategory): List<DailyLesson> {
        return lessons.filter { it.category == category }
    }

    // Sample lessons (in production, load from database)
    val lessons = listOf(
        // Week 1: Foundation
        DailyLesson(
            id = "lesson_001",
            day = 1,
            title = "Welcome to Your Journey",
            category = LessonCategory.PSYCHOLOGY_BASICS,
            readTime = 5,
            content = """
                Welcome to DailyWell! You're not just tracking calories—you're transforming your relationship with food.

                Most diets fail because they focus on WHAT you eat, not WHY you eat. We're different.

                Over the next 180 days, you'll learn:
                • Why you overeat (it's not willpower!)
                • How to break emotional eating patterns
                • How to build sustainable healthy habits
                • How to love yourself throughout the journey

                This isn't a diet. This is behavior change powered by psychology.

                Research shows that understanding the "why" behind your eating is the #1 predictor of long-term success.

                Today's lesson: Self-awareness is your superpower. Start noticing your eating patterns without judgment.
            """.trimIndent(),
            keyTakeaways = listOf(
                "Diets fail because they focus on WHAT, not WHY",
                "Self-awareness is the foundation of change",
                "This is about behavior change, not willpower",
                "You'll learn psychology-based strategies over 180 days"
            ),
            actionItem = "Today, before each meal, pause and ask: 'Am I physically hungry or eating for another reason?'",
            quiz = listOf(
                QuizQuestion(
                    question = "What is the #1 reason most diets fail?",
                    options = listOf(
                        "Not enough willpower",
                        "Focusing on WHAT instead of WHY",
                        "Not tracking calories accurately",
                        "Eating too many carbs"
                    ),
                    correctAnswer = 1,
                    explanation = "Research shows that understanding WHY you eat is more important than strict food rules."
                )
            ),
            reflection = "What motivated you to start this journey today?"
        ),

        DailyLesson(
            id = "lesson_002",
            day = 2,
            title = "Understanding Hunger vs. Cravings",
            category = LessonCategory.PSYCHOLOGY_BASICS,
            readTime = 7,
            content = """
                Let's talk about something critical: the difference between HUNGER and CRAVINGS.

                Physical Hunger:
                • Builds gradually over time
                • Any food sounds good
                • Goes away when you eat
                • No guilt afterward

                Emotional Cravings:
                • Come on suddenly
                • Specific food (usually comfort food)
                • Eating doesn't satisfy it
                • Often followed by guilt

                Most people eat when they're not hungry 60% of the time!

                Common triggers for emotional eating:
                • Stress (cortisol makes you crave sugar/fat)
                • Boredom (food = entertainment)
                • Sadness (food = comfort)
                • Habit (always eat while watching TV)
                • Social pressure (everyone else is eating)

                The Hunger Scale (1-10):
                1-2: Starving, dizzy, cranky
                3-4: Hungry, stomach growling
                5-6: Neutral, satisfied
                7-8: Full, slightly uncomfortable
                9-10: Stuffed, need to unbutton pants

                Ideal eating: Start at 3-4, stop at 6-7

                Today you'll learn to identify TRUE hunger.
            """.trimIndent(),
            keyTakeaways = listOf(
                "Physical hunger builds gradually, cravings come suddenly",
                "We eat emotionally 60% of the time without realizing it",
                "Use the 1-10 hunger scale before eating",
                "Ideal: Eat when at 3-4, stop at 6-7"
            ),
            actionItem = "Use the 1-10 hunger scale before each meal today. Write down your number.",
            reflection = "What emotion triggers your eating most often? Stress, boredom, sadness, or something else?"
        ),

        DailyLesson(
            id = "lesson_003",
            day = 3,
            title = "The Psychology of Calorie Density",
            category = LessonCategory.NUTRITION_SCIENCE,
            readTime = 8,
            content = """
                Here's a game-changer: Not all calories are equal in how they make you feel.

                CALORIE DENSITY = Calories per gram of food

                🟢 GREEN FOODS (< 0.8 cal/g):
                • Vegetables, fruits, broth soups
                • Fill you up with few calories
                • High water and fiber content
                • Eat as much as you want!
                • Examples: Watermelon (0.3), Broccoli (0.34), Strawberries (0.33)

                🟡 YELLOW FOODS (0.8 - 2.5 cal/g):
                • Lean proteins, whole grains, legumes
                • Moderate calorie density
                • Eat in moderation
                • Examples: Chicken breast (1.65), Brown rice (1.12), Eggs (1.43)

                🔴 RED FOODS (> 2.5 cal/g):
                • Oils, nuts, processed foods, desserts
                • High calorie density (easy to overeat!)
                • Eat in small portions
                • Examples: Peanut butter (5.88), Chocolate (5.46), Pizza (2.66)

                Psychology Insight:
                Your brain measures VOLUME of food, not calories. A plate of broccoli (100 cal) feels more satisfying than 10 M&Ms (100 cal) because your stomach stretches more.

                This is why people on Ozempic lose weight—they feel full faster. But you can achieve the same by choosing lower calorie density foods!

                The Strategy:
                • Fill 50% of plate with GREEN
                • 30% with YELLOW
                • 20% with RED

                You'll eat the same VOLUME but fewer calories, so you feel satisfied without overeating.
            """.trimIndent(),
            keyTakeaways = listOf(
                "Calorie density determines how filling foods are",
                "Your brain measures volume, not calories",
                "Green foods: eat freely, Yellow: moderate, Red: small portions",
                "Fill 50% plate with green, 30% yellow, 20% red"
            ),
            actionItem = "At your next meal, identify which foods are Green, Yellow, and Red. Try to make half your plate Green.",
            quiz = listOf(
                QuizQuestion(
                    question = "Why do Green foods help you feel full with fewer calories?",
                    options = listOf(
                        "They have more protein",
                        "They have high water and fiber content",
                        "They boost your metabolism",
                        "They have less sugar"
                    ),
                    correctAnswer = 1,
                    explanation = "Green foods have high water and fiber content, which fills your stomach with very few calories."
                ),
                QuizQuestion(
                    question = "What's the ideal plate composition?",
                    options = listOf(
                        "33% Green, 33% Yellow, 33% Red",
                        "50% Green, 30% Yellow, 20% Red",
                        "70% Green, 20% Yellow, 10% Red",
                        "All Green foods only"
                    ),
                    correctAnswer = 1,
                    explanation = "50% Green, 30% Yellow, 20% Red gives you balanced nutrition while keeping you satisfied."
                )
            )
        ),

        // Week 2: Emotional Eating
        DailyLesson(
            id = "lesson_004",
            day = 4,
            title = "Why Stress Makes You Eat",
            category = LessonCategory.EMOTIONAL_EATING,
            readTime = 6,
            content = """
                Let's talk about stress eating—the #1 reason people struggle with weight.

                The Science:
                When you're stressed, your body releases cortisol (the stress hormone). Cortisol triggers cravings for sugar and fat because your brain thinks you need quick energy to fight a threat.

                This made sense 10,000 years ago (running from a lion). Today, your "threat" is a work deadline, but your brain still wants cookies.

                The Cycle:
                1. Stressful event happens
                2. Cortisol spikes
                3. Crave comfort food (usually sugar/fat)
                4. Eat the food
                5. Get temporary relief (dopamine spike)
                6. Feel guilty afterward
                7. More stress from guilt
                8. Repeat cycle

                Breaking the Cycle:
                Instead of food, try these cortisol-lowering strategies:
                • Deep breathing (4-7-8 technique)
                • 10-minute walk
                • Call a friend
                • Journaling
                • Dancing to music
                • Cold water on face

                The 10-Minute Rule:
                When you crave food due to stress, wait 10 minutes and do one of the above activities. Often, the craving passes.

                Remember: Food is not therapy. It's fuel.
            """.trimIndent(),
            keyTakeaways = listOf(
                "Cortisol (stress hormone) triggers sugar and fat cravings",
                "Stress eating creates a guilt cycle that makes it worse",
                "Use the 10-minute rule: wait and try a stress-relief activity",
                "Food is fuel, not therapy"
            ),
            actionItem = "Next time you feel stressed today, try the 4-7-8 breathing: Inhale 4 sec, hold 7 sec, exhale 8 sec. Repeat 3 times.",
            reflection = "What are your biggest stress triggers? Work? Relationships? Money?"
        ),

        DailyLesson(
            id = "lesson_005",
            day = 5,
            title = "Boredom Eating: The Hidden Saboteur",
            category = LessonCategory.EMOTIONAL_EATING,
            readTime = 6,
            content = """
                "I'm not hungry, I'm just bored."

                Sound familiar? Boredom eating is sneaky because it doesn't FEEL emotional—it feels like "nothing else to do."

                Why We Do It:
                • Food = instant entertainment
                • Eating activates dopamine (reward chemical)
                • Breaks up monotony
                • Mindless habit (TV + snacks)

                The Problem:
                Boredom eating adds 300-500 extra calories per day on average. That's 36,500 calories per year = 10 pounds of weight gain!

                Signs You're Boredom Eating:
                ✓ Not physically hungry (stomach isn't growling)
                ✓ Grazing mindlessly (handful of chips, then another...)
                ✓ Eating while doing something else (scrolling phone)
                ✓ Can't remember what you just ate

                The Fix: Boredom Busters
                Instead of reaching for food, try:
                • Text a friend
                • Do a 2-minute plank
                • Reorganize one drawer
                • Learn a TikTok dance
                • Drink flavored sparkling water
                • Go outside for 5 minutes

                The "Is This Hunger?" Test:
                Ask yourself: "Would I eat an apple right now?"
                • If yes → You're actually hungry, eat something nutritious
                • If no → You're bored, not hungry. Do a boredom buster instead.
            """.trimIndent(),
            keyTakeaways = listOf(
                "Boredom eating adds 300-500 calories daily (10 lbs/year)",
                "Food becomes entertainment when we're understimulated",
                "Use the 'Would I eat an apple?' test",
                "Have a list of 2-minute boredom busters ready"
            ),
            actionItem = "Make a list of 5 activities you can do in under 2 minutes when bored (that aren't eating).",
            reflection = "When during the day are you most likely to eat out of boredom?"
        ),

        // Week 3: Mindset
        DailyLesson(
            id = "lesson_010",
            day = 10,
            title = "Self-Compassion vs. Self-Criticism",
            category = LessonCategory.MINDSET,
            readTime = 7,
            content = """
                Here's a truth bomb: Being mean to yourself doesn't work.

                The Research:
                Studies show that people who practice self-compassion lose MORE weight and keep it off longer than people who use harsh self-criticism.

                Why? Because shame triggers stress → stress triggers cortisol → cortisol triggers overeating.

                Self-Criticism:
                "I ate a cookie. I'm so weak. I have no willpower. I'll never lose weight. I might as well eat the whole box."

                Self-Compassion:
                "I ate a cookie. That's okay. One cookie doesn't undo my progress. Tomorrow I'll make a different choice. I'm learning."

                The Science Behind It:
                Self-compassion activates the parasympathetic nervous system (calm state). Self-criticism activates the sympathetic nervous system (fight-or-flight), which makes you MORE likely to emotionally eat.

                How to Practice Self-Compassion:
                1. Acknowledge the struggle: "This is hard."
                2. Remember you're human: "Everyone struggles with this."
                3. Talk to yourself like a friend: "What would I tell my best friend?"

                The Mantra:
                "I am learning. I am growing. I am worthy of love at every size."

                Your "mistakes" are data points, not failures.
            """.trimIndent(),
            keyTakeaways = listOf(
                "Self-compassion leads to better weight loss than self-criticism",
                "Shame triggers stress, which triggers overeating",
                "Talk to yourself like you'd talk to a best friend",
                "Mistakes are data points, not failures"
            ),
            actionItem = "Next time you eat something unplanned, practice self-compassion. Say: 'That's okay. I'm learning.'",
            reflection = "What would you say to a friend who ate something they felt guilty about? Now say that to yourself."
        )
    )
}

// Emotion tracking for meals (integrated with nutrition)
@Serializable
enum class EatingEmotion {
    PHYSICALLY_HUNGRY,      // True hunger
    STRESSED,               // Work, life pressure
    BORED,                  // Nothing to do
    SAD,                    // Emotional distress
    HAPPY,                  // Celebration
    SOCIAL,                 // Others are eating
    HABIT,                  // Always eat at this time
    TIRED,                  // Fatigue-driven eating
    ANXIOUS,                // Nervous energy
    REWARDING_MYSELF        // "I deserve this"
}

@Serializable
data class EmotionLog(
    val userId: String,
    val date: String,
    val mealId: String,
    val emotionBefore: EatingEmotion,
    val hungerLevel: Int,               // 1-10 scale
    val fullnessAfter: Int,             // 1-10 scale
    val satisfactionLevel: Int,         // 1-10 how satisfied
    val notes: String? = null,
    val timestamp: Instant
)

@Serializable
data class EmotionInsights(
    val userId: String,
    val weekStart: String,
    val emotionCounts: Map<EatingEmotion, Int>,
    val topTrigger: EatingEmotion,
    val insight: String,
    val recommendation: String
) {
    companion object {
        fun generate(emotionLogs: List<EmotionLog>): EmotionInsights {
            val emotionCounts = emotionLogs.groupingBy { it.emotionBefore }.eachCount()
            val topTrigger = emotionCounts.maxByOrNull { it.value }?.key ?: EatingEmotion.PHYSICALLY_HUNGRY

            val insight = when (topTrigger) {
                EatingEmotion.STRESSED -> "You ate from stress ${emotionCounts[topTrigger]} times this week. Stress is your biggest trigger."
                EatingEmotion.BORED -> "Boredom led to ${emotionCounts[topTrigger]} eating episodes this week."
                EatingEmotion.SAD -> "Sadness triggered ${emotionCounts[topTrigger]} eating moments. Food became comfort."
                EatingEmotion.HABIT -> "Habit-based eating happened ${emotionCounts[topTrigger]} times. You ate automatically."
                else -> "Great awareness! You tracked ${emotionLogs.size} meals this week."
            }

            val recommendation = when (topTrigger) {
                EatingEmotion.STRESSED -> "Try the 4-7-8 breathing technique before eating. Practice stress management from Day 4 lesson."
                EatingEmotion.BORED -> "Create a boredom buster list. Review Day 5 lesson on boredom eating."
                EatingEmotion.SAD -> "Food isn't therapy. Call a friend, journal, or go for a walk instead."
                EatingEmotion.HABIT -> "Break automatic eating patterns. Eat mindfully at a table without screens."
                else -> "Keep tracking emotions to understand your patterns!"
            }

            return EmotionInsights(
                userId = emotionLogs.first().userId,
                weekStart = emotionLogs.first().date,
                emotionCounts = emotionCounts,
                topTrigger = topTrigger,
                insight = insight,
                recommendation = recommendation
            )
        }
    }
}
