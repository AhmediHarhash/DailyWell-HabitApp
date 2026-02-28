package com.dailywell.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.dailywell.app.api.AuthState
import com.dailywell.app.api.FirebaseService
import kotlinx.coroutines.launch
import com.dailywell.app.billing.BillingManager
import com.dailywell.app.billing.PurchaseState
import com.dailywell.app.core.theme.DailyWellTheme
import com.dailywell.app.data.model.CalendarOAuthCallback
import com.dailywell.app.data.model.CalendarProvider
import com.dailywell.app.data.model.PhotoAngle
import com.dailywell.app.data.model.ThemeMode
import com.dailywell.app.data.model.UserSettings
import com.dailywell.app.data.repository.SettingsRepository
import com.dailywell.app.ui.navigation.MainNavigation
import com.dailywell.app.ui.screens.paywall.PaywallScreen
import com.dailywell.app.ui.screens.paywall.PaywallViewModel
import com.dailywell.app.ui.screens.calendar.TrackerExportMode
import com.dailywell.app.ui.screens.calendar.TrackerExportPayload
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Debug flag for testing premium features without billing
// IMPORTANT: Set to false for production release!
private const val DEBUG_ENABLE_PREMIUM = false

class MainActivity : ComponentActivity() {

    private val billingManager: BillingManager by inject()

    /** Deep link from notification tap — consumed by MainNavigation */
    var pendingDeepLink = mutableStateOf<String?>(null)
        private set

    /** OAuth callback from Calendar provider — consumed by Calendar screen */
    var pendingCalendarOAuth = mutableStateOf<CalendarOAuthCallback?>(null)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }

        // Note: ProactiveNotificationManager.initialize() is called in DailyWellApplication.onCreate()
        // — no need to duplicate here. WorkManager survives activity recreation.

        // Handle app intents (notifications + calendar OAuth callbacks)
        handleAppIntent(intent)

        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                DailyWellApp(
                    activity = this,
                    billingManager = billingManager
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAppIntent(intent)
    }

    private fun handleAppIntent(intent: Intent?) {
        handleCalendarOAuthIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val notificationId = intent?.getStringExtra("notification_id") ?: return
        val notificationType = intent.getStringExtra("notification_type") ?: return

        // Record notification open for smart timing learning
        try {
            val repo = org.koin.java.KoinJavaComponent.getKoin()
                .get<com.dailywell.app.data.repository.ProactiveNotificationRepository>()
            kotlinx.coroutines.MainScope().launch {
                repo.recordNotificationOpened(notificationId)
            }
        } catch (_: Exception) {}

        // Extract deep link for navigation (e.g., "dailywell://today", "dailywell://insights")
        val deepLink = intent.getStringExtra("deep_link")
        if (deepLink != null) {
            pendingDeepLink.value = deepLink
        }
    }

    private fun handleCalendarOAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (!data.isHierarchical) return
        if (!data.scheme.equals("com.dailywell.android", ignoreCase = true)) return

        val authCode = data.getQueryParameter("code") ?: return
        if (authCode.isBlank()) return

        val provider = when {
            data.host.equals("auth", ignoreCase = true) -> CalendarProvider.OUTLOOK
            data.host.equals("oauth2callback", ignoreCase = true) -> CalendarProvider.GOOGLE
            data.path?.contains("oauth2callback", ignoreCase = true) == true -> CalendarProvider.GOOGLE
            data.getQueryParameter("state")?.contains("outlook", ignoreCase = true) == true -> CalendarProvider.OUTLOOK
            else -> CalendarProvider.GOOGLE
        }

        pendingCalendarOAuth.value = CalendarOAuthCallback(
            provider = provider,
            authCode = authCode
        )
    }

    /** Call after navigating to clear the pending deep link */
    fun consumeDeepLink() {
        pendingDeepLink.value = null
    }

    fun consumeCalendarOAuth() {
        pendingCalendarOAuth.value = null
    }

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
    }
}

@Composable
fun DailyWellApp(
    activity: ComponentActivity,
    billingManager: BillingManager
) {
    val settingsRepository: SettingsRepository = koinInject()
    var settings by remember { mutableStateOf(UserSettings()) }
    var showPaywall by remember { mutableStateOf(false) }
    var pendingProgressPhotoCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var pendingProgressPhotoAngle by remember { mutableStateOf<PhotoAngle?>(null) }

    val currentDate = remember {
        LocalDate.now().toString()
    }
    val hasFullAccess = settings.hasPremiumAccess(currentDate)

    val progressPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        val callback = pendingProgressPhotoCallback
        val angle = pendingProgressPhotoAngle
        pendingProgressPhotoCallback = null
        pendingProgressPhotoAngle = null

        if (bitmap != null && callback != null && angle != null) {
            saveBitmapToStorage(activity, bitmap, angle)?.let { photoPath ->
                callback(photoPath)
            }
        }
    }

    val onTakeProgressPhoto: (PhotoAngle, (String) -> Unit) -> Unit = { angle, onSaved ->
        pendingProgressPhotoAngle = angle
        pendingProgressPhotoCallback = onSaved
        progressPhotoLauncher.launch(null)
    }

    // Share streak function - for viral marketing
    val shareStreak: (String, String) -> Unit = { text, _ ->
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "My DailyWell Streak")
        }
        activity.startActivity(Intent.createChooser(shareIntent, "Share your streak"))
    }

    val shareCalendarTrackerImage: (TrackerExportPayload) -> Unit = { payload ->
        shareTrackerAsImage(activity, payload)
    }

    val exportCalendarTrackerPdf: (TrackerExportPayload) -> Unit = { payload ->
        shareTrackerAsPdf(activity, payload)
    }

    // Collect settings
    LaunchedEffect(Unit) {
        settingsRepository.getSettings().collect {
            settings = it
        }
    }

    // Debug: Enable premium for testing
    LaunchedEffect(Unit) {
        if (DEBUG_ENABLE_PREMIUM && !settings.isPremium) {
            settingsRepository.setPremium(true)
        }
    }

    // Sync premium status from billing (bidirectional)
    LaunchedEffect(Unit) {
        billingManager.isPremium.collect { isPremium ->
            if (isPremium != settings.isPremium) {
                settingsRepository.setPremium(isPremium)
            }
        }
    }

    val firebaseService: FirebaseService = koinInject()

    // Auth state tracking
    val authState by firebaseService.authState.collectAsState()

    // Sync Firebase auth state to settings
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Authenticated -> {
                // Sync premium entitlement from the server profile (source of truth for account upgrades).
                // This is important because local `USER_SETTINGS` may be stale after reinstall / restore.
                val profile = firebaseService.getUserProfile()
                if (profile != null) {
                    runCatching {
                        settingsRepository.setPremium(profile.isPremium)
                    }.onFailure { }
                }

                val current = settingsRepository.getSettingsSnapshot()
                settingsRepository.updateSettings(
                    current.copy(
                        hasCompletedAuth = true,
                        firebaseUid = state.uid,
                        userEmail = state.email,
                        displayName = state.displayName,
                        photoUrl = state.photoUrl,
                        isEmailVerified = state.isEmailVerified,
                        authProvider = state.providers.firstOrNull() ?: "email"
                    )
                )
            }
            is AuthState.SignedOut -> {
                val current = settingsRepository.getSettingsSnapshot()
                if (current.firebaseUid != null) {
                    // User was signed in, now signed out — clear auth fields
                    settingsRepository.updateSettings(
                        current.copy(
                            hasCompletedAuth = false,
                            authSkipped = false,
                            firebaseUid = null,
                            userEmail = null,
                            displayName = null,
                            photoUrl = null,
                            isEmailVerified = false,
                            authProvider = "none"
                        )
                    )
                }
            }
            else -> { /* Unknown / Anonymous — no sync needed */ }
        }
    }

    // Derive auth display values from authState
    val isSignedIn = authState is AuthState.Authenticated
    val accountEmail = (authState as? AuthState.Authenticated)?.email
    val accountDisplayName = (authState as? AuthState.Authenticated)?.displayName
    val isEmailVerified = (authState as? AuthState.Authenticated)?.isEmailVerified == true
    val authProvider = (authState as? AuthState.Authenticated)?.providers?.firstOrNull() ?: "none"

    val systemDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDarkTheme
    }
    val mainActivity = activity as? MainActivity
    val pendingDeepLink = mainActivity?.pendingDeepLink?.value
    val pendingCalendarOAuth = mainActivity?.pendingCalendarOAuth?.value

    DailyWellTheme(darkTheme = useDarkTheme) {
        if (showPaywall) {
            PaywallWithBilling(
                activity = activity,
                billingManager = billingManager,
                onDismiss = { showPaywall = false },
                onPurchaseSuccess = { showPaywall = false }
            )
        } else {
            MainNavigation(
                hasCompletedOnboarding = settings.hasCompletedOnboarding,
                isPremium = hasFullAccess,
                onOnboardingComplete = { },
                onNavigateToSettings = { },
                onNavigateToPaywall = { showPaywall = true },
                onOpenHealthConnectSettings = {
                    val settingsIntent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
                    runCatching { activity.startActivity(settingsIntent) }.onFailure {
                        activity.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                            )
                        )
                    }
                },
                onInstallHealthConnect = {
                    activity.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                        )
                    )
                },
                onTakeProgressPhoto = onTakeProgressPhoto,
                onShareStreak = shareStreak,
                onShareCalendarTrackerImage = shareCalendarTrackerImage,
                onExportCalendarTrackerPdf = exportCalendarTrackerPdf,
                hasCompletedAuth = settings.hasCompletedAuth,
                authSkipped = settings.authSkipped,
                onAuthComplete = { /* Settings updated by AuthViewModel */ },
                onSignIn = { email, password ->
                    firebaseService.signInWithEmailSimple(email, password)
                },
                onSignUp = { email, password, displayName ->
                    firebaseService.createAccountSimple(email, password, displayName)
                },
                onGoogleSignIn = {
                    firebaseService.signInWithGoogleSimple(activity)
                },
                onForgotPassword = { email ->
                    firebaseService.sendPasswordResetEmail(email)
                },
                onSignOut = {
                    firebaseService.signOut()
                },
                onDeleteAccount = {
                    firebaseService.deleteAccount()
                },
                onChangePassword = { currentPassword, newPassword ->
                    firebaseService.reauthenticateAndChangePassword(currentPassword, newPassword)
                },
                onUpdateDisplayName = { name ->
                    firebaseService.updateDisplayName(name)
                },
                isSignedIn = isSignedIn,
                accountEmail = accountEmail,
                accountDisplayName = accountDisplayName,
                isEmailVerified = isEmailVerified,
                authProvider = authProvider,
                pendingDeepLink = pendingDeepLink,
                onDeepLinkConsumed = { mainActivity?.consumeDeepLink() },
                pendingCalendarOAuth = pendingCalendarOAuth,
                onCalendarOAuthConsumed = { mainActivity?.consumeCalendarOAuth() }
            )
        }
    }
}

@Composable
private fun PaywallWithBilling(
    activity: ComponentActivity,
    billingManager: BillingManager,
    onDismiss: () -> Unit,
    onPurchaseSuccess: () -> Unit
) {
    val viewModel: PaywallViewModel = koinViewModel()
    val purchaseState by billingManager.purchaseState.collectAsState()
    val products by billingManager.products.collectAsState()

    // Update prices from billing
    LaunchedEffect(products) {
        val monthly = products.find { it.productId == BillingManager.PRODUCT_MONTHLY }
        val annual = products.find { it.productId == BillingManager.PRODUCT_ANNUAL }

        val monthlyPrice = monthly?.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$9.99"
        val annualPrice = annual?.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$79.99"

        viewModel.updatePrices(monthlyPrice, annualPrice)
    }

    // Handle purchase state changes
    LaunchedEffect(purchaseState) {
        when (purchaseState) {
            is PurchaseState.Success -> {
                viewModel.onPurchaseSuccess()
                billingManager.resetPurchaseState()
            }
            is PurchaseState.Error -> {
                viewModel.onPurchaseError((purchaseState as PurchaseState.Error).message)
                billingManager.resetPurchaseState()
            }
            is PurchaseState.Cancelled -> {
                viewModel.onPurchaseCancelled()
                billingManager.resetPurchaseState()
            }
            is PurchaseState.NothingToRestore -> {
                viewModel.onRestoreNothingToRestore()
                billingManager.resetPurchaseState()
            }
            is PurchaseState.Loading -> {
                // Already handled in ViewModel
            }
            is PurchaseState.Idle -> {
                // Nothing to do
            }
        }
    }

    PaywallScreen(
        onDismiss = onDismiss,
        onPurchaseSuccess = onPurchaseSuccess,
        onPurchaseProduct = { productId ->
            billingManager.launchPurchaseFlow(activity, productId)
        },
        onRestorePurchases = {
            billingManager.restorePurchases()
        },
        viewModel = viewModel
    )
}

private fun saveBitmapToStorage(
    activity: ComponentActivity,
    bitmap: Bitmap,
    angle: PhotoAngle
): String? {
    return runCatching {
        val photosDir = File(activity.filesDir, "progress_photos").apply { mkdirs() }
        val file = File(
            photosDir,
            "progress_${angle.name.lowercase()}_${System.currentTimeMillis()}.jpg"
        )
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        }
        file.absolutePath
    }.getOrNull()
}

private fun shareTrackerAsImage(
    activity: ComponentActivity,
    payload: TrackerExportPayload
) {
    runCatching {
        val exportsDir = File(activity.cacheDir, "exports").apply { mkdirs() }
        val timestamp = System.currentTimeMillis()
        val imageFile = File(exportsDir, "dailywell_tracker_$timestamp.png")

        val bitmap = renderTrackerBitmap(payload)
        FileOutputStream(imageFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }

        val uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            imageFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, buildTrackerShareText(payload))
            putExtra(Intent.EXTRA_SUBJECT, payload.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(shareIntent, "Share tracker image"))
    }
}

private fun shareTrackerAsPdf(
    activity: ComponentActivity,
    payload: TrackerExportPayload
) {
    runCatching {
        val exportsDir = File(activity.cacheDir, "exports").apply { mkdirs() }
        val timestamp = System.currentTimeMillis()
        val pdfFile = File(exportsDir, "dailywell_tracker_$timestamp.pdf")

        writeTrackerPdf(pdfFile, payload)

        val uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            pdfFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, payload.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(shareIntent, "Share tracker PDF"))
    }
}

private fun renderTrackerBitmap(payload: TrackerExportPayload): Bitmap {
    val lines = buildTrackerSummaryLines(payload)
    val width = 1440
    val headerHeight = 240
    val rowHeight = 44
    val bottomPadding = 120
    val height = (headerHeight + lines.size * rowHeight + bottomPadding).coerceAtLeast(900)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.parseColor("#0F172A"))

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 58f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 34f
    }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E2E8F0")
        textSize = 30f
    }

    canvas.drawText(payload.title, 72f, 92f, titlePaint)
    canvas.drawText(payload.subtitle, 72f, 142f, subtitlePaint)
    canvas.drawText("Generated ${payload.generatedAtIso}", 72f, 188f, subtitlePaint)

    var y = 260f
    lines.forEach { line ->
        canvas.drawText(line, 72f, y, linePaint)
        y += rowHeight.toFloat()
    }

    return bitmap
}

private fun writeTrackerPdf(file: File, payload: TrackerExportPayload) {
    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(1240, 1754, 1).create()
    val page = pdf.startPage(pageInfo)
    val canvas = page.canvas

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 30f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 16f
    }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 14f
    }

    canvas.drawText(payload.title, 56f, 68f, titlePaint)
    canvas.drawText(payload.subtitle, 56f, 98f, subtitlePaint)
    canvas.drawText("Generated ${payload.generatedAtIso}", 56f, 126f, subtitlePaint)

    var y = 166f
    buildTrackerSummaryLines(payload).forEach { line ->
        if (y > 1680f) return@forEach
        canvas.drawText(line, 56f, y, linePaint)
        y += 22f
    }

    pdf.finishPage(page)
    FileOutputStream(file).use { output -> pdf.writeTo(output) }
    pdf.close()
}

private fun buildTrackerSummaryLines(payload: TrackerExportPayload): List<String> {
    val header = when (payload.mode) {
        TrackerExportMode.MONTHLY -> "Mode: Monthly matrix (${payload.dates.size} days)"
        TrackerExportMode.WEEKLY -> "Mode: Weekly matrix (${payload.dates.size} days)"
    }
    val datePreview = if (payload.dates.size <= 8) {
        payload.dates.joinToString(", ")
    } else {
        payload.dates.take(8).joinToString(", ") + " ..."
    }

    val rows = payload.rows.map { row ->
        if (payload.mode == TrackerExportMode.MONTHLY) {
            val weekly = row.weeklyCompleted.zip(row.weeklyTargets).joinToString(" ") { (done, target) ->
                if (target <= 0) "--" else "$done/$target"
            }
            val totalDone = row.completionsByDate.values.count { it }
            "${row.habitEmoji} ${row.habitName}: total $totalDone | weekly $weekly | run ${row.longestRun}d"
        } else {
            val totalDone = row.completionsByDate.values.count { it }
            val totalTarget = row.completionsByDate.size
            "${row.habitEmoji} ${row.habitName}: $totalDone/$totalTarget"
        }
    }

    return buildList {
        add(header)
        add("Dates: $datePreview")
        add("Habits: ${payload.rows.size}")
        add("")
        addAll(rows)
    }
}

private fun buildTrackerShareText(payload: TrackerExportPayload): String {
    val modeLabel = if (payload.mode == TrackerExportMode.MONTHLY) "monthly" else "weekly"
    return "My DailyWell $modeLabel tracker snapshot (${payload.subtitle})."
}
