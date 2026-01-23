package com.dailywell.app.data.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class DailyEntry(
    val date: String, // ISO date string (yyyy-MM-dd)
    val completions: Map<String, Boolean>, // habitId -> completed
    val notes: String? = null,
    val mood: MoodLevel? = null, // Daily mood tracking (FBI psychology: labeling emotions)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isCompleted(habitId: String): Boolean = completions[habitId] == true

    fun completedCount(): Int = completions.count { it.value }

    fun totalCount(): Int = completions.size

    fun completionRate(): Float {
        if (totalCount() == 0) return 0f
        return completedCount().toFloat() / totalCount().toFloat()
    }

    fun withCompletion(habitId: String, completed: Boolean): DailyEntry {
        return copy(
            completions = completions + (habitId to completed),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun withMood(newMood: MoodLevel): DailyEntry {
        return copy(
            mood = newMood,
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        fun create(date: String, habitIds: List<String>): DailyEntry {
            return DailyEntry(
                date = date,
                completions = habitIds.associateWith { false }
            )
        }

        fun empty(date: String): DailyEntry {
            return DailyEntry(
                date = date,
                completions = emptyMap()
            )
        }
    }
}

@Serializable
data class DailyEntryList(
    val entries: List<DailyEntry>
)
