package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Advanced AI Coaching features
 */
interface AICoachingRepository {
    // Coach persona
    fun getSelectedCoach(): Flow<CoachPersona>
    suspend fun selectCoach(coachId: String)
    fun getAvailableCoaches(): List<CoachPersona>

    // Daily insights
    fun getDailyInsight(): Flow<DailyCoachingInsight?>
    suspend fun generateDailyInsight()
    suspend fun markSuggestedActionDone(actionId: String)

    // Coaching sessions
    fun getActiveSessions(): Flow<List<AICoachingSession>>
    fun getSessionHistory(): Flow<List<AICoachingSession>>
    suspend fun startSession(type: CoachingSessionType): AICoachingSession
    suspend fun sendMessage(sessionId: String, message: String): CoachingMessage
    suspend fun selectQuickReply(sessionId: String, reply: String): CoachingMessage
    suspend fun completeSession(sessionId: String)
    suspend fun abandonSession(sessionId: String)

    // Action items
    fun getActionItems(): Flow<List<CoachingActionItem>>
    suspend fun completeActionItem(itemId: String)
    suspend fun dismissActionItem(itemId: String)

    // Weekly summary
    fun getWeeklySummary(): Flow<WeeklyCoachingSummary?>
    suspend fun generateWeeklySummary()

    // Contextual coaching
    suspend fun getMotivationBoost(): String
    suspend fun getRecoveryMessage(habitId: String): String
    suspend fun getCelebrationMessage(habitId: String, completionCount: Int): String
}
