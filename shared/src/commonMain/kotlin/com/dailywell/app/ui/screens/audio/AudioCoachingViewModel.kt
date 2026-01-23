package com.dailywell.app.ui.screens.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.AudioCoachingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class AudioCoachingUiState(
    val tracks: List<AudioTrack> = emptyList(),
    val playlists: List<AudioPlaylist> = emptyList(),
    val completedTrackIds: Set<String> = emptySet(),
    val favoriteTrackIds: Set<String> = emptySet(),
    val currentPlayingTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val playbackProgress: Float = 0f,
    val selectedCategory: AudioCategory? = null,
    val recommendedTrack: AudioTrack? = null,
    val totalListenTime: Int = 0,
    val preferences: AudioPreferences = AudioPreferences(),
    val isLoading: Boolean = true
)

/**
 * ViewModel for Audio Micro-Coaching
 */
class AudioCoachingViewModel(
    private val audioRepository: AudioCoachingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AudioCoachingUiState())
    val uiState: StateFlow<AudioCoachingUiState> = _uiState.asStateFlow()

    init {
        loadAudioData()
    }

    private fun loadAudioData() {
        // Load tracks and playlists
        _uiState.value = _uiState.value.copy(
            tracks = audioRepository.getAllTracks(),
            playlists = audioRepository.getAllPlaylists(),
            recommendedTrack = audioRepository.getRecommendedTrack(
                Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .hour
            )
        )

        // Load user data
        viewModelScope.launch {
            audioRepository.getAudioData().collect { data ->
                _uiState.value = _uiState.value.copy(
                    completedTrackIds = data.completedTracks.toSet(),
                    favoriteTrackIds = data.favoritesTracks.toSet(),
                    totalListenTime = data.totalListenTime,
                    preferences = data.preferences,
                    isLoading = false
                )
            }
        }
    }

    fun selectCategory(category: AudioCategory?) {
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            tracks = if (category == null) {
                audioRepository.getAllTracks()
            } else {
                audioRepository.getTracksByCategory(category)
            }
        )
    }

    fun playTrack(track: AudioTrack) {
        _uiState.value = _uiState.value.copy(
            currentPlayingTrack = track,
            isPlaying = true,
            playbackProgress = 0f
        )
        // Actually play the track using TTS
        audioRepository.playTrack(track) {
            // On complete callback
            viewModelScope.launch {
                audioRepository.markTrackCompleted(track.id)
            }
            _uiState.value = _uiState.value.copy(
                isPlaying = false,
                playbackProgress = 1f
            )
        }
    }

    fun pauseTrack() {
        audioRepository.stopPlayback()
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }

    fun resumeTrack() {
        val track = _uiState.value.currentPlayingTrack
        if (track != null) {
            audioRepository.playTrack(track) {
                _uiState.value = _uiState.value.copy(isPlaying = false)
            }
        }
        _uiState.value = _uiState.value.copy(isPlaying = true)
    }

    fun stopTrack() {
        val currentTrack = _uiState.value.currentPlayingTrack
        audioRepository.stopPlayback()
        if (currentTrack != null && _uiState.value.playbackProgress > 0.8f) {
            // Mark as completed if listened to >80%
            viewModelScope.launch {
                audioRepository.markTrackCompleted(currentTrack.id)
            }
        }
        _uiState.value = _uiState.value.copy(
            currentPlayingTrack = null,
            isPlaying = false,
            playbackProgress = 0f
        )
    }

    fun updateProgress(progress: Float) {
        _uiState.value = _uiState.value.copy(playbackProgress = progress)
    }

    fun toggleFavorite(trackId: String) {
        viewModelScope.launch {
            audioRepository.toggleFavorite(trackId)
        }
    }

    fun markTrackCompleted(trackId: String) {
        viewModelScope.launch {
            audioRepository.markTrackCompleted(trackId)
        }
    }

    fun recordListenTime(seconds: Int) {
        viewModelScope.launch {
            audioRepository.recordListenTime(seconds)
        }
    }

    fun updatePreferences(preferences: AudioPreferences) {
        viewModelScope.launch {
            audioRepository.updatePreferences(preferences)
        }
    }

    fun getTracksByHabit(habitId: String): List<AudioTrack> {
        return audioRepository.getTracksForHabit(habitId)
    }

    fun isTrackUnlocked(trackId: String, isPremium: Boolean): Boolean {
        return audioRepository.isTrackUnlocked(trackId, isPremium)
    }

    fun getFreeTracks(): List<AudioTrack> {
        return audioRepository.getFreeTracks()
    }

    fun formatDuration(seconds: Int): String {
        return AudioLibrary.formatDuration(seconds)
    }

    /**
     * Speak a coaching message with TTS
     */
    fun speakCoachingMessage(text: String, personality: String = "calm") {
        audioRepository.speakCoachingMessage(text, personality) {
            // Callback when done speaking
        }
    }

    /**
     * Play morning motivation audio
     */
    fun playMorningMotivation() {
        audioRepository.playMorningMotivation {
            // Callback when done
        }
    }

    /**
     * Celebrate a streak milestone
     */
    fun playStreakCelebration(streakDays: Int) {
        audioRepository.playStreakCelebration(streakDays) {
            // Callback when done
        }
    }

    /**
     * Check if audio is currently playing
     */
    fun isAudioPlaying(): Boolean = audioRepository.isPlaying()

    override fun onCleared() {
        super.onCleared()
        audioRepository.release()
    }
}
