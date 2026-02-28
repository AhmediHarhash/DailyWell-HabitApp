package com.dailywell.app.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreakInfoTest {

    @Test
    fun getMilestoneReturnsExpectedMilestone() {
        assertEquals(null, StreakInfo(currentStreak = 0).getMilestone())
        assertEquals(StreakMilestone.FIRST_DAY, StreakInfo(currentStreak = 1).getMilestone())
        assertEquals(StreakMilestone.WEEK, StreakInfo(currentStreak = 7).getMilestone())
        assertEquals(StreakMilestone.HABIT_FORMED, StreakInfo(currentStreak = 21).getMilestone())
        assertEquals(StreakMilestone.MONTH, StreakInfo(currentStreak = 30).getMilestone())
    }

    @Test
    fun isNewMilestoneOnlyWhenCrossingBoundary() {
        val current = StreakInfo(currentStreak = 7)

        assertTrue(current.isNewMilestone(previousStreak = 6))
        assertFalse(current.isNewMilestone(previousStreak = 7))
        assertFalse(current.isNewMilestone(previousStreak = 9))
    }

    @Test
    fun isActiveReflectsCurrentStreak() {
        assertFalse(StreakInfo(currentStreak = 0).isActive)
        assertTrue(StreakInfo(currentStreak = 1).isActive)
    }
}
