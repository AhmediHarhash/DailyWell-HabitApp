package com.dailywell.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dailywell.app.data.model.Habit

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val name: String,
    val emoji: String,
    val threshold: String,
    val isEnabled: Boolean = true,
    val order: Int = 0,
    val isCustom: Boolean = false,
    val customQuestion: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toHabit(): Habit = Habit(
        id = id,
        type = type,
        name = name,
        emoji = emoji,
        threshold = threshold,
        isEnabled = isEnabled,
        order = order,
        isCustom = isCustom,
        customQuestion = customQuestion
    )

    companion object {
        fun fromHabit(habit: Habit): HabitEntity = HabitEntity(
            id = habit.id,
            type = habit.type,
            name = habit.name,
            emoji = habit.emoji,
            threshold = habit.threshold,
            isEnabled = habit.isEnabled,
            order = habit.order,
            isCustom = habit.isCustom,
            customQuestion = habit.customQuestion
        )
    }
}
