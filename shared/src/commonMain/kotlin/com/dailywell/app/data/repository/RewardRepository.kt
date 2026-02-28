package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface RewardRepository {

    // Balance Management
    suspend fun getCoinBalance(userId: String): Result<WellCoinBalance>
    fun observeCoinBalance(userId: String): Flow<WellCoinBalance>
    suspend fun updateCoinBalance(balance: WellCoinBalance): Result<Unit>

    // Earning Coins
    suspend fun awardCoins(
        userId: String,
        amount: Int,
        trigger: EarningTrigger,
        description: String,
        relatedHabitId: String? = null
    ): Result<CoinTransaction>

    suspend fun processHabitCompletion(userId: String, habitId: String): Result<CoinTransaction>
    suspend fun processDailyCheckin(userId: String): Result<CoinTransaction>
    suspend fun processPerfectDay(userId: String): Result<CoinTransaction>
    suspend fun processStreak(userId: String, streakDays: Int): Result<CoinTransaction>
    suspend fun processVoiceChat(userId: String): Result<CoinTransaction>
    suspend fun processChallengeComplete(userId: String, challengeId: String): Result<CoinTransaction>

    // Transaction History
    suspend fun getTransactionHistory(userId: String, limit: Int = 50): Result<List<CoinTransaction>>
    fun observeTransactions(userId: String): Flow<List<CoinTransaction>>
    suspend fun getTransactionById(transactionId: String): Result<CoinTransaction>

    // Reward Store
    suspend fun getAvailableRewards(): Result<List<RewardItem>>
    suspend fun getRewardsByCategory(category: RedemptionCategory): Result<List<RewardItem>>
    suspend fun getRewardById(itemId: String): Result<RewardItem>

    // Redemption
    suspend fun redeemReward(
        userId: String,
        itemId: String
    ): Result<RedemptionHistory>

    suspend fun getRedemptionHistory(userId: String): Result<List<RedemptionHistory>>
    suspend fun fulfillRedemption(redemptionId: String, fulfillmentData: String): Result<Unit>

    // Analytics
    suspend fun getDailyCoinSummary(userId: String, date: Instant): Result<DailyCoinSummary>
    suspend fun getWeeklyCoinSummary(userId: String, weekStart: Instant): Result<List<DailyCoinSummary>>
}
