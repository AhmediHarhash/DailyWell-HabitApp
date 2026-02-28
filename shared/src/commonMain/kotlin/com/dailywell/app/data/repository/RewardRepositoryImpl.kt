package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class RewardRepositoryImpl(
    private val firestore: FirebaseFirestore
) : RewardRepository {

    private val balancesCollection = firestore.collection("coin_balances")
    private val transactionsCollection = firestore.collection("coin_transactions")
    private val redemptionsCollection = firestore.collection("redemptions")

    override suspend fun getCoinBalance(userId: String): Result<WellCoinBalance> = runCatching {
        val doc = balancesCollection.document(userId).get()
        if (doc.exists) {
            doc.data()
        } else {
            // Initialize new balance
            val newBalance = WellCoinBalance(
                userId = userId,
                totalCoins = 0,
                lifetimeEarned = 0,
                lifetimeSpent = 0,
                lastUpdated = Clock.System.now()
            )
            balancesCollection.document(userId).set(newBalance)
            newBalance
        }
    }

    override fun observeCoinBalance(userId: String): Flow<WellCoinBalance> = flow {
        balancesCollection.document(userId).snapshots.collect { snapshot ->
            if (snapshot.exists) {
                emit(snapshot.data<WellCoinBalance>())
            } else {
                emit(WellCoinBalance(
                    userId = userId,
                    totalCoins = 0,
                    lifetimeEarned = 0,
                    lifetimeSpent = 0,
                    lastUpdated = Clock.System.now()
                ))
            }
        }
    }

    override suspend fun updateCoinBalance(balance: WellCoinBalance): Result<Unit> = runCatching {
        balancesCollection.document(balance.userId).set(balance)
    }

    override suspend fun awardCoins(
        userId: String,
        amount: Int,
        trigger: EarningTrigger,
        description: String,
        relatedHabitId: String?
    ): Result<CoinTransaction> = runCatching {
        // Check if this trigger has daily limits
        val rule = CoinEarningRules.DEFAULT_RULES.find { it.trigger == trigger }
        if (rule?.maxPerDay != null) {
            val todayStart = Clock.System.todayIn(TimeZone.currentSystemDefault())
                .atStartOfDayIn(TimeZone.currentSystemDefault())
            val todayTransactions = getTransactionHistory(userId, limit = 100)
                .getOrNull()
                ?.filter {
                    it.type == TransactionType.EARNED &&
                    it.source == trigger.name.lowercase() &&
                    it.timestamp > todayStart
                } ?: emptyList()

            if (todayTransactions.size >= rule.maxPerDay) {
                throw IllegalStateException("Daily limit reached for ${trigger.name}")
            }
        }

        // Get current balance
        val balance = getCoinBalance(userId).getOrThrow()

        // Create transaction
        val transaction = CoinTransaction(
            id = Uuid.random().toString(),
            userId = userId,
            amount = amount,
            type = TransactionType.EARNED,
            source = trigger.name.lowercase(),
            description = description,
            relatedHabitId = relatedHabitId,
            relatedItemId = null,
            timestamp = Clock.System.now()
        )

        // Save transaction
        transactionsCollection.document(transaction.id).set(transaction)

        // Update balance
        val updatedBalance = balance.copy(
            totalCoins = balance.totalCoins + amount,
            lifetimeEarned = balance.lifetimeEarned + amount,
            lastUpdated = Clock.System.now()
        )
        updateCoinBalance(updatedBalance).getOrThrow()

        transaction
    }

    override suspend fun processHabitCompletion(userId: String, habitId: String): Result<CoinTransaction> {
        return awardCoins(
            userId = userId,
            amount = 5,
            trigger = EarningTrigger.HABIT_COMPLETION,
            description = "Completed a habit",
            relatedHabitId = habitId
        )
    }

    override suspend fun processDailyCheckin(userId: String): Result<CoinTransaction> {
        return awardCoins(
            userId = userId,
            amount = 10,
            trigger = EarningTrigger.DAILY_CHECKIN,
            description = "Daily check-in bonus"
        )
    }

    override suspend fun processPerfectDay(userId: String): Result<CoinTransaction> {
        return awardCoins(
            userId = userId,
            amount = 20,
            trigger = EarningTrigger.PERFECT_DAY,
            description = "Perfect day! All habits completed"
        )
    }

    override suspend fun processStreak(userId: String, streakDays: Int): Result<CoinTransaction> {
        val (amount, trigger, desc) = when {
            streakDays >= 30 -> Triple(200, EarningTrigger.STREAK_30, "30-day streak!")
            streakDays >= 7 -> Triple(50, EarningTrigger.STREAK_7, "7-day streak!")
            else -> return Result.failure(IllegalArgumentException("Streak too short"))
        }

        return awardCoins(
            userId = userId,
            amount = amount,
            trigger = trigger,
            description = desc
        )
    }

    override suspend fun processVoiceChat(userId: String): Result<CoinTransaction> {
        return awardCoins(
            userId = userId,
            amount = 15,
            trigger = EarningTrigger.VOICE_CHAT_SESSION,
            description = "Voice chat with AI coach"
        )
    }

    override suspend fun processChallengeComplete(userId: String, challengeId: String): Result<CoinTransaction> {
        return awardCoins(
            userId = userId,
            amount = 30,
            trigger = EarningTrigger.CHALLENGE_COMPLETE,
            description = "Completed daily challenge"
        )
    }

    override suspend fun getTransactionHistory(userId: String, limit: Int): Result<List<CoinTransaction>> = runCatching {
        transactionsCollection
            .where { "userId" equalTo userId }
            .orderBy("timestamp", dev.gitlive.firebase.firestore.Direction.DESCENDING)
            .limit(limit)
            .get()
            .documents
            .map { it.data<CoinTransaction>() }
    }

    override fun observeTransactions(userId: String): Flow<List<CoinTransaction>> = flow {
        transactionsCollection
            .where { "userId" equalTo userId }
            .orderBy("timestamp", dev.gitlive.firebase.firestore.Direction.DESCENDING)
            .limit(50)
            .snapshots
            .collect { snapshot ->
                emit(snapshot.documents.map { it.data<CoinTransaction>() })
            }
    }

    override suspend fun getTransactionById(transactionId: String): Result<CoinTransaction> = runCatching {
        transactionsCollection.document(transactionId).get().data()
    }

    override suspend fun getAvailableRewards(): Result<List<RewardItem>> = runCatching {
        RewardStore.REWARD_CATALOG.filter { it.isAvailable }
    }

    override suspend fun getRewardsByCategory(category: RedemptionCategory): Result<List<RewardItem>> = runCatching {
        RewardStore.REWARD_CATALOG.filter { it.category == category && it.isAvailable }
    }

    override suspend fun getRewardById(itemId: String): Result<RewardItem> = runCatching {
        RewardStore.REWARD_CATALOG.find { it.id == itemId }
            ?: throw NoSuchElementException("Reward item not found: $itemId")
    }

    override suspend fun redeemReward(
        userId: String,
        itemId: String
    ): Result<RedemptionHistory> = runCatching {
        // Get item details
        val item = getRewardById(itemId).getOrThrow()

        // Get user balance
        val balance = getCoinBalance(userId).getOrThrow()

        // Check if user has enough coins
        if (balance.totalCoins < item.coinCost) {
            throw IllegalStateException("Insufficient coins. Need ${item.coinCost}, have ${balance.totalCoins}")
        }

        // Create redemption record
        val redemption = RedemptionHistory(
            id = Uuid.random().toString(),
            userId = userId,
            itemId = itemId,
            itemName = item.name,
            coinCost = item.coinCost,
            redeemedAt = Clock.System.now(),
            status = RedemptionStatus.PENDING,
            fulfillmentData = null
        )

        // Save redemption
        redemptionsCollection.document(redemption.id).set(redemption)

        // Create transaction for spending
        val transaction = CoinTransaction(
            id = Uuid.random().toString(),
            userId = userId,
            amount = -item.coinCost,
            type = TransactionType.SPENT,
            source = "redemption",
            description = "Redeemed: ${item.name}",
            relatedHabitId = null,
            relatedItemId = itemId,
            timestamp = Clock.System.now()
        )
        transactionsCollection.document(transaction.id).set(transaction)

        // Update balance
        val updatedBalance = balance.copy(
            totalCoins = balance.totalCoins - item.coinCost,
            lifetimeSpent = balance.lifetimeSpent + item.coinCost,
            lastUpdated = Clock.System.now()
        )
        updateCoinBalance(updatedBalance).getOrThrow()

        redemption
    }

    override suspend fun getRedemptionHistory(userId: String): Result<List<RedemptionHistory>> = runCatching {
        redemptionsCollection
            .where { "userId" equalTo userId }
            .orderBy("redeemedAt", dev.gitlive.firebase.firestore.Direction.DESCENDING)
            .get()
            .documents
            .map { it.data<RedemptionHistory>() }
    }

    override suspend fun fulfillRedemption(redemptionId: String, fulfillmentData: String): Result<Unit> = runCatching {
        val redemption = redemptionsCollection.document(redemptionId).get().data<RedemptionHistory>()
        val updated = redemption.copy(
            status = RedemptionStatus.FULFILLED,
            fulfillmentData = fulfillmentData
        )
        redemptionsCollection.document(redemptionId).set(updated)
    }

    override suspend fun getDailyCoinSummary(userId: String, date: Instant): Result<DailyCoinSummary> = runCatching {
        // Get all transactions for the day
        val transactions = getTransactionHistory(userId, limit = 500).getOrThrow()
            .filter { it.timestamp.epochSeconds / 86400 == date.epochSeconds / 86400 }

        val earned = transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val spent = transactions.filter { it.amount < 0 }.sumOf { -it.amount }
        val topSource = transactions
            .filter { it.amount > 0 }
            .groupBy { it.source }
            .maxByOrNull { it.value.size }
            ?.key ?: "none"

        DailyCoinSummary(
            date = date,
            coinsEarned = earned,
            coinsSpent = spent,
            netChange = earned - spent,
            topEarningSource = topSource
        )
    }

    override suspend fun getWeeklyCoinSummary(userId: String, weekStart: Instant): Result<List<DailyCoinSummary>> = runCatching {
        val summaries = mutableListOf<DailyCoinSummary>()
        for (day in 0..6) {
            val date = Instant.fromEpochSeconds(weekStart.epochSeconds + (day * 86400))
            summaries.add(getDailyCoinSummary(userId, date).getOrThrow())
        }
        summaries
    }
}
