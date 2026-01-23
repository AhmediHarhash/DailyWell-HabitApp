package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for biometric data and correlations
 */
interface BiometricRepository {
    // Data access
    fun getBiometricData(): Flow<BiometricData>
    fun getDashboardSummary(): Flow<BiometricDashboardSummary>
    fun getCorrelations(): Flow<List<BiometricCorrelation>>
    fun getInsights(): Flow<List<BiometricInsight>>
    fun getConnectedDevices(): Flow<List<ConnectedDevice>>

    // Sleep data
    fun getSleepRecords(days: Int = 30): Flow<List<SleepBiometricRecord>>
    suspend fun addSleepRecord(record: SleepBiometricRecord)

    // HRV data
    fun getHrvRecords(days: Int = 30): Flow<List<HrvRecord>>
    suspend fun addHrvRecord(record: HrvRecord)

    // Activity data
    fun getActivityRecords(days: Int = 30): Flow<List<ActivityRecord>>
    suspend fun addActivityRecord(record: ActivityRecord)

    // Device management
    suspend fun connectDevice(source: BiometricSource, deviceName: String)
    suspend fun disconnectDevice(source: BiometricSource)
    suspend fun syncDevice(source: BiometricSource)

    // Analysis
    suspend fun analyzeCorrelations(habitCompletions: Map<String, List<Boolean>>)
    suspend fun generateInsights()
    fun getRecoveryScore(): Flow<Int>

    // Insights management
    suspend fun dismissInsight(insightId: String)
}
