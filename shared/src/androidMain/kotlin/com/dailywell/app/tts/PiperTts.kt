package com.dailywell.app.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.GeneratedAudio
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Piper TTS - Natural Neural Voice Engine
 *
 * Uses sherpa-onnx with Piper VITS model for high-quality,
 * natural-sounding text-to-speech without internet connection.
 *
 * Model: en_US-lessac-medium (~60MB)
 * Quality: Near-human natural speech
 * Latency: ~200ms for short phrases
 *
 * Ported from PosturePal with 2026 audio optimizations:
 * - noiseScale = 0.0f for pure sound (no variance)
 * - noiseScaleW = 0.0f for pure sound (no duration variance)
 * - 8x buffer for ultra-clean playback
 * - Audio warmup to eliminate first-play static
 * - Fade-in to mask initialization artifacts
 */
class PiperTts(private val context: Context) {

    companion object {
        private const val TAG = "PiperTts"
        private const val MODEL_DIR = "vits-piper-en_US-lessac-medium"
        private const val MODEL_FILE = "en_US-lessac-medium.onnx"
        private const val TOKENS_FILE = "tokens.txt"
        private const val DATA_DIR = "espeak-ng-data"
        private const val DEBUG = true // Set to BuildConfig.DEBUG in production
    }

    private var tts: OfflineTts? = null
    private var audioTrack: AudioTrack? = null
    private var isInitialized = false
    private var isSpeaking = false
    private var isAudioWarmed = false

    // Voice parameters
    private var speakerId = 0
    private var speed = 1.0f // 0.5 = slow, 1.0 = normal, 2.0 = fast

    /**
     * Initialize the Piper TTS engine
     * Must be called before any speech synthesis
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) return@withContext true

            // Extract model files from assets to internal storage
            val modelDir = extractModelFiles()
            if (modelDir == null) {
                if (DEBUG) Log.e(TAG, "Failed to extract model files")
                return@withContext false
            }

            // Create TTS configuration for Piper VITS model - 2026 optimized for pure sound
            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = File(modelDir, MODEL_FILE).absolutePath,
                        tokens = File(modelDir, TOKENS_FILE).absolutePath,
                        dataDir = File(modelDir, DATA_DIR).absolutePath,
                        noiseScale = 0.0f,     // Set to 0 for pure sound (no variance)
                        noiseScaleW = 0.0f,    // Set to 0 for pure sound (no duration variance)
                        lengthScale = 1.0f / speed
                    ),
                    numThreads = 2,
                    debug = false,
                    provider = "cpu"
                ),
                maxNumSentences = 1
            )

            // Initialize TTS engine
            tts = OfflineTts(config = config)

            isInitialized = tts != null
            if (DEBUG) Log.d(TAG, "Piper TTS initialized successfully. Sample rate: ${tts?.sampleRate()}")

            // Pre-warm audio system to eliminate first-play static
            if (isInitialized) {
                warmUpAudioSystem()
            }

            return@withContext isInitialized
        } catch (e: Exception) {
            if (DEBUG) Log.e(TAG, "Failed to initialize Piper TTS", e)
            return@withContext false
        }
    }

    /**
     * Extract model files from assets to internal storage
     * (Required because sherpa-onnx needs file paths, not asset streams)
     */
    private fun extractModelFiles(): File? {
        try {
            val modelDir = File(context.filesDir, MODEL_DIR)

            // Check if already extracted
            val modelFile = File(modelDir, MODEL_FILE)
            if (modelFile.exists() && modelFile.length() > 0) {
                if (DEBUG) Log.d(TAG, "Model already extracted at ${modelDir.absolutePath}")
                return modelDir
            }

            if (DEBUG) Log.d(TAG, "Extracting model files to ${modelDir.absolutePath}")

            // Create directory structure
            modelDir.mkdirs()
            File(modelDir, DATA_DIR).mkdirs()

            // Copy all files from assets
            copyAssetFolder(MODEL_DIR, modelDir)

            if (DEBUG) Log.d(TAG, "Model extraction complete")
            return modelDir
        } catch (e: Exception) {
            if (DEBUG) Log.e(TAG, "Failed to extract model files", e)
            return null
        }
    }

    private fun copyAssetFolder(assetPath: String, targetDir: File) {
        val assetManager = context.assets
        val files = assetManager.list(assetPath) ?: return

        for (file in files) {
            val assetFilePath = "$assetPath/$file"
            val targetFile = File(targetDir, file)

            // Check if it's a directory
            val subFiles = assetManager.list(assetFilePath)
            if (!subFiles.isNullOrEmpty()) {
                // It's a directory, recurse
                targetFile.mkdirs()
                copyAssetFolder(assetFilePath, targetFile)
            } else {
                // It's a file, copy it
                assetManager.open(assetFilePath).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    /**
     * Speak text using Piper TTS
     * Plays audio directly through AudioTrack
     */
    suspend fun speak(text: String, flush: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            if (DEBUG) Log.w(TAG, "TTS not initialized, attempting to initialize...")
            if (!initialize()) {
                return@withContext false
            }
        }

        try {
            if (flush) {
                stop()
            }

            // Wait if already speaking
            while (isSpeaking && !flush) {
                kotlinx.coroutines.delay(50)
            }

            isSpeaking = true

            // Generate audio from text
            val audio = tts?.generate(
                text = text,
                sid = speakerId,
                speed = speed
            )

            if (audio == null || audio.samples.isEmpty()) {
                if (DEBUG) Log.w(TAG, "No audio generated for: $text")
                isSpeaking = false
                return@withContext false
            }

            // Play the audio
            playAudio(audio)

            isSpeaking = false
            return@withContext true
        } catch (e: Exception) {
            if (DEBUG) Log.e(TAG, "Failed to speak: $text", e)
            isSpeaking = false
            return@withContext false
        }
    }

    /**
     * Warm up audio system to eliminate first-play static
     * Plays a tiny bit of silence to initialize AudioTrack properly
     */
    private fun warmUpAudioSystem() {
        try {
            val sampleRate = tts?.sampleRate() ?: 22050
            // Create 50ms of silence
            val silenceSamples = (sampleRate * 0.05).toInt()
            val silenceData = ShortArray(silenceSamples) { 0 }

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val warmupTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            warmupTrack.write(silenceData, 0, silenceData.size)
            warmupTrack.play()
            Thread.sleep(50) // Brief pause for audio system initialization
            warmupTrack.stop()
            warmupTrack.release()

            isAudioWarmed = true
            if (DEBUG) Log.d(TAG, "Audio system warmed up successfully")
        } catch (e: Exception) {
            if (DEBUG) Log.w(TAG, "Failed to warm up audio system: ${e.message}")
        }
    }

    /**
     * Apply fade-in to audio samples to mask initialization artifacts
     * Applies smooth 20ms fade-in at the start
     */
    private fun applyFadeIn(samples: ShortArray, sampleRate: Int) {
        val fadeInSamples = (sampleRate * 0.02).toInt() // 20ms fade
        val actualFadeSamples = minOf(fadeInSamples, samples.size)

        for (i in 0 until actualFadeSamples) {
            val fadeMultiplier = i.toFloat() / actualFadeSamples
            samples[i] = (samples[i] * fadeMultiplier).toInt().toShort()
        }
    }

    /**
     * Play generated audio through AudioTrack
     */
    private suspend fun playAudio(audio: GeneratedAudio) {
        try {
            val sampleRate = tts?.sampleRate() ?: 22050

            // Convert float samples to 16-bit PCM with improved precision
            val pcmData = ShortArray(audio.samples.size)
            for (i in audio.samples.indices) {
                // Clamp to avoid clipping, then convert with better precision
                val normalizedSample = audio.samples[i].coerceIn(-1.0f, 1.0f)
                val sample = (normalizedSample * 32767.0f).toInt().coerceIn(-32768, 32767)
                pcmData[i] = sample.toShort()
            }

            // Apply fade-in to mask any initialization artifacts
            applyFadeIn(pcmData, sampleRate)

            // Create AudioTrack with optimized buffer for clean playback
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            // Use 8x minimum buffer for ultra-clean playback (2026 optimization)
            val bufferSize = maxOf(minBufferSize * 8, pcmData.size * 2)

            audioTrack?.release()
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack?.write(pcmData, 0, pcmData.size)
            audioTrack?.play()

            // Wait for playback to complete using non-blocking delay
            val durationMs = (pcmData.size * 1000L) / sampleRate
            kotlinx.coroutines.delay(durationMs + 100)

            audioTrack?.stop()
        } catch (e: Exception) {
            if (DEBUG) Log.e(TAG, "Failed to play audio", e)
        }
    }

    /**
     * Speak with streaming callback (for longer text)
     * Plays audio as it's generated for lower latency
     */
    suspend fun speakStreaming(text: String): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            if (!initialize()) return@withContext false
        }

        try {
            isSpeaking = true

            val sampleRate = tts?.sampleRate() ?: 22050
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            // Use 6x minimum buffer for streaming to prevent underruns
            val bufferSize = minBufferSize * 6

            audioTrack?.release()
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()

            // Generate with callback for streaming
            tts?.generateWithCallback(
                text = text,
                sid = speakerId,
                speed = speed
            ) { samples ->
                if (!isSpeaking) return@generateWithCallback 0 // Stop generation

                // Convert and play chunk with improved precision
                val pcmData = ShortArray(samples.size)
                for (i in samples.indices) {
                    val normalizedSample = samples[i].coerceIn(-1.0f, 1.0f)
                    val sample = (normalizedSample * 32767.0f).toInt().coerceIn(-32768, 32767)
                    pcmData[i] = sample.toShort()
                }
                audioTrack?.write(pcmData, 0, pcmData.size)

                1 // Continue generation
            }

            // Wait for buffer to drain using non-blocking delay
            kotlinx.coroutines.delay(500)
            audioTrack?.stop()

            isSpeaking = false
            return@withContext true
        } catch (e: Exception) {
            if (DEBUG) Log.e(TAG, "Failed streaming speech", e)
            isSpeaking = false
            return@withContext false
        }
    }

    /**
     * Stop current speech
     */
    fun stop() {
        isSpeaking = false
        audioTrack?.stop()
        audioTrack?.flush()
    }

    /**
     * Set speech speed
     * @param rate 0.5 = slow, 1.0 = normal, 2.0 = fast
     */
    fun setSpeed(rate: Float) {
        speed = rate.coerceIn(0.5f, 2.0f)
    }

    /**
     * Get current speed setting
     */
    fun getSpeed(): Float = speed

    /**
     * Check if currently speaking
     */
    fun isSpeaking(): Boolean = isSpeaking

    /**
     * Check if TTS is ready
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Release all resources
     */
    fun release() {
        stop()
        audioTrack?.release()
        audioTrack = null
        tts?.release()
        tts = null
        isInitialized = false
    }
}
