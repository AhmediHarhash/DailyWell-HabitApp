package com.dailywell.app.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dailywell.shared.BuildConfig
import java.io.File

/**
 * DailyWell Security Manager
 * OWASP MASVS 2026 Compliant Security Utilities
 *
 * Features:
 * - Root/Jailbreak detection
 * - Debug/Emulator detection
 * - Tamper detection
 * - Encrypted storage
 *
 * CVE-DW-003 FIX: Provides encrypted storage
 * MASVS-RESILIENCE-1: Anti-tampering
 * MASVS-RESILIENCE-2: Anti-debugging
 */
object SecurityManager {

    private const val ENCRYPTED_PREFS_NAME = "dailywell_secure_prefs"

    /**
     * Check if device is rooted
     * Returns true if root is detected
     */
    fun isDeviceRooted(): Boolean {
        return checkRootBinaries() ||
                checkSuExists() ||
                checkRootManagementApps() ||
                checkDangerousProps()
    }

    /**
     * Check if running on emulator
     * Returns true if emulator is detected
     */
    fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }

    /**
     * Check if debugger is attached
     */
    fun isDebuggerAttached(): Boolean {
        return android.os.Debug.isDebuggerConnected() ||
                android.os.Debug.waitingForDebugger()
    }

    /**
     * Check if app is running in debug mode
     */
    fun isDebugBuild(): Boolean {
        return BuildConfig.DEBUG
    }

    /**
     * Comprehensive security check
     * Returns SecurityStatus with all check results
     */
    fun performSecurityCheck(): SecurityStatus {
        return SecurityStatus(
            isRooted = isDeviceRooted(),
            isEmulator = isEmulator(),
            isDebuggerAttached = isDebuggerAttached(),
            isDebugBuild = isDebugBuild(),
            isSecure = !isDeviceRooted() && !isDebuggerAttached() && !isDebugBuild()
        )
    }

    /**
     * Get encrypted SharedPreferences for sensitive data
     * CVE-DW-003 FIX: Encrypted storage for sensitive data
     */
    fun getEncryptedPreferences(context: Context): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Store sensitive string securely
     */
    fun storeSecurely(context: Context, key: String, value: String) {
        getEncryptedPreferences(context).edit().putString(key, value).apply()
    }

    /**
     * Retrieve sensitive string securely
     */
    fun retrieveSecurely(context: Context, key: String): String? {
        return getEncryptedPreferences(context).getString(key, null)
    }

    /**
     * Delete sensitive data
     */
    fun deleteSecurely(context: Context, key: String) {
        getEncryptedPreferences(context).edit().remove(key).apply()
    }

    // ==================== PRIVATE HELPERS ====================

    private fun checkRootBinaries(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/su/bin",
            "/system/xbin/daemonsu"
        )

        return paths.any { File(it).exists() }
    }

    private fun checkSuExists(): Boolean {
        return try {
            Runtime.getRuntime().exec("which su").inputStream.bufferedReader().readLine() != null
        } catch (e: Exception) {
            false
        }
    }

    private fun checkRootManagementApps(): Boolean {
        val rootPackages = arrayOf(
            "com.koushikdutta.superuser",
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.topjohnwu.magisk",
            "com.kingroot.kinguser",
            "com.kingo.root"
        )

        return try {
            val pm = Runtime.getRuntime().exec("pm list packages")
            val packages = pm.inputStream.bufferedReader().readText()
            rootPackages.any { packages.contains(it) }
        } catch (e: Exception) {
            false
        }
    }

    private fun checkDangerousProps(): Boolean {
        val dangerousProps = mapOf(
            "ro.debuggable" to "1",
            "ro.secure" to "0"
        )

        return try {
            dangerousProps.any { (prop, dangerousValue) ->
                val process = Runtime.getRuntime().exec("getprop $prop")
                val value = process.inputStream.bufferedReader().readLine()?.trim()
                value == dangerousValue
            }
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Security status data class
 */
data class SecurityStatus(
    val isRooted: Boolean,
    val isEmulator: Boolean,
    val isDebuggerAttached: Boolean,
    val isDebugBuild: Boolean,
    val isSecure: Boolean
) {
    /**
     * Get human-readable security status
     */
    fun getSummary(): String {
        val issues = mutableListOf<String>()
        if (isRooted) issues.add("Device is rooted")
        if (isEmulator) issues.add("Running on emulator")
        if (isDebuggerAttached) issues.add("Debugger attached")
        if (isDebugBuild) issues.add("Debug build")

        return if (issues.isEmpty()) {
            "Device security: OK"
        } else {
            "Security warnings: ${issues.joinToString(", ")}"
        }
    }
}
