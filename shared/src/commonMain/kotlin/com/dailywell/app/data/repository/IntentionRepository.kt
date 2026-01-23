package com.dailywell.app.data.repository

import com.dailywell.app.data.model.ImplementationIntention
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Implementation Intentions
 * Based on Peter Gollwitzer's research - "When X, I will Y" planning
 */
interface IntentionRepository {

    /**
     * Get all implementation intentions
     */
    fun getAllIntentions(): Flow<List<ImplementationIntention>>

    /**
     * Get intentions for a specific habit
     */
    fun getIntentionsForHabit(habitId: String): Flow<List<ImplementationIntention>>

    /**
     * Add a new implementation intention
     */
    suspend fun addIntention(intention: ImplementationIntention)

    /**
     * Update an existing intention
     */
    suspend fun updateIntention(intention: ImplementationIntention)

    /**
     * Toggle an intention's enabled status
     */
    suspend fun toggleIntention(intentionId: String)

    /**
     * Delete an intention
     */
    suspend fun deleteIntention(intentionId: String)

    /**
     * Record that an intention was triggered/completed
     */
    suspend fun recordIntentionTriggered(intentionId: String)

    /**
     * Clear all intentions (for testing or reset)
     */
    suspend fun clearAllIntentions()
}
