package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WeekData(
    val days: List<DayStatus>,
    val weekStartDate: String, // ISO date
    val weekEndDate: String,   // ISO date
    val completionRate: Float,
    val totalCompleted: Int,
    val totalPossible: Int
) {
    val isCurrentWeek: Boolean
        get() = days.any { it.isToday }

    fun getDayStatus(dayOfWeek: Int): DayStatus? = days.getOrNull(dayOfWeek)
}

@Serializable
data class DayStatus(
    val date: String, // ISO date
    val dayOfWeek: Int, // 0 = Monday, 6 = Sunday
    val dayLabel: String, // "M", "T", "W", etc.
    val status: CompletionStatus,
    val completedCount: Int,
    val totalCount: Int,
    val isToday: Boolean,
    val isFuture: Boolean
)

@Serializable
enum class CompletionStatus {
    NONE,       // No habits completed
    PARTIAL,    // Some habits completed
    COMPLETE,   // All habits completed
    FUTURE,     // Day hasn't happened yet
    NO_DATA     // No tracking data for this day
}
