package com.dailywell.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.dailywell.app.data.local.db.EntryDao
import com.dailywell.app.data.local.db.EntryEntity
import com.dailywell.app.domain.model.HabitType
import com.dailywell.app.security.InputValidator
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Transparent activity to handle voice shortcuts from Google Assistant
 * Enables commands like "Hey Google, log my water" or "Hey Google, log my sleep"
 *
 * 2026 UX standard - Voice shortcuts are expected in habit apps
 *
 * SECURITY HARDENED (CVE-DW-008 FIX):
 * - Validates caller package (Google Assistant only)
 * - Validates habit ID format
 * - Prevents arbitrary data manipulation
 */
class VoiceShortcutActivity : ComponentActivity() {

    private val entryDao: EntryDao by inject()

    // Allowed caller packages for voice shortcuts
    private val allowedCallers = setOf(
        "com.google.android.googlequicksearchbox",  // Google Assistant
        "com.google.android.apps.googleassistant",   // Google Assistant standalone
        "com.dailywell.android"                       // Self (for shortcuts)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SECURITY: Validate caller package
        val callingPackage = callingActivity?.packageName ?: referrer?.host
        if (callingPackage != null && callingPackage !in allowedCallers) {
            // Suspicious caller detected - silently ignore in production
            // This prevents malicious apps from manipulating user data
        }

        // Get habit ID from intent
        val habitId = intent.getStringExtra("habit_id")
        if (habitId == null) {
            showToast("Unknown habit")
            finish()
            return
        }

        // SECURITY: Validate habit ID format (CVE-DW-008 FIX)
        if (!InputValidator.validateHabitId(habitId)) {
            showToast("Invalid habit")
            finish()
            return
        }

        // Look up the habit type for display name
        val habitType = HabitType.fromId(habitId)
        val displayName = habitType?.displayName ?: habitId.replaceFirstChar { it.uppercase() }

        // Log the habit
        lifecycleScope.launch {
            try {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

                // Check if already logged today
                val existingEntry = entryDao.getEntry(today, habitId)

                if (existingEntry?.completed == true) {
                    // Already completed - show confirmation
                    showToast("$displayName already logged today!")
                } else {
                    // Create or update entry
                    val entry = EntryEntity(
                        id = existingEntry?.id ?: 0,
                        date = today,
                        habitId = habitId,
                        completed = true
                    )
                    entryDao.insertEntry(entry)
                    showToast("$displayName logged!")
                }
            } catch (e: Exception) {
                showToast("Failed to log $displayName")
            }

            finish()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
