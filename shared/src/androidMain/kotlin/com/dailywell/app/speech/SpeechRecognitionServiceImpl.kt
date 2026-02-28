package com.dailywell.app.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

private val Context.speechDataStore by preferencesDataStore(name = "speech_settings")

class SpeechRecognitionServiceImpl(
    private val context: Context
) : SpeechRecognitionService {

    companion object {
        private const val TAG = "SpeechRecognition"

        // DataStore keys
        private val KEY_INPUT_MODE = stringPreferencesKey("input_mode")
        private val KEY_AUTO_SEND = booleanPreferencesKey("auto_send")
        private val KEY_SILENCE_TIMEOUT = longPreferencesKey("silence_timeout")
        private val KEY_MAX_RECORDING = longPreferencesKey("max_recording")
        private val KEY_TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false

    private val _state = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
    override val state: StateFlow<SpeechRecognitionState> = _state.asStateFlow()

    private val _settings = MutableStateFlow(SpeechSettings())
    override val settings: StateFlow<SpeechSettings> = _settings.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    override val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    init {
        // Load settings
        scope.launch {
            loadSettings()
        }

        // Initialize TTS
        initializeTTS()
    }

    private suspend fun loadSettings() {
        try {
            val settings = context.speechDataStore.data.map { prefs ->
                SpeechSettings(
                    inputMode = try {
                        VoiceInputMode.valueOf(prefs[KEY_INPUT_MODE] ?: VoiceInputMode.TAP_TO_TOGGLE.name)
                    } catch (e: Exception) {
                        VoiceInputMode.TAP_TO_TOGGLE
                    },
                    autoSendAfterSpeech = prefs[KEY_AUTO_SEND] ?: true,
                    silenceTimeoutMs = prefs[KEY_SILENCE_TIMEOUT] ?: 2000L,
                    maxRecordingTimeMs = prefs[KEY_MAX_RECORDING] ?: 60000L,
                    ttsEnabled = prefs[KEY_TTS_ENABLED] ?: true,
                    language = prefs[KEY_LANGUAGE] ?: "en-US"
                )
            }.first()

            _settings.value = settings
        } catch (e: Exception) {
            Log.e(TAG, "Error loading speech settings", e)
        }
    }

    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInitialized = true
                val locale = Locale.forLanguageTag(_settings.value.language)
                val result = textToSpeech?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fall back to default locale
                    textToSpeech?.setLanguage(Locale.US)
                }

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })

                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed: $status")
            }
        }
    }

    override suspend fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun requestPermission() {
        // Permission request must be done from Activity
        // This will be handled by the UI layer
    }

    override fun startListening() {
        if (!isAvailable) {
            _state.value = SpeechRecognitionState.Error(
                "Speech recognition not available",
                SpeechErrorCodes.NOT_AVAILABLE
            )
            return
        }

        // Create speech recognizer on main thread
        scope.launch {
            try {
                // Release previous instance
                speechRecognizer?.destroy()

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, _settings.value.language)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    // Silence detection
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, _settings.value.silenceTimeoutMs)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, _settings.value.silenceTimeoutMs / 2)
                }

                _state.value = SpeechRecognitionState.Listening
                speechRecognizer?.startListening(intent)

                Log.d(TAG, "Started listening")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognition", e)
                _state.value = SpeechRecognitionState.Error(
                    e.message ?: "Failed to start listening",
                    SpeechErrorCodes.RECOGNITION_ERROR
                )
            }
        }
    }

    override fun stopListening() {
        scope.launch {
            try {
                speechRecognizer?.stopListening()
                Log.d(TAG, "Stopped listening")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognition", e)
            }
        }
    }

    override fun cancelListening() {
        scope.launch {
            try {
                speechRecognizer?.cancel()
                _state.value = SpeechRecognitionState.Idle
                Log.d(TAG, "Cancelled listening")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling speech recognition", e)
            }
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                _state.value = SpeechRecognitionState.Listening
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed - could be used for visual feedback
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
                _state.value = SpeechRecognitionState.Processing
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error: $error"
                }

                val errorCode = when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechErrorCodes.PERMISSION_DENIED
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechErrorCodes.NETWORK_ERROR
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechErrorCodes.NO_SPEECH
                    else -> SpeechErrorCodes.RECOGNITION_ERROR
                }

                Log.e(TAG, "Speech recognition error: $errorMessage")
                _state.value = SpeechRecognitionState.Error(errorMessage, errorCode)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    val confidence = confidences?.getOrNull(0) ?: 0.8f

                    Log.d(TAG, "Recognition result: $text (confidence: $confidence)")
                    _state.value = SpeechRecognitionState.Result(text, confidence)
                } else {
                    _state.value = SpeechRecognitionState.Error("No results", SpeechErrorCodes.NO_SPEECH)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d(TAG, "Partial result: ${matches[0]}")
                    _state.value = SpeechRecognitionState.PartialResult(matches[0])
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Additional events
            }
        }
    }

    override suspend fun updateSettings(settings: SpeechSettings) {
        _settings.value = settings

        // Update TTS language if changed
        if (ttsInitialized) {
            val locale = Locale.forLanguageTag(settings.language)
            textToSpeech?.setLanguage(locale)
        }

        // Save to DataStore
        try {
            context.speechDataStore.edit { prefs ->
                prefs[KEY_INPUT_MODE] = settings.inputMode.name
                prefs[KEY_AUTO_SEND] = settings.autoSendAfterSpeech
                prefs[KEY_SILENCE_TIMEOUT] = settings.silenceTimeoutMs
                prefs[KEY_MAX_RECORDING] = settings.maxRecordingTimeMs
                prefs[KEY_TTS_ENABLED] = settings.ttsEnabled
                prefs[KEY_LANGUAGE] = settings.language
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving speech settings", e)
        }
    }

    override suspend fun speak(text: String) {
        if (!_settings.value.ttsEnabled || !ttsInitialized) {
            return
        }

        suspendCancellableCoroutine { continuation ->
            val utteranceId = UUID.randomUUID().toString()

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(id: String?) {
                    _isSpeaking.value = false
                    if (id == utteranceId && continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }

                override fun onError(id: String?) {
                    _isSpeaking.value = false
                    if (id == utteranceId && continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
            })

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }

            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)

            continuation.invokeOnCancellation {
                textToSpeech?.stop()
                _isSpeaking.value = false
            }
        }
    }

    override fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    override fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null

        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
