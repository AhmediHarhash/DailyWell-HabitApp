package com.dailywell.android

import androidx.compose.runtime.staticCompositionLocalOf
import android.graphics.Bitmap
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

/**
 * Provides drawable resources from the app module to the shared module
 * Uses direct R.drawable references for reliability
 */
class DrawableResourceProvider(private val context: Context) {

    private val drawableCache = mutableMapOf<String, Bitmap?>()

    // Habit icons
    private val habitDrawables = mapOf(
        "habit_rest" to R.drawable.habit_rest,
        "habit_hydrate" to R.drawable.habit_hydrate,
        "habit_move" to R.drawable.habit_move,
        "habit_nourish" to R.drawable.habit_nourish,
        "habit_calm" to R.drawable.habit_calm,
        "habit_connect" to R.drawable.habit_connect,
        "habit_unplug" to R.drawable.habit_unplug
    )

    // Coach avatars
    private val coachDrawables = mapOf(
        "coach_sam" to R.drawable.coach_sam,
        "coach_alex" to R.drawable.coach_alex,
        "coach_dana" to R.drawable.coach_dana,
        "coach_grace" to R.drawable.coach_grace
    )

    // Badge icons
    private val badgeDrawables = mapOf(
        "badge_streak_7" to R.drawable.badge_streak_7,
        "badge_streak_30" to R.drawable.badge_streak_30,
        "badge_streak_100" to R.drawable.badge_streak_100,
        "badge_first_habit" to R.drawable.badge_first_habit,
        "badge_perfect_week" to R.drawable.badge_perfect_week,
        "badge_early_bird" to R.drawable.badge_early_bird,
        "badge_night_owl" to R.drawable.badge_night_owl,
        "badge_comeback" to R.drawable.badge_comeback
    )

    fun getHabitBitmap(habitId: String): Bitmap? {
        val drawableName = when (habitId.lowercase()) {
            "sleep", "rest" -> "habit_rest"
            "water", "hydrate" -> "habit_hydrate"
            "move", "exercise" -> "habit_move"
            "vegetables", "nourish" -> "habit_nourish"
            "calm", "meditate", "mindfulness" -> "habit_calm"
            "connect", "social" -> "habit_connect"
            "unplug", "digital_detox" -> "habit_unplug"
            else -> "habit_calm"
        }
        return getBitmapCached(drawableName, habitDrawables)
    }

    fun getCoachBitmap(coachId: String): Bitmap? {
        val drawableName = when (coachId.lowercase()) {
            "coach_sam", "sam" -> "coach_sam"
            "coach_alex", "alex" -> "coach_alex"
            "coach_dana", "dana" -> "coach_dana"
            "coach_grace", "grace" -> "coach_grace"
            else -> "coach_sam"
        }
        return getBitmapCached(drawableName, coachDrawables)
    }

    fun getBadgeBitmap(badgeName: String): Bitmap? {
        val drawableName = when (badgeName.lowercase()) {
            "streak_7", "week_streak" -> "badge_streak_7"
            "streak_30", "month_streak" -> "badge_streak_30"
            "streak_100", "century_streak" -> "badge_streak_100"
            "first_habit", "first_completion" -> "badge_first_habit"
            "perfect_week" -> "badge_perfect_week"
            "early_bird" -> "badge_early_bird"
            "night_owl" -> "badge_night_owl"
            "comeback", "comeback_champion" -> "badge_comeback"
            else -> "badge_first_habit"
        }
        return getBitmapCached(drawableName, badgeDrawables)
    }

    private fun getBitmapCached(name: String, drawableMap: Map<String, Int>): Bitmap? {
        return drawableCache.getOrPut(name) {
            val resourceId = drawableMap[name] ?: return@getOrPut null
            try {
                ContextCompat.getDrawable(context, resourceId)?.toBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
}

val LocalDrawableResourceProvider = staticCompositionLocalOf<DrawableResourceProvider?> { null }
