package com.dailywell.app.data.model

import com.dailywell.app.domain.model.HabitType
import kotlinx.serialization.Serializable

@Serializable
data class Habit(
    val id: String,
    val type: String, // HabitType.id
    val name: String,
    val emoji: String,
    val threshold: String,
    val isEnabled: Boolean = true,
    val order: Int = 0,
    val isCustom: Boolean = false,
    val customQuestion: String? = null
) {
    val habitType: HabitType?
        get() = HabitType.fromId(type)

    val displayQuestion: String
        get() = customQuestion ?: habitType?.question ?: "Did you complete $name?"

    companion object {
        fun fromHabitType(habitType: HabitType, order: Int = 0, isEnabled: Boolean = true): Habit {
            return Habit(
                id = habitType.id,
                type = habitType.id,
                name = habitType.displayName,
                emoji = habitType.emoji,
                threshold = habitType.defaultThreshold,
                isEnabled = isEnabled,
                order = order,
                isCustom = false,
                customQuestion = null
            )
        }

        fun createCustom(
            id: String,
            name: String,
            emoji: String,
            threshold: String,
            question: String,
            order: Int
        ): Habit {
            return Habit(
                id = id,
                type = "custom",
                name = name,
                emoji = emoji,
                threshold = threshold,
                isEnabled = true,
                order = order,
                isCustom = true,
                customQuestion = question
            )
        }
    }
}
