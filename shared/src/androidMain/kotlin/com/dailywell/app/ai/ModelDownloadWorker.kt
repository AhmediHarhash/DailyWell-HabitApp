package com.dailywell.app.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Background worker that downloads the on-device Qwen model from HuggingFace CDN.
 *
 * Features:
 * - Downloads from HuggingFace (FREE, no hosting cost, no egress fees)
 * - Foreground notification with progress bar
 * - Survives app kill (WorkManager)
 * - Constraints: WiFi only + battery not low
 * - Resume support: uses HTTP Range header if temp file exists
 * - Retries: 3 attempts with exponential backoff
 */
class ModelDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val modelDownloadManager: ModelDownloadManager by inject()

    companion object {
        private const val TAG = "ModelDownloadWorker"
        const val WORK_NAME = "model_download"
        const val KEY_MODEL_FILENAME = "model_filename"
        const val CHANNEL_ID = "model_download_channel"
        private const val NOTIFICATION_ID = 3000

        // HuggingFace direct download URLs (FREE CDN, $0 cost)
        // Primary: bartowski GGUF repo
        // Fallback: your mirror repo (if available)
        private const val PRIMARY_URL =
            "https://huggingface.co/bartowski/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/"
        private const val FALLBACK_URL =
            "https://huggingface.co/hekaxai/dailywell-qwen2.5-0.5b/resolve/main/"

        private const val BUFFER_SIZE = 8192

        fun enqueue(
            context: Context,
            modelFilename: String,
            replaceExisting: Boolean = false
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)  // WiFi only
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_MODEL_FILENAME to modelFilename))
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .build()

            val policy = if (replaceExisting) {
                ExistingWorkPolicy.REPLACE
            } else {
                ExistingWorkPolicy.KEEP
            }

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                policy,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val filename = inputData.getString(KEY_MODEL_FILENAME) ?: return@withContext Result.failure()

        createNotificationChannel()
        setForeground(createForegroundInfo("Downloading AI coach...", 0))

        try {
            val destFile = File(modelDownloadManager.getModelsDir(), filename)
            val tempFile = File(modelDownloadManager.getModelsDir(), "$filename.tmp")

            // Already downloaded — skip
            if (destFile.exists() && destFile.length() > ModelDownloadManager.MIN_VALID_MODEL_BYTES) {
                modelDownloadManager.onDownloadComplete(filename)
                return@withContext Result.success()
            }

            // Try our repo first, fall back to bartowski
            val downloadUrl = "$PRIMARY_URL$filename"
            val fallbackUrl = "$FALLBACK_URL$filename"
            Log.d(TAG, "Downloading $filename from HuggingFace")

            // Check how much we already have (resume support)
            val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

            // Try primary, then fallback URL
            val connection = openConnectionWithFallback(downloadUrl, fallbackUrl, existingBytes)
            if (connection == null) {
                modelDownloadManager.onDownloadFailed("Both download URLs failed")
                return@withContext Result.retry()
            }

            val responseCode = connection.responseCode

            // Total file size
            val contentLength = connection.contentLengthLong
            val totalBytes = if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                existingBytes + contentLength
            } else {
                contentLength
            }

            // Download with progress
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tempFile, existingBytes > 0 && responseCode == HttpURLConnection.HTTP_PARTIAL)

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            var downloadedBytes = existingBytes
            var lastReportedPercent = -1
            var lastReportAtMs = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            Log.d(TAG, "Download cancelled by system")
                            connection.disconnect()
                            return@withContext Result.failure()
                        }

                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        // Update progress
                        if (totalBytes > 0) {
                            val progress = downloadedBytes.toFloat() / totalBytes
                            val percent = (progress * 100).toInt()
                            val now = System.currentTimeMillis()
                            val shouldReport =
                                percent >= lastReportedPercent + 1 ||
                                    now - lastReportAtMs >= 1500L ||
                                    downloadedBytes >= totalBytes

                            if (shouldReport) {
                                modelDownloadManager.updateProgress(progress)
                                setForeground(createForegroundInfo(
                                    "Downloading AI coach... $percent%", percent
                                ))
                                lastReportedPercent = percent
                                lastReportAtMs = now
                            }
                        }
                    }
                }
            }

            connection.disconnect()

            // Verify download
            if (!tempFile.exists() || tempFile.length() < ModelDownloadManager.MIN_VALID_MODEL_BYTES) {
                tempFile.delete()
                modelDownloadManager.onDownloadFailed("Download incomplete")
                return@withContext Result.retry()
            }

            // Move temp → final
            if (destFile.exists()) destFile.delete()
            tempFile.renameTo(destFile)

            Log.d(TAG, "Download complete: ${destFile.length() / 1024 / 1024}MB")
            modelDownloadManager.onDownloadComplete(filename)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            modelDownloadManager.onDownloadFailed(e.message ?: "Download failed")

            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    /**
     * Try primary URL, fall back to secondary if primary fails.
     * Supports HTTP Range for resume.
     */
    private fun openConnectionWithFallback(
        primaryUrl: String,
        fallbackUrl: String,
        existingBytes: Long
    ): HttpURLConnection? {
        for (url in listOf(primaryUrl, fallbackUrl)) {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 30_000
                conn.readTimeout = 30_000
                if (existingBytes > 0) {
                    conn.setRequestProperty("Range", "bytes=$existingBytes-")
                }
                conn.connect()
                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_PARTIAL) {
                    Log.d(TAG, "Connected to $url (HTTP $code)")
                    return conn
                }
                conn.disconnect()
                Log.w(TAG, "Failed $url (HTTP $code), trying fallback...")
            } catch (e: Exception) {
                Log.w(TAG, "Failed $url: ${e.message}, trying fallback...")
            }
        }
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AI Model Download",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress while downloading the AI model"
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(text: String, progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle("DailyWell")
            .setContentText(text)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
}
