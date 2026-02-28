package com.dailywell.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dailywell.app.data.model.HabitStack
import com.dailywell.app.data.model.ImplementationIntention
import com.dailywell.app.data.model.SmartReminderData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dailywell_prefs")

class DataStoreManager(private val context: Context) {

    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun stringKey(name: String) = stringPreferencesKey(name)

    // Keys for new Phase 2 features
    private val HABIT_STACKS_KEY = "habit_stacks"
    private val IMPLEMENTATION_INTENTIONS_KEY = "implementation_intentions"
    private val SMART_REMINDERS_KEY = "smart_reminders"
    private val RECOVERY_STATE_KEY = "recovery_state"

    suspend fun putString(key: String, value: String) {
        context.dataStore.edit { preferences ->
            preferences[stringKey(key)] = value
        }
    }

    fun getString(key: String): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[stringKey(key)]
        }
    }

    suspend fun remove(key: String) {
        context.dataStore.edit { preferences ->
            preferences.remove(stringKey(key))
        }
    }

    suspend fun <T> putObject(key: String, value: T, serializer: (T) -> String) {
        putString(key, serializer(value))
    }

    fun <T> getObject(key: String, deserializer: (String) -> T): Flow<T?> {
        return getString(key).map { jsonStr ->
            jsonStr?.let { deserializer(it) }
        }
    }

    inline fun <reified T> putSerializable(key: String, value: T): suspend () -> Unit = {
        putString(key, json.encodeToString(value))
    }

    inline fun <reified T> getSerializable(key: String): Flow<T?> {
        return getString(key).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<T>(it)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    // ============================================================
    // HABIT STACKING
    // ============================================================

    val habitStacks: Flow<List<HabitStack>>
        get() = getString(HABIT_STACKS_KEY).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<List<HabitStack>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

    suspend fun updateHabitStacks(stacks: List<HabitStack>) {
        putString(HABIT_STACKS_KEY, json.encodeToString(stacks))
    }

    // ============================================================
    // IMPLEMENTATION INTENTIONS
    // ============================================================

    val implementationIntentions: Flow<List<ImplementationIntention>>
        get() = getString(IMPLEMENTATION_INTENTIONS_KEY).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<List<ImplementationIntention>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

    suspend fun updateImplementationIntentions(intentions: List<ImplementationIntention>) {
        putString(IMPLEMENTATION_INTENTIONS_KEY, json.encodeToString(intentions))
    }

    // ============================================================
    // SMART REMINDERS
    // ============================================================

    val smartReminders: Flow<SmartReminderData?>
        get() = getString(SMART_REMINDERS_KEY).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<SmartReminderData>(it)
                } catch (e: Exception) {
                    null
                }
            }
        }

    suspend fun updateSmartReminders(data: SmartReminderData) {
        putString(SMART_REMINDERS_KEY, json.encodeToString(data))
    }

    // ============================================================
    // RECOVERY STATE
    // ============================================================

    val recoveryState: Flow<String?>
        get() = getString(RECOVERY_STATE_KEY)

    suspend fun updateRecoveryState(state: String?) {
        if (state != null) {
            putString(RECOVERY_STATE_KEY, state)
        } else {
            context.dataStore.edit { preferences ->
                preferences.remove(stringKey(RECOVERY_STATE_KEY))
            }
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
