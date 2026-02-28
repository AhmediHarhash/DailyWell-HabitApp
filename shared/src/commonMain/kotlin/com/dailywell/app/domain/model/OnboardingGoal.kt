package com.dailywell.app.domain.model

/**
 * Goals presented during onboarding: "What brought you here?"
 * Each goal has a personalized assessment question (1-5 scale).
 */
enum class OnboardingGoal(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val assessmentQuestion: String,
    val lowLabel: String,
    val highLabel: String
) {
    SLEEP_BETTER(
        id = "sleep_better",
        title = "Sleep better",
        subtitle = "Wake up feeling rested",
        emoji = "🌙",
        assessmentQuestion = "How would you rate your sleep quality lately?",
        lowLabel = "Poor",
        highLabel = "Great"
    ),
    MORE_ENERGY(
        id = "more_energy",
        title = "More energy",
        subtitle = "Feel alive throughout the day",
        emoji = "⚡",
        assessmentQuestion = "How are your energy levels on a typical day?",
        lowLabel = "Drained",
        highLabel = "Energized"
    ),
    LESS_STRESS(
        id = "less_stress",
        title = "Less stress",
        subtitle = "Find calm in the chaos",
        emoji = "🧘",
        assessmentQuestion = "How stressed do you feel on most days?",
        lowLabel = "Very stressed",
        highLabel = "Very calm"
    ),
    GET_HEALTHIER(
        id = "get_healthier",
        title = "Get healthier",
        subtitle = "Build a stronger body",
        emoji = "💪",
        assessmentQuestion = "How would you rate your overall health right now?",
        lowLabel = "Needs work",
        highLabel = "Excellent"
    ),
    BUILD_DISCIPLINE(
        id = "build_discipline",
        title = "Build discipline",
        subtitle = "Follow through on what matters",
        emoji = "🎯",
        assessmentQuestion = "How consistent are you with your daily routines?",
        lowLabel = "Inconsistent",
        highLabel = "Very consistent"
    ),
    FEEL_HAPPIER(
        id = "feel_happier",
        title = "Feel happier",
        subtitle = "More joy in everyday life",
        emoji = "☀️",
        assessmentQuestion = "How happy do you feel on most days?",
        lowLabel = "Struggling",
        highLabel = "Thriving"
    );

    companion object {
        fun fromId(id: String): OnboardingGoal? = entries.find { it.id == id }
    }
}
