package com.dailywell.app.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.StatFs
import android.util.Log
import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.shared.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.io.File
import java.io.InputStream

/**
 * Play-first model manager.
 *
 * Production behavior:
 * - Looks for the SLM model delivered by Google Play Asset Delivery (install-time pack).
 * - Copies bundled model to app-local storage on first availability.
 * - Does not rely on external model hosting in release builds.
 *
 * Dev behavior:
 * - Keeps remote fallback download worker in debug builds.
 */
class ModelDownloadManager(
    private val context: Context,
    private val dataStoreManager: DataStoreManager
) : SLMDownloadInfo {

    companion object {
        private const val TAG = "ModelDownloadManager"

        const val MODEL_FILENAME = "Qwen2.5-0.5B-Instruct-Q4_K_M.gguf"
        const val MODEL_SIZE_BYTES = 380_000_000L // ~380MB for progress UI
        const val MODEL_SIZE_DISPLAY = "380 MB"
        const val MIN_VALID_MODEL_BYTES = 150L * 1024 * 1024

        private const val MIN_FREE_STORAGE = 1_200L * 1024 * 1024 // ~1.2GB headroom
        private const val ASSET_PACK_NAME = "slm_model_pack"

        private val BUNDLED_ASSET_CANDIDATES = listOf(
            "slm/$MODEL_FILENAME",
            "models/$MODEL_FILENAME",
            MODEL_FILENAME
        )

        const val ACTIVE_MODEL_KEY = "active_model"
        const val AUTO_DOWNLOAD_ATTEMPTED_KEY = "auto_download_attempted"
        const val CLOUD_CALLS_TODAY_KEY = "cloud_calls_today"
        const val CLOUD_CALLS_DATE_KEY = "cloud_calls_date"

        const val MAX_CLOUD_CALLS_PER_DAY_NO_SLM = 10
    }

    /** Callback invoked when model becomes ready. */
    var onModelReady: (() -> Unit)? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.NotStarted)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    private val _hasModel = MutableStateFlow(false)
    val hasModel: StateFlow<Boolean> = _hasModel.asStateFlow()

    private val _downloadProgress = MutableStateFlow<SLMDownloadProgress>(SLMDownloadProgress.NotStarted)
    override val downloadProgress: StateFlow<SLMDownloadProgress> = _downloadProgress.asStateFlow()

    private var downloadStartedAtMs: Long = 0L
    @Volatile private var bundledMonitorRunning: Boolean = false

    // In release we avoid external model hosting dependency.
    private val remoteFallbackEnabled: Boolean = BuildConfig.DEBUG

    init {
        scope.launch {
            detectExistingModel()
            syncDownloadProgress()

            if (!_hasModel.value) {
                if (remoteFallbackEnabled) {
                    attemptAutoDownload()
                } else {
                    startBundledModelMonitor(force = false)
                }
            }
        }
    }

    private fun syncDownloadProgress() {
        _downloadProgress.value = when (val state = _downloadState.value) {
            is ModelDownloadState.NotStarted -> SLMDownloadProgress.NotStarted
            is ModelDownloadState.Downloading -> SLMDownloadProgress.Downloading(MODEL_FILENAME, state.progress)
            is ModelDownloadState.Completed -> SLMDownloadProgress.Ready(MODEL_FILENAME)
            is ModelDownloadState.Failed -> SLMDownloadProgress.Failed(state.error)
            is ModelDownloadState.NeedsStorage -> SLMDownloadProgress.NeedsStorage(state.needBytes, state.haveBytes)
            is ModelDownloadState.WaitingForWifi -> SLMDownloadProgress.WaitingForWifi
        }
    }

    /** Check local model first, then Play-delivered bundle. */
    private fun detectExistingModel() {
        val modelFile = File(getModelsDir(), MODEL_FILENAME)
        if (isValidModelFile(modelFile)) {
            markModelReady(modelFile.length(), "Local model found")
            return
        }

        if (restoreBundledModelIfPresent(modelFile)) {
            markModelReady(modelFile.length(), "Bundled Play asset pack model restored")
            return
        }

        _hasModel.value = false

        if (remoteFallbackEnabled) {
            val partialProgress = getTempDownloadProgress()
            if (partialProgress > 0f && hasEnoughStorage()) {
                _downloadState.value = ModelDownloadState.Downloading(MODEL_FILENAME, partialProgress)
                downloadStartedAtMs = getTempModelFile().lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
                if (isOnWifi()) {
                    scheduleDownloadWorker()
                }
            } else {
                evaluateStorageState()
            }
            Log.d(TAG, "No local model found, using debug fallback mode")
        } else {
            _downloadState.value = ModelDownloadState.Downloading(MODEL_FILENAME, 0f)
            Log.d(TAG, "No local model found, waiting for Play asset pack installation")
        }
    }

    private fun markModelReady(bytes: Long, reason: String) {
        _hasModel.value = true
        _downloadState.value = ModelDownloadState.Completed(MODEL_FILENAME)
        downloadStartedAtMs = 0L
        scope.launch {
            dataStoreManager.putString(ACTIVE_MODEL_KEY, "QWEN_0_5B")
        }
        cleanupOldModels()
        syncDownloadProgress()
        onModelReady?.invoke()
        Log.d(TAG, "$reason (${bytes / 1024 / 1024}MB)")
    }

    private fun isValidModelFile(file: File): Boolean {
        return file.exists() && file.length() > MIN_VALID_MODEL_BYTES
    }

    /**
     * Try bundled model from:
     * 1) merged assets path
     * 2) extracted Play asset pack directory
     */
    private fun restoreBundledModelIfPresent(destinationFile: File): Boolean {
        if (isValidModelFile(destinationFile)) return true

        if (!hasEnoughStorage()) {
            return false
        }

        if (copyFromBundledAssets(destinationFile)) {
            return isValidModelFile(destinationFile)
        }

        val extractedModel = findModelInExtractedAssetPacks()
        if (extractedModel != null && copyFileToLocalModel(extractedModel, destinationFile)) {
            return isValidModelFile(destinationFile)
        }

        return false
    }

    private fun copyFromBundledAssets(destinationFile: File): Boolean {
        for (assetPath in BUNDLED_ASSET_CANDIDATES) {
            try {
                context.assets.open(assetPath).use { input ->
                    copyStreamToLocalModel(input, destinationFile)
                }
                Log.d(TAG, "Copied model from assets path: $assetPath")
                return true
            } catch (_: Exception) {
                // Try next candidate.
            }
        }
        return false
    }

    private fun findModelInExtractedAssetPacks(): File? {
        val filesParent = context.filesDir.parentFile

        val roots = listOfNotNull(
            File(context.filesDir, "assetpacks/$ASSET_PACK_NAME"),
            File(context.filesDir, "assetpacks"),
            filesParent?.let { File(it, "files/assetpacks/$ASSET_PACK_NAME") },
            filesParent?.let { File(it, "files/assetpacks") },
            filesParent?.let { File(it, "assetpacks/$ASSET_PACK_NAME") },
            filesParent?.let { File(it, "assetpacks") }
        )

        roots.forEach { root ->
            if (!root.exists()) return@forEach
            val found = root
                .walkTopDown()
                .maxDepth(8)
                .firstOrNull { it.isFile && it.name == MODEL_FILENAME && it.length() > MIN_VALID_MODEL_BYTES }
            if (found != null) {
                Log.d(TAG, "Found extracted Play asset pack model at: ${found.absolutePath}")
                return found
            }
        }

        return null
    }

    private fun copyFileToLocalModel(source: File, destinationFile: File): Boolean {
        return try {
            source.inputStream().use { input ->
                copyStreamToLocalModel(input, destinationFile)
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed copying extracted asset pack model", e)
            false
        }
    }

    private fun copyStreamToLocalModel(input: InputStream, destinationFile: File) {
        val parent = destinationFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        val temp = getTempModelFile()
        input.use { stream ->
            temp.outputStream().use { output ->
                stream.copyTo(output)
            }
        }

        if (destinationFile.exists()) {
            destinationFile.delete()
        }
        temp.renameTo(destinationFile)
    }

    private fun cleanupOldModels() {
        val modelsDir = getModelsDir()
        modelsDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".gguf") && file.name != MODEL_FILENAME) {
                val deleted = file.delete()
                Log.d(TAG, "Cleaned old model ${file.name}: deleted=$deleted")
            }
        }
    }

    private fun getAvailableStorage(): Long {
        val stat = StatFs(context.filesDir.path)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    private fun hasEnoughStorage(): Boolean = getAvailableStorage() >= MIN_FREE_STORAGE

    private fun evaluateStorageState() {
        if (hasEnoughStorage()) {
            if (isOnWifi()) {
                _downloadState.value = ModelDownloadState.NotStarted
            } else {
                _downloadState.value = ModelDownloadState.WaitingForWifi
            }
        } else {
            val available = getAvailableStorage()
            _downloadState.value = ModelDownloadState.NeedsStorage(
                needBytes = MIN_FREE_STORAGE - available,
                haveBytes = available
            )
        }
    }

    private fun isOnWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    @Suppress("unused")
    private fun isCharging(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return false
        return bm.isCharging
    }

    private suspend fun attemptAutoDownload() {
        val canAutoStart = hasEnoughStorage() && isOnWifi()
        if (!canAutoStart) return

        if (_downloadState.value is ModelDownloadState.Downloading ||
            _downloadState.value is ModelDownloadState.Completed
        ) {
            return
        }

        dataStoreManager.putString(AUTO_DOWNLOAD_ATTEMPTED_KEY, "true")
        _downloadState.value = ModelDownloadState.Downloading(MODEL_FILENAME, getTempDownloadProgress())
        downloadStartedAtMs = System.currentTimeMillis()
        syncDownloadProgress()
        scheduleDownloadWorker()
    }

    override fun startDownload() {
        if (restoreBundledModelIfPresent(File(getModelsDir(), MODEL_FILENAME))) {
            val modelFile = File(getModelsDir(), MODEL_FILENAME)
            markModelReady(modelFile.length(), "Bundled model became available")
            return
        }

        if (!remoteFallbackEnabled) {
            startBundledModelMonitor(force = true)
            return
        }

        if (!hasEnoughStorage()) {
            val available = getAvailableStorage()
            _downloadState.value = ModelDownloadState.NeedsStorage(
                needBytes = MIN_FREE_STORAGE - available,
                haveBytes = available
            )
            syncDownloadProgress()
            return
        }

        if (!isOnWifi()) {
            _downloadState.value = ModelDownloadState.WaitingForWifi
            syncDownloadProgress()
            return
        }

        _downloadState.value = ModelDownloadState.Downloading(MODEL_FILENAME, getTempDownloadProgress())
        downloadStartedAtMs = System.currentTimeMillis()
        syncDownloadProgress()
        scheduleDownloadWorker()
    }

    private fun startBundledModelMonitor(force: Boolean) {
        if (bundledMonitorRunning && !force) return

        bundledMonitorRunning = true
        scope.launch {
            val destination = File(getModelsDir(), MODEL_FILENAME)
            var attempts = 0
            val maxAttempts = 24 // ~2 minutes at 5s interval

            while (!_hasModel.value && attempts < maxAttempts) {
                attempts++

                if (restoreBundledModelIfPresent(destination)) {
                    markModelReady(destination.length(), "Play asset pack model installed")
                    bundledMonitorRunning = false
                    return@launch
                }

                val progress = (attempts.toFloat() / maxAttempts.toFloat()).coerceAtMost(0.95f)
                _downloadState.value = ModelDownloadState.Downloading(MODEL_FILENAME, progress)
                syncDownloadProgress()
                delay(5_000)
            }

            if (!_hasModel.value) {
                _downloadState.value = ModelDownloadState.Failed(
                    "AI model pack is not available yet. Reopen app after Play Store finishes installation."
                )
                syncDownloadProgress()
            }

            bundledMonitorRunning = false
        }
    }

    private fun scheduleDownloadWorker(forceReplace: Boolean = false) {
        ModelDownloadWorker.enqueue(
            context = context,
            modelFilename = MODEL_FILENAME,
            replaceExisting = forceReplace
        )
    }

    fun updateProgress(progress: Float) {
        val current = _downloadState.value
        if (current is ModelDownloadState.Downloading) {
            _downloadState.value = current.copy(progress = progress)
            syncDownloadProgress()
        }
    }

    fun onDownloadComplete(filename: String) {
        _downloadState.value = ModelDownloadState.Completed(filename)
        _hasModel.value = true
        downloadStartedAtMs = 0L
        scope.launch {
            dataStoreManager.putString(ACTIVE_MODEL_KEY, "QWEN_0_5B")
        }
        cleanupOldModels()
        syncDownloadProgress()
        onModelReady?.invoke()
        Log.d(TAG, "Model download complete - SLM ready")
    }

    fun onDownloadFailed(error: String) {
        _downloadState.value = ModelDownloadState.Failed(error)
        downloadStartedAtMs = 0L
        Log.e(TAG, "Download failed: $error")
        syncDownloadProgress()
    }

    fun maybeRecoverStalledDownload(stallMs: Long = 120_000L) {
        val state = _downloadState.value
        if (state !is ModelDownloadState.Downloading) return

        if (!remoteFallbackEnabled) {
            startBundledModelMonitor(force = false)
            return
        }

        val partialProgress = getTempDownloadProgress()
        if (partialProgress > state.progress + 0.001f) {
            _downloadState.value = state.copy(progress = partialProgress)
            downloadStartedAtMs = System.currentTimeMillis()
            syncDownloadProgress()
            return
        }
        if (state.progress > 0f) return

        val now = System.currentTimeMillis()
        if (downloadStartedAtMs == 0L) {
            downloadStartedAtMs = now
            return
        }
        if (now - downloadStartedAtMs < stallMs) return

        when {
            !hasEnoughStorage() -> {
                evaluateStorageState()
                syncDownloadProgress()
            }
            !isOnWifi() -> {
                _downloadState.value = ModelDownloadState.WaitingForWifi
                syncDownloadProgress()
            }
            else -> {
                Log.w(TAG, "Download stalled at 0%, re-enqueueing model download worker")
                downloadStartedAtMs = now
                scheduleDownloadWorker(forceReplace = true)
            }
        }
    }

    fun getModelPath(): String? {
        val file = File(getModelsDir(), MODEL_FILENAME)
        return if (isValidModelFile(file)) file.absolutePath else null
    }

    fun getModelsDir(): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getTempModelFile(): File = File(getModelsDir(), "$MODEL_FILENAME.tmp")

    private fun getTempDownloadProgress(): Float {
        val tmp = getTempModelFile()
        if (!tmp.exists()) return 0f
        val bytes = tmp.length()
        if (bytes <= 0L) return 0f
        return (bytes.toDouble() / MODEL_SIZE_BYTES.toDouble())
            .coerceIn(0.001, 0.99)
            .toFloat()
    }

    suspend fun canMakeCloudCall(): Boolean {
        if (_hasModel.value) return true

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        val storedDate = dataStoreManager.getString(CLOUD_CALLS_DATE_KEY).first()
        val storedCount = dataStoreManager.getString(CLOUD_CALLS_TODAY_KEY).first()?.toIntOrNull() ?: 0
        val todayCount = if (storedDate == today) storedCount else 0

        return todayCount < MAX_CLOUD_CALLS_PER_DAY_NO_SLM
    }

    suspend fun recordCloudCall() {
        if (_hasModel.value) return

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        val storedDate = dataStoreManager.getString(CLOUD_CALLS_DATE_KEY).first()
        val storedCount = dataStoreManager.getString(CLOUD_CALLS_TODAY_KEY).first()?.toIntOrNull() ?: 0
        val todayCount = if (storedDate == today) storedCount + 1 else 1

        dataStoreManager.putString(CLOUD_CALLS_DATE_KEY, today)
        dataStoreManager.putString(CLOUD_CALLS_TODAY_KEY, todayCount.toString())
    }

    suspend fun getRemainingCloudCalls(): Int {
        if (_hasModel.value) return Int.MAX_VALUE

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        val storedDate = dataStoreManager.getString(CLOUD_CALLS_DATE_KEY).first()
        val storedCount = dataStoreManager.getString(CLOUD_CALLS_TODAY_KEY).first()?.toIntOrNull() ?: 0
        val todayCount = if (storedDate == today) storedCount else 0

        return (MAX_CLOUD_CALLS_PER_DAY_NO_SLM - todayCount).coerceAtLeast(0)
    }

    override fun dismissDownloadCard() {
        val current = _downloadState.value
        if (current is ModelDownloadState.NeedsStorage) return
        _downloadProgress.value = SLMDownloadProgress.Dismissed
    }
}

sealed class ModelDownloadState {
    object NotStarted : ModelDownloadState()
    data class Downloading(val model: String, val progress: Float) : ModelDownloadState()
    data class Completed(val model: String) : ModelDownloadState()
    data class Failed(val error: String) : ModelDownloadState()
    data class NeedsStorage(val needBytes: Long, val haveBytes: Long) : ModelDownloadState()
    object WaitingForWifi : ModelDownloadState()
}
