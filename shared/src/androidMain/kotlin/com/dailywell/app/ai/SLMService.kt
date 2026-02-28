package com.dailywell.app.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.llamatik.library.platform.GenStream
import com.llamatik.library.platform.LlamaBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * On-Device AI Service - Qwen2.5 0.5B via Llamatik (llama.cpp)
 *
 * Single model: Qwen2.5 0.5B Instruct Q4_K_M (~380 MB)
 * No fallback model. Consistent quality for every user.
 *
 * Usage Hierarchy:
 * 1. Decision Tree (FREE, instant) - Simple routing
 * 2. On-device Qwen 0.5B (FREE, offline) - Coaching, insights, encouragement
 * 3. Claude Haiku (PAID, cloud) - Rate-limited when no SLM
 * 4. Claude Sonnet (PAID, cloud) - Vision/complex tasks only
 */
class SLMService(
    private val context: Context,
    private val modelDownloadManager: ModelDownloadManager? = null,
    private val userProfileBuilder: UserProfileBuilder? = null
) {

    companion object {
        private const val TAG = "SLMService"
        private const val MIN_MODEL_SIZE_BYTES = 100L * 1024 * 1024 // 100MB sanity check
        private const val MIN_TOTAL_RAM_BYTES = 3L * 1024 * 1024 * 1024 // 3GB
        private const val MIN_AVAILABLE_RAM_FOR_GENERATION_BYTES = 450L * 1024 * 1024 // 450MB
    }

    // Model state
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Cached user profile for system prompt personalization
    private var cachedUserProfile: String? = null
    private val generationMutex = Mutex()

    // CoroutineExceptionHandler prevents UnsatisfiedLinkError from crashing the process
    private val scope = CoroutineScope(
        Dispatchers.Default + SupervisorJob() +
            CoroutineExceptionHandler { _, throwable ->
                Log.e(TAG, "SLM coroutine error (non-fatal)", throwable)
            }
    )

    /**
     * Check if Qwen 0.5B model file is available on disk
     */
    fun isModelAvailable(): Boolean {
        // Check via ModelDownloadManager
        modelDownloadManager?.getModelPath()?.let { path ->
            val file = File(path)
            if (file.exists() && file.length() > MIN_MODEL_SIZE_BYTES) return true
        }

        // Legacy check for directly pushed models (dev/emulator)
        val modelsDir = File(context.filesDir, "models")
        val modelFile = File(modelsDir, ModelDownloadManager.MODEL_FILENAME)
        return modelFile.exists() && modelFile.length() > MIN_MODEL_SIZE_BYTES
    }

    /**
     * Device capability gate for safe on-device inference.
     */
    fun isRuntimeSupported(): Boolean {
        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: ""
        if (!primaryAbi.startsWith("arm64")) return false

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return false
        val memoryInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfo)

        if (am.isLowRamDevice && memoryInfo.totalMem < MIN_TOTAL_RAM_BYTES) return false
        return memoryInfo.totalMem >= MIN_TOTAL_RAM_BYTES
    }

    private fun hasEnoughAvailableRam(): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return false
        val memoryInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem >= MIN_AVAILABLE_RAM_FOR_GENERATION_BYTES
    }

    /**
     * Initialize the SLM engine via Llamatik (llama.cpp)
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (_isInitialized.value) return@withContext true

        // llama.cpp native libs only ship for ARM64
        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: ""
        if (!primaryAbi.startsWith("arm64")) {
            Log.d(TAG, "SLM requires ARM64 device (current ABI: $primaryAbi) - skipping")
            return@withContext false
        }

        try {
            val modelPath = findModelPath()
            if (modelPath == null) {
                Log.w(TAG, "No Qwen 0.5B model file available")
                return@withContext false
            }

            Log.d(TAG, "Initializing SLM with Qwen 0.5B: ${File(modelPath).name}")

            LlamaBridge.initGenerateModel(modelPath)

            _isInitialized.value = true
            Log.d(TAG, "SLM initialized: Qwen 0.5B (${File(modelPath).length() / 1024 / 1024}MB)")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize SLM engine", e)
            false
        }
    }

    /**
     * Generate a response using the on-device Qwen 0.5B model
     */
    suspend fun generateResponse(
        prompt: String,
        systemPrompt: String? = null,
        maxTokens: Int = 256
    ): Result<String> = withContext(Dispatchers.Default) {
        if (!_isInitialized.value) {
            if (!initialize()) {
                return@withContext Result.failure(Exception("Model not initialized"))
            }
        }

        _isGenerating.value = true

        // Load user profile for personalization (cached, rebuilt daily)
        if (cachedUserProfile == null) {
            cachedUserProfile = try {
                userProfileBuilder?.getProfile()
            } catch (_: Exception) { null }
        }

        try {
            val fullPrompt = buildQwenPrompt(prompt, systemPrompt)

            val response = LlamaBridge.generate(fullPrompt)

            _isGenerating.value = false

            if (response.isNullOrBlank()) {
                Result.failure(Exception("Empty response from SLM"))
            } else {
                val cleaned = cleanResponse(response, fullPrompt)
                if (cleaned.isNotEmpty()) {
                    Log.d(TAG, "Generated ${cleaned.length} chars via Qwen 0.5B")
                    Result.success(cleaned)
                } else {
                    Result.failure(Exception("Empty response after cleaning"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate response", e)
            _isGenerating.value = false
            Result.failure(e)
        }
    }

    /**
     * Generate a streaming response
     */
    fun generateResponseStream(
        prompt: String,
        systemPrompt: String? = null
    ): Flow<String> = callbackFlow<String> {
        if (!_isInitialized.value) {
            close()
            return@callbackFlow
        }

        val fullPrompt = buildQwenPrompt(prompt, systemPrompt)

        val callback = object : GenStream {
            override fun onDelta(text: String) {
                trySend(text)
            }
            override fun onComplete() {
                close()
            }
            override fun onError(message: String) {
                close(Exception(message))
            }
        }

        LlamaBridge.generateStream(fullPrompt, callback)

        awaitClose { }
    }.flowOn(Dispatchers.Default)

    /**
     * Build a chat prompt using Qwen ChatML format.
     */
    private fun buildQwenPrompt(userMessage: String, systemPrompt: String?): String {
        val baseSystem = systemPrompt ?: DEFAULT_SYSTEM_PROMPT
        // Append user profile for personalization
        val system = if (!cachedUserProfile.isNullOrBlank()) {
            "$baseSystem\n\n$cachedUserProfile"
        } else {
            baseSystem
        }

        return buildString {
            append("<|im_start|>system\n")
            append(system)
            append("\n<|im_end|>\n")
            append("<|im_start|>user\n")
            append(userMessage)
            append("\n<|im_end|>\n")
            append("<|im_start|>assistant\n")
        }
    }

    /**
     * Clean the raw model output
     */
    private fun cleanResponse(fullText: String, prompt: String): String {
        var response = fullText
        if (response.startsWith(prompt)) {
            response = response.removePrefix(prompt)
        }
        // Strip ChatML markers
        response = response
            .replace("<|im_end|>", "")
            .replace("<|im_start|>assistant", "")
            .replace("<|im_start|>system", "")
            .replace("<|im_start|>user", "")
        return response.trim()
    }

    /**
     * Find the Qwen model path on disk.
     */
    private fun findModelPath(): String? {
        // Check ModelDownloadManager first
        modelDownloadManager?.getModelPath()?.let { return it }

        // Legacy: check models directory (dev/emulator pushed model)
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) return null

        val modelFile = File(modelsDir, ModelDownloadManager.MODEL_FILENAME)
        if (modelFile.exists() && modelFile.length() > MIN_MODEL_SIZE_BYTES) {
            return modelFile.absolutePath
        }

        // Any GGUF file as last resort (dev testing)
        return modelsDir.listFiles()
            ?.filter { it.name.endsWith(".gguf") && it.length() > MIN_MODEL_SIZE_BYTES }
            ?.maxByOrNull { it.length() }
            ?.absolutePath
    }

    /**
     * Release all native resources
     */
    fun release() {
        try {
            LlamaBridge.shutdown()
            _isInitialized.value = false
            Log.d(TAG, "SLM resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing SLM resources", e)
        }
    }
}

private const val DEFAULT_SYSTEM_PROMPT =
    "You are a supportive health and wellness coach in the DailyWell app. " +
    "Keep responses concise (2-3 sentences). Be warm, practical, and encouraging. " +
    "Focus on habits, wellness, and actionable advice."


