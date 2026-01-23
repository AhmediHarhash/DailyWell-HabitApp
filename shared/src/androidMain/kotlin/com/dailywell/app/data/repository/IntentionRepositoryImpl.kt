package com.dailywell.app.data.repository

import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.ImplementationIntention
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Implementation of IntentionRepository using DataStore
 */
class IntentionRepositoryImpl(
    private val dataStoreManager: DataStoreManager
) : IntentionRepository {

    override fun getAllIntentions(): Flow<List<ImplementationIntention>> {
        return dataStoreManager.implementationIntentions
    }

    override fun getIntentionsForHabit(habitId: String): Flow<List<ImplementationIntention>> {
        return dataStoreManager.implementationIntentions.map { intentions ->
            intentions.filter { it.habitId == habitId && it.isEnabled }
        }
    }

    override suspend fun addIntention(intention: ImplementationIntention) {
        val currentIntentions = dataStoreManager.implementationIntentions.first()
        dataStoreManager.updateImplementationIntentions(currentIntentions + intention)
    }

    override suspend fun updateIntention(intention: ImplementationIntention) {
        val currentIntentions = dataStoreManager.implementationIntentions.first()
        val updatedIntentions = currentIntentions.map {
            if (it.id == intention.id) intention else it
        }
        dataStoreManager.updateImplementationIntentions(updatedIntentions)
    }

    override suspend fun toggleIntention(intentionId: String) {
        val currentIntentions = dataStoreManager.implementationIntentions.first()
        val updatedIntentions = currentIntentions.map { intention ->
            if (intention.id == intentionId) {
                intention.copy(isEnabled = !intention.isEnabled)
            } else {
                intention
            }
        }
        dataStoreManager.updateImplementationIntentions(updatedIntentions)
    }

    override suspend fun deleteIntention(intentionId: String) {
        val currentIntentions = dataStoreManager.implementationIntentions.first()
        dataStoreManager.updateImplementationIntentions(
            currentIntentions.filter { it.id != intentionId }
        )
    }

    override suspend fun recordIntentionTriggered(intentionId: String) {
        val currentIntentions = dataStoreManager.implementationIntentions.first()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

        val updatedIntentions = currentIntentions.map { intention ->
            if (intention.id == intentionId) {
                intention.copy(
                    completionCount = intention.completionCount + 1,
                    lastTriggeredAt = today
                )
            } else {
                intention
            }
        }
        dataStoreManager.updateImplementationIntentions(updatedIntentions)
    }

    override suspend fun clearAllIntentions() {
        dataStoreManager.updateImplementationIntentions(emptyList())
    }
}
