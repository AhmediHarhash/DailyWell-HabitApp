package com.dailywell.app.data.repository

import com.dailywell.app.api.CalendarService
import com.dailywell.app.api.GoogleEventCreate
import com.dailywell.app.api.OutlookEventCreate
import com.dailywell.app.api.OutlookEventBody
import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Implementation of CalendarRepository for Android
 * Handles Google Calendar and Outlook Calendar integration
 */
class CalendarRepositoryImpl(
    private val dataStoreManager: DataStoreManager,
    private val calendarService: CalendarService,
    private val habitRepository: HabitRepository
) : CalendarRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Cached data
    private val _connectedAccounts = MutableStateFlow<List<CalendarAccount>>(emptyList())
    private val _cachedEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    private val _syncState = MutableStateFlow(CalendarSyncState())
    private val _habitCalendarSettings = MutableStateFlow<Map<String, HabitCalendarSettings>>(emptyMap())
    private val _calendarNotifications = MutableStateFlow<List<CalendarNotification>>(emptyList())

    companion object {
        private const val KEY_CALENDAR_ACCOUNTS = "calendar_accounts"
        private const val KEY_CACHED_EVENTS = "calendar_cached_events"
        private const val KEY_HABIT_CALENDAR_SETTINGS = "habit_calendar_settings"
        private const val KEY_SYNC_STATE = "calendar_sync_state"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            try { loadAccounts() } catch (_: Exception) {}
            try { loadHabitCalendarSettings() } catch (_: Exception) {}
        }
    }

    // ==================== CONNECTION STATUS ====================

    override fun getConnectedAccounts(): Flow<List<CalendarAccount>> = _connectedAccounts

    override fun isCalendarConnected(): Flow<Boolean> = _connectedAccounts.map { it.isNotEmpty() && it.any { acc -> acc.isConnected } }

    override fun getSyncState(): Flow<CalendarSyncState> = _syncState

    // ==================== OAUTH AUTHENTICATION ====================

    override suspend fun startGoogleOAuth(): String {
        return calendarService.getGoogleAuthUrl()
    }

    override suspend fun completeGoogleOAuth(authCode: String): Result<CalendarAccount> {
        val tokensResult = calendarService.exchangeGoogleCode(authCode)
        return tokensResult.fold(
            onSuccess = { tokens ->
                // Get user's primary calendar info
                val calendarsResult = calendarService.getGoogleCalendars(tokens.accessToken)
                val primaryCalendar = calendarsResult.getOrNull()?.find { it.primary }

                val account = CalendarAccount(
                    id = UUID.randomUUID().toString(),
                    provider = CalendarProvider.GOOGLE,
                    email = primaryCalendar?.summary ?: "Google Calendar",
                    displayName = primaryCalendar?.summary ?: "Google Calendar",
                    isConnected = true,
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    tokenExpiry = System.currentTimeMillis() + (tokens.expiresIn * 1000),
                    lastSyncTime = null,
                    syncEnabled = true
                )

                // Add to connected accounts
                val currentAccounts = _connectedAccounts.value.toMutableList()
                currentAccounts.removeAll { it.provider == CalendarProvider.GOOGLE }
                currentAccounts.add(account)
                _connectedAccounts.value = currentAccounts

                // Save to DataStore
                saveAccounts(currentAccounts)

                Result.success(account)
            },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun startOutlookOAuth(): String {
        return calendarService.getOutlookAuthUrl()
    }

    override suspend fun completeOutlookOAuth(authCode: String): Result<CalendarAccount> {
        val tokensResult = calendarService.exchangeOutlookCode(authCode)
        return tokensResult.fold(
            onSuccess = { tokens ->
                // Get user profile
                val profileResult = calendarService.getOutlookProfile(tokens.accessToken)
                val profile = profileResult.getOrNull()

                val account = CalendarAccount(
                    id = UUID.randomUUID().toString(),
                    provider = CalendarProvider.OUTLOOK,
                    email = profile?.mail ?: profile?.userPrincipalName ?: "Outlook Calendar",
                    displayName = profile?.displayName ?: "Outlook Calendar",
                    isConnected = true,
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    tokenExpiry = System.currentTimeMillis() + (tokens.expiresIn * 1000),
                    lastSyncTime = null,
                    syncEnabled = true
                )

                // Add to connected accounts
                val currentAccounts = _connectedAccounts.value.toMutableList()
                currentAccounts.removeAll { it.provider == CalendarProvider.OUTLOOK }
                currentAccounts.add(account)
                _connectedAccounts.value = currentAccounts

                // Save to DataStore
                saveAccounts(currentAccounts)

                Result.success(account)
            },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun disconnectAccount(accountId: String) {
        val currentAccounts = _connectedAccounts.value.toMutableList()
        currentAccounts.removeAll { it.id == accountId }
        _connectedAccounts.value = currentAccounts
        saveAccounts(currentAccounts)

        // Remove cached events from this account
        val currentEvents = _cachedEvents.value.toMutableList()
        currentEvents.removeAll { it.calendarId == accountId }
        _cachedEvents.value = currentEvents
    }

    override suspend fun refreshToken(accountId: String): Boolean {
        val account = _connectedAccounts.value.find { it.id == accountId } ?: return false
        val refreshToken = account.refreshToken ?: return false

        val result = when (account.provider) {
            CalendarProvider.GOOGLE -> calendarService.refreshGoogleToken(refreshToken)
            CalendarProvider.OUTLOOK -> calendarService.refreshOutlookToken(refreshToken)
        }

        return result.fold(
            onSuccess = { tokens ->
                val updatedAccount = account.copy(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken ?: account.refreshToken,
                    tokenExpiry = System.currentTimeMillis() + (tokens.expiresIn * 1000)
                )
                updateAccount(updatedAccount)
                true
            },
            onFailure = { false }
        )
    }

    // ==================== CALENDAR EVENTS ====================

    override suspend fun syncEvents(): Result<Int> {
        _syncState.value = _syncState.value.copy(isSyncing = true, lastError = null)

        val accounts = _connectedAccounts.value.filter { it.isConnected && it.syncEnabled }
        if (accounts.isEmpty()) {
            _syncState.value = _syncState.value.copy(isSyncing = false)
            return Result.success(0)
        }

        val allEvents = mutableListOf<CalendarEvent>()
        val today = LocalDate.now()
        val startDate = today.minusDays(7)
        val endDate = today.plusDays(30)

        for (account in accounts) {
            try {
                // Check if token needs refresh
                if (account.tokenExpiry != null && account.tokenExpiry < System.currentTimeMillis()) {
                    if (!refreshToken(account.id)) {
                        continue
                    }
                }

                val accessToken = _connectedAccounts.value.find { it.id == account.id }?.accessToken ?: continue

                when (account.provider) {
                    CalendarProvider.GOOGLE -> {
                        val startStr = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
                        val endStr = endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toString()

                        val result = calendarService.getGoogleEvents(
                            accessToken = accessToken,
                            timeMin = startStr,
                            timeMax = endStr
                        )

                        result.onSuccess { events ->
                            allEvents.addAll(events.map { googleEvent ->
                                calendarService.googleEventToCalendarEvent(googleEvent, "primary", account.id)
                            })
                        }
                    }

                    CalendarProvider.OUTLOOK -> {
                        val startStr = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
                        val endStr = endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toString()

                        val result = calendarService.getOutlookEvents(
                            accessToken = accessToken,
                            startDateTime = startStr,
                            endDateTime = endStr
                        )

                        result.onSuccess { events ->
                            allEvents.addAll(events.map { outlookEvent ->
                                calendarService.outlookEventToCalendarEvent(outlookEvent, account.id)
                            })
                        }
                    }
                }

                // Update account last sync time
                updateAccount(account.copy(lastSyncTime = System.currentTimeMillis()))

            } catch (e: Exception) {
                _syncState.value = _syncState.value.copy(lastError = e.message)
            }
        }

        _cachedEvents.value = allEvents
        _syncState.value = _syncState.value.copy(
            isSyncing = false,
            lastSyncTime = System.currentTimeMillis(),
            eventsCount = allEvents.size
        )

        return Result.success(allEvents.size)
    }

    override fun getEventsForDate(date: String): Flow<List<CalendarEvent>> = flow {
        val targetDate = LocalDate.parse(date)
        val startOfDay = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = targetDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        _cachedEvents.collect { events ->
            emit(events.filter { event ->
                event.startTime >= startOfDay && event.startTime < endOfDay
            }.sortedBy { it.startTime })
        }
    }

    override suspend fun getEventsForRange(startDate: String, endDate: String): List<CalendarEvent> {
        val start = LocalDate.parse(startDate).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = LocalDate.parse(endDate).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return _cachedEvents.value.filter { event ->
            event.startTime >= start && event.startTime <= end
        }.sortedBy { it.startTime }
    }

    override fun getTodaySchedule(): Flow<CalendarDaySchedule> {
        return getScheduleForDate(LocalDate.now().toString())
    }

    override fun getScheduleForDate(date: String): Flow<CalendarDaySchedule> = flow {
        val targetDate = LocalDate.parse(date)
        val startOfDay = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = targetDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        combine(_cachedEvents, _connectedAccounts) { events, accounts ->
            val dayEvents = events.filter { event ->
                event.startTime >= startOfDay && event.startTime < endOfDay
            }.sortedBy { it.startTime }

            val freeSlots = calculateFreeSlotsInternal(dayEvents, startOfDay, endOfDay)
            val habitSuggestions = generateHabitSuggestions(freeSlots)

            // Calculate busy percentage (9 AM to 6 PM working hours)
            val workingStartHour = 9
            val workingEndHour = 18
            val workingMinutes = (workingEndHour - workingStartHour) * 60
            val busyMinutes = dayEvents
                .filter { it.busyStatus == BusyStatus.BUSY }
                .sumOf { event ->
                    val eventStart = Instant.ofEpochMilli(event.startTime).atZone(ZoneId.systemDefault()).toLocalTime()
                    val eventEnd = Instant.ofEpochMilli(event.endTime).atZone(ZoneId.systemDefault()).toLocalTime()
                    val workStart = LocalTime.of(workingStartHour, 0)
                    val workEnd = LocalTime.of(workingEndHour, 0)

                    val effectiveStart = if (eventStart.isBefore(workStart)) workStart else eventStart
                    val effectiveEnd = if (eventEnd.isAfter(workEnd)) workEnd else eventEnd

                    if (effectiveEnd.isAfter(effectiveStart)) {
                        java.time.Duration.between(effectiveStart, effectiveEnd).toMinutes().toInt()
                    } else 0
                }

            val busyPercentage = (busyMinutes.toFloat() / workingMinutes * 100).coerceIn(0f, 100f)

            CalendarDaySchedule(
                date = date,
                events = dayEvents,
                freeSlots = freeSlots,
                habitSuggestions = habitSuggestions,
                busyPercentage = busyPercentage,
                isCalendarConnected = accounts.any { it.isConnected }
            )
        }.collect { emit(it) }
    }

    // ==================== FREE TIME SLOTS ====================

    override suspend fun calculateFreeSlots(date: String, minDurationMinutes: Int): List<FreeTimeSlot> {
        val targetDate = LocalDate.parse(date)
        val startOfDay = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = targetDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val events = _cachedEvents.value.filter { event ->
            event.startTime >= startOfDay && event.startTime < endOfDay && event.busyStatus == BusyStatus.BUSY
        }.sortedBy { it.startTime }

        return calculateFreeSlotsInternal(events, startOfDay, endOfDay, minDurationMinutes)
    }

    private suspend fun calculateFreeSlotsInternal(
        events: List<CalendarEvent>,
        dayStart: Long,
        dayEnd: Long,
        minDurationMinutes: Int = 15
    ): List<FreeTimeSlot> {
        val freeSlots = mutableListOf<FreeTimeSlot>()
        val busyEvents = events.filter { it.busyStatus == BusyStatus.BUSY }.sortedBy { it.startTime }

        // Define working hours (8 AM to 9 PM)
        val targetDate = Instant.ofEpochMilli(dayStart).atZone(ZoneId.systemDefault()).toLocalDate()
        val workingStart = targetDate.atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val workingEnd = targetDate.atTime(21, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        var currentTime = maxOf(workingStart, System.currentTimeMillis())

        for (event in busyEvents) {
            if (event.startTime > currentTime) {
                val slotEnd = minOf(event.startTime, workingEnd)
                val durationMinutes = ((slotEnd - currentTime) / 60000).toInt()

                if (durationMinutes >= minDurationMinutes) {
                    val qualityScore = calculateSlotQuality(currentTime, slotEnd)
                    freeSlots.add(
                        FreeTimeSlot(
                            startTime = currentTime,
                            endTime = slotEnd,
                            durationMinutes = durationMinutes,
                            qualityScore = qualityScore
                        )
                    )
                }
            }
            currentTime = maxOf(currentTime, event.endTime)
        }

        // Add remaining time until end of working hours
        if (currentTime < workingEnd) {
            val durationMinutes = ((workingEnd - currentTime) / 60000).toInt()
            if (durationMinutes >= minDurationMinutes) {
                val qualityScore = calculateSlotQuality(currentTime, workingEnd)
                freeSlots.add(
                    FreeTimeSlot(
                        startTime = currentTime,
                        endTime = workingEnd,
                        durationMinutes = durationMinutes,
                        qualityScore = qualityScore
                    )
                )
            }
        }

        return freeSlots
    }

    private fun calculateSlotQuality(startTime: Long, endTime: Long): Float {
        val startHour = Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault()).hour
        val durationMinutes = ((endTime - startTime) / 60000).toInt()

        // Quality factors:
        // - Morning slots (6-10 AM) are great for exercise
        // - Lunch slots (12-1 PM) are good for quick habits
        // - Afternoon slots (2-5 PM) are good for meditation/breaks
        // - Evening slots (6-9 PM) are good for reflection/journaling

        var score = 0.5f

        // Time of day bonus
        score += when (startHour) {
            in 6..9 -> 0.2f  // Morning bonus
            in 12..13 -> 0.1f // Lunch
            in 14..17 -> 0.15f // Afternoon
            in 18..21 -> 0.1f // Evening
            else -> 0f
        }

        // Duration bonus (longer slots are better)
        score += when {
            durationMinutes >= 60 -> 0.2f
            durationMinutes >= 45 -> 0.15f
            durationMinutes >= 30 -> 0.1f
            else -> 0f
        }

        return score.coerceIn(0f, 1f)
    }

    override fun getTodayFreeSlots(): Flow<List<FreeTimeSlot>> = flow {
        getTodaySchedule().collect { schedule ->
            emit(schedule.freeSlots)
        }
    }

    // ==================== HABIT TIME SUGGESTIONS ====================

    private suspend fun generateHabitSuggestions(freeSlots: List<FreeTimeSlot>): List<HabitTimeSuggestion> {
        val habits = habitRepository.getAllHabits().first().filter { it.isEnabled }
        val suggestions = mutableListOf<HabitTimeSuggestion>()
        val today = LocalDate.now().toString()

        for (habit in habits) {
            val settings = _habitCalendarSettings.value[habit.id]
            val preferredDuration = settings?.preferredDuration ?: 30

            // Find the best slot for this habit
            val suitableSlots = applySchedulingPreferences(
                slots = freeSlots,
                date = today,
                settings = settings,
                requiredDuration = preferredDuration
            )
            if (suitableSlots.isEmpty()) continue

            // Sort by quality score and pick the best one
            val bestSlot = suitableSlots.maxByOrNull { it.qualityScore } ?: continue
            val endTime = bestSlot.startTime + (preferredDuration * 60000L)

            val timeStr = Instant.ofEpochMilli(bestSlot.startTime)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("h:mm a"))

            suggestions.add(
                HabitTimeSuggestion(
                    habitId = habit.id,
                    habitName = habit.name,
                    habitEmoji = habit.emoji,
                    suggestedTime = bestSlot.startTime,
                    endTime = endTime,
                    reason = "${bestSlot.durationMinutes}-minute gap at $timeStr",
                    confidence = bestSlot.qualityScore,
                    isOptimal = bestSlot == suitableSlots.maxByOrNull { it.qualityScore }
                )
            )
        }

        return suggestions.sortedByDescending { it.confidence }
    }

    override fun getHabitTimeSuggestions(): Flow<List<HabitTimeSuggestion>> = flow {
        getTodaySchedule().collect { schedule ->
            emit(schedule.habitSuggestions)
        }
    }

    override suspend fun getBestTimeForHabit(habitId: String, date: String): BestTimeAnalysis {
        val habits = habitRepository.getAllHabits().first()
        val habit = habits.find { it.id == habitId }
        val settings = _habitCalendarSettings.value[habitId]
        val preferredDuration = settings?.preferredDuration ?: 30

        val freeSlots = calculateFreeSlots(date, preferredDuration)
        val suitableSlots = applySchedulingPreferences(
            slots = freeSlots,
            date = date,
            settings = settings,
            requiredDuration = preferredDuration
        )

        val bestSlots = suitableSlots.sortedByDescending { it.qualityScore }.take(3)
        val alternativeSlots = suitableSlots.drop(3).take(2)

        val recommendation = if (bestSlots.isNotEmpty()) {
            val timeStr = Instant.ofEpochMilli(bestSlots.first().startTime)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("h:mm a"))
            "Best time for ${habit?.name ?: "this habit"} today is $timeStr"
        } else {
            "No suitable time slots found for ${habit?.name ?: "this habit"} today"
        }

        return BestTimeAnalysis(
            habitId = habitId,
            date = date,
            bestSlots = bestSlots,
            recommendation = recommendation,
            hasConflicts = suitableSlots.isEmpty(),
            alternativeSlots = alternativeSlots
        )
    }

    override suspend fun analyzeOptimalTimes(habitId: String, durationMinutes: Int): List<FreeTimeSlot> {
        val today = LocalDate.now().toString()
        val freeSlots = calculateFreeSlots(today, durationMinutes)
        return freeSlots.sortedByDescending { it.qualityScore }
    }

    // ==================== AUTO-BLOCK HABITS ====================

    override fun getHabitCalendarSettings(habitId: String): Flow<HabitCalendarSettings?> = flow {
        _habitCalendarSettings.collect { settings ->
            emit(settings[habitId])
        }
    }

    override suspend fun updateHabitCalendarSettings(settings: HabitCalendarSettings) {
        val currentSettings = _habitCalendarSettings.value.toMutableMap()
        currentSettings[settings.habitId] = settings
        _habitCalendarSettings.value = currentSettings
        saveHabitCalendarSettings()
    }

    override suspend fun autoBlockHabit(habitId: String, date: String): AutoBlockResult {
        val settings = _habitCalendarSettings.value[habitId]
            ?: return AutoBlockResult(habitId, false, error = "No calendar settings for habit")

        if (!settings.autoBlockEnabled) {
            return AutoBlockResult(habitId, false, error = "Auto-block not enabled")
        }

        if (!isPreferredWeekday(date, settings)) {
            return AutoBlockResult(habitId, false, error = "Date is outside selected weekdays")
        }

        if (hasExistingHabitBlockForDate(habitId, date)) {
            return AutoBlockResult(habitId, false, error = "Habit already blocked for this date")
        }

        val analysis = getBestTimeForHabit(habitId, date)
        if (analysis.bestSlots.isEmpty()) {
            return AutoBlockResult(habitId, false, error = "No available time slots")
        }

        val bestSlot = analysis.bestSlots.first()
        val habits = habitRepository.getAllHabits().first()
        val habit = habits.find { it.id == habitId }
            ?: return AutoBlockResult(habitId, false, error = "Habit not found")

        val title = settings.blockTitle ?: "${habit.emoji} ${habit.name}"
        val accounts = _connectedAccounts.value.filter { it.isConnected }

        if (accounts.isEmpty()) {
            return AutoBlockResult(habitId, false, error = "No calendar connected")
        }

        val account = accounts.first()
        val result = createHabitEvent(
            habitId = habitId,
            title = title,
            startTime = bestSlot.startTime,
            durationMinutes = settings.preferredDuration,
            accountId = account.id
        )

        return result.fold(
            onSuccess = { event ->
                AutoBlockResult(
                    habitId = habitId,
                    success = true,
                    eventId = event.id,
                    blockedTime = bestSlot.startTime
                )
            },
            onFailure = {
                AutoBlockResult(habitId, false, error = it.message)
            }
        )
    }

    override suspend fun removeHabitBlock(habitId: String, eventId: String): Boolean {
        val account = _connectedAccounts.value.firstOrNull { it.isConnected } ?: return false
        val accessToken = account.accessToken ?: return false

        val result = when (account.provider) {
            CalendarProvider.GOOGLE -> calendarService.deleteGoogleEvent(accessToken, "primary", eventId)
            CalendarProvider.OUTLOOK -> calendarService.deleteOutlookEvent(accessToken, eventId)
        }

        return result.isSuccess
    }

    override suspend fun createHabitEvent(
        habitId: String,
        title: String,
        startTime: Long,
        durationMinutes: Int,
        accountId: String
    ): Result<CalendarEvent> {
        val account = _connectedAccounts.value.find { it.id == accountId }
            ?: return Result.failure(Exception("Account not found"))

        val accessToken = account.accessToken
            ?: return Result.failure(Exception("No access token"))

        val endTime = startTime + (durationMinutes * 60000L)
        val startInstant = Instant.ofEpochMilli(startTime)
        val endInstant = Instant.ofEpochMilli(endTime)

        val createResult = when (account.provider) {
            CalendarProvider.GOOGLE -> {
                val event = GoogleEventCreate(
                    summary = title,
                    description = "Auto-blocked by DailyWell for habit tracking",
                    start = GoogleEventTime(dateTime = startInstant.toString()),
                    end = GoogleEventTime(dateTime = endInstant.toString())
                )

                calendarService.createGoogleEvent(accessToken, "primary", event).map { googleEvent ->
                    calendarService.googleEventToCalendarEvent(googleEvent, "primary", accountId).copy(
                        isHabitBlock = true,
                        habitId = habitId
                    )
                }
            }

            CalendarProvider.OUTLOOK -> {
                val event = OutlookEventCreate(
                    subject = title,
                    body = OutlookEventBody(content = "Auto-blocked by DailyWell for habit tracking"),
                    start = OutlookEventTime(dateTime = startInstant.toString()),
                    end = OutlookEventTime(dateTime = endInstant.toString())
                )

                calendarService.createOutlookEvent(accessToken, event).map { outlookEvent ->
                    calendarService.outlookEventToCalendarEvent(outlookEvent, accountId).copy(
                        isHabitBlock = true,
                        habitId = habitId
                    )
                }
            }
        }

        createResult.onSuccess { createdEvent ->
            addOrUpdateCachedEvent(createdEvent)
        }
        return createResult
    }

    // ==================== RESCHEDULE SUGGESTIONS ====================

    /**
     * Check for conflicts between scheduled habit blocks and calendar events
     * Returns reschedule suggestions when conflicts are found
     */
    override suspend fun checkForConflicts(habitId: String): List<RescheduleSuggestion> {
        val settings = _habitCalendarSettings.value[habitId] ?: return emptyList()
        if (!settings.autoBlockEnabled) return emptyList()

        val habits = habitRepository.getAllHabits().first()
        val habit = habits.find { it.id == habitId } ?: return emptyList()

        val today = LocalDate.now().toString()
        val todayEvents = _cachedEvents.value.filter { event ->
            val eventDate = Instant.ofEpochMilli(event.startTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()
            eventDate == today && event.busyStatus == BusyStatus.BUSY && !event.isHabitBlock
        }

        val suggestions = mutableListOf<RescheduleSuggestion>()

        // Get scheduled habit blocks for today
        val habitBlocks = _cachedEvents.value.filter { event ->
            val eventDate = Instant.ofEpochMilli(event.startTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()
            eventDate == today && event.isHabitBlock && event.habitId == habitId
        }

        for (habitBlock in habitBlocks) {
            // Check if any calendar event overlaps with this habit block
            val conflicts = todayEvents.filter { event ->
                // Events overlap if one starts before the other ends
                (event.startTime < habitBlock.endTime && event.endTime > habitBlock.startTime)
            }

            if (conflicts.isNotEmpty()) {
                // Find alternative time slots
                val freeSlots = calculateFreeSlots(today, settings.preferredDuration)
                    .filter { it.startTime != habitBlock.startTime } // Exclude current slot

                if (freeSlots.isNotEmpty()) {
                    val bestAlternative = freeSlots.maxByOrNull { it.qualityScore }!!
                    val alternativeTimeStr = Instant.ofEpochMilli(bestAlternative.startTime)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("h:mm a"))

                    suggestions.add(
                        RescheduleSuggestion(
                            habitId = habitId,
                            habitName = habit.name,
                            habitEmoji = habit.emoji,
                            originalTime = habitBlock.startTime,
                            suggestedTime = bestAlternative.startTime,
                            conflictingEvent = conflicts.first().title,
                            reason = "Conflict with \"${conflicts.first().title}\". Available slot at $alternativeTimeStr",
                            priority = if (freeSlots.size <= 2) ReschedulePriority.HIGH else ReschedulePriority.MEDIUM
                        )
                    )
                } else {
                    // No alternative slots available
                    suggestions.add(
                        RescheduleSuggestion(
                            habitId = habitId,
                            habitName = habit.name,
                            habitEmoji = habit.emoji,
                            originalTime = habitBlock.startTime,
                            suggestedTime = null,
                            conflictingEvent = conflicts.first().title,
                            reason = "Conflict with \"${conflicts.first().title}\". No alternative time available today.",
                            priority = ReschedulePriority.HIGH
                        )
                    )
                }
            }
        }

        return suggestions
    }

    /**
     * Get all reschedule suggestions for habits with conflicts
     */
    override fun getRescheduleSuggestions(): Flow<List<RescheduleSuggestion>> = flow {
        val habits = habitRepository.getAllHabits().first().filter { it.isEnabled }
        val allSuggestions = mutableListOf<RescheduleSuggestion>()

        for (habit in habits) {
            val settings = _habitCalendarSettings.value[habit.id]
            if (settings?.autoBlockEnabled == true) {
                val conflicts = checkForConflicts(habit.id)
                allSuggestions.addAll(conflicts)
            }
        }

        emit(allSuggestions.sortedByDescending { it.priority })
    }

    // ==================== NOTIFICATIONS ====================

    override fun getCalendarNotifications(): Flow<List<CalendarNotification>> = _calendarNotifications

    override suspend fun scheduleFreeSlotNotification(
        habitId: String,
        freeSlot: FreeTimeSlot,
        message: String
    ) {
        val notification = CalendarNotification(
            id = UUID.randomUUID().toString(),
            type = CalendarNotificationType.FREE_SLOT_OPPORTUNITY,
            habitId = habitId,
            message = message,
            scheduledTime = freeSlot.startTime - (15 * 60000), // 15 min before
            freeSlot = freeSlot
        )

        val current = _calendarNotifications.value.toMutableList()
        current.add(notification)
        _calendarNotifications.value = current
    }

    override suspend fun dismissNotification(notificationId: String) {
        val current = _calendarNotifications.value.toMutableList()
        current.removeAll { it.id == notificationId }
        _calendarNotifications.value = current
    }

    // ==================== CALENDAR VIEW DATA ====================

    override fun getWeeklyCalendarView(startDate: String): Flow<List<CalendarDaySchedule>> = flow {
        val start = LocalDate.parse(startDate)
        val schedules = (0..6).map { dayOffset ->
            val date = start.plusDays(dayOffset.toLong())
            getScheduleForDate(date.toString()).first()
        }
        emit(schedules)
    }

    override suspend fun getMonthlyCalendarSummary(yearMonth: String): List<CalendarDaySchedule> {
        val parts = yearMonth.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val firstDay = LocalDate.of(year, month, 1)
        val lastDay = firstDay.plusMonths(1).minusDays(1)

        return (1..lastDay.dayOfMonth).map { day ->
            val date = LocalDate.of(year, month, day)
            getScheduleForDate(date.toString()).first()
        }
    }

    // ==================== HELPER METHODS ====================

    private fun updateAccount(account: CalendarAccount) {
        val currentAccounts = _connectedAccounts.value.toMutableList()
        val index = currentAccounts.indexOfFirst { it.id == account.id }
        if (index >= 0) {
            currentAccounts[index] = account
        } else {
            currentAccounts.add(account)
        }
        _connectedAccounts.value = currentAccounts
        // Save to DataStore would happen here
    }

    /**
     * Persist calendar accounts to DataStore
     * Stores OAuth tokens securely for offline access
     */
    private suspend fun saveAccounts(accounts: List<CalendarAccount>) {
        val accountsJson = json.encodeToString(accounts)
        dataStoreManager.putString(KEY_CALENDAR_ACCOUNTS, accountsJson)
    }

    /**
     * Load calendar accounts from DataStore
     * Called during initialization to restore connected accounts
     */
    suspend fun loadAccounts() {
        val accountsJson = dataStoreManager.getString(KEY_CALENDAR_ACCOUNTS).first()
        if (accountsJson != null) {
            try {
                val accounts = json.decodeFromString<List<CalendarAccount>>(accountsJson)
                _connectedAccounts.value = accounts
            } catch (e: Exception) {
                // Handle corrupted data - start fresh
                _connectedAccounts.value = emptyList()
            }
        }
    }

    /**
     * Save habit calendar settings to DataStore
     */
    private suspend fun saveHabitCalendarSettings() {
        val settingsJson = json.encodeToString(_habitCalendarSettings.value)
        dataStoreManager.putString(KEY_HABIT_CALENDAR_SETTINGS, settingsJson)
    }

    private fun hasExistingHabitBlockForDate(habitId: String, date: String): Boolean {
        return try {
            val targetDate = LocalDate.parse(date)
            val startOfDay = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = targetDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            _cachedEvents.value.any { event ->
                event.isHabitBlock &&
                    event.habitId == habitId &&
                    event.startTime in startOfDay until endOfDay
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun addOrUpdateCachedEvent(event: CalendarEvent) {
        val currentEvents = _cachedEvents.value.toMutableList()
        val existingIndex = currentEvents.indexOfFirst { existing ->
            existing.id == event.id && existing.calendarId == event.calendarId
        }

        if (existingIndex >= 0) {
            currentEvents[existingIndex] = event
        } else {
            currentEvents.add(event)
        }

        _cachedEvents.value = currentEvents.sortedBy { it.startTime }
    }

    private fun isPreferredWeekday(
        date: String,
        settings: HabitCalendarSettings?
    ): Boolean {
        if (settings == null) return true
        val preferredDays = settings.preferredWeekdays.ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) }.toSet()
        val dayOfWeek = runCatching { LocalDate.parse(date).dayOfWeek.value }.getOrElse { return false }
        return preferredDays.contains(dayOfWeek)
    }

    private fun applySchedulingPreferences(
        slots: List<FreeTimeSlot>,
        date: String,
        settings: HabitCalendarSettings?,
        requiredDuration: Int
    ): List<FreeTimeSlot> {
        val baseSlots = slots.filter { it.durationMinutes >= requiredDuration }
        if (settings == null) return baseSlots
        if (!isPreferredWeekday(date, settings)) return emptyList()

        val day = runCatching { LocalDate.parse(date) }.getOrElse { return baseSlots }
        val dayStart = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val windowStart = dayStart + (settings.preferredTimeStart.coerceIn(0, 24 * 60) * 60_000L)
        val windowEnd = dayStart + (settings.preferredTimeEnd.coerceIn(0, 24 * 60) * 60_000L)
        if (windowEnd <= windowStart) return emptyList()

        return baseSlots.mapNotNull { slot ->
            val adjustedStart = maxOf(slot.startTime, windowStart)
            val adjustedEnd = minOf(slot.endTime, windowEnd)
            val adjustedDuration = ((adjustedEnd - adjustedStart) / 60_000L).toInt()
            if (adjustedDuration < requiredDuration) {
                null
            } else {
                slot.copy(
                    startTime = adjustedStart,
                    endTime = adjustedEnd,
                    durationMinutes = adjustedDuration,
                    qualityScore = calculateSlotQuality(adjustedStart, adjustedEnd)
                )
            }
        }
    }

    /**
     * Load habit calendar settings from DataStore
     */
    suspend fun loadHabitCalendarSettings() {
        val settingsJson = dataStoreManager.getString(KEY_HABIT_CALENDAR_SETTINGS).first()
        if (settingsJson != null) {
            try {
                val settings = json.decodeFromString<Map<String, HabitCalendarSettings>>(settingsJson)
                _habitCalendarSettings.value = settings
            } catch (e: Exception) {
                _habitCalendarSettings.value = emptyMap()
            }
        }
    }
}
