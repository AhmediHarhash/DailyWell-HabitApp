package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Audio Micro-Coaching features
 */
interface AudioCoachingRepository {

    /**
     * Get audio coaching data
     */
    fun getAudioData(): Flow<AudioCoachingData>

    /**
     * Get all available tracks
     */
    fun getAllTracks(): List<AudioTrack>

    /**
     * Get tracks by category
     */
    fun getTracksByCategory(category: AudioCategory): List<AudioTrack>

    /**
     * Get tracks for a specific habit
     */
    fun getTracksForHabit(habitId: String): List<AudioTrack>

    /**
     * Get free tracks (available to all users)
     */
    fun getFreeTracks(): List<AudioTrack>

    /**
     * Get all playlists
     */
    fun getAllPlaylists(): List<AudioPlaylist>

    /**
     * Mark track as completed
     */
    suspend fun markTrackCompleted(trackId: String)

    /**
     * Toggle track favorite status
     */
    suspend fun toggleFavorite(trackId: String)

    /**
     * Check if track is unlocked
     */
    fun isTrackUnlocked(trackId: String, isPremium: Boolean): Boolean

    /**
     * Record listen time
     */
    suspend fun recordListenTime(seconds: Int)

    /**
     * Update audio preferences
     */
    suspend fun updatePreferences(preferences: AudioPreferences)

    /**
     * Get recommended track for current time of day
     */
    fun getRecommendedTrack(currentHour: Int): AudioTrack?

    /**
     * Clear all audio data
     */
    suspend fun clearAllAudioData()

    // ==================== TTS PLAYBACK METHODS ====================

    /**
     * Play a track using TTS
     */
    fun playTrack(track: AudioTrack, onComplete: () -> Unit = {})

    /**
     * Stop current playback
     */
    fun stopPlayback()

    /**
     * Check if audio is currently playing
     */
    fun isPlaying(): Boolean

    /**
     * Speak a coaching message
     */
    fun speakCoachingMessage(text: String, coachPersonality: String = "calm", onComplete: () -> Unit = {})

    /**
     * Play morning motivation
     */
    fun playMorningMotivation(onComplete: () -> Unit = {})

    /**
     * Play streak celebration
     */
    fun playStreakCelebration(streakDays: Int, onComplete: () -> Unit = {})

    /**
     * Speak habit completion
     */
    fun speakHabitComplete(habitName: String)

    /**
     * Release TTS resources
     */
    fun release()
}
