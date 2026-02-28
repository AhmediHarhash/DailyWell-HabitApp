package com.dailywell.app.domain.model

/**
 * Maps each onboarding goal to recommended habits with "why" explanations.
 * Primary habits are auto-selected; secondary are suggested.
 */
data class HabitRecommendation(
    val habitType: HabitType,
    val isPrimary: Boolean,
    val reason: String
)

object GoalHabitMapping {

    fun getRecommendations(goal: OnboardingGoal): List<HabitRecommendation> {
        return when (goal) {
            OnboardingGoal.SLEEP_BETTER -> listOf(
                HabitRecommendation(
                    HabitType.SLEEP, isPrimary = true,
                    reason = "Tracking sleep builds awareness of your rest patterns"
                ),
                HabitRecommendation(
                    HabitType.UNPLUG, isPrimary = true,
                    reason = "Screens before bed suppress melatonin by up to 50%"
                ),
                HabitRecommendation(
                    HabitType.CALM, isPrimary = false,
                    reason = "Evening meditation reduces the time it takes to fall asleep"
                )
            )

            OnboardingGoal.MORE_ENERGY -> listOf(
                HabitRecommendation(
                    HabitType.MOVE, isPrimary = true,
                    reason = "Just 20 min of movement boosts energy for hours"
                ),
                HabitRecommendation(
                    HabitType.WATER, isPrimary = true,
                    reason = "Even mild dehydration causes fatigue and brain fog"
                ),
                HabitRecommendation(
                    HabitType.VEGETABLES, isPrimary = false,
                    reason = "Whole foods provide sustained energy without crashes"
                )
            )

            OnboardingGoal.LESS_STRESS -> listOf(
                HabitRecommendation(
                    HabitType.BREATHE, isPrimary = true,
                    reason = "Box breathing activates your parasympathetic nervous system"
                ),
                HabitRecommendation(
                    HabitType.CALM, isPrimary = true,
                    reason = "5 minutes of stillness rewires your stress response over time"
                ),
                HabitRecommendation(
                    HabitType.NATURE, isPrimary = false,
                    reason = "20 min in nature lowers cortisol by 12% on average"
                )
            )

            OnboardingGoal.GET_HEALTHIER -> listOf(
                HabitRecommendation(
                    HabitType.MOVE, isPrimary = true,
                    reason = "Consistent movement is the #1 predictor of long-term health"
                ),
                HabitRecommendation(
                    HabitType.VEGETABLES, isPrimary = true,
                    reason = "Adding greens is the simplest high-impact nutrition change"
                ),
                HabitRecommendation(
                    HabitType.WATER, isPrimary = false,
                    reason = "Hydration supports every system in your body"
                )
            )

            OnboardingGoal.BUILD_DISCIPLINE -> listOf(
                HabitRecommendation(
                    HabitType.FOCUS, isPrimary = true,
                    reason = "Deep work trains your brain to resist distraction"
                ),
                HabitRecommendation(
                    HabitType.LEARN, isPrimary = true,
                    reason = "Daily learning compounds into mastery over time"
                ),
                HabitRecommendation(
                    HabitType.MOVE, isPrimary = false,
                    reason = "Physical discipline spills over into mental discipline"
                )
            )

            OnboardingGoal.FEEL_HAPPIER -> listOf(
                HabitRecommendation(
                    HabitType.GRATITUDE, isPrimary = true,
                    reason = "Gratitude journaling increases happiness by 25% in 10 weeks"
                ),
                HabitRecommendation(
                    HabitType.CONNECT, isPrimary = true,
                    reason = "Meaningful connection is the strongest happiness predictor"
                ),
                HabitRecommendation(
                    HabitType.NATURE, isPrimary = false,
                    reason = "Nature exposure boosts mood and reduces rumination"
                )
            )
        }
    }

    /** Returns just the primary (auto-selected) habit IDs for a goal */
    fun getPrimaryHabitIds(goal: OnboardingGoal): Set<String> {
        return getRecommendations(goal)
            .filter { it.isPrimary }
            .map { it.habitType.id }
            .toSet()
    }

    /** Returns just the secondary (suggested) habit IDs for a goal */
    fun getSecondaryHabitIds(goal: OnboardingGoal): Set<String> {
        return getRecommendations(goal)
            .filter { !it.isPrimary }
            .map { it.habitType.id }
            .toSet()
    }
}
