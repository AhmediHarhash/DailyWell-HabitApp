package com.dailywell.app.data.repository

import com.dailywell.app.tts.PiperTtsService
import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock

/**
 * Implementation of AudioCoachingRepository with FREE Android TTS
 *
 * Uses Android's built-in neural TTS engine which:
 * - Is completely FREE (no API costs)
 * - Works OFFLINE
 * - Has high-quality neural voices on modern Android (11+)
 * - Supports multiple languages
 *
 * Note: Can be upgraded to Sherpa-ONNX/Piper for even higher quality
 * if needed in the future.
 */
class AudioCoachingRepositoryImpl(
    private val dataStoreManager: DataStoreManager,
    private val ttsService: PiperTtsService
) : AudioCoachingRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        private const val AUDIO_DATA_KEY = "audio_coaching_data"
    }

    private fun getAudioDataFlow(): Flow<AudioCoachingData> {
        return dataStoreManager.getString(AUDIO_DATA_KEY).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<AudioCoachingData>(it)
                } catch (e: Exception) {
                    AudioCoachingData()
                }
            } ?: AudioCoachingData()
        }
    }

    private suspend fun updateAudioData(transform: (AudioCoachingData) -> AudioCoachingData) {
        val currentData = getAudioDataFlow().first()
        val updatedData = transform(currentData)
        dataStoreManager.putString(AUDIO_DATA_KEY, json.encodeToString(updatedData))
    }

    override fun getAudioData(): Flow<AudioCoachingData> = getAudioDataFlow()

    override fun getAllTracks(): List<AudioTrack> = AudioLibrary.tracks

    override fun getTracksByCategory(category: AudioCategory): List<AudioTrack> =
        AudioLibrary.getTracksByCategory(category)

    override fun getTracksForHabit(habitId: String): List<AudioTrack> =
        AudioLibrary.getTracksByHabit(habitId)

    override fun getFreeTracks(): List<AudioTrack> = AudioLibrary.getFreeTracks()

    override fun getAllPlaylists(): List<AudioPlaylist> = AudioLibrary.playlists

    override suspend fun markTrackCompleted(trackId: String) {
        updateAudioData { data ->
            if (trackId in data.completedTracks) {
                data
            } else {
                data.copy(
                    completedTracks = data.completedTracks + trackId,
                    lastListenedAt = Clock.System.now().toString()
                )
            }
        }
    }

    override suspend fun toggleFavorite(trackId: String) {
        updateAudioData { data ->
            if (trackId in data.favoritesTracks) {
                data.copy(favoritesTracks = data.favoritesTracks - trackId)
            } else {
                data.copy(favoritesTracks = data.favoritesTracks + trackId)
            }
        }
    }

    override fun isTrackUnlocked(trackId: String, isPremium: Boolean): Boolean {
        val track = AudioLibrary.getTrackById(trackId) ?: return false
        return !track.isPremium || isPremium
    }

    override suspend fun recordListenTime(seconds: Int) {
        updateAudioData { data ->
            data.copy(
                totalListenTime = data.totalListenTime + seconds,
                lastListenedAt = Clock.System.now().toString()
            )
        }
    }

    override suspend fun updatePreferences(preferences: AudioPreferences) {
        updateAudioData { data ->
            data.copy(preferences = preferences)
        }
    }

    override fun getRecommendedTrack(currentHour: Int): AudioTrack? {
        return when (currentHour) {
            in 5..10 -> AudioLibrary.getTracksByCategory(AudioCategory.MORNING_MINDSET).firstOrNull()
            in 11..16 -> AudioLibrary.getTracksByCategory(AudioCategory.FOCUS).firstOrNull()
                ?: AudioLibrary.getTracksByCategory(AudioCategory.MOTIVATION).firstOrNull()
            in 17..20 -> AudioLibrary.getTracksByCategory(AudioCategory.STRESS_RELIEF).firstOrNull()
            in 21..23, in 0..4 -> AudioLibrary.getTracksByCategory(AudioCategory.EVENING_WINDDOWN).firstOrNull()
            else -> AudioLibrary.getFreeTracks().firstOrNull()
        }
    }

    override suspend fun clearAllAudioData() {
        updateAudioData { AudioCoachingData() }
    }

    // ==================== TTS PLAYBACK METHODS ====================

    /**
     * Stop any currently playing audio
     */
    override fun stopPlayback() {
        ttsService.stop()
    }

    /**
     * Check if audio is currently playing
     */
    override fun isPlaying(): Boolean = ttsService.isSpeaking.value

    /**
     * Play a track using TTS
     */
    override fun playTrack(track: AudioTrack, onComplete: () -> Unit) {
        val script = getScriptForTrack(track)
        val persona = getPersonaForTrack(track)
        ttsService.speak(script, persona, onComplete)
    }

    /**
     * Speak a coaching message
     */
    override fun speakCoachingMessage(text: String, coachPersonality: String, onComplete: () -> Unit) {
        val persona = when (coachPersonality) {
            "energetic" -> PiperTtsService.VoicePersona.MOTIVATIONAL
            "warm" -> PiperTtsService.VoicePersona.SUPPORTIVE
            "direct" -> PiperTtsService.VoicePersona.DIRECT
            "gentle" -> PiperTtsService.VoicePersona.GENTLE
            else -> PiperTtsService.VoicePersona.ANALYTICAL
        }
        ttsService.speak(text, persona, onComplete)
    }

    /**
     * Play morning motivation
     */
    override fun playMorningMotivation(onComplete: () -> Unit) {
        val script = """
            Good morning! Take a deep breath and set your intention for today.
            Remember, every habit you complete is a vote for the person you want to become.
            Start small, stay consistent, and celebrate your progress.
            You've got this. Let's make today count.
        """.trimIndent()
        ttsService.speak(script, PiperTtsService.VoicePersona.SUPPORTIVE, onComplete)
    }

    /**
     * Play streak celebration
     */
    override fun playStreakCelebration(streakDays: Int, onComplete: () -> Unit) {
        ttsService.speakStreakCelebration(streakDays)
    }

    /**
     * Speak habit completion celebration
     */
    override fun speakHabitComplete(habitName: String) {
        ttsService.speakHabitComplete(habitName)
    }

    /**
     * Speak trial reminder
     */
    fun speakTrialReminder(daysLeft: Int) {
        ttsService.speakTrialReminder(daysLeft)
    }

    private fun getScriptForTrack(track: AudioTrack): String {
        return when (track.category) {
            AudioCategory.MORNING_MINDSET -> """
                Good morning! Take a moment to set your intentions for today.
                What's one habit you want to focus on? Visualize yourself completing it successfully.
                You have the power to make today meaningful. Let's begin.
            """.trimIndent()

            AudioCategory.MOTIVATION -> """
                Remember why you started this journey. Every habit you complete is a vote for the person you want to become.
                You're not just building habits, you're building character.
                Take a deep breath and let's make progress today.
            """.trimIndent()

            AudioCategory.FOCUS -> """
                Let's clear your mind and focus on what matters most right now.
                Set aside distractions. This moment is for your growth.
                What's the one thing you need to do next? Focus on that single action.
            """.trimIndent()

            AudioCategory.STRESS_RELIEF -> """
                Take a deep breath in through your nose. Hold for a moment. Now exhale slowly.
                Let the tension leave your body. You're doing great.
                Remember, progress isn't always visible, but every effort counts.
            """.trimIndent()

            AudioCategory.EVENING_WINDDOWN -> """
                As your day comes to a close, reflect on what went well.
                Celebrate your wins, no matter how small. Let go of what didn't go as planned.
                Tomorrow is a fresh start. Rest well tonight.
            """.trimIndent()

            AudioCategory.HABIT_SCIENCE -> """
                Let's explore the science behind habits. Understanding how habits work gives you power over them.
                Every habit follows a loop: cue, routine, reward.
                The more you understand this, the easier change becomes.
            """.trimIndent()

            AudioCategory.BREATHING -> """
                Let's practice breathing together.
                Inhale slowly for four counts. One, two, three, four.
                Hold for four counts. One, two, three, four.
                Exhale for six counts. One, two, three, four, five, six.
                Feel the calm wash over you.
            """.trimIndent()

            AudioCategory.SLEEP_STORIES -> """
                It's time to wind down. Let your body relax, starting from your toes, up through your legs, your stomach, your chest, your arms, your shoulders, and finally your face.
                Breathe deeply and let sleep come naturally.
            """.trimIndent()

            AudioCategory.CELEBRATION -> """
                Congratulations! You've accomplished something worth celebrating.
                Take a moment to recognize how far you've come.
                Every milestone matters. You're building something great.
            """.trimIndent()

            AudioCategory.COMEBACK -> """
                Welcome back! It doesn't matter how long you've been away.
                What matters is that you're here now, ready to try again.
                Every new start is a chance to build something amazing.
            """.trimIndent()
        }
    }

    private fun getPersonaForTrack(track: AudioTrack): PiperTtsService.VoicePersona {
        return when (track.category) {
            AudioCategory.MOTIVATION, AudioCategory.COMEBACK, AudioCategory.CELEBRATION ->
                PiperTtsService.VoicePersona.MOTIVATIONAL
            AudioCategory.STRESS_RELIEF, AudioCategory.SLEEP_STORIES, AudioCategory.BREATHING ->
                PiperTtsService.VoicePersona.GENTLE
            AudioCategory.EVENING_WINDDOWN ->
                PiperTtsService.VoicePersona.SUPPORTIVE
            AudioCategory.FOCUS, AudioCategory.HABIT_SCIENCE ->
                PiperTtsService.VoicePersona.ANALYTICAL
            AudioCategory.MORNING_MINDSET ->
                PiperTtsService.VoicePersona.SUPPORTIVE
        }
    }

    /**
     * Clean up TTS resources
     */
    override fun release() {
        ttsService.release()
    }
}
