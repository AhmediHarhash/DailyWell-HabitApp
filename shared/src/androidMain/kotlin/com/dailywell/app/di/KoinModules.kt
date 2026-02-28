package com.dailywell.app.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.dailywell.app.ai.ModelDownloadManager
import com.dailywell.app.ai.SLMDownloadInfo
import com.dailywell.app.ai.SLMService
import com.dailywell.app.ai.UserProfileBuilder
import com.dailywell.app.api.ClaudeApiClient
import com.dailywell.app.tts.PiperTtsService
import com.dailywell.app.api.FirebaseService
import com.dailywell.app.speech.SpeechRecognitionService
import com.dailywell.app.speech.SpeechRecognitionServiceImpl
import com.dailywell.app.api.HealthConnectService
import com.dailywell.app.api.CalendarService
import com.dailywell.app.billing.BillingManager
import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.local.db.DailyWellDatabase
import com.dailywell.app.data.repository.*
import com.dailywell.app.data.repository.NutritionRepository
import com.dailywell.app.data.repository.WorkoutRepository
import com.dailywell.app.data.repository.BodyMetricsRepository
import com.dailywell.app.ui.screens.insights.InsightsViewModel
import com.dailywell.app.ui.screens.intentions.IntentionsViewModel
import com.dailywell.app.ui.screens.auth.AuthViewModel
import com.dailywell.app.ui.screens.onboarding.OnboardingViewModel
import com.dailywell.app.ui.screens.paywall.PaywallViewModel
import com.dailywell.app.ui.screens.recovery.RecoveryViewModel
import com.dailywell.app.ui.screens.reminders.SmartRemindersViewModel
import com.dailywell.app.ui.screens.settings.SettingsViewModel
import com.dailywell.app.ui.screens.stacking.HabitStackingViewModel
import com.dailywell.app.ui.screens.today.TodayViewModel
import com.dailywell.app.ui.screens.week.WeekViewModel
import com.dailywell.app.ui.screens.insights.AIInsightsViewModel
import com.dailywell.app.ui.screens.audio.AudioCoachingViewModel
import com.dailywell.app.ui.screens.biometric.BiometricViewModel
import com.dailywell.app.ui.screens.coaching.AICoachingViewModel
import com.dailywell.app.ui.screens.gamification.GamificationViewModel
import com.dailywell.app.ui.screens.notifications.ProactiveNotificationSettingsViewModel
import com.dailywell.app.ui.screens.calendar.CalendarViewModel
import com.dailywell.app.ui.screens.atrisk.AtRiskViewModel
import com.dailywell.app.api.OpenFoodFactsClient
import com.dailywell.app.ui.screens.scan.FoodScanViewModel
import com.dailywell.app.ui.screens.scan.FoodScanViewModelImpl
import com.dailywell.app.ui.screens.water.WaterTrackingViewModel
import com.dailywell.app.ui.screens.water.WaterTrackingViewModelImpl
import com.dailywell.app.ui.screens.reflection.WeeklyReflectionViewModel
import com.dailywell.app.ui.screens.insights.InsightsTabViewModel
import com.dailywell.app.ui.screens.reflections.ReflectionsViewModel
import com.dailywell.app.ui.screens.achievements.AchievementsViewModel
import com.dailywell.app.ui.screens.healthconnect.HealthConnectViewModel
import com.dailywell.app.ui.screens.rewards.RewardStoreViewModel
import com.dailywell.app.ui.screens.wellness.WellnessScoreViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single { DailyWellDatabase.getInstance(androidContext()) }
    single { get<DailyWellDatabase>().habitDao() }
    single { get<DailyWellDatabase>().entryDao() }
    single { get<DailyWellDatabase>().achievementDao() }

    // AI Feature DAOs (v2 - Persistence for all 5 advanced features)
    single { get<DailyWellDatabase>().abTestDao() }
    single { get<DailyWellDatabase>().insightSchedulerDao() }
    single { get<DailyWellDatabase>().contextCacheDao() }
    single { get<DailyWellDatabase>().opusSchedulerDao() }
    single { get<DailyWellDatabase>().userAISettingsDao() }
    single { get<DailyWellDatabase>().aiUsageDao() }
    single { get<DailyWellDatabase>().aiInteractionDao() }

    // AI Feature Persistence Layer (wraps all AI DAOs)
    single {
        com.dailywell.app.data.local.AIFeaturePersistence(
            abTestDao = get(),
            insightSchedulerDao = get(),
            contextCacheDao = get(),
            opusSchedulerDao = get(),
            userAISettingsDao = get(),
            aiUsageDao = get(),
            aiInteractionDao = get()
        )
    }

    // DataStore
    single { DataStoreManager(androidContext()) }

    // Billing
    single { BillingManager(androidContext()) }

    // API Services - Real production integrations
    single { ClaudeApiClient() }
    single { PiperTtsService(androidContext()) }  // FREE Android TTS (offline, neural voices)
    single { FirebaseService() }
    single { HealthConnectService(androidContext()) }
    single { CalendarService(androidContext()) }
    single { com.dailywell.app.api.LocationService(androidContext()) }

    // Firebase Firestore - Core database for real-time sync
    single { Firebase.firestore }

    // Speech Recognition Service (Voice Input for AI Chat)
    single<SpeechRecognitionService> { SpeechRecognitionServiceImpl(androidContext()) }

    // SLM Model Download Manager (background model download from Firebase Storage)
    single { ModelDownloadManager(androidContext(), get()) }
    single<SLMDownloadInfo> { get<ModelDownloadManager>() }

    // User Profile Builder (SLM personalization â€” learns from user behavior)
    single { UserProfileBuilder(get<SettingsRepository>(), get<HabitRepository>(), get<EntryRepository>(), get()) }

    // On-device SLM: Qwen 0.5B only via Llamatik llama.cpp (FREE, offline)
    single {
        val slm = SLMService(androidContext(), get<ModelDownloadManager>(), get<UserProfileBuilder>())
        // Wire: when model download finishes, proactively init SLM so it's instant for user
        get<ModelDownloadManager>().onModelReady = {
            CoroutineScope(Dispatchers.IO).launch { slm.initialize() }
        }
        // Also try init now if model is already on disk (app restart after prior download)
        CoroutineScope(Dispatchers.IO).launch {
            if (slm.isModelAvailable()) slm.initialize()
        }
        slm
    }

    // Food Scanning - Open Food Facts (FREE API)
    single { OpenFoodFactsClient() }

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
    single<AudioCoachingRepository> { AudioCoachingRepositoryImpl(get(), get<PiperTtsService>()) }

    // Phase 4 Repositories
    single<BiometricRepository> { BiometricRepositoryImpl(get(), get<HealthConnectService>()) }
    single<AICoachingRepository> {
        AICoachingRepositoryImpl(
            dataStoreManager = get(),
            claudeApiClient = get<ClaudeApiClient>(),
            habitRepository = get<HabitRepository>(),
            entryRepository = get<EntryRepository>(),
            aiFeaturePersistence = get(),  // Persistence for all 5 AI features
            slmService = get<SLMService>(),  // On-device Qwen 0.5B
            settingsRepository = get<SettingsRepository>(),
            modelDownloadManager = get<ModelDownloadManager>()  // Cloud rate limiter when no SLM
        )
    }

    // Phase 5 Repositories - Gamification
    single<GamificationRepository> { GamificationRepositoryImpl(get(), get<FirebaseService>()) }

    // Phase 6 Repositories - Proactive AI Notifications
    single<ProactiveNotificationRepository> {
        ProactiveNotificationRepositoryImpl(
            get(),
            get<ClaudeApiClient>(),
            get<HabitRepository>(),
            get<EntryRepository>(),
            get<SettingsRepository>()
        )
    }

    // Phase 7 Repositories - Calendar Integration
    single<CalendarRepository> {
        CalendarRepositoryImpl(
            get(),
            get<CalendarService>(),
            get<HabitRepository>()
        )
    }

    // Phase 8 Repositories - At-Risk Predictions
    single<AtRiskRepository> {
        AtRiskRepositoryImpl(
            androidContext(),
            get<HabitRepository>(),
            get<CalendarRepository>(),
            get<EntryRepository>(),
            get<com.dailywell.app.api.LocationService>()
        )
    }

    // Phase 10 Repositories - Water Tracking
    single<WaterTrackingRepository> { WaterTrackingRepositoryImpl(get()) }

    // Phase 11 Repositories - Nutrition & Fitness (NEW)
    single {
        com.dailywell.app.api.ClaudeFoodVisionApi(
            apiKey = com.dailywell.app.api.ApiConfig.CLAUDE_API_KEY,
            model = com.dailywell.app.api.ApiConfig.CLAUDE_MODEL
        )
    }
    single { NutritionRepository(get(), get<SettingsRepository>(), get<AICoachingRepository>()) }
    single { WorkoutRepository() }
    single { BodyMetricsRepository() }

    // Phase 12 - Health Connect Repository (Full Health Connect Integration)
    single<HealthConnectRepository> { HealthConnectRepositoryImpl(androidContext()) }

    // Phase 13 - Reward System Repository (Coin Economy)
    single<RewardRepository> { RewardRepositoryImpl(get()) }

    // Phase 14 - 365 Daily Insights Repository (PRODUCTION-READY)
    // Provides unique insight for each day of the year, backed by Firebase
    single<DailyInsightsRepository> { DailyInsightsRepositoryImpl() }

    // Phase 15 - 120 Reflection Prompts Repository (PRODUCTION-READY)
    // Weekly reflection system with Firebase-backed persistence
    single<ReflectionPromptsRepository> { ReflectionPromptsRepositoryImpl(get<ClaudeApiClient>()) }

    // Phase 16 - 365 Micro-Challenges Repository (PRODUCTION-READY - Task #8)
    // Daily micro-challenges with Firebase-backed persistence and streak tracking
    single<MicroChallengeRepository> { MicroChallengeRepositoryImpl(get()) }

    // ViewModels
    // TodayViewModel: HabitRepo, EntryRepo, SettingsRepo, AchievementRepo, RewardRepo,
    //                 DailyInsightsRepo, MicroChallengeRepo, HabitStackRepo, HealthConnectRepo, IntentionRepo, RecoveryRepo, SmartReminderRepo, AudioCoachingRepo, AICoachingRepo, GamificationRepo, SLMDownloadInfo
    viewModel { TodayViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get<SLMDownloadInfo>()) }
    viewModel { WeekViewModel(get(), get()) }
    viewModel { OnboardingViewModel(get(), get(), get<AICoachingRepository>()) }
    viewModel { InsightsViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { PaywallViewModel(get(), get()) }
    viewModel {
        val firebaseService = get<FirebaseService>()
        AuthViewModel(
            settingsRepository = get(),
            getFirebaseUid = { firebaseService.getCurrentUserId() },
            getFirebaseDisplayName = { firebaseService.getCurrentUser()?.displayName },
            getFirebaseEmail = { firebaseService.getCurrentUser()?.email }
        )
    }

    // Phase 2 ViewModels
    viewModel { HabitStackingViewModel(get(), get()) }
    viewModel { IntentionsViewModel(get()) }
    viewModel { SmartRemindersViewModel(get(), get<HabitRepository>()) }
    viewModel { RecoveryViewModel(get()) }

    // Phase 3 ViewModels
    viewModel { AIInsightsViewModel(get()) }
    viewModel { AudioCoachingViewModel(get()) }

    // Phase 4 ViewModels
    viewModel { BiometricViewModel(get()) }
    viewModel { AICoachingViewModel(get(), get<SpeechRecognitionService>(), get<SLMDownloadInfo>()) }

    // Phase 5 ViewModels - Gamification & Rewards
    viewModel { RewardStoreViewModel(get(), get()) }
    viewModel { GamificationViewModel(get()) }

    // Phase 6 ViewModels - Proactive Notifications
    viewModel { ProactiveNotificationSettingsViewModel(get()) }

    // Phase 7 ViewModels - Calendar Integration
    viewModel { CalendarViewModel(get(), get(), get()) }

    // Phase 8 ViewModels - At-Risk Predictions
    viewModel { AtRiskViewModel(get()) }

    // Insights Tab Hub ViewModel (Features 35-36: real stats + featured insight)
    viewModel { InsightsTabViewModel(get(), get(), get(), get<PatternInsightRepository>()) }

    // Reflections Hub ViewModel (Feature 40: weekly reflections hub screen)
    viewModel { ReflectionsViewModel(get<ReflectionPromptsRepository>()) }

    // Phase 9 ViewModels - Food Scanning (Smart Scan)
    single<FoodScanViewModel> { FoodScanViewModelImpl(get(), get<ClaudeApiClient>()) }

    // Phase 10 ViewModels - Water Tracking
    single<WaterTrackingViewModel> { WaterTrackingViewModelImpl(get()) }

    // Wellness Score ViewModel
    single<WellnessScoreViewModel> { WellnessScoreViewModel(get()) }

    // Phase 15 ViewModels - Weekly Reflection (120 Prompts)
    viewModel { WeeklyReflectionViewModel(get()) }

    // Phase 16 ViewModels - Achievements (75 Creative Achievements with Firebase)
    viewModel { AchievementsViewModel(get()) }

    // Phase 17 ViewModels - Health Connect Integration (Task #10)
    // Full Health Connect integration for auto-completing habits from wearables/health apps
    viewModel { HealthConnectViewModel(get()) }
}

