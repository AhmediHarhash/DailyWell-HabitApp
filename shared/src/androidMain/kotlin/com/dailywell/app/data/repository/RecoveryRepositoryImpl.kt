package com.dailywell.app.data.repository

import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementation of RecoveryRepository using DataStore
 */
class RecoveryRepositoryImpl(
    private val dataStoreManager: DataStoreManager
) : RecoveryRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val RECOVERY_STATE_KEY = "recovery_state"
    private val RECOVERY_STATS_KEY = "recovery_stats"

    override fun getRecoveryState(): Flow<RecoveryState?> {
        return dataStoreManager.getString(RECOVERY_STATE_KEY).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<RecoveryState>(it)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override suspend fun startRecovery(previousStreak: Int) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        val state = RecoveryState(
            isInRecovery = true,
            streakBrokenAt = today,
            previousStreak = previousStreak,
            recoveryStartedAt = today,
            recoveryPhase = RecoveryPhase.ACKNOWLEDGE
        )
        dataStoreManager.putString(RECOVERY_STATE_KEY, json.encodeToString(state))
    }

    override suspend fun advanceToPhase(phase: RecoveryPhase) {
        val currentState = getRecoveryState().first() ?: return
        val updatedState = currentState.copy(recoveryPhase = phase)
        dataStoreManager.putString(RECOVERY_STATE_KEY, json.encodeToString(updatedState))
    }

    override suspend fun setStreakBreakReason(reason: StreakBreakReason) {
        val currentState = getRecoveryState().first() ?: return
        val updatedState = currentState.copy(selectedReason = reason)
        dataStoreManager.putString(RECOVERY_STATE_KEY, json.encodeToString(updatedState))
    }

    override suspend fun setReflectionAnswer(answer: String) {
        val currentState = getRecoveryState().first() ?: return
        val updatedState = currentState.copy(reflectionAnswer = answer)
        dataStoreManager.putString(RECOVERY_STATE_KEY, json.encodeToString(updatedState))
    }

    override suspend fun setCommitmentLevel(level: CommitmentLevel) {
        val currentState = getRecoveryState().first() ?: return
        val updatedState = currentState.copy(commitmentLevel = level)
        dataStoreManager.putString(RECOVERY_STATE_KEY, json.encodeToString(updatedState))
    }

    override suspend fun completeRecovery() {
        val currentState = getRecoveryState().first() ?: return
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

        // Record recovery event in stats
        val stats = getRecoveryStatsInternal()
        val recoveryDays = calculateDaysBetween(currentState.streakBrokenAt, today)

        val event = RecoveryEvent(
            brokenAt = currentState.streakBrokenAt ?: today,
            recoveredAt = today,
            previousStreak = currentState.previousStreak,
            reason = currentState.selectedReason ?: StreakBreakReason.OTHER,
            daysToRecover = recoveryDays
        )

        val updatedHistory = (stats.recoveryHistory + event).takeLast(50)
        val updatedStats = RecoveryStats(
            totalRecoveries = stats.totalRecoveries + 1,
            fastestRecoveryDays = minOf(stats.fastestRecoveryDays ?: recoveryDays, recoveryDays),
            averageRecoveryDays = calculateAverageRecovery(updatedHistory),
            longestStreakAfterRecovery = stats.longestStreakAfterRecovery,
            recoveryHistory = updatedHistory
        )

        // Save stats
        dataStoreManager.putString(RECOVERY_STATS_KEY, json.encodeToString(updatedStats))

        // Clear recovery state
        val completedState = currentState.copy(
            isInRecovery = false,
            recoveryPhase = RecoveryPhase.NONE,
            recoveryCompletedAt = today
        )
        dataStoreManager.putString(RECOVERY_STATE_KEY, json.encodeToString(completedState))
    }

    override suspend fun cancelRecovery() {
        val clearedState = RecoveryState()
        dataStoreManager.putString(RECOVERY_STATE_KEY, json.encodeToString(clearedState))
    }

    override fun getRecoveryStats(): Flow<RecoveryStats> {
        return dataStoreManager.getString(RECOVERY_STATS_KEY).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<RecoveryStats>(it)
                } catch (e: Exception) {
                    RecoveryStats()
                }
            } ?: RecoveryStats()
        }
    }

    override suspend fun checkShouldStartRecovery(): Boolean {
        val currentState = getRecoveryState().first()
        // Don't start if already in recovery
        if (currentState?.isInRecovery == true) return false

        // This would be called by the main habit tracking logic
        // when it detects a missed day. For now, return false.
        return false
    }

    private suspend fun getRecoveryStatsInternal(): RecoveryStats {
        return getRecoveryStats().first()
    }

    private fun calculateDaysBetween(start: String?, end: String): Int {
        if (start == null) return 1
        return try {
            val startDate = LocalDate.parse(start)
            val endDate = LocalDate.parse(end)
            (endDate.toEpochDays() - startDate.toEpochDays()).coerceAtLeast(1)
        } catch (_: Exception) {
            1
        }
    }

    private fun calculateAverageRecovery(history: List<RecoveryEvent>): Float {
        if (history.isEmpty()) return 0f
        return history.map { it.daysToRecover }.average().toFloat()
    }
}
