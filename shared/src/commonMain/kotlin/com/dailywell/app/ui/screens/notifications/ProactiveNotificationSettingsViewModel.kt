package com.dailywell.app.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.ProactiveNotificationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProactiveNotificationSettingsViewModel(
    private val notificationRepository: ProactiveNotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProactiveNotificationSettingsState())
    val uiState: StateFlow<ProactiveNotificationSettingsState> = _uiState.asStateFlow()

    init {
        loadPreferences()
        loadHistory()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            notificationRepository.getPreferences().collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            notificationRepository.getNotificationHistory(7).collect { history ->
                val stats = calculateStats(history)
                _uiState.update {
                    it.copy(
                        recentHistory = history.take(10),
                        stats = stats
                    )
                }
            }
        }
    }

    private fun calculateStats(history: List<NotificationHistory>): NotificationStats {
        if (history.isEmpty()) return NotificationStats()

        val totalSent = history.size
        val totalOpened = history.count { it.opened }
        val openRate = if (totalSent > 0) totalOpened.toFloat() / totalSent else 0f

        val typeBreakdown = history.groupBy { it.type }
            .mapValues { it.value.size }

        val mostEngagingType = typeBreakdown.maxByOrNull { it.value }?.key

        return NotificationStats(
            totalSent = totalSent,
            totalOpened = totalOpened,
            openRate = openRate,
            typeBreakdown = typeBreakdown,
            mostEngagingType = mostEngagingType
        )
    }

    fun toggleProactiveNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.preferences
            notificationRepository.updatePreferences(current.copy(enabled = enabled))
        }
    }

    fun toggleNotificationType(type: ProactiveNotificationType, enabled: Boolean) {
        viewModelScope.launch {
            notificationRepository.toggleNotificationType(type, enabled)
        }
    }

    fun updateTone(tone: NotificationTone) {
        viewModelScope.launch {
            val current = _uiState.value.preferences
            notificationRepository.updatePreferences(current.copy(tone = tone))
        }
    }

    fun updateMorningWindow(start: Int, end: Int) {
        viewModelScope.launch {
            val current = _uiState.value.preferences
            notificationRepository.updatePreferences(
                current.copy(morningWindowStart = start, morningWindowEnd = end)
            )
        }
    }

    fun updateEveningWindow(start: Int, end: Int) {
        viewModelScope.launch {
            val current = _uiState.value.preferences
            notificationRepository.updatePreferences(
                current.copy(eveningWindowStart = start, eveningWindowEnd = end)
            )
        }
    }

    fun updateQuietHours(start: Int, end: Int) {
        viewModelScope.launch {
            notificationRepository.setQuietHours(start, end)
        }
    }

    fun updateMaxNotificationsPerDay(max: Int) {
        viewModelScope.launch {
            val current = _uiState.value.preferences
            notificationRepository.updatePreferences(current.copy(maxNotificationsPerDay = max))
        }
    }

    fun toggleSmartTiming(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.preferences
            notificationRepository.updatePreferences(current.copy(useSmartTiming = enabled))
        }
    }
}

data class ProactiveNotificationSettingsState(
    val preferences: ProactiveNotificationPreferences = ProactiveNotificationPreferences(),
    val recentHistory: List<NotificationHistory> = emptyList(),
    val stats: NotificationStats = NotificationStats()
)

data class NotificationStats(
    val totalSent: Int = 0,
    val totalOpened: Int = 0,
    val openRate: Float = 0f,
    val typeBreakdown: Map<ProactiveNotificationType, Int> = emptyMap(),
    val mostEngagingType: ProactiveNotificationType? = null
)
