package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

/**
 * Calendar Integration Models for DailyWell
 * Supports Google Calendar and Outlook Calendar sync
 */

// ==================== CALENDAR ACCOUNT ====================

@Serializable
data class CalendarAccount(
    val id: String,
    val provider: CalendarProvider,
    val email: String,
    val displayName: String,
    val isConnected: Boolean = false,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val tokenExpiry: Long? = null,
    val lastSyncTime: Long? = null,
    val syncEnabled: Boolean = true
)

@Serializable
enum class CalendarProvider {
    GOOGLE,
    OUTLOOK
}

// ==================== CALENDAR EVENT ====================

@Serializable
data class CalendarEvent(
    val id: String,
    val calendarId: String,
    val provider: CalendarProvider,
    val title: String,
    val description: String? = null,
    val startTime: Long, // epoch millis
    val endTime: Long,
    val isAllDay: Boolean = false,
    val location: String? = null,
    val status: EventStatus = EventStatus.CONFIRMED,
    val busyStatus: BusyStatus = BusyStatus.BUSY,
    val recurrence: String? = null,
    val attendees: List<String> = emptyList(),
    val isHabitBlock: Boolean = false, // true if created by DailyWell
    val habitId: String? = null // linked habit if this is a habit block
)

@Serializable
enum class EventStatus {
    CONFIRMED,
    TENTATIVE,
    CANCELLED
}

@Serializable
enum class BusyStatus {
    FREE,
    BUSY,
    TENTATIVE,
    OUT_OF_OFFICE
}

// ==================== FREE TIME SLOT ====================

@Serializable
data class FreeTimeSlot(
    val startTime: Long, // epoch millis
    val endTime: Long,
    val durationMinutes: Int,
    val qualityScore: Float = 0f, // 0-1, higher = better for habits
    val suggestedHabits: List<String> = emptyList() // habit IDs that fit well
)

// ==================== HABIT TIME SUGGESTION ====================

@Serializable
data class HabitTimeSuggestion(
    val habitId: String,
    val habitName: String,
    val habitEmoji: String,
    val suggestedTime: Long, // epoch millis
    val endTime: Long,
    val reason: String, // "30-minute gap before your meeting"
    val confidence: Float = 0f, // 0-1
    val isOptimal: Boolean = false // true if this is the best time today
)

// ==================== HABIT CALENDAR SETTINGS ====================

@Serializable
data class HabitCalendarSettings(
    val habitId: String,
    val autoBlockEnabled: Boolean = false,
    val preferredDuration: Int = 30, // minutes
    val preferredTimeStart: Int = 9 * 60, // minutes from midnight (9 AM)
    val preferredTimeEnd: Int = 17 * 60, // 5 PM
    val preferredWeekdays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // java.time.DayOfWeek 1..7
    val blockTitle: String? = null, // custom title for calendar block
    val blockColor: String? = null, // color for calendar event
    val reminderMinutesBefore: Int = 15
)

// ==================== CALENDAR SYNC STATE ====================

@Serializable
data class CalendarSyncState(
    val lastSyncTime: Long = 0L,
    val isSyncing: Boolean = false,
    val lastError: String? = null,
    val eventsCount: Int = 0,
    val freeSlotsCachedUntil: Long = 0L
)

// ==================== CALENDAR DAY SCHEDULE ====================

@Serializable
data class CalendarDaySchedule(
    val date: String, // YYYY-MM-DD
    val events: List<CalendarEvent> = emptyList(),
    val freeSlots: List<FreeTimeSlot> = emptyList(),
    val habitSuggestions: List<HabitTimeSuggestion> = emptyList(),
    val busyPercentage: Float = 0f, // 0-100
    val isCalendarConnected: Boolean = false
)

// ==================== BEST TIME ANALYSIS ====================

@Serializable
data class BestTimeAnalysis(
    val habitId: String,
    val date: String, // YYYY-MM-DD
    val bestSlots: List<FreeTimeSlot>,
    val recommendation: String, // "Best time for Meditation today is 2:00 PM"
    val hasConflicts: Boolean = false,
    val alternativeSlots: List<FreeTimeSlot> = emptyList()
)

// ==================== AUTO-BLOCK RESULT ====================

@Serializable
data class AutoBlockResult(
    val habitId: String,
    val success: Boolean,
    val eventId: String? = null,
    val blockedTime: Long? = null,
    val error: String? = null
)

// ==================== RESCHEDULE SUGGESTION ====================

@Serializable
data class RescheduleSuggestion(
    val habitId: String,
    val habitName: String,
    val habitEmoji: String = "",
    val originalTime: Long,          // Original scheduled time
    val suggestedTime: Long? = null, // Null if no alternative available
    val reason: String,              // "New meeting conflicts with your workout"
    val conflictingEvent: String? = null,  // Title of conflicting event
    val priority: ReschedulePriority = ReschedulePriority.MEDIUM
)

@Serializable
enum class ReschedulePriority {
    LOW,     // Minor overlap, can proceed
    MEDIUM,  // Conflict detected, alternatives available
    HIGH     // Critical conflict, no alternatives or urgent
}

// ==================== CALENDAR NOTIFICATION ====================

@Serializable
data class CalendarNotification(
    val id: String,
    val type: CalendarNotificationType,
    val habitId: String? = null,
    val habitName: String? = null,
    val message: String,
    val scheduledTime: Long,
    val freeSlot: FreeTimeSlot? = null
)

@Serializable
enum class CalendarNotificationType {
    FREE_SLOT_OPPORTUNITY,   // "You have a free 30 min at 2 PM - perfect for your workout"
    SCHEDULE_CONFLICT,       // "Your workout time has a new conflict"
    RESCHEDULE_SUGGESTION,   // "Consider moving meditation to 3 PM"
    HABIT_BLOCK_REMINDER,    // "Your blocked habit time starts in 15 min"
    BUSY_DAY_ALERT          // "Busy day ahead - plan your habits early"
}

// ==================== OAUTH TOKENS ====================

@Serializable
data class OAuthTokens(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Long, // seconds
    val tokenType: String = "Bearer",
    val scope: String? = null
)

@Serializable
data class CalendarOAuthCallback(
    val provider: CalendarProvider,
    val authCode: String
)

// ==================== API RESPONSES ====================

@Serializable
data class GoogleCalendarList(
    val items: List<GoogleCalendarInfo> = emptyList()
)

@Serializable
data class GoogleCalendarInfo(
    val id: String,
    val summary: String,
    val primary: Boolean = false,
    val accessRole: String = "reader"
)

@Serializable
data class GoogleEventsResponse(
    val items: List<GoogleEventItem> = emptyList(),
    val nextPageToken: String? = null
)

@Serializable
data class GoogleEventItem(
    val id: String,
    val summary: String? = null,
    val description: String? = null,
    val start: GoogleEventTime,
    val end: GoogleEventTime,
    val status: String = "confirmed",
    val location: String? = null,
    val attendees: List<GoogleEventAttendee>? = null,
    val recurrence: List<String>? = null
)

@Serializable
data class GoogleEventTime(
    val dateTime: String? = null, // ISO 8601
    val date: String? = null, // YYYY-MM-DD for all-day events
    val timeZone: String? = null
)

@Serializable
data class GoogleEventAttendee(
    val email: String,
    val responseStatus: String? = null
)

// Outlook API models
@Serializable
data class OutlookEventsResponse(
    val value: List<OutlookEventItem> = emptyList()
)

@Serializable
data class OutlookEventItem(
    val id: String,
    val subject: String? = null,
    val bodyPreview: String? = null,
    val start: OutlookEventTime,
    val end: OutlookEventTime,
    val isAllDay: Boolean = false,
    val location: OutlookLocation? = null,
    val showAs: String? = null, // free, tentative, busy, oof
    val attendees: List<OutlookAttendee>? = null
)

@Serializable
data class OutlookEventTime(
    val dateTime: String, // ISO 8601
    val timeZone: String? = null
)

@Serializable
data class OutlookLocation(
    val displayName: String? = null
)

@Serializable
data class OutlookAttendee(
    val emailAddress: OutlookEmailAddress
)

@Serializable
data class OutlookEmailAddress(
    val address: String,
    val name: String? = null
)
