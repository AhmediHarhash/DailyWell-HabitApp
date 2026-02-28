package com.dailywell.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.CalendarRepository
import com.dailywell.app.data.repository.EntryRepository
import com.dailywell.app.data.repository.HabitRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class TrackerMode {
    WEEKLY,
    MONTHLY
}

data class CalendarUiState(
    val isLoading: Boolean = true,
    val isCalendarConnected: Boolean = false,
    val connectedAccounts: List<CalendarAccount> = emptyList(),
    val selectedDate: String = LocalDate.now().toString(),
    val todaySchedule: CalendarDaySchedule? = null,
    val weeklySchedules: List<CalendarDaySchedule> = emptyList(),
    val weekDates: List<String> = emptyList(),
    val weeklyHabitGrid: List<WeeklyHabitGridRow> = emptyList(),
    val habitTimeSuggestions: List<HabitTimeSuggestion> = emptyList(),
    val freeTimeSlots: List<FreeTimeSlot> = emptyList(),
    val syncState: CalendarSyncState = CalendarSyncState(),
    val showConnectDialog: Boolean = false,
    val showHabitSettingsDialog: Boolean = false,
    val selectedHabitForSettings: String? = null,
    val habitCalendarSettings: HabitCalendarSettings? = null,
    val trackerMode: TrackerMode = TrackerMode.WEEKLY,
    val visibleMonth: String = YearMonth.now().toString(),
    val monthDates: List<String> = emptyList(),
    val monthlyHabitGrid: List<MonthlyHabitGridRow> = emptyList(),
    val planProgressByHabit: Map<String, HabitPlanProgress> = emptyMap(),
    val updatingGridCells: Set<String> = emptySet(),
    val error: String? = null,
    val successMessage: String? = null,
    // OAuth state
    val isConnecting: Boolean = false,
    val oauthUrl: String? = null
)

data class CalendarSchedulePlan(
    val days: Int = 30,
    val durationMinutes: Int = 30,
    val startHour: Int = 9,
    val endHour: Int = 17,
    val weekdays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val blockTitle: String? = null
)

data class HabitPlanProgress(
    val habitId: String,
    val planStartDate: String,
    val planEndDate: String,
    val targetDays: Int,
    val scheduledDays: Int,
    val completedDays: Int,
    val completionPercent: Float,
    val scheduledPercent: Float,
    val lastUpdatedEpochMs: Long = System.currentTimeMillis()
)

data class WeeklyHabitGridRow(
    val habitId: String,
    val habitName: String,
    val habitEmoji: String,
    val completionsByDate: Map<String, Boolean>,
    val completedCount: Int
)

data class MonthlyHabitGridRow(
    val habitId: String,
    val habitName: String,
    val habitEmoji: String,
    val completionsByDate: Map<String, Boolean>,
    val weeklyCompleted: List<Int>,
    val weeklyTargets: List<Int>,
    val longestRun: Int
)

/**
 * ViewModel for Calendar Integration screen
 */
class CalendarViewModel(
    private val calendarRepository: CalendarRepository,
    private val habitRepository: HabitRepository,
    private val entryRepository: EntryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    private var weeklySchedulesJob: Job? = null
    private var weeklyHabitGridJob: Job? = null
    private var monthlyHabitGridJob: Job? = null

    init {
        loadCalendarData()
    }

    private fun loadCalendarData() {
        // Load connected accounts
        viewModelScope.launch {
            calendarRepository.getConnectedAccounts().collect { accounts ->
                _uiState.value = _uiState.value.copy(
                    connectedAccounts = accounts,
                    isCalendarConnected = accounts.any { it.isConnected }
                )
            }
        }

        // Load sync state
        viewModelScope.launch {
            calendarRepository.getSyncState().collect { state ->
                _uiState.value = _uiState.value.copy(syncState = state)
            }
        }

        // Load today's schedule
        viewModelScope.launch {
            calendarRepository.getTodaySchedule().collect { schedule ->
                _uiState.value = _uiState.value.copy(
                    todaySchedule = schedule,
                    freeTimeSlots = schedule.freeSlots,
                    isLoading = false
                )
            }
        }

        // Load habit time suggestions
        viewModelScope.launch {
            calendarRepository.getHabitTimeSuggestions().collect { suggestions ->
                _uiState.value = _uiState.value.copy(habitTimeSuggestions = suggestions)
            }
        }

        // Load weekly view
        loadWeeklySchedules(referenceDate = LocalDate.now())
        loadMonthlyHabitGrid(YearMonth.now())
    }

    private fun loadWeeklySchedules(referenceDate: LocalDate = LocalDate.now()) {
        val startOfWeek = referenceDate.minusDays((referenceDate.dayOfWeek.value - 1).toLong())
        val weekDates = getWeekDates(startOfWeek)

        weeklySchedulesJob?.cancel()
        weeklySchedulesJob = viewModelScope.launch {
            calendarRepository.getWeeklyCalendarView(startOfWeek.toString()).collect { schedules ->
                _uiState.value = _uiState.value.copy(
                    weeklySchedules = schedules,
                    weekDates = weekDates
                )
            }
        }

        observeWeeklyHabitGrid(startOfWeek)
    }

    // ==================== CALENDAR CONNECTION ====================

    fun showConnectDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showConnectDialog = show)
    }

    fun connectGoogleCalendar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true)
            try {
                val authUrl = calendarRepository.startGoogleOAuth()
                _uiState.value = _uiState.value.copy(
                    oauthUrl = authUrl,
                    showConnectDialog = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to start Google OAuth: ${e.message}",
                    isConnecting = false
                )
            }
        }
    }

    fun connectOutlookCalendar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true)
            try {
                val authUrl = calendarRepository.startOutlookOAuth()
                _uiState.value = _uiState.value.copy(
                    oauthUrl = authUrl,
                    showConnectDialog = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to start Outlook OAuth: ${e.message}",
                    isConnecting = false
                )
            }
        }
    }

    fun handleOAuthCallback(provider: CalendarProvider, authCode: String) {
        viewModelScope.launch {
            try {
                val result = when (provider) {
                    CalendarProvider.GOOGLE -> calendarRepository.completeGoogleOAuth(authCode)
                    CalendarProvider.OUTLOOK -> calendarRepository.completeOutlookOAuth(authCode)
                }

                result.fold(
                    onSuccess = { account ->
                        _uiState.value = _uiState.value.copy(
                            isConnecting = false,
                            oauthUrl = null,
                            successMessage = "${account.displayName} connected successfully!"
                        )
                        // Sync events after connection
                        syncCalendar()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isConnecting = false,
                            oauthUrl = null,
                            error = "Failed to connect: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    oauthUrl = null,
                    error = "OAuth error: ${e.message}"
                )
            }
        }
    }

    fun disconnectCalendar(accountId: String) {
        viewModelScope.launch {
            calendarRepository.disconnectAccount(accountId)
            _uiState.value = _uiState.value.copy(
                successMessage = "Calendar disconnected"
            )
        }
    }

    fun syncCalendar() {
        viewModelScope.launch {
            try {
                val result = calendarRepository.syncEvents()
                result.fold(
                    onSuccess = { count ->
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Synced $count events"
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = "Sync failed: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Sync error: ${e.message}"
                )
            }
        }
    }

    // ==================== DATE SELECTION ====================

    fun selectDate(date: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadScheduleForDate(date)
        runCatching { LocalDate.parse(date) }
            .onSuccess {
                loadWeeklySchedules(referenceDate = it)
                loadMonthlyHabitGrid(YearMonth.from(it))
            }
    }

    private fun loadScheduleForDate(date: String) {
        viewModelScope.launch {
            calendarRepository.getScheduleForDate(date).collect { schedule ->
                _uiState.value = _uiState.value.copy(
                    todaySchedule = schedule,
                    freeTimeSlots = schedule.freeSlots
                )
            }
        }
    }

    // ==================== HABIT SETTINGS ====================

    fun showHabitSettingsDialog(habitId: String?) {
        viewModelScope.launch {
            if (habitId != null) {
                calendarRepository.getHabitCalendarSettings(habitId).collect { settings ->
                    _uiState.value = _uiState.value.copy(
                        showHabitSettingsDialog = true,
                        selectedHabitForSettings = habitId,
                        habitCalendarSettings = settings ?: HabitCalendarSettings(habitId = habitId)
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    showHabitSettingsDialog = false,
                    selectedHabitForSettings = null,
                    habitCalendarSettings = null
                )
            }
        }
    }

    fun updateHabitCalendarSettings(settings: HabitCalendarSettings) {
        viewModelScope.launch {
            calendarRepository.updateHabitCalendarSettings(settings)
            _uiState.value = _uiState.value.copy(
                habitCalendarSettings = settings,
                successMessage = "Settings saved"
            )
        }
    }

    fun toggleAutoBlock(habitId: String, enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.habitCalendarSettings
                ?: HabitCalendarSettings(habitId = habitId)
            val updatedSettings = currentSettings.copy(autoBlockEnabled = enabled)
            calendarRepository.updateHabitCalendarSettings(updatedSettings)

            if (enabled) {
                // Auto-block immediately for today
                autoBlockHabit(habitId)
            }
        }
    }

    // ==================== AUTO-BLOCKING ====================

    fun autoBlockHabit(habitId: String) {
        viewModelScope.launch {
            val date = _uiState.value.selectedDate
            val result = calendarRepository.autoBlockHabit(habitId, date)

            if (result.success) {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Habit time blocked on your calendar!"
                )
                syncCalendar() // Refresh to show new event
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.error ?: "Failed to block habit time"
                )
            }
        }
    }

    fun autoBlockHabitForDays(
        habitId: String,
        days: Int = 30,
        customPlan: CalendarSchedulePlan? = null
    ) {
        viewModelScope.launch {
            val startDate = LocalDate.parse(_uiState.value.selectedDate)
            val totalDays = (customPlan?.days ?: days).coerceAtLeast(1)
            var successCount = 0
            var alreadyPlannedCount = 0
            var outsideWeekdayCount = 0
            var noSlotCount = 0
            var failedCount = 0

            val currentSettings = calendarRepository.getHabitCalendarSettings(habitId).first()
            val baselineSettings = currentSettings ?: HabitCalendarSettings(habitId = habitId)
            val allowedWeekdays = customPlan?.weekdays
                ?.filter { it in 1..7 }
                ?.toSet()
                ?.ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }
                ?: baselineSettings.preferredWeekdays
                    .filter { it in 1..7 }
                    .toSet()
                    .ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }
            val updatedSettings = baselineSettings.copy(
                autoBlockEnabled = true,
                preferredDuration = customPlan?.durationMinutes?.coerceIn(15, 180)
                    ?: baselineSettings.preferredDuration,
                preferredTimeStart = (customPlan?.startHour?.coerceIn(0, 23)?.times(60))
                    ?: baselineSettings.preferredTimeStart,
                preferredTimeEnd = (customPlan?.endHour?.coerceIn(1, 24)?.times(60))
                    ?: baselineSettings.preferredTimeEnd,
                preferredWeekdays = allowedWeekdays.sorted(),
                blockTitle = customPlan?.blockTitle?.trim()?.takeIf { it.isNotEmpty() }
                    ?: baselineSettings.blockTitle
            )
            calendarRepository.updateHabitCalendarSettings(updatedSettings)

            repeat(totalDays) { offset ->
                val dateObj = startDate.plusDays(offset.toLong())
                if (!allowedWeekdays.contains(dateObj.dayOfWeek.value)) {
                    outsideWeekdayCount++
                    return@repeat
                }

                val date = dateObj.toString()
                val result = calendarRepository.autoBlockHabit(habitId, date)

                when {
                    result.success -> successCount++
                    result.error?.contains("already blocked", ignoreCase = true) == true -> alreadyPlannedCount++
                    result.error?.contains("no available time slots", ignoreCase = true) == true -> noSlotCount++
                    else -> failedCount++
                }
            }

            // Refresh from providers once after bulk schedule.
            calendarRepository.syncEvents()

            val targetDays = totalDays - outsideWeekdayCount
            val scheduledDays = successCount + alreadyPlannedCount
            val endDate = startDate.plusDays((totalDays - 1).toLong())
            val completedDays = entryRepository.getEntriesInRange(
                startDate = startDate.toString(),
                endDate = endDate.toString()
            ).first()
                .asSequence()
                .filter { it.habitId == habitId && it.completed }
                .mapNotNull { entry ->
                    runCatching { LocalDate.parse(entry.date) }.getOrNull()
                }
                .filter { date ->
                    !date.isBefore(startDate) &&
                        !date.isAfter(endDate) &&
                        allowedWeekdays.contains(date.dayOfWeek.value)
                }
                .map { it.toString() }
                .distinct()
                .count()

            val completionPercent = if (targetDays > 0) {
                (completedDays.toFloat() / targetDays.toFloat()) * 100f
            } else {
                0f
            }
            val scheduledPercent = if (targetDays > 0) {
                (scheduledDays.toFloat() / targetDays.toFloat()) * 100f
            } else {
                0f
            }

            val progressMap = _uiState.value.planProgressByHabit.toMutableMap()
            progressMap[habitId] = HabitPlanProgress(
                habitId = habitId,
                planStartDate = startDate.toString(),
                planEndDate = endDate.toString(),
                targetDays = targetDays,
                scheduledDays = scheduledDays,
                completedDays = completedDays,
                completionPercent = completionPercent.coerceIn(0f, 100f),
                scheduledPercent = scheduledPercent.coerceIn(0f, 100f)
            )

            val message = buildString {
                append("Scheduled ")
                append(successCount + alreadyPlannedCount)
                append("/")
                append(targetDays)
                append(" days")
                if (alreadyPlannedCount > 0) {
                    append(" (")
                    append(alreadyPlannedCount)
                    append(" already planned)")
                }
            }

            val errors = buildList {
                if (noSlotCount > 0) add("$noSlotCount day(s) had no free slot")
                if (failedCount > 0) add("$failedCount day(s) failed")
            }

            _uiState.value = _uiState.value.copy(
                planProgressByHabit = progressMap,
                successMessage = message,
                error = if (errors.isNotEmpty()) errors.joinToString(". ") + "." else null
            )
        }
    }

    fun autoBlockHabitWithCustomPlan(habitId: String, plan: CalendarSchedulePlan) {
        autoBlockHabitForDays(habitId = habitId, days = plan.days, customPlan = plan)
    }

    fun removeHabitBlock(habitId: String, eventId: String) {
        viewModelScope.launch {
            val success = calendarRepository.removeHabitBlock(habitId, eventId)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Habit block removed"
                )
                syncCalendar()
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to remove habit block"
                )
            }
        }
    }

    // ==================== SUGGESTIONS ====================

    fun acceptSuggestion(suggestion: HabitTimeSuggestion) {
        viewModelScope.launch {
            val accounts = _uiState.value.connectedAccounts.filter { it.isConnected }
            if (accounts.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    error = "No calendar connected"
                )
                return@launch
            }

            val account = accounts.first()
            val result = calendarRepository.createHabitEvent(
                habitId = suggestion.habitId,
                title = suggestion.habitName,
                startTime = suggestion.suggestedTime,
                durationMinutes = ((suggestion.endTime - suggestion.suggestedTime) / 60000).toInt(),
                accountId = account.id
            )

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "${suggestion.habitName} scheduled!"
                    )
                    syncCalendar()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to schedule: ${error.message}"
                    )
                }
            )
        }
    }

    // ==================== NOTIFICATIONS ====================

    fun scheduleNotificationForFreeSlot(habitId: String, freeSlot: FreeTimeSlot) {
        viewModelScope.launch {
            val habits = habitRepository.getAllHabits().first()
            val habit = habits.find { it.id == habitId }
            val message = "You have ${freeSlot.durationMinutes} minutes free - perfect for ${habit?.name ?: "your habit"}!"

            calendarRepository.scheduleFreeSlotNotification(habitId, freeSlot, message)
            _uiState.value = _uiState.value.copy(
                successMessage = "Reminder scheduled"
            )
        }
    }

    fun toggleHabitCompletionForDate(
        habitId: String,
        date: String,
        currentlyCompleted: Boolean
    ) {
        val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return
        if (parsedDate.isAfter(LocalDate.now())) {
            _uiState.value = _uiState.value.copy(
                error = "Future days can't be checked off yet"
            )
            return
        }

        val cellKey = buildCellKey(habitId, date)
        if (_uiState.value.updatingGridCells.contains(cellKey)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                updatingGridCells = _uiState.value.updatingGridCells + cellKey
            )
            runCatching {
                entryRepository.setHabitCompletion(
                    date = date,
                    habitId = habitId,
                    completed = !currentlyCompleted
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = "Could not update tracker: ${error.message}"
                )
            }

            _uiState.value = _uiState.value.copy(
                updatingGridCells = _uiState.value.updatingGridCells - cellKey
            )
        }
    }

    fun setTrackerMode(mode: TrackerMode) {
        _uiState.value = _uiState.value.copy(trackerMode = mode)
    }

    fun showPreviousMonth() {
        val currentMonth = parseVisibleMonthOrNow()
        val targetMonth = currentMonth.minusMonths(1)
        loadMonthlyHabitGrid(targetMonth)
        selectDate(alignDateToMonth(targetMonth))
    }

    fun showNextMonth() {
        val currentMonth = parseVisibleMonthOrNow()
        val targetMonth = currentMonth.plusMonths(1)
        loadMonthlyHabitGrid(targetMonth)
        selectDate(alignDateToMonth(targetMonth))
    }

    // ==================== UTILITY ====================

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun dismissOAuthUrl() {
        _uiState.value = _uiState.value.copy(
            oauthUrl = null,
            isConnecting = false
        )
    }

    private fun observeWeeklyHabitGrid(startOfWeek: LocalDate) {
        val endOfWeek = startOfWeek.plusDays(6)
        val weekDates = getWeekDates(startOfWeek)

        weeklyHabitGridJob?.cancel()
        weeklyHabitGridJob = viewModelScope.launch {
            combine(
                habitRepository.getAllHabits(),
                entryRepository.getEntriesInRange(startOfWeek.toString(), endOfWeek.toString())
            ) { habits, entries ->
                val activeHabits = habits
                    .filter { it.isEnabled }
                    .sortedBy { it.order }

                activeHabits.map { habit ->
                    val completionsByDate = weekDates.associateWith { date ->
                        entries.any { entry ->
                            entry.habitId == habit.id &&
                                entry.date == date &&
                                entry.completed
                        }
                    }
                    WeeklyHabitGridRow(
                        habitId = habit.id,
                        habitName = habit.name,
                        habitEmoji = habit.emoji,
                        completionsByDate = completionsByDate,
                        completedCount = completionsByDate.values.count { it }
                    )
                }
            }.collect { rows ->
                _uiState.value = _uiState.value.copy(
                    weekDates = weekDates,
                    weeklyHabitGrid = rows
                )
            }
        }
    }

    private fun loadMonthlyHabitGrid(yearMonth: YearMonth) {
        val monthStart = yearMonth.atDay(1)
        val monthEnd = yearMonth.atEndOfMonth()
        val monthDates = (1..yearMonth.lengthOfMonth()).map { day ->
            yearMonth.atDay(day).toString()
        }
        val weekCount = ((yearMonth.lengthOfMonth() - 1) / 7) + 1
        val today = LocalDate.now()

        monthlyHabitGridJob?.cancel()
        monthlyHabitGridJob = viewModelScope.launch {
            combine(
                habitRepository.getAllHabits(),
                entryRepository.getEntriesInRange(monthStart.toString(), monthEnd.toString())
            ) { habits, entries ->
                val activeHabits = habits
                    .filter { it.isEnabled }
                    .sortedBy { it.order }

                activeHabits.map { habit ->
                    val completionsByDate = monthDates.associateWith { date ->
                        entries.any { entry ->
                            entry.habitId == habit.id &&
                                entry.date == date &&
                                entry.completed
                        }
                    }

                    val weeklyCompleted = MutableList(weekCount) { 0 }
                    val weeklyTargets = MutableList(weekCount) { 0 }
                    val trackableDates = mutableListOf<String>()

                    monthDates.forEach { date ->
                        val localDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return@forEach
                        val weekIndex = ((localDate.dayOfMonth - 1) / 7).coerceIn(0, weekCount - 1)

                        if (!localDate.isAfter(today)) {
                            weeklyTargets[weekIndex] = weeklyTargets[weekIndex] + 1
                            trackableDates.add(date)
                        }

                        if (completionsByDate[date] == true) {
                            weeklyCompleted[weekIndex] = weeklyCompleted[weekIndex] + 1
                        }
                    }

                    MonthlyHabitGridRow(
                        habitId = habit.id,
                        habitName = habit.name,
                        habitEmoji = habit.emoji,
                        completionsByDate = completionsByDate,
                        weeklyCompleted = weeklyCompleted,
                        weeklyTargets = weeklyTargets,
                        longestRun = calculateLongestRun(completionsByDate, trackableDates)
                    )
                }
            }.collect { rows ->
                _uiState.value = _uiState.value.copy(
                    visibleMonth = yearMonth.toString(),
                    monthDates = monthDates,
                    monthlyHabitGrid = rows
                )
            }
        }
    }

    private fun getWeekDates(startOfWeek: LocalDate): List<String> {
        return (0..6).map { offset ->
            startOfWeek.plusDays(offset.toLong()).toString()
        }
    }

    private fun parseVisibleMonthOrNow(): YearMonth {
        return runCatching { YearMonth.parse(_uiState.value.visibleMonth) }
            .getOrElse { YearMonth.now() }
    }

    private fun alignDateToMonth(yearMonth: YearMonth): String {
        val currentDate = runCatching { LocalDate.parse(_uiState.value.selectedDate) }
            .getOrElse { LocalDate.now() }
        val day = currentDate.dayOfMonth.coerceIn(1, yearMonth.lengthOfMonth())
        return yearMonth.atDay(day).toString()
    }

    private fun calculateLongestRun(
        completionsByDate: Map<String, Boolean>,
        orderedDates: List<String>
    ): Int {
        var longest = 0
        var current = 0
        orderedDates.forEach { date ->
            if (completionsByDate[date] == true) {
                current += 1
                if (current > longest) longest = current
            } else {
                current = 0
            }
        }
        return longest
    }

    private fun buildCellKey(habitId: String, date: String): String = "$habitId|$date"
}
