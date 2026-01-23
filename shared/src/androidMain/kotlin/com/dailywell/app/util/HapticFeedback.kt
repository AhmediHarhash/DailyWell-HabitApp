package com.dailywell.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

object HapticFeedback {

    fun light(context: Context) {
        vibrate(context, 10L, VibrationEffect.EFFECT_TICK)
    }

    fun medium(context: Context) {
        vibrate(context, 20L, VibrationEffect.EFFECT_CLICK)
    }

    fun heavy(context: Context) {
        vibrate(context, 30L, VibrationEffect.EFFECT_HEAVY_CLICK)
    }

    fun success(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val vibrator = getVibrator(context)
            vibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            )
        } else {
            vibrate(context, 50L)
        }
    }

    fun error(context: Context) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 50, 50, 50)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 50, 50, 50), -1)
        }
    }

    fun celebration(context: Context) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 50, 100, 50, 100, 100)
            val amplitudes = intArrayOf(0, 100, 0, 150, 0, 255)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 50, 100, 50, 100, 100), -1)
        }
    }

    private fun vibrate(context: Context, duration: Long, effectId: Int? = null) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && effectId != null) {
            try {
                vibrator?.vibrate(VibrationEffect.createPredefined(effectId))
                return
            } catch (e: Exception) {
                // Fall through to duration-based vibration
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // View-based haptic feedback (for compose with LocalView)
    fun performHapticFeedback(view: View, feedbackConstant: Int = HapticFeedbackConstants.KEYBOARD_TAP) {
        view.performHapticFeedback(feedbackConstant)
    }
}
