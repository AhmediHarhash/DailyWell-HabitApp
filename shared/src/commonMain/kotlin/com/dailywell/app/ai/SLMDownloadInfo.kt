package com.dailywell.app.ai

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-neutral interface for SLM model download state.
 * Allows commonMain ViewModels to observe download progress without
 * depending on Android-specific ModelDownloadManager.
 */
interface SLMDownloadInfo {
    val downloadProgress: StateFlow<SLMDownloadProgress>

    fun startDownload()
    fun dismissDownloadCard()
}

sealed class SLMDownloadProgress {
    /** No model downloaded yet, WiFi available, ready to start */
    object NotStarted : SLMDownloadProgress()
    /** Currently downloading */
    data class Downloading(val modelName: String, val progress: Float) : SLMDownloadProgress()
    /** Model ready to use */
    data class Ready(val modelName: String) : SLMDownloadProgress()
    /** Download failed */
    data class Failed(val error: String) : SLMDownloadProgress()
    /** Not enough storage — user must free space. Cannot be dismissed. */
    data class NeedsStorage(val needBytes: Long, val haveBytes: Long) : SLMDownloadProgress()
    /** Has storage but needs WiFi to download */
    object WaitingForWifi : SLMDownloadProgress()
    /** User dismissed card (only for non-critical states) */
    object Dismissed : SLMDownloadProgress()
}
