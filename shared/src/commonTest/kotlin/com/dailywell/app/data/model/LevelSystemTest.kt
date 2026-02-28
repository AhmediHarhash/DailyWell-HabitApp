package com.dailywell.app.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LevelSystemTest {

    @Test
    fun levelThresholdsMatchExpectedXpCurve() {
        assertEquals(0L, LevelSystem.xpForLevel(1))
        assertEquals(100L, LevelSystem.xpForLevel(2))
        assertEquals(900L, LevelSystem.xpForLevel(10))
        assertEquals(1050L, LevelSystem.xpForLevel(11))
        assertEquals(3150L, LevelSystem.xpForLevel(25))
        assertEquals(3400L, LevelSystem.xpForLevel(26))
    }

    @Test
    fun levelForXpHandlesBoundaryValues() {
        assertEquals(1, LevelSystem.levelForXp(0))
        assertEquals(1, LevelSystem.levelForXp(99))
        assertEquals(2, LevelSystem.levelForXp(100))
        assertEquals(10, LevelSystem.levelForXp(900))
        assertEquals(11, LevelSystem.levelForXp(1050))
    }

    @Test
    fun progressToNextLevelStaysWithinRange() {
        val progressAtStart = LevelSystem.progressToNextLevel(0)
        val progressMid = LevelSystem.progressToNextLevel(50)

        assertTrue(progressAtStart in 0f..1f)
        assertTrue(progressMid in 0f..1f)
        assertTrue(progressMid > progressAtStart)
    }
}
