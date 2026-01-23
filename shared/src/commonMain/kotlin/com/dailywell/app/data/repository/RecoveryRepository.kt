package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Recovery Protocol
 */
interface RecoveryRepository {

    /**
     * Get current recovery state
     */
    fun getRecoveryState(): Flow<RecoveryState?>

    /**
     * Start recovery protocol after a streak break
     */
    suspend fun startRecovery(previousStreak: Int)

    /**
     * Update current recovery phase
     */
    suspend fun advanceToPhase(phase: RecoveryPhase)

    /**
     * Set the reason for streak break
     */
    suspend fun setStreakBreakReason(reason: StreakBreakReason)

    /**
     * Set user's reflection answer
     */
    suspend fun setReflectionAnswer(answer: String)

    /**
     * Set commitment level for moving forward
     */
    suspend fun setCommitmentLevel(level: CommitmentLevel)

    /**
     * Complete recovery and exit protocol
     */
    suspend fun completeRecovery()

    /**
     * Cancel recovery (user dismisses)
     */
    suspend fun cancelRecovery()

    /**
     * Get recovery statistics
     */
    fun getRecoveryStats(): Flow<RecoveryStats>

    /**
     * Check if user should enter recovery (based on missed habits)
     */
    suspend fun checkShouldStartRecovery(): Boolean
}
