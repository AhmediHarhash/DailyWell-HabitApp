package com.dailywell.app.di

import com.dailywell.app.api.ClaudeApiClient
import com.dailywell.app.tts.PiperTtsService
import com.dailywell.app.api.FirebaseService
import com.dailywell.app.api.HealthConnectService
import com.dailywell.app.billing.BillingManager
import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.local.db.DailyWellDatabase
import com.dailywell.app.data.repository.*
import com.dailywell.app.ui.screens.insights.InsightsViewModel
import com.dailywell.app.ui.screens.intentions.IntentionsViewModel
import com.dailywell.app.ui.screens.onboarding.OnboardingViewModel
import com.dailywell.app.ui.screens.paywall.PaywallViewModel
import com.dailywell.app.ui.screens.recovery.RecoveryViewModel
import com.dailywell.app.ui.screens.reminders.SmartRemindersViewModel
import com.dailywell.app.ui.screens.settings.SettingsViewModel
import com.dailywell.app.ui.screens.stacking.HabitStackingViewModel
import com.dailywell.app.ui.screens.today.TodayViewModel
import com.dailywell.app.ui.screens.week.WeekViewModel
import com.dailywell.app.ui.screens.insights.AIInsightsViewModel
import com.dailywell.app.ui.screens.social.SocialViewModel
import com.dailywell.app.ui.screens.audio.AudioCoachingViewModel
import com.dailywell.app.ui.screens.biometric.BiometricViewModel
import com.dailywell.app.ui.screens.family.FamilyViewModel
import com.dailywell.app.ui.screens.coaching.AICoachingViewModel
import com.dailywell.app.ui.screens.gamification.GamificationViewModel
import com.dailywell.app.ui.screens.challenges.ChallengeViewModel
import com.dailywell.app.ui.screens.leaderboard.LeaderboardViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single { DailyWellDatabase.getInstance(androidContext()) }
    single { get<DailyWellDatabase>().habitDao() }
    single { get<DailyWellDatabase>().entryDao() }
    single { get<DailyWellDatabase>().achievementDao() }

    // DataStore
    single { DataStoreManager(androidContext()) }

    // Billing
    single { BillingManager(androidContext()) }

    // API Services - Real production integrations
    single { ClaudeApiClient() }
    single { PiperTtsService(androidContext()) }  // FREE Android TTS (offline, neural voices)
    single { FirebaseService() }
    single { HealthConnectService(androidContext()) }

    // Repositories - order matters for dependencies
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<AchievementRepository> { AchievementRepositoryImpl(get()) }
    single<HabitRepository> { HabitRepositoryImpl(get(), get(), get<SettingsRepository>()) }
    single<EntryRepository> { EntryRepositoryImpl(get(), get(), get<SettingsRepository>()) }

    // Phase 2 Repositories
    single<HabitStackRepository> { HabitStackRepositoryImpl(get()) }
    single<IntentionRepository> { IntentionRepositoryImpl(get()) }
    single<SmartReminderRepository> { SmartReminderRepositoryImpl(get()) }
    single<RecoveryRepository> { RecoveryRepositoryImpl(get()) }

    // Phase 3 Repositories
    single<PatternInsightRepository> { PatternInsightRepositoryImpl(get()) }
    single<SocialRepository> { SocialRepositoryImpl(get(), get<FirebaseService>()) }
    single<AudioCoachingRepository> { AudioCoachingRepositoryImpl(get(), get<PiperTtsService>()) }

    // Phase 4 Repositories
    single<BiometricRepository> { BiometricRepositoryImpl(get(), get<HealthConnectService>()) }
    single<FamilyRepository> { FamilyRepositoryImpl(get(), get<FirebaseService>()) }
    single<AICoachingRepository> { AICoachingRepositoryImpl(get(), get<ClaudeApiClient>(), get<HabitRepository>(), get<EntryRepository>()) }

    // Phase 5 Repositories - Gamification
    single<GamificationRepository> { GamificationRepositoryImpl(get(), get<FirebaseService>()) }
    single<ChallengeRepository> { ChallengeRepositoryImpl(get(), get<FirebaseService>()) }
    single<LeaderboardRepository> { LeaderboardRepositoryImpl(get(), get<FirebaseService>()) }

    // ViewModels
    viewModel { TodayViewModel(get(), get(), get(), get()) }
    viewModel { WeekViewModel(get(), get()) }
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { InsightsViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { PaywallViewModel(get()) }

    // Phase 2 ViewModels
    viewModel { HabitStackingViewModel(get(), get()) }
    viewModel { IntentionsViewModel(get()) }
    viewModel { SmartRemindersViewModel(get()) }
    viewModel { RecoveryViewModel(get()) }

    // Phase 3 ViewModels
    viewModel { AIInsightsViewModel(get()) }
    viewModel { SocialViewModel(get()) }
    viewModel { AudioCoachingViewModel(get()) }

    // Phase 4 ViewModels
    viewModel { BiometricViewModel(get()) }
    viewModel { FamilyViewModel(get()) }
    viewModel { AICoachingViewModel(get()) }

    // Phase 5 ViewModels - Gamification
    viewModel { GamificationViewModel(get()) }
    viewModel { ChallengeViewModel(get(), get()) }
    viewModel { LeaderboardViewModel(get(), get()) }
}
