package com.dailywell.app.tts

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * TTS Service for Natural Voice Guidance
 *
 * Uses REAL Piper TTS with sherpa-onnx for high-quality,
 * natural-sounding text-to-speech without internet connection.
 *
 * Model: en_US-lessac-medium (~60MB VITS neural model)
 * Quality: Near-human natural speech
 * Latency: ~200ms for short phrases
 *
 * Ported from PosturePal with 2026 audio optimizations:
 * - noiseScale = 0.0f for pure sound
 * - 8x audio buffer for ultra-clean playback
 * - Audio warmup to eliminate first-play static
 */
class PiperTtsService(private val context: Context) {

    companion object {
        private const val TAG = "PiperTtsService"
    }

    private val piperTts = PiperTts(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var currentPersona: VoicePersona = VoicePersona.SUPPORTIVE

    /**
     * Voice personas for different coaching styles
     * Speed adjustments applied to Piper's speed parameter
     */
    enum class VoicePersona(val pitch: Float, val speed: Float, val description: String) {
        SUPPORTIVE(1.0f, 0.95f, "Warm and encouraging, celebrates every win"),
        ANALYTICAL(0.9f, 1.0f, "Calm and measured, focused on data"),
        DIRECT(1.1f, 1.1f, "Energetic and action-focused"),
        GENTLE(0.95f, 0.85f, "Soothing and patient, compassionate tone"),
        MOTIVATIONAL(1.15f, 1.05f, "High-energy, inspiring")
    }

    /**
     * Initialize TTS engine
     * Loads the Piper VITS model (~60MB on first run)
     */
    fun initialize(onReady: (() -> Unit)? = null) {
        if (_isInitialized.value) {
            onReady?.invoke()
            return
        }

        scope.launch {
            try {
                val success = piperTts.initialize()
                if (success) {
                    _isInitialized.value = true
                    Log.d(TAG, "Piper TTS initialized successfully")
                    onReady?.invoke()
                } else {
                    Log.e(TAG, "Failed to initialize Piper TTS")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Piper TTS", e)
            }
        }
    }

    /**
     * Speak text with the specified persona
     */
    fun speak(
        text: String,
        persona: VoicePersona = VoicePersona.SUPPORTIVE,
        onComplete: (() -> Unit)? = null
    ) {
        scope.launch {
            // Ensure initialized
            if (!_isInitialized.value) {
                val success = piperTts.initialize()
                if (success) {
                    _isInitialized.value = true
                } else {
                    Log.w(TAG, "TTS not initialized, skipping speech")
                    return@launch
                }
            }

            // Apply persona speed (Piper doesn't have pitch control)
            if (currentPersona != persona) {
                currentPersona = persona
                piperTts.setSpeed(persona.speed)
            }

            // Process text for better speech
            val processedText = preprocessText(text)

            _isSpeaking.value = true

            try {
                piperTts.speak(processedText, flush = true)
            } catch (e: Exception) {
                Log.e(TAG, "Error speaking: $text", e)
            } finally {
                _isSpeaking.value = false
                onComplete?.invoke()
            }
        }
    }

    /**
     * Speak text asynchronously (suspend function)
     */
    suspend fun speakAsync(
        text: String,
        persona: VoicePersona = VoicePersona.SUPPORTIVE
    ) = suspendCancellableCoroutine { continuation ->
        speak(text, persona) {
            if (continuation.isActive) {
                continuation.resumeWith(Result.success(Unit))
            }
        }

        continuation.invokeOnCancellation {
            stop()
        }
    }

    /**
     * Stop current speech
     */
    fun stop() {
        piperTts.stop()
        _isSpeaking.value = false
    }

    /**
     * Preprocess text for better TTS output
     */
    private fun preprocessText(text: String): String {
        return text
            // Convert emoji to spoken words
            .replace("🎉", " celebration ")
            .replace("🔥", " fire ")
            .replace("💪", " strong ")
            .replace("⭐", " star ")
            .replace("🏆", " trophy ")
            .replace("👑", " crown ")
            .replace("🌟", " amazing ")
            .replace("✨", " ")
            .replace("💯", " one hundred percent ")
            .replace("🌱", " ")
            .replace("😊", "")
            .replace("🙂", "")
            .replace("😐", "")
            .replace("😔", "")
            .replace("😢", "")
            // Clean up extra whitespace
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Pre-built voice messages for common scenarios
     */
    object VoiceMessages {

        val MORNING_GREETING = listOf(
            "Good morning! Ready to make today count?",
            "Rise and shine! Your habits are waiting for you.",
            "A new day, a new opportunity. Let's do this!",
            "Good morning! Time to build those habits."
        )

        val EVENING_CHECKIN = listOf(
            "Hey there! How did your day go? Let's check in on your habits.",
            "Evening check-in time! Let's see how you did today.",
            "Before you wind down, let's celebrate your progress today."
        )

        fun streakCelebration(days: Int): String = when (days) {
            1 -> "You did it! Day one complete. This is where it begins."
            3 -> "Three days strong! You're building real momentum now."
            7 -> "One week streak! You're officially in the habit zone. Keep going!"
            14 -> "Two weeks! Your brain is literally rewiring itself. Amazing work!"
            21 -> "Twenty-one days! Science says you're forming a real habit now. Incredible!"
            30 -> "One month streak! You've proven you can do this. You're unstoppable!"
            60 -> "Sixty days! This isn't just a habit anymore, it's who you are."
            90 -> "Ninety days! Quarter champion status unlocked. You're inspiring!"
            100 -> "One hundred days! Welcome to the Century Club. Legendary!"
            365 -> "One year! You've transformed your life. You are a true legend!"
            else -> when {
                days < 7 -> "Day $days complete! Keep that streak alive!"
                days < 30 -> "$days days and counting! You're on fire!"
                days < 100 -> "$days day streak! You're in the elite now."
                else -> "$days days! You're an absolute legend!"
            }
        }

        val PERFECT_DAY = listOf(
            "Perfect day achieved! Every single habit completed. You're amazing!",
            "All habits done! That's what I call a perfect day. Celebrate this!",
            "One hundred percent completion today! You should be so proud."
        )

        val STREAK_BREAK_COMPASSION = listOf(
            "Hey, I noticed you missed yesterday. That's okay. What matters is you're here now.",
            "Life happens. Missing a day doesn't erase all your progress. Let's get back on track.",
            "One day doesn't define your journey. You've built real strength. Let's keep going."
        )

        val MOTIVATION_BOOST = listOf(
            "Remember why you started. You're capable of amazing things.",
            "Every habit you complete is a vote for the person you want to become.",
            "Small steps, big changes. You're doing better than you think.",
            "Progress isn't always visible, but it's happening. Trust the process."
        )

        fun habitComplete(habitName: String): String = when (habitName.lowercase()) {
            "sleep", "rest" -> "Great rest logged! Quality sleep is the foundation of everything."
            "water", "hydrate" -> "Hydration goal reached! Your body thanks you."
            "move", "exercise" -> "Movement complete! You're keeping your body strong."
            "vegetables", "nourish" -> "Nutrition checked! Fueling your body right."
            "calm", "meditate" -> "Mindfulness done! Your mind is clearer now."
            "connect" -> "Social connection logged! Relationships matter."
            "unplug" -> "Digital detox achieved! Your mind needed that break."
            else -> "$habitName complete! One step closer to your goals."
        }

        fun trialReminder(daysLeft: Int): String = when (daysLeft) {
            14 -> "Welcome to your free trial! You have full access to everything for the next two weeks."
            7 -> "One week into your journey! How's it going? You still have 7 days of full access."
            3 -> "Your trial ends in 3 days. You've made great progress, keep it going!"
            2 -> "Just 2 days left in your trial. Don't lose your streak and progress!"
            1 -> "Last day of your trial tomorrow. Ready to continue your transformation?"
            else -> "You have $daysLeft days left to experience all premium features."
        }

        val COACHING_SESSION_START = listOf(
            "Let's check in on how you're doing. I'm here to help.",
            "Time for your coaching session. Tell me, how are you feeling about your habits?",
            "Ready to dive into your progress? Let's see what insights we can discover."
        )
    }

    /**
     * Speak a random message from a list
     */
    fun speakRandom(
        messages: List<String>,
        persona: VoicePersona = VoicePersona.SUPPORTIVE
    ) {
        speak(messages.random(), persona)
    }

    /**
     * Speak habit completion
     */
    fun speakHabitComplete(habitName: String) {
        speak(VoiceMessages.habitComplete(habitName), VoicePersona.SUPPORTIVE)
    }

    /**
     * Speak streak celebration
     */
    fun speakStreakCelebration(days: Int) {
        val persona = when {
            days >= 100 -> VoicePersona.MOTIVATIONAL
            days >= 30 -> VoicePersona.SUPPORTIVE
            else -> VoicePersona.SUPPORTIVE
        }
        speak(VoiceMessages.streakCelebration(days), persona)
    }

    /**
     * Speak trial reminder
     */
    fun speakTrialReminder(daysLeft: Int) {
        speak(VoiceMessages.trialReminder(daysLeft), VoicePersona.SUPPORTIVE)
    }

    /**
     * Clean up resources
     */
    fun release() {
        stop()
        piperTts.release()
        _isInitialized.value = false
        scope.cancel()
    }
}
