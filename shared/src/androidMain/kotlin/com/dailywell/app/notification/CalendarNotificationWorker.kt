package com.dailywell.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.dailywell.app.data.model.CalendarNotificationType
import com.dailywell.app.data.repository.CalendarRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * WorkManager Worker for Calendar-based notifications
 * Sends notifications about free time slots and habit scheduling opportunities
 */
class CalendarNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val calendarRepository: CalendarRepository by inject()

    companion object {
        const val CHANNEL_ID = "calendar_notifications"
        const val CHANNEL_NAME = "Calendar Suggestions"
        const val NOTIFICATION_ID_BASE = 5000
        const val WORK_NAME = "calendar_notification_check"

        private const val INPUT_HABIT_ID = "habit_id"
        private const val INPUT_SLOT_START = "slot_start"
        private const val INPUT_SLOT_END = "slot_end"
        private const val INPUT_MESSAGE = "message"
        private const val INPUT_NOTIFICATION_TYPE = "notification_type"

        /**
         * Schedule a one-time notification for a free slot
         */
        fun scheduleNotification(
            context: Context,
            habitId: String,
            slotStart: Long,
            slotEnd: Long,
            message: String,
            type: CalendarNotificationType,
            notifyAt: Long
        ) {
            val delay = notifyAt - System.currentTimeMillis()
            if (delay <= 0) return // Don't schedule past notifications

            val inputData = Data.Builder()
                .putString(INPUT_HABIT_ID, habitId)
                .putLong(INPUT_SLOT_START, slotStart)
                .putLong(INPUT_SLOT_END, slotEnd)
                .putString(INPUT_MESSAGE, message)
                .putString(INPUT_NOTIFICATION_TYPE, type.name)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<CalendarNotificationWorker>()
                .setInputData(inputData)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("calendar_notification_$habitId")
                .build()

            WorkManager.getInstance(context)
                .enqueue(workRequest)
        }

        /**
         * Schedule periodic calendar sync and notification check
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<CalendarSyncWorker>(
                30, TimeUnit.MINUTES, // Sync every 30 minutes
                15, TimeUnit.MINUTES  // Flex interval
            )
                .setConstraints(constraints)
                .addTag("calendar_sync")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "calendar_sync_periodic",
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicWork
                )
        }

        /**
         * Cancel all calendar notifications for a habit
         */
        fun cancelNotificationsForHabit(context: Context, habitId: String) {
            WorkManager.getInstance(context)
                .cancelAllWorkByTag("calendar_notification_$habitId")
        }
    }

    override suspend fun doWork(): Result {
        createNotificationChannel()

        val habitId = inputData.getString(INPUT_HABIT_ID)
        val slotStart = inputData.getLong(INPUT_SLOT_START, 0)
        val slotEnd = inputData.getLong(INPUT_SLOT_END, 0)
        val message = inputData.getString(INPUT_MESSAGE) ?: return Result.failure()
        val notificationType = inputData.getString(INPUT_NOTIFICATION_TYPE)
            ?.let { CalendarNotificationType.valueOf(it) }
            ?: CalendarNotificationType.FREE_SLOT_OPPORTUNITY

        // Format time for display
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
        val startTimeStr = Instant.ofEpochMilli(slotStart)
            .atZone(ZoneId.systemDefault())
            .format(timeFormatter)
        val endTimeStr = Instant.ofEpochMilli(slotEnd)
            .atZone(ZoneId.systemDefault())
            .format(timeFormatter)

        val title = when (notificationType) {
            CalendarNotificationType.FREE_SLOT_OPPORTUNITY -> "Perfect Time for Your Habit!"
            CalendarNotificationType.SCHEDULE_CONFLICT -> "Schedule Conflict Detected"
            CalendarNotificationType.RESCHEDULE_SUGGESTION -> "Reschedule Suggestion"
            CalendarNotificationType.HABIT_BLOCK_REMINDER -> "Habit Time Starting Soon"
            CalendarNotificationType.BUSY_DAY_ALERT -> "Busy Day Ahead"
        }

        val expandedMessage = when (notificationType) {
            CalendarNotificationType.FREE_SLOT_OPPORTUNITY ->
                "$message\n$startTimeStr - $endTimeStr is free!"
            CalendarNotificationType.HABIT_BLOCK_REMINDER ->
                "Your blocked habit time starts at $startTimeStr"
            else -> message
        }

        showNotification(
            notificationId = NOTIFICATION_ID_BASE + (habitId?.hashCode() ?: 0),
            title = title,
            message = expandedMessage,
            notificationType = notificationType
        )

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about calendar suggestions and free time slots"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(
        notificationId: Int,
        title: String,
        message: String,
        notificationType: CalendarNotificationType
    ) {
        // Check permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Create intent to open app
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val emoji = when (notificationType) {
            CalendarNotificationType.FREE_SLOT_OPPORTUNITY -> "🎯"
            CalendarNotificationType.SCHEDULE_CONFLICT -> "⚠️"
            CalendarNotificationType.RESCHEDULE_SUGGESTION -> "🔄"
            CalendarNotificationType.HABIT_BLOCK_REMINDER -> "⏰"
            CalendarNotificationType.BUSY_DAY_ALERT -> "📅"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("$emoji $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, notification)
    }
}

/**
 * Worker for periodic calendar sync
 */
class CalendarSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val calendarRepository: CalendarRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            // Sync calendar events
            val syncResult = calendarRepository.syncEvents()

            if (syncResult.isSuccess) {
                // After sync, check for free slots and notify user
                checkAndNotifyFreeSlots()
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun checkAndNotifyFreeSlots() {
        // Get today's free slots
        val freeSlots = calendarRepository.getTodayFreeSlots().first()

        // Get habit suggestions
        val suggestions = calendarRepository.getHabitTimeSuggestions().first()

        // Find the best opportunity to notify about
        val bestSuggestion = suggestions
            .filter { it.isOptimal }
            .maxByOrNull { it.confidence }

        if (bestSuggestion != null) {
            val correspondingSlot = freeSlots.find {
                it.startTime == bestSuggestion.suggestedTime
            }

            if (correspondingSlot != null) {
                // Schedule notification 15 minutes before the slot
                val notifyAt = correspondingSlot.startTime - (15 * 60 * 1000)

                if (notifyAt > System.currentTimeMillis()) {
                    CalendarNotificationWorker.scheduleNotification(
                        context = context,
                        habitId = bestSuggestion.habitId,
                        slotStart = correspondingSlot.startTime,
                        slotEnd = correspondingSlot.endTime,
                        message = "Great time for ${bestSuggestion.habitName}!",
                        type = CalendarNotificationType.FREE_SLOT_OPPORTUNITY,
                        notifyAt = notifyAt
                    )
                }
            }
        }
    }
}
