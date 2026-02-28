package com.dailywell.app.speech

import kotlinx.coroutines.flow.StateFlow

/**
 * Speech Recognition State
 */
sealed class SpeechRecognitionState {
    data object Idle : SpeechRecognitionState()
    data object Listening : SpeechRecognitionState()
    data object Processing : SpeechRecognitionState()
    data class PartialResult(val text: String) : SpeechRecognitionState()
    data class Result(val text: String, val confidence: Float) : SpeechRecognitionState()
    data class Error(val message: String, val errorCode: Int) : SpeechRecognitionState()
}

/**
 * Voice Input Mode
 */
enum class VoiceInputMode {
    PUSH_TO_TALK,    // Hold button to record
    TAP_TO_TOGGLE    // Tap to start, tap again to stop
}

/**
 * Speech Recognition Settings
 */
data class SpeechSettings(
    val inputMode: VoiceInputMode = VoiceInputMode.TAP_TO_TOGGLE,
    val autoSendAfterSpeech: Boolean = true,
    val silenceTimeoutMs: Long = 2000,  // Stop after 2 seconds of silence
    val maxRecordingTimeMs: Long = 60000,  // Max 60 seconds
    val ttsEnabled: Boolean = true,  // Play AI responses aloud
    val language: String = "en-US"
)

/**
 * Speech Recognition Service Interface
 * Platform-specific implementations handle actual speech recognition
 */
interface SpeechRecognitionService {

    /**
     * Current recognition state
     */
    val state: StateFlow<SpeechRecognitionState>

    /**
     * Whether speech recognition is available on this device
     */
    val isAvailable: Boolean

    /**
     * Current settings
     */
    val settings: StateFlow<SpeechSettings>

    /**
     * Check and request microphone permission
     * Returns true if permission is granted
     */
    suspend fun checkPermission(): Boolean

    /**
     * Request microphone permission
     */
    suspend fun requestPermission()

    /**
     * Start listening for speech
     */
    fun startListening()

    /**
     * Stop listening
     */
    fun stopListening()

    /**
     * Cancel current recognition
     */
    fun cancelListening()

    /**
     * Update settings
     */
    suspend fun updateSettings(settings: SpeechSettings)

    /**
     * Speak text using TTS
     */
    suspend fun speak(text: String)

    /**
     * Stop TTS playback
     */
    fun stopSpeaking()

    /**
     * Whether TTS is currently speaking
     */
    val isSpeaking: StateFlow<Boolean>

    /**
     * Release resources
     */
    fun release()
}

/**
 * Error codes for speech recognition
 */
object SpeechErrorCodes {
    const val PERMISSION_DENIED = 1
    const val NETWORK_ERROR = 2
    const val NO_SPEECH = 3
    const val RECOGNITION_ERROR = 4
    const val NOT_AVAILABLE = 5
    const val TIMEOUT = 6
    const val CANCELLED = 7
}
