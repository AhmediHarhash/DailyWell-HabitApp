package com.dailywell.app.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.dailywell.app.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Calendar Service for Google Calendar and Outlook Calendar integration
 * Handles OAuth authentication and event fetching/creation
 */
class CalendarService(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val _connectionStatus = MutableStateFlow(CalendarConnectionStatus.NOT_CONNECTED)
    val connectionStatus: StateFlow<CalendarConnectionStatus> = _connectionStatus

    enum class CalendarConnectionStatus {
        NOT_CONNECTED,
        CONNECTING,
        GOOGLE_CONNECTED,
        OUTLOOK_CONNECTED,
        BOTH_CONNECTED,
        ERROR
    }

    // ==================== GOOGLE CALENDAR ====================

    companion object {
        // Google OAuth Configuration
        // Credentials injected from local.properties via BuildConfig
        private val GOOGLE_CLIENT_ID = com.dailywell.shared.BuildConfig.GOOGLE_OAUTH_CLIENT_ID
        private const val GOOGLE_REDIRECT_URI = "com.dailywell.android:/oauth2callback"
        private const val GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        private const val GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val GOOGLE_CALENDAR_API = "https://www.googleapis.com/calendar/v3"
        private val GOOGLE_SCOPES = listOf(
            "https://www.googleapis.com/auth/calendar.readonly",
            "https://www.googleapis.com/auth/calendar.events"
        )

        // Microsoft/Outlook OAuth Configuration
        // Credentials injected from local.properties via BuildConfig
        private val OUTLOOK_CLIENT_ID = com.dailywell.shared.BuildConfig.OUTLOOK_OAUTH_CLIENT_ID
        private const val OUTLOOK_REDIRECT_URI = "com.dailywell.android://auth"
        private const val OUTLOOK_AUTH_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize"
        private const val OUTLOOK_TOKEN_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/token"
        private const val OUTLOOK_GRAPH_API = "https://graph.microsoft.com/v1.0"
        private val OUTLOOK_SCOPES = listOf(
            "offline_access",
            "Calendars.Read",
            "Calendars.ReadWrite"
        )

        /**
         * Check if Google OAuth is configured
         */
        fun isGoogleConfigured(): Boolean = GOOGLE_CLIENT_ID.isNotBlank()

        /**
         * Check if Outlook OAuth is configured
         */
        fun isOutlookConfigured(): Boolean = OUTLOOK_CLIENT_ID.isNotBlank()

        /**
         * Check if any calendar OAuth is configured
         */
        fun isAnyCalendarConfigured(): Boolean = isGoogleConfigured() || isOutlookConfigured()
    }

    // ==================== GOOGLE OAUTH ====================

    /**
     * Generate Google OAuth authorization URL
     * @throws IllegalStateException if Google OAuth is not configured
     */
    fun getGoogleAuthUrl(): String {
        require(isGoogleConfigured()) {
            "Google OAuth not configured. Add GOOGLE_OAUTH_CLIENT_ID to local.properties"
        }
        val scope = GOOGLE_SCOPES.joinToString(" ")
        val params = mapOf(
            "client_id" to GOOGLE_CLIENT_ID,
            "redirect_uri" to GOOGLE_REDIRECT_URI,
            "response_type" to "code",
            "scope" to scope,
            "access_type" to "offline",
            "prompt" to "consent"
        )
        val queryString = params.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
        return "$GOOGLE_AUTH_URL?$queryString"
    }

    /**
     * Get intent to launch Google OAuth in browser
     * Returns null if Google OAuth is not configured
     */
    fun getGoogleAuthIntent(): Intent? {
        if (!isGoogleConfigured()) return null
        return Intent(Intent.ACTION_VIEW, Uri.parse(getGoogleAuthUrl()))
    }

    /**
     * Exchange Google authorization code for tokens
     */
    suspend fun exchangeGoogleCode(authCode: String): Result<OAuthTokens> {
        return withContext(Dispatchers.IO) {
            try {
                _connectionStatus.value = CalendarConnectionStatus.CONNECTING

                val response = httpClient.post(GOOGLE_TOKEN_URL) {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(
                        listOf(
                            "code" to authCode,
                            "client_id" to GOOGLE_CLIENT_ID,
                            "redirect_uri" to GOOGLE_REDIRECT_URI,
                            "grant_type" to "authorization_code"
                        ).formUrlEncode()
                    )
                }

                if (response.status.isSuccess()) {
                    val tokenResponse: GoogleTokenResponse = response.body()
                    val tokens = OAuthTokens(
                        accessToken = tokenResponse.access_token,
                        refreshToken = tokenResponse.refresh_token,
                        expiresIn = tokenResponse.expires_in,
                        tokenType = tokenResponse.token_type,
                        scope = tokenResponse.scope
                    )
                    _connectionStatus.value = CalendarConnectionStatus.GOOGLE_CONNECTED
                    Result.success(tokens)
                } else {
                    _connectionStatus.value = CalendarConnectionStatus.ERROR
                    Result.failure(Exception("Failed to exchange code: ${response.status}"))
                }
            } catch (e: Exception) {
                _connectionStatus.value = CalendarConnectionStatus.ERROR
                Result.failure(e)
            }
        }
    }

    /**
     * Refresh Google access token
     */
    suspend fun refreshGoogleToken(refreshToken: String): Result<OAuthTokens> {
        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.post(GOOGLE_TOKEN_URL) {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(
                        listOf(
                            "refresh_token" to refreshToken,
                            "client_id" to GOOGLE_CLIENT_ID,
                            "grant_type" to "refresh_token"
                        ).formUrlEncode()
                    )
                }

                if (response.status.isSuccess()) {
                    val tokenResponse: GoogleTokenResponse = response.body()
                    Result.success(
                        OAuthTokens(
                            accessToken = tokenResponse.access_token,
                            refreshToken = refreshToken, // Keep the original refresh token
                            expiresIn = tokenResponse.expires_in,
                            tokenType = tokenResponse.token_type,
                            scope = tokenResponse.scope
                        )
                    )
                } else {
                    Result.failure(Exception("Failed to refresh token: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get Google Calendar list
     */
    suspend fun getGoogleCalendars(accessToken: String): Result<List<GoogleCalendarInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.get("$GOOGLE_CALENDAR_API/users/me/calendarList") {
                    header("Authorization", "Bearer $accessToken")
                }

                if (response.status.isSuccess()) {
                    val calendarList: GoogleCalendarList = response.body()
                    Result.success(calendarList.items)
                } else {
                    Result.failure(Exception("Failed to get calendars: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get Google Calendar events for a date range
     */
    suspend fun getGoogleEvents(
        accessToken: String,
        calendarId: String = "primary",
        timeMin: String, // ISO 8601
        timeMax: String
    ): Result<List<GoogleEventItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.get("$GOOGLE_CALENDAR_API/calendars/$calendarId/events") {
                    header("Authorization", "Bearer $accessToken")
                    parameter("timeMin", timeMin)
                    parameter("timeMax", timeMax)
                    parameter("singleEvents", "true")
                    parameter("orderBy", "startTime")
                    parameter("maxResults", "100")
                }

                if (response.status.isSuccess()) {
                    val eventsResponse: GoogleEventsResponse = response.body()
                    Result.success(eventsResponse.items)
                } else {
                    Result.failure(Exception("Failed to get events: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Create a Google Calendar event (for habit blocking)
     */
    suspend fun createGoogleEvent(
        accessToken: String,
        calendarId: String = "primary",
        event: GoogleEventCreate
    ): Result<GoogleEventItem> {
        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.post("$GOOGLE_CALENDAR_API/calendars/$calendarId/events") {
                    header("Authorization", "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                    setBody(event)
                }

                if (response.status.isSuccess()) {
                    val createdEvent: GoogleEventItem = response.body()
                    Result.success(createdEvent)
                } else {
                    Result.failure(Exception("Failed to create event: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Delete a Google Calendar event
     */
    suspend fun deleteGoogleEvent(
        accessToken: String,
        calendarId: String = "primary",
        eventId: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.delete("$GOOGLE_CALENDAR_API/calendars/$calendarId/events/$eventId") {
                    header("Authorization", "Bearer $accessToken")
                }

                if (response.status.isSuccess() || response.status == HttpStatusCode.NoContent) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete event: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ==================== OUTLOOK/MICROSOFT OAUTH ====================

    /**
     * Generate Outlook OAuth authorization URL
     * @throws IllegalStateException if Outlook OAuth is not configured
     */
    fun getOutlookAuthUrl(): String {
        require(isOutlookConfigured()) {
            "Outlook OAuth not configured. Add OUTLOOK_OAUTH_CLIENT_ID to local.properties"
        }
        val scope = OUTLOOK_SCOPES.joinToString(" ")
        val params = mapOf(
            "client_id" to OUTLOOK_CLIENT_ID,
            "redirect_uri" to OUTLOOK_REDIRECT_URI,
            "response_type" to "code",
            "scope" to scope,
            "response_mode" to "query"
        )
        val queryString = params.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
        return "$OUTLOOK_AUTH_URL?$queryString"
    }

    /**
     * Get intent to launch Outlook OAuth in browser
     * Returns null if Outlook OAuth is not configured
     */
    fun getOutlookAuthIntent(): Intent? {
        if (!isOutlookConfigured()) return null
        return Intent(Intent.ACTION_VIEW, Uri.parse(getOutlookAuthUrl()))
    }

    /**
     * Exchange Outlook authorization code for tokens
     */
    suspend fun exchangeOutlookCode(authCode: String): Result<OAuthTokens> {
        return withContext(Dispatchers.IO) {
            try {
                _connectionStatus.value = CalendarConnectionStatus.CONNECTING

                val response = httpClient.post(OUTLOOK_TOKEN_URL) {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(
                        listOf(
                            "code" to authCode,
                            "client_id" to OUTLOOK_CLIENT_ID,
                            "redirect_uri" to OUTLOOK_REDIRECT_URI,
                            "grant_type" to "authorization_code",
                            "scope" to OUTLOOK_SCOPES.joinToString(" ")
                        ).formUrlEncode()
                    )
                }

                if (response.status.isSuccess()) {
                    val tokenResponse: OutlookTokenResponse = response.body()
                    val tokens = OAuthTokens(
                        accessToken = tokenResponse.access_token,
                        refreshToken = tokenResponse.refresh_token,
                        expiresIn = tokenResponse.expires_in,
                        tokenType = tokenResponse.token_type,
                        scope = tokenResponse.scope
                    )
                    _connectionStatus.value = CalendarConnectionStatus.OUTLOOK_CONNECTED
                    Result.success(tokens)
                } else {
                    _connectionStatus.value = CalendarConnectionStatus.ERROR
                    Result.failure(Exception("Failed to exchange code: ${response.status}"))
                }
            } catch (e: Exception) {
                _connectionStatus.value = CalendarConnectionStatus.ERROR
                Result.failure(e)
            }
        }
    }

    /**
     * Refresh Outlook access token
     */
    suspend fun refreshOutlookToken(refreshToken: String): Result<OAuthTokens> {
        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.post(OUTLOOK_TOKEN_URL) {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(
                        listOf(
                            "refresh_token" to refreshToken,
                            "client_id" to OUTLOOK_CLIENT_ID,
                            "grant_type" to "refresh_token",
                            "scope" to OUTLOOK_SCOPES.joinToString(" ")
                        ).formUrlEncode()
                    )
                }

                if (response.status.isSuccess()) {
                    val tokenResponse: OutlookTokenResponse = response.body()
                    Result.success(
                        OAuthTokens(
                            accessToken = tokenResponse.access_token,
                            refreshToken = tokenResponse.refresh_token ?: refreshToken,
                            expiresIn = tokenResponse.expires_in,
                            tokenType = tokenResponse.token_type,
                            scope = tokenResponse.scope
                        )
                    )
                } else {
                    Result.failure(Exception("Failed to refresh token: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get Outlook user profile (for email/name)
     */
    suspend fun getOutlookProfile(accessToken: String): Result<OutlookProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.get("$OUTLOOK_GRAPH_API/me") {
                    header("Authorization", "Bearer $accessToken")
                }

                if (response.status.isSuccess()) {
                    val profile: OutlookProfile = response.body()
                    Result.success(profile)
                } else {
                    Result.failure(Exception("Failed to get profile: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get Outlook Calendar events for a date range
     */
    suspend fun getOutlookEvents(
        accessToken: String,
        startDateTime: String, // ISO 8601
        endDateTime: String
    ): Result<List<OutlookEventItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.get("$OUTLOOK_GRAPH_API/me/calendarView") {
                    header("Authorization", "Bearer $accessToken")
                    parameter("startDateTime", startDateTime)
                    parameter("endDateTime", endDateTime)
                    parameter("\$top", "100")
                    parameter("\$orderby", "start/dateTime")
                }

                if (response.status.isSuccess()) {
                    val eventsResponse: OutlookEventsResponse = response.body()
                    Result.success(eventsResponse.value)
                } else {
                    Result.failure(Exception("Failed to get events: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Create an Outlook Calendar event (for habit blocking)
     */
    suspend fun createOutlookEvent(
        accessToken: String,
        event: OutlookEventCreate
    ): Result<OutlookEventItem> {
        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.post("$OUTLOOK_GRAPH_API/me/events") {
                    header("Authorization", "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                    setBody(event)
                }

                if (response.status.isSuccess()) {
                    val createdEvent: OutlookEventItem = response.body()
                    Result.success(createdEvent)
                } else {
                    Result.failure(Exception("Failed to create event: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Delete an Outlook Calendar event
     */
    suspend fun deleteOutlookEvent(
        accessToken: String,
        eventId: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.delete("$OUTLOOK_GRAPH_API/me/events/$eventId") {
                    header("Authorization", "Bearer $accessToken")
                }

                if (response.status.isSuccess() || response.status == HttpStatusCode.NoContent) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete event: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ==================== UTILITY ====================

    /**
     * Convert Google event to common CalendarEvent model
     */
    fun googleEventToCalendarEvent(
        googleEvent: GoogleEventItem,
        calendarId: String,
        accountId: String
    ): CalendarEvent {
        val startTime = parseGoogleEventTime(googleEvent.start)
        val endTime = parseGoogleEventTime(googleEvent.end)
        val isAllDay = googleEvent.start.date != null

        return CalendarEvent(
            id = googleEvent.id,
            calendarId = calendarId,
            provider = CalendarProvider.GOOGLE,
            title = googleEvent.summary ?: "(No title)",
            description = googleEvent.description,
            startTime = startTime,
            endTime = endTime,
            isAllDay = isAllDay,
            location = googleEvent.location,
            status = when (googleEvent.status) {
                "tentative" -> EventStatus.TENTATIVE
                "cancelled" -> EventStatus.CANCELLED
                else -> EventStatus.CONFIRMED
            },
            busyStatus = BusyStatus.BUSY,
            attendees = googleEvent.attendees?.map { it.email } ?: emptyList()
        )
    }

    /**
     * Convert Outlook event to common CalendarEvent model
     */
    fun outlookEventToCalendarEvent(
        outlookEvent: OutlookEventItem,
        accountId: String
    ): CalendarEvent {
        val startTime = parseOutlookEventTime(outlookEvent.start)
        val endTime = parseOutlookEventTime(outlookEvent.end)

        return CalendarEvent(
            id = outlookEvent.id,
            calendarId = "primary",
            provider = CalendarProvider.OUTLOOK,
            title = outlookEvent.subject ?: "(No title)",
            description = outlookEvent.bodyPreview,
            startTime = startTime,
            endTime = endTime,
            isAllDay = outlookEvent.isAllDay,
            location = outlookEvent.location?.displayName,
            status = EventStatus.CONFIRMED,
            busyStatus = when (outlookEvent.showAs) {
                "free" -> BusyStatus.FREE
                "tentative" -> BusyStatus.TENTATIVE
                "oof" -> BusyStatus.OUT_OF_OFFICE
                else -> BusyStatus.BUSY
            },
            attendees = outlookEvent.attendees?.map { it.emailAddress.address } ?: emptyList()
        )
    }

    private fun parseGoogleEventTime(eventTime: GoogleEventTime): Long {
        return if (eventTime.dateTime != null) {
            // DateTime format
            Instant.parse(eventTime.dateTime).toEpochMilli()
        } else if (eventTime.date != null) {
            // All-day event (date only)
            LocalDate.parse(eventTime.date)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } else {
            System.currentTimeMillis()
        }
    }

    private fun parseOutlookEventTime(eventTime: OutlookEventTime): Long {
        return try {
            Instant.parse(eventTime.dateTime).toEpochMilli()
        } catch (e: Exception) {
            // Try alternative format
            try {
                LocalDateTime.parse(eventTime.dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .atZone(ZoneId.of(eventTime.timeZone ?: "UTC"))
                    .toInstant()
                    .toEpochMilli()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    private fun List<Pair<String, String>>.formUrlEncode(): String {
        return joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
    }
}

// Token response models
@kotlinx.serialization.Serializable
data class GoogleTokenResponse(
    val access_token: String,
    val refresh_token: String? = null,
    val expires_in: Long,
    val token_type: String = "Bearer",
    val scope: String? = null
)

@kotlinx.serialization.Serializable
data class OutlookTokenResponse(
    val access_token: String,
    val refresh_token: String? = null,
    val expires_in: Long,
    val token_type: String = "Bearer",
    val scope: String? = null
)

@kotlinx.serialization.Serializable
data class OutlookProfile(
    val id: String,
    val displayName: String? = null,
    val mail: String? = null,
    val userPrincipalName: String? = null
)

// Event creation models
@kotlinx.serialization.Serializable
data class GoogleEventCreate(
    val summary: String,
    val description: String? = null,
    val start: GoogleEventTime,
    val end: GoogleEventTime,
    val colorId: String? = null
)

@kotlinx.serialization.Serializable
data class OutlookEventCreate(
    val subject: String,
    val body: OutlookEventBody? = null,
    val start: OutlookEventTime,
    val end: OutlookEventTime,
    val showAs: String = "busy"
)

@kotlinx.serialization.Serializable
data class OutlookEventBody(
    val contentType: String = "text",
    val content: String
)
