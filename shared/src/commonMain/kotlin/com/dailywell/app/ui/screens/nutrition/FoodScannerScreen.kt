package com.dailywell.app.ui.screens.nutrition

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.dailywell.app.data.model.*
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.StaggeredItem
import com.dailywell.app.ui.components.pressScale
import com.dailywell.app.ui.components.DailyWellSprings
import com.dailywell.app.ui.components.rememberBreathingScale
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.GlassCard
import com.dailywell.app.ui.components.ElevationLevel
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import com.dailywell.app.core.theme.LocalDailyWellColors
import com.dailywell.app.ui.screens.scan.FoodScanViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import androidx.compose.foundation.BorderStroke
import kotlin.math.roundToInt

/**
 * AI Food Scanner - Camera interface for scanning meals
 * FULLY WIRED with Claude Vision API
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun FoodScannerScreen(
    userId: String,
    nutritionRepository: com.dailywell.app.data.repository.NutritionRepository,
    aiCoachingRepository: com.dailywell.app.data.repository.AICoachingRepository? = null,
    foodScanViewModel: FoodScanViewModel? = null,
    onBack: () -> Unit = {},
    onMealLogged: () -> Unit = {}
) {
    var scanState by remember { mutableStateOf(ScanState.READY) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<FoodScanResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var capturedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var analysisJob by remember { mutableStateOf<Job?>(null) }
    var analysisStatusJob by remember { mutableStateOf<Job?>(null) }
    var analysisStatusIndex by remember { mutableStateOf(0) }
    var analysisProgress by remember { mutableStateOf(0f) }
    var activeAnalysisSteps by remember { mutableStateOf(ANALYSIS_STEPS) }
    var transientStatusMessage by remember { mutableStateOf<String?>(null) }
    var retryScanAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var scanRecoveryNotice by remember { mutableStateOf<String?>(null) }
    var todayNutrition by remember { mutableStateOf<DailyNutrition?>(null) }
    var recentScans by remember { mutableStateOf<List<RecentScanUiItem>>(emptyList()) }
    var latestScanSource by remember { mutableStateOf(ScanResultSource.AI_PHOTO) }

    val barcodeUiState = foodScanViewModel?.uiState?.collectAsState()
    val barcodeState = barcodeUiState?.value
    val isBarcodeLookupRunning = barcodeState?.isLoading == true
    val scope = rememberCoroutineScope()

    fun appendRecentScan(item: RecentScanUiItem) {
        recentScans = (listOf(item) + recentScans)
            .distinctBy { it.id }
            .sortedByDescending { it.timestampEpochMs }
            .take(8)
    }

    fun resetAnalysisState() {
        analysisStatusJob?.cancel()
        analysisStatusJob = null
        analysisStatusIndex = 0
        analysisProgress = 0f
        activeAnalysisSteps = ANALYSIS_STEPS
        transientStatusMessage = null
        isAnalyzing = false
        analysisJob = null
    }

    suspend fun runScanWithRetry(
        imageBytes: ByteArray,
        mealType: MealType?,
        scanMode: ScanInputMode
    ): Result<FoodScanResult> {
        transientStatusMessage = null
        suspend fun attempt(timeoutMs: Long): Result<FoodScanResult> {
            return withTimeout(timeoutMs) {
                nutritionRepository.scanFoodPhoto(
                    userId = userId,
                    imageBytes = imageBytes,
                    mealType = mealType,
                    photoPath = "food_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}.jpg"
                )
            }
        }

        val first = attempt(timeoutMs = 45_000L)
        if (first.isSuccess) return first

        val message = first.exceptionOrNull()?.message?.lowercase().orEmpty()
        val shouldRetry = "timeout" in message || "network" in message || "connection" in message
        if (!shouldRetry) return first

        transientStatusMessage = when (scanMode) {
            ScanInputMode.BARCODE -> "Retrying..."
            ScanInputMode.FOOD_LABEL -> "Label read failed once. Retrying quickly..."
            ScanInputMode.LIBRARY -> "Upload was interrupted. Retrying..."
            else -> "Connection unstable. Retrying scan..."
        }
        delay(700L)
        return try {
            attempt(timeoutMs = 35_000L)
        } finally {
            transientStatusMessage = null
        }
    }

    suspend fun persistExternalSummary(scan: FoodScanResult, source: String) {
        nutritionRepository.saveExternalScanSummary(
            FoodScanSummary(
                id = scan.id,
                userId = userId,
                mealSuggestion = scan.mealSuggestion,
                totalCalories = scan.totalCalories,
                confidence = scan.confidence,
                foodCount = scan.recognizedFoods.size,
                photoPath = scan.photoPath,
                timestamp = scan.timestamp.toString(),
                source = source
            )
        )
    }

    fun startScanAnalysis(request: PendingScanRequest) {
        if (analysisJob?.isActive == true) return
        if (request.inputMode == ScanInputMode.BARCODE) return

        retryScanAction = null
        errorMessage = null
        scanRecoveryNotice = null
        capturedImageBytes = request.imageBytes
        isAnalyzing = true

        val steps = when (request.inputMode) {
            ScanInputMode.FOOD_LABEL -> LABEL_ANALYSIS_STEPS
            ScanInputMode.LIBRARY -> LIBRARY_ANALYSIS_STEPS
            else -> ANALYSIS_STEPS
        }
        activeAnalysisSteps = steps
        analysisStatusIndex = 0
        analysisProgress = steps.firstOrNull()?.progress ?: 0f

        analysisStatusJob?.cancel()
        analysisStatusJob = scope.launch {
            steps.drop(1).forEachIndexed { index, step ->
                delay(if (index == 0) 500L else 850L)
                analysisStatusIndex = index + 1
                analysisProgress = step.progress
            }
        }

        analysisJob = scope.launch {
            try {
                val useLocalLabel = request.inputMode == ScanInputMode.FOOD_LABEL &&
                    request.labelOcrDraft?.isCompleteForLocalLog() == true

                val scan = if (useLocalLabel) {
                    request.labelOcrDraft!!.toFoodScanResult(
                        imagePath = "label_local_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}.jpg"
                    )
                } else {
                    runScanWithRetry(
                        imageBytes = request.imageBytes,
                        mealType = request.mealType,
                        scanMode = request.inputMode
                    ).getOrElse { throw it }
                }

                latestScanSource = when {
                    useLocalLabel -> ScanResultSource.OCR_LABEL
                    request.inputMode == ScanInputMode.FOOD_LABEL -> ScanResultSource.AI_LABEL
                    request.inputMode == ScanInputMode.LIBRARY -> ScanResultSource.AI_LIBRARY
                    else -> ScanResultSource.AI_PHOTO
                }

                val normalized = normalizeScanResult(scan)
                if (normalized.wasRecovered) {
                    scanRecoveryNotice = "Scan had low clarity, so we filled safe estimates. You can adjust before saving."
                }
                val finalScan = normalized.result

                persistExternalSummary(
                    scan = finalScan,
                    source = latestScanSource.persistenceKey()
                )
                aiCoachingRepository?.storePendingScanHandoff(
                    buildScanToCoachHandoff(finalScan, latestScanSource)
                )

                analysisStatusIndex = steps.lastIndex.coerceAtLeast(0)
                analysisProgress = 1f
                delay(180L)
                scanResult = finalScan
                appendRecentScan(
                    RecentScanUiItem(
                        id = finalScan.id,
                        title = finalScan.mealSuggestion,
                        calories = finalScan.totalCalories,
                        confidencePercent = (finalScan.confidence * 100f).roundToInt().coerceIn(0, 100),
                        source = latestScanSource.displayLabel(),
                        timestampEpochMs = finalScan.timestamp.toEpochMilliseconds()
                    )
                )
                retryScanAction = null
                scanState = ScanState.RESULTS
            } catch (_: TimeoutCancellationException) {
                retryScanAction = { startScanAnalysis(request) }
                errorMessage = "Analysis timed out. Tap retry."
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                retryScanAction = { startScanAnalysis(request) }
                errorMessage = e.message ?: "Failed to analyze image. Tap retry."
            } finally {
                resetAnalysisState()
            }
        }
    }

    LaunchedEffect(userId) {
        todayNutrition = nutritionRepository.getTodayNutrition(userId)
        recentScans = nutritionRepository.getRecentScanSummaries(userId, limit = 8).map { summary ->
            RecentScanUiItem(
                id = summary.id,
                title = summary.mealSuggestion,
                calories = summary.totalCalories,
                confidencePercent = (summary.confidence * 100f).roundToInt().coerceIn(0, 100),
                source = scanSourceFromPersistence(summary.source).displayLabel(),
                timestampEpochMs = parseInstantMillis(summary.timestamp)
            )
        }
    }

    LaunchedEffect(barcodeState?.showResults, barcodeState?.scannedFood?.id) {
        val scannedFood = barcodeState?.scannedFood ?: return@LaunchedEffect
        if (barcodeState.showResults) {
            val converted = scannedFood.toFoodScanResult()
            scanResult = converted
            capturedImageBytes = null
            scanState = ScanState.RESULTS
            errorMessage = null
            scanRecoveryNotice = null
            latestScanSource = ScanResultSource.BARCODE
            retryScanAction = null
            resetAnalysisState()
            persistExternalSummary(converted, source = ScanResultSource.BARCODE.persistenceKey())
            aiCoachingRepository?.storePendingScanHandoff(
                buildScanToCoachHandoff(converted, ScanResultSource.BARCODE)
            )
            appendRecentScan(scannedFood.toRecentScanUiItem(scanSource = ScanResultSource.BARCODE))
            foodScanViewModel?.closeResults()
        }
    }

    LaunchedEffect(barcodeState?.error) {
        barcodeState?.error?.let {
            retryScanAction = null
            errorMessage = it
        }
    }

    GlassScreenWrapper {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = scanState,
            transitionSpec = {
                when {
                    // Analyzing slides up and fades in over the camera
                    targetState == ScanState.ANALYZING -> {
                        (fadeIn(animationSpec = tween(280, easing = EaseOutCubic)) +
                            slideInVertically(
                                animationSpec = tween(280, easing = EaseOutCubic),
                                initialOffsetY = { it / 6 }
                            )).togetherWith(
                            fadeOut(animationSpec = tween(220, easing = EaseInCubic))
                        )
                    }
                    // Results slide up from below
                    targetState == ScanState.RESULTS -> {
                        (fadeIn(animationSpec = tween(340, easing = EaseOutCubic)) +
                            slideInVertically(
                                animationSpec = tween(340, easing = EaseOutCubic),
                                initialOffsetY = { it / 4 }
                            )).togetherWith(
                            fadeOut(animationSpec = tween(220))
                        )
                    }
                    // Going back to camera fades in quickly
                    else -> {
                        (fadeIn(animationSpec = tween(220))).togetherWith(
                            fadeOut(animationSpec = tween(220)) +
                                slideOutVertically(
                                    animationSpec = tween(220, easing = EaseInCubic),
                                    targetOffsetY = { it / 6 }
                                )
                        )
                    }
                }.using(SizeTransform(clip = false))
            },
            label = "scanStateTransition"
        ) { state ->
            when (state) {
                ScanState.READY -> Box(modifier = Modifier.fillMaxSize()) {
                    CameraView(
                        onBack = onBack,
                        onCapture = { imageBytes, mealType, inputMode, labelOcrDraft ->
                            startScanAnalysis(
                                PendingScanRequest(
                                    imageBytes = imageBytes,
                                    mealType = mealType,
                                    inputMode = inputMode,
                                    labelOcrDraft = labelOcrDraft
                                )
                            )
                        },
                        onBarcodeDetected = { barcode ->
                            if (foodScanViewModel == null) {
                                retryScanAction = null
                                errorMessage = "Barcode scanner is not available on this device build."
                                return@CameraView
                            }
                            if (barcode.length < 8) return@CameraView
                            foodScanViewModel.onBarcodeDetected(barcode)
                        }
                    )

                    if (recentScans.isNotEmpty() && !isAnalyzing && !isBarcodeLookupRunning) {
                        RecentScansCard(
                            scans = recentScans.take(3),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 16.dp, vertical = 112.dp)
                        )
                    }

                    if (isAnalyzing || isBarcodeLookupRunning) {
                        val steps = activeAnalysisSteps.ifEmpty { ANALYSIS_STEPS }
                        val activeStatus = if (isBarcodeLookupRunning) {
                            BARCODE_LOOKUP_STEP
                        } else {
                            steps[analysisStatusIndex.coerceIn(0, steps.lastIndex.coerceAtLeast(0))]
                        }
                        val liveHint = if (isBarcodeLookupRunning) {
                            barcodeState?.lastBarcode?.takeIf { it.isNotBlank() }?.let { code ->
                                "Barcode: $code"
                            }
                        } else {
                            transientStatusMessage
                        }
                        InlineAnalyzingStatus(
                            title = activeStatus.title,
                            detail = activeStatus.detail,
                            progress = if (isBarcodeLookupRunning) BARCODE_LOOKUP_STEP.progress else analysisProgress,
                            hint = liveHint,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 24.dp, vertical = 172.dp)
                        )
                    }
                }

                ScanState.ANALYZING -> AnalyzingView()

                ScanState.RESULTS -> scanResult?.let { result ->
                    ResultsView(
                        result = result,
                        scanSource = latestScanSource,
                        recoveryNotice = scanRecoveryNotice,
                        todayNutrition = todayNutrition,
                        capturedImageBytes = capturedImageBytes,
                        onLogMeal = { adjustedResult, mealType, emotion, hungerLevel, notes ->
                            scope.launch {
                                nutritionRepository.logMeal(
                                    userId = userId,
                                    scanResult = adjustedResult,
                                    mealType = mealType,
                                    emotion = emotion,
                                    hungerLevel = hungerLevel,
                                    notes = notes
                                ).onSuccess {
                                    todayNutrition = nutritionRepository.getTodayNutrition(userId)
                                    onMealLogged()
                                    onBack()
                                }.onFailure { error ->
                                    retryScanAction = null
                                    errorMessage = error.message ?: "Failed to log meal"
                                }
                            }
                        },
                        onRetake = {
                            scanState = ScanState.READY
                            isAnalyzing = false
                            scanResult = null
                            capturedImageBytes = null
                            latestScanSource = ScanResultSource.AI_PHOTO
                            retryScanAction = null
                            transientStatusMessage = null
                            scanRecoveryNotice = null
                            resetAnalysisState()
                            foodScanViewModel?.closeResults()
                        }
                    )
                }
            }
        }

        // Error snackbar
        errorMessage?.let { error ->
            val retryAction = retryScanAction
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(
                        onClick = {
                            if (retryAction != null) {
                                errorMessage = null
                                retryScanAction = null
                                retryAction()
                            } else {
                                errorMessage = null
                            }
                        }
                    ) {
                        Text(if (retryAction != null) "Retry" else "Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
    } // GlassScreenWrapper
}

enum class ScanState {
    READY,
    ANALYZING,
    RESULTS
}

enum class ScanInputMode {
    FOOD_CAMERA,
    BARCODE,
    FOOD_LABEL,
    LIBRARY
}

enum class ScanResultSource(
    val title: String,
    val subtitle: String
) {
    AI_PHOTO(
        title = "AI photo analysis",
        subtitle = "Cloud vision estimate from meal photo"
    ),
    AI_LABEL(
        title = "AI label analysis",
        subtitle = "Enhanced cloud read for harder label photos"
    ),
    OCR_LABEL(
        title = "On-device label OCR",
        subtitle = "Parsed locally on your device without cloud cost"
    ),
    BARCODE(
        title = "Barcode database match",
        subtitle = "Free product lookup from barcode index"
    ),
    AI_LIBRARY(
        title = "AI library analysis",
        subtitle = "Cloud analysis of imported gallery image"
    )
}

private fun ScanResultSource.displayLabel(): String = when (this) {
    ScanResultSource.BARCODE -> "Barcode"
    ScanResultSource.OCR_LABEL -> "Label OCR"
    ScanResultSource.AI_LABEL -> "Label AI"
    ScanResultSource.AI_LIBRARY -> "Library AI"
    ScanResultSource.AI_PHOTO -> "Photo AI"
}

private fun ScanResultSource.persistenceKey(): String = when (this) {
    ScanResultSource.BARCODE -> "barcode"
    ScanResultSource.OCR_LABEL -> "label_ocr"
    ScanResultSource.AI_LABEL -> "label_ai"
    ScanResultSource.AI_LIBRARY -> "library_ai"
    ScanResultSource.AI_PHOTO -> "photo_ai"
}

private fun scanSourceFromPersistence(source: String): ScanResultSource {
    return when (source.lowercase()) {
        "barcode" -> ScanResultSource.BARCODE
        "label", "label_ocr" -> ScanResultSource.OCR_LABEL
        "label_ai" -> ScanResultSource.AI_LABEL
        "library", "library_ai" -> ScanResultSource.AI_LIBRARY
        "photo", "photo_ai" -> ScanResultSource.AI_PHOTO
        else -> ScanResultSource.AI_PHOTO
    }
}

data class LabelOcrDraft(
    val productName: String?,
    val servingText: String?,
    val calories: Int?,
    val proteinGrams: Int?,
    val carbsGrams: Int?,
    val fatGrams: Int?,
    val sugarGrams: Int?,
    val fiberGrams: Int?,
    val sodiumMg: Int?,
    val confidence: Float,
    val extractedText: String = ""
)

private data class PendingScanRequest(
    val imageBytes: ByteArray,
    val mealType: MealType?,
    val inputMode: ScanInputMode,
    val labelOcrDraft: LabelOcrDraft?
)

private data class RecentScanUiItem(
    val id: String,
    val title: String,
    val calories: Int,
    val confidencePercent: Int,
    val source: String,
    val timestampEpochMs: Long
)

private fun LabelOcrDraft.isCompleteForLocalLog(): Boolean {
    val keyFields = listOf(calories, proteinGrams, carbsGrams, fatGrams).count { it != null }
    return calories != null && keyFields >= 2 && confidence >= 0.52f
}

private fun LabelOcrDraft.toFoodScanResult(imagePath: String): FoodScanResult {
    val protein = proteinGrams ?: 0
    val carbs = carbsGrams ?: 0
    val fat = fatGrams ?: 0
    val calculatedCalories = ((protein * 4) + (carbs * 4) + (fat * 9)).coerceAtLeast(0)
    val finalCalories = calories ?: calculatedCalories
    val suggestion = productName?.takeIf { it.isNotBlank() } ?: "Scanned label meal"
    val serving = servingText?.takeIf { it.isNotBlank() } ?: "1 serving"

    return FoodScanResult(
        id = "label_local_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}",
        photoPath = imagePath,
        recognizedFoods = listOf(
            FoodItem(
                id = "label_item_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}",
                name = suggestion,
                serving = serving,
                quantity = 1f,
                calories = finalCalories,
                macros = MacroNutrients(
                    protein = protein,
                    carbs = carbs,
                    fat = fat
                ),
                micronutrients = MicroNutrients(
                    fiber = fiberGrams ?: 0,
                    sugar = sugarGrams ?: 0,
                    sodium = sodiumMg ?: 0
                )
            )
        ),
        totalCalories = finalCalories,
        totalMacros = MacroNutrients(
            protein = protein,
            carbs = carbs,
            fat = fat
        ),
        confidence = confidence.coerceIn(0.4f, 0.98f),
        mealSuggestion = suggestion,
        timestamp = kotlinx.datetime.Clock.System.now()
    )
}

private data class AnalysisStep(
    val title: String,
    val detail: String,
    val progress: Float
)

private val ANALYSIS_STEPS = listOf(
    AnalysisStep(
        title = "Uploading photo",
        detail = "Sending your image securely for nutrition analysis.",
        progress = 0.12f
    ),
    AnalysisStep(
        title = "Detecting foods",
        detail = "Identifying each visible ingredient on your plate.",
        progress = 0.34f
    ),
    AnalysisStep(
        title = "Estimating portions",
        detail = "Calculating serving size and meal density.",
        progress = 0.56f
    ),
    AnalysisStep(
        title = "Calculating nutrition",
        detail = "Estimating calories, macros, and micronutrients.",
        progress = 0.78f
    ),
    AnalysisStep(
        title = "Building recommendations",
        detail = "Preparing your best next action before saving.",
        progress = 0.92f
    )
)

private val LABEL_ANALYSIS_STEPS = listOf(
    AnalysisStep(
        title = "Reading label",
        detail = "Extracting nutrition text from your package photo.",
        progress = 0.16f
    ),
    AnalysisStep(
        title = "Matching nutrition fields",
        detail = "Locating calories, serving size, and macros.",
        progress = 0.42f
    ),
    AnalysisStep(
        title = "Building food entry",
        detail = "Converting label data into a clean meal summary.",
        progress = 0.76f
    ),
    AnalysisStep(
        title = "Finishing",
        detail = "Preparing next-step recommendations.",
        progress = 0.93f
    )
)

private val LIBRARY_ANALYSIS_STEPS = listOf(
    AnalysisStep(
        title = "Importing image",
        detail = "Preparing your gallery photo for analysis.",
        progress = 0.14f
    ),
    AnalysisStep(
        title = "Detecting foods",
        detail = "Analyzing visible ingredients from the uploaded image.",
        progress = 0.46f
    ),
    AnalysisStep(
        title = "Estimating nutrition",
        detail = "Calculating calories and macros from the detected meal.",
        progress = 0.78f
    ),
    AnalysisStep(
        title = "Preparing your summary",
        detail = "Building your ready-to-save result card.",
        progress = 0.93f
    )
)

private val BARCODE_LOOKUP_STEP = AnalysisStep(
    title = "Looking up barcode",
    detail = "Matching product details from the nutrition database.",
    progress = 0.58f
)

private fun parseInstantMillis(value: String): Long {
    return kotlin.runCatching { kotlinx.datetime.Instant.parse(value).toEpochMilliseconds() }
        .getOrElse { kotlinx.datetime.Clock.System.now().toEpochMilliseconds() }
}

private fun buildScanToCoachHandoff(
    scan: FoodScanResult,
    source: ScanResultSource
): ScanToCoachHandoff {
    return ScanToCoachHandoff(
        mealName = scan.mealSuggestion.ifBlank { "Scanned meal" },
        calories = scan.totalCalories.coerceAtLeast(0),
        protein = scan.totalMacros.protein.coerceAtLeast(0),
        carbs = scan.totalMacros.carbs.coerceAtLeast(0),
        fat = scan.totalMacros.fat.coerceAtLeast(0),
        confidencePercent = (scan.confidence * 100f).toInt().coerceIn(0, 100),
        source = source.displayLabel(),
        capturedAt = scan.timestamp.toString()
    )
}

private data class NormalizedScanOutcome(
    val result: FoodScanResult,
    val wasRecovered: Boolean
)

private fun normalizeScanResult(raw: FoodScanResult): NormalizedScanOutcome {
    var recovered = false

    val sanitizedFoods = raw.recognizedFoods.mapIndexed { index, food ->
        val safeName = food.name.trim().takeIf { it.isNotBlank() } ?: run {
            recovered = true
            if (index == 0) "Unidentified meal item" else "Unidentified item ${index + 1}"
        }
        val safeServing = food.serving.trim().takeIf { it.isNotBlank() } ?: run {
            recovered = true
            "1 serving (est.)"
        }
        val safeProtein = food.macros.protein.coerceAtLeast(0)
        val safeCarbs = food.macros.carbs.coerceAtLeast(0)
        val safeFat = food.macros.fat.coerceAtLeast(0)
        val macroCalories = (safeProtein * 4) + (safeCarbs * 4) + (safeFat * 9)
        val safeCalories = when {
            food.calories > 0 -> food.calories
            macroCalories > 0 -> {
                recovered = true
                macroCalories
            }
            else -> {
                recovered = true
                120
            }
        }

        food.copy(
            name = safeName,
            serving = safeServing,
            calories = safeCalories,
            macros = MacroNutrients(
                protein = safeProtein,
                carbs = safeCarbs,
                fat = safeFat
            ),
            weightGrams = food.weightGrams?.takeIf { it > 0f }
        )
    }

    val finalFoods = if (sanitizedFoods.isNotEmpty()) {
        sanitizedFoods
    } else {
        recovered = true
        listOf(
            buildRecoveryFallbackFood(
                mealSuggestion = raw.mealSuggestion,
                calorieHint = raw.totalCalories.takeIf { it > 0 },
                macroHint = raw.totalMacros
            )
        )
    }

    val totals = calculateMealTotals(finalFoods)
    val mealName = raw.mealSuggestion.trim().takeIf { it.isNotBlank() } ?: run {
        recovered = true
        finalFoods.firstOrNull()?.name ?: "Unidentified meal"
    }
    val confidence = if (recovered) {
        raw.confidence.coerceIn(0.34f, 0.58f)
    } else {
        raw.confidence.coerceIn(0.45f, 0.98f)
    }

    return NormalizedScanOutcome(
        result = raw.copy(
            recognizedFoods = finalFoods,
            totalCalories = totals.totalCalories,
            totalMacros = totals.totalMacros,
            confidence = confidence,
            mealSuggestion = mealName
        ),
        wasRecovered = recovered
    )
}

private fun buildRecoveryFallbackFood(
    mealSuggestion: String,
    calorieHint: Int?,
    macroHint: MacroNutrients
): FoodItem {
    val calories = calorieHint
        ?: macroHint.totalCalories.takeIf { it > 0 }
        ?: 260

    val protein = macroHint.protein.takeIf { it > 0 } ?: (calories * 20 / 100 / 4).coerceAtLeast(8)
    val fat = macroHint.fat.takeIf { it > 0 } ?: (calories * 30 / 100 / 9).coerceAtLeast(6)
    val carbs = macroHint.carbs.takeIf { it > 0 }
        ?: ((calories - (protein * 4) - (fat * 9)) / 4).coerceAtLeast(10)

    return FoodItem(
        id = "recovery_food_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}",
        name = mealSuggestion.takeIf { it.isNotBlank() } ?: "Unidentified meal item",
        serving = "1 serving (est.)",
        quantity = 1f,
        calories = calories,
        macros = MacroNutrients(
            protein = protein,
            carbs = carbs,
            fat = fat
        ),
        weightGrams = null,
        calorieType = null
    )
}

private fun ScannedFood.toFoodScanResult(): FoodScanResult {
    val isBarcodeMatch = scanSource.equals("barcode", ignoreCase = true)
    val foodItem = FoodItem(
        id = if (barcode.isNotBlank()) barcode else id,
        name = name.ifBlank { "Scanned item" },
        serving = quantity.ifBlank { nutrients.servingSize.ifBlank { "1 serving" } },
        quantity = 1f,
        calories = nutrients.calories,
        macros = MacroNutrients(
            protein = nutrients.protein.roundToInt(),
            carbs = nutrients.carbohydrates.roundToInt(),
            fat = nutrients.fat.roundToInt()
        ),
        micronutrients = MicroNutrients(
            fiber = nutrients.fiber.roundToInt(),
            sugar = nutrients.sugars.roundToInt(),
            sodium = nutrients.sodium.roundToInt()
        ),
        weightGrams = null,
        calorieType = null
    )

    return FoodScanResult(
        id = "barcode_${if (barcode.isNotBlank()) barcode else this.id}_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}",
        photoPath = imageUrl,
        recognizedFoods = listOf(foodItem),
        totalCalories = nutrients.calories,
        totalMacros = foodItem.macros,
        confidence = if (isBarcodeMatch) 0.95f else 0.8f,
        mealSuggestion = if (brand.isNotBlank()) "$brand $name" else name,
        timestamp = kotlinx.datetime.Instant.fromEpochMilliseconds(
            scannedAt.takeIf { it > 0L } ?: kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        )
    )
}

private fun ScannedFood.toRecentScanUiItem(
    scanSource: ScanResultSource = if (this.scanSource.equals("barcode", ignoreCase = true)) {
        ScanResultSource.BARCODE
    } else {
        ScanResultSource.AI_PHOTO
    }
): RecentScanUiItem {
    val isBarcodeSource = scanSource == ScanResultSource.BARCODE
    return RecentScanUiItem(
        id = if (id.isNotBlank()) id else barcode,
        title = if (brand.isNotBlank()) "$brand $name" else name,
        calories = nutrients.calories,
        confidencePercent = if (isBarcodeSource) 95 else 80,
        source = scanSource.displayLabel(),
        timestampEpochMs = scannedAt.takeIf { it > 0L } ?: kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    )
}

@Composable
private fun InlineAnalyzingStatus(
    title: String,
    detail: String,
    progress: Float,
    hint: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 280.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(progress.coerceIn(0f, 1f) * 100f).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            hint?.takeIf { it.isNotBlank() }?.let { statusHint ->
                Text(
                    text = statusHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun RecentScansCard(
    scans: List<RecentScanUiItem>,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        elevation = ElevationLevel.Subtle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PremiumSectionChip(
                text = "Recent uploads",
                icon = DailyWellIcons.Health.FoodScan
            )
            scans.forEachIndexed { index, scan ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = scan.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = "${scan.source} - ${scan.calories} cal - ${scan.confidencePercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatRelativeScanTime(scan.timestampEpochMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (index < scans.lastIndex) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                }
            }
        }
    }
}

private fun formatRelativeScanTime(timestampMillis: Long): String {
    val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    val delta = (now - timestampMillis).coerceAtLeast(0L)
    val minutes = delta / 60_000L
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        else -> "${minutes / (60 * 24)}d ago"
    }
}

/**
 * Platform-specific camera implementation using expect/actual pattern
 * Android: Uses CameraX
 * iOS: Uses AVFoundation
 */
expect @Composable fun CameraView(
    onBack: () -> Unit,
    onCapture: (
        imageBytes: ByteArray,
        mealType: MealType?,
        inputMode: ScanInputMode,
        labelOcrDraft: LabelOcrDraft?
    ) -> Unit,
    onBarcodeDetected: (String) -> Unit
)

@Composable
expect fun CapturedMealImage(
    imageBytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier = Modifier
)

/**
 * DEPRECATED: Fallback UI-only camera view
 * Use platform-specific implementations above
 */
@Composable
private fun CameraViewPlaceholder(
    onBack: () -> Unit,
    onCapture: (
        imageBytes: ByteArray,
        mealType: MealType?,
        inputMode: ScanInputMode,
        labelOcrDraft: LabelOcrDraft?
    ) -> Unit,
    onBarcodeDetected: (String) -> Unit = {}
) {
    var selectedMealType by remember { mutableStateOf<MealType?>(null) }
    var showMealTypePicker by remember { mutableStateOf(false) }

    val capturePhoto = {
        // This is a placeholder for platforms without camera implementation
        val dummyImageBytes = ByteArray(0)
        onCapture(dummyImageBytes, selectedMealType, ScanInputMode.FOOD_CAMERA, null)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera preview would go here
        // In real app: Use CameraX or platform-specific camera
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DailyWellIcons.Health.FoodScan,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White.copy(alpha = 0.5f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Camera Preview",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 18.sp
                )
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    "Back",
                    tint = Color.White
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { /* Flash toggle */ },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Misc.Flash,
                        contentDescription = "Flash",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { /* Gallery */ },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Misc.Gallery,
                        contentDescription = "Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Prompt overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Prompt card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Text(
                    "Explain your meal with few lines",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Capture button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .border(4.dp, Color.White, CircleShape)
                    .clickable { capturePhoto() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnalyzingView() {
    // Quiet loading state without staged status copy.
    val breathingScale = rememberBreathingScale(
        minScale = 0.92f,
        maxScale = 1.08f,
        durationMs = 1200
    )

    GlassScreenWrapper {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                elevation = ElevationLevel.Prominent,
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PremiumSectionChip(
                        text = "Smart Scan",
                        icon = DailyWellIcons.Health.FoodScan
                    )
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(62.dp)
                            .graphicsLayer {
                                scaleX = breathingScale
                                scaleY = breathingScale
                            },
                        strokeWidth = 5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Checking nutrition details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Identifying foods, portions, and macro estimates. This usually takes a few seconds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsView(
    result: FoodScanResult,
    scanSource: ScanResultSource,
    recoveryNotice: String? = null,
    todayNutrition: DailyNutrition?,
    capturedImageBytes: ByteArray?,
    onLogMeal: (result: FoodScanResult, mealType: MealType, emotion: String?, hungerLevel: Int?, notes: String?) -> Unit,
    onRetake: () -> Unit
) {
    var showEmotionPicker by remember { mutableStateOf(false) }
    var selectedEmotion by remember { mutableStateOf<String?>(null) }
    var hungerLevel by remember { mutableStateOf(5) }
    var selectedMealType by remember { mutableStateOf(MealType.LUNCH) }
    var notes by remember { mutableStateOf("") }
    var editableFoods by remember(result.id) { mutableStateOf(result.recognizedFoods) }

    val computedTotals = remember(editableFoods) { calculateMealTotals(editableFoods) }
    val adjustmentCount = remember(editableFoods, result.recognizedFoods) {
        val removedCount = (result.recognizedFoods.size - editableFoods.size).coerceAtLeast(0)
        val changedCount = editableFoods.zip(result.recognizedFoods).count { (edited, original) ->
            edited.calories != original.calories || edited.macros != original.macros
        }
        removedCount + changedCount
    }
    val confidencePercent = remember(result.confidence, adjustmentCount) {
        ((result.confidence * 100f).toInt() - (adjustmentCount * 2)).coerceIn(45, 99)
    }

    val adjustedResult = remember(result, editableFoods, computedTotals, confidencePercent) {
        result.copy(
            recognizedFoods = editableFoods,
            totalCalories = computedTotals.totalCalories,
            totalMacros = computedTotals.totalMacros,
            confidence = confidencePercent / 100f
        )
    }

    // Animated calorie counter
    val animatedCalories by animateIntAsState(
        targetValue = computedTotals.totalCalories,
        animationSpec = tween(
            durationMillis = 1000,
            easing = EaseOutCubic
        ),
        label = "calorieCount"
    )
    val portionLabel = remember(computedTotals.totalCalories) {
        when {
            computedTotals.totalCalories < 350 -> "Light portion"
            computedTotals.totalCalories < 700 -> "Balanced portion"
            else -> "Hearty portion"
        }
    }
    val nextAction = remember(computedTotals.totalCalories, computedTotals.totalMacros) {
        when {
            computedTotals.totalMacros.protein < 20 -> "Add a lean protein source next time."
            computedTotals.totalCalories > 850 -> "Consider splitting this into two servings."
            computedTotals.totalMacros.carbs > computedTotals.totalMacros.protein * 3 ->
                "Pair with protein/fiber for steadier energy."
            else -> "Looks balanced. Log it and keep the streak going."
        }
    }
    val micronutrients = remember(editableFoods) {
        calculateMicronutrientTotals(editableFoods)
    }

    if (showEmotionPicker) {
        EmotionPickerDialog(
            selectedEmotion = selectedEmotion,
            hungerLevel = hungerLevel,
            mealType = selectedMealType,
            onEmotionSelected = { selectedEmotion = it },
            onHungerLevelChange = { hungerLevel = it },
            onMealTypeSelected = { selectedMealType = it },
            onDismiss = { showEmotionPicker = false },
            onConfirm = {
                showEmotionPicker = false
                onLogMeal(
                    adjustedResult,
                    selectedMealType,
                    selectedEmotion,
                    hungerLevel,
                    notes.takeIf { it.isNotEmpty() }
                )
            }
        )
    }

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Meal Analysis",
                subtitle = "Review before saving",
                onNavigationClick = onRetake
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Food image - staggered index 0
            StaggeredItem(index = 0) {
                val photoBytes = capturedImageBytes
                if (photoBytes != null && photoBytes.isNotEmpty()) {
                    CapturedMealImage(
                        imageBytes = photoBytes,
                        contentDescription = "Scanned meal photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = DailyWellIcons.Health.Nutrition,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Food Image",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Snapshot summary - staggered index 1
            StaggeredItem(index = 1) {
                ScanSnapshotCard(
                    mealSuggestion = result.mealSuggestion,
                    calories = animatedCalories,
                    macros = computedTotals.totalMacros,
                    micronutrients = micronutrients,
                    confidencePercent = confidencePercent,
                    detectedItems = editableFoods.size,
                    portionLabel = portionLabel,
                    nextAction = nextAction
                )
            }

            StaggeredItem(index = 2) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    recoveryNotice?.takeIf { it.isNotBlank() }?.let { notice ->
                        ScanRecoveryNoticeCard(message = notice)
                    }
                    ScanSourceCard(scanSource = scanSource)
                }
            }

            StaggeredItem(index = 3) {
                BudgetImpactCard(
                    todayNutrition = todayNutrition,
                    scannedCalories = computedTotals.totalCalories,
                    scannedMacros = computedTotals.totalMacros
                )
            }

            // Fast correction workflow - staggered index 4
            StaggeredItem(index = 4) {
                EditableDetectedFoodsCard(
                    foods = editableFoods,
                    onScaleFood = { foodId, factor ->
                        editableFoods = editableFoods.map { food ->
                            if (food.id == foodId) scaleFoodByFactor(food, factor) else food
                        }
                    },
                    onRemoveFood = { foodId ->
                        editableFoods = editableFoods.filterNot { it.id == foodId }
                    }
                )
            }

            // Color-coded meal balance - staggered index 5
            val colorAnalysis = CalorieDensityHelper.analyzeMeal(editableFoods)
            StaggeredItem(index = 5) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = DailyWellIcons.Misc.Palette,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Meal balance",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                "Portion guidance",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Color breakdown bars with animated progress
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ColorBreakdownItem(
                                label = "Green Foods",
                                percent = colorAnalysis.greenCaloriesPercent,
                                color = Color(0xFF4CAF50),
                                description = "Eat freely"
                            )
                            ColorBreakdownItem(
                                label = "Yellow Foods",
                                percent = colorAnalysis.yellowCaloriesPercent,
                                color = Color(0xFFFFEB3B),
                                description = "Moderate"
                            )
                            ColorBreakdownItem(
                                label = "Red Foods",
                                percent = colorAnalysis.redCaloriesPercent,
                                color = Color(0xFFF44336),
                                description = "Small portions"
                            )
                        }

                        // AI Feedback
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                colorAnalysis.feedback,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Trust notes - staggered index 6
            StaggeredItem(index = 6) {
                ConfidenceAssumptionsCard(
                    overallConfidence = confidencePercent,
                    foods = editableFoods,
                    scanSource = scanSource
                )
            }

            // Ingredients - staggered index 7
            StaggeredItem(index = 7) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = DailyWellIcons.Health.Nutrition,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Ingredients",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                Icons.Default.Edit,
                                "Edit",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        FoodNutritionTable(foods = editableFoods)
                    }
                }
            }

            // Retention loop - staggered index 8
            StaggeredItem(index = 8) {
                MomentumRetentionCard(
                    todayNutrition = todayNutrition,
                    scannedCalories = computedTotals.totalCalories
                )
            }

            Spacer(Modifier.weight(1f))

            // Log meal button (with emotion tracking) - staggered index 9 + pressScale
            StaggeredItem(index = 9) {
                Button(
                    onClick = { showEmotionPicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .pressScale(),
                    enabled = editableFoods.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF536DFE)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = DailyWellIcons.Actions.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (editableFoods.isEmpty()) "Add at least one item" else "Save to Log",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private data class MealTotals(
    val totalCalories: Int,
    val totalMacros: MacroNutrients
)

private fun calculateMealTotals(foods: List<FoodItem>): MealTotals {
    val calories = foods.sumOf { it.calories }.coerceAtLeast(0)
    val macros = MacroNutrients(
        protein = foods.sumOf { it.macros.protein }.coerceAtLeast(0),
        carbs = foods.sumOf { it.macros.carbs }.coerceAtLeast(0),
        fat = foods.sumOf { it.macros.fat }.coerceAtLeast(0)
    )
    return MealTotals(totalCalories = calories, totalMacros = macros)
}

private fun scaleFoodByFactor(food: FoodItem, factor: Float): FoodItem {
    val safeFactor = factor.coerceIn(0.65f, 1.45f)
    val newQuantity = (food.quantity * safeFactor).coerceAtLeast(0.1f)
    val newMicros = food.micronutrients?.let { micro ->
        MicroNutrients(
            fiber = (micro.fiber * safeFactor).roundToInt().coerceAtLeast(0),
            sugar = (micro.sugar * safeFactor).roundToInt().coerceAtLeast(0),
            sodium = (micro.sodium * safeFactor).roundToInt().coerceAtLeast(0),
            cholesterol = (micro.cholesterol * safeFactor).roundToInt().coerceAtLeast(0),
            vitaminC = (micro.vitaminC * safeFactor).roundToInt().coerceAtLeast(0),
            calcium = (micro.calcium * safeFactor).roundToInt().coerceAtLeast(0),
            iron = (micro.iron * safeFactor).roundToInt().coerceAtLeast(0)
        )
    }

    return food.copy(
        quantity = (newQuantity * 10f).roundToInt() / 10f,
        calories = (food.calories * safeFactor).roundToInt().coerceAtLeast(1),
        macros = MacroNutrients(
            protein = (food.macros.protein * safeFactor).roundToInt().coerceAtLeast(0),
            carbs = (food.macros.carbs * safeFactor).roundToInt().coerceAtLeast(0),
            fat = (food.macros.fat * safeFactor).roundToInt().coerceAtLeast(0)
        ),
        micronutrients = newMicros
    )
}

@Composable
private fun EditableDetectedFoodsCard(
    foods: List<FoodItem>,
    onScaleFood: (foodId: String, factor: Float) -> Unit,
    onRemoveFood: (foodId: String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = DailyWellIcons.Actions.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Adjust before save",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "${foods.size} items",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (foods.isEmpty()) {
                Text(
                    text = "No foods left. Retake or keep at least one item to save.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                foods.forEachIndexed { index, food ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text(
                                    text = food.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${food.calories} cal - P${food.macros.protein} C${food.macros.carbs} F${food.macros.fat}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { onScaleFood(food.id, 0.85f) }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Reduce portion")
                                }
                                IconButton(onClick = { onScaleFood(food.id, 1.15f) }) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase portion")
                                }
                                IconButton(onClick = { onRemoveFood(food.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove item",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                    if (index < foods.lastIndex) {
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetImpactCard(
    todayNutrition: DailyNutrition?,
    scannedCalories: Int,
    scannedMacros: MacroNutrients
) {
    val nutrition = todayNutrition
    if (nutrition == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = "Loading today's budget...",
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val projectedCalories = nutrition.caloriesConsumed + scannedCalories
    val projectedRemaining = nutrition.calorieGoal - projectedCalories
    val projectedProteinRemaining = nutrition.macroGoals.protein - (nutrition.macros.protein + scannedMacros.protein)
    val projectedCarbRemaining = nutrition.macroGoals.carbs - (nutrition.macros.carbs + scannedMacros.carbs)
    val projectedFatRemaining = nutrition.macroGoals.fat - (nutrition.macros.fat + scannedMacros.fat)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        elevation = ElevationLevel.Subtle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PremiumSectionChip(
                text = "Today budget impact",
                icon = DailyWellIcons.Health.Calories
            )
            Text(
                text = if (projectedRemaining >= 0) {
                    "$projectedRemaining calories left after save"
                } else {
                    "${-projectedRemaining} calories over today goal"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Remaining macros: P ${projectedProteinRemaining.coerceAtLeast(0)} - C ${projectedCarbRemaining.coerceAtLeast(0)} - F ${projectedFatRemaining.coerceAtLeast(0)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { (projectedCalories.toFloat() / nutrition.calorieGoal.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun ConfidenceAssumptionsCard(
    overallConfidence: Int,
    foods: List<FoodItem>,
    scanSource: ScanResultSource
) {
    val usedCloudCredits = scanSource !in setOf(ScanResultSource.BARCODE, ScanResultSource.OCR_LABEL)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PremiumSectionChip(
                text = "Confidence & assumptions",
                icon = DailyWellIcons.Analytics.Pattern
            )
            Text(
                text = "Overall confidence: $overallConfidence%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (usedCloudCredits) {
                    "Method: ${scanSource.title} (uses cloud AI credits)."
                } else {
                    "Method: ${scanSource.title} (no cloud AI credits used)."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            foods.take(3).forEach { food ->
                val confidence = estimateFoodConfidence(food, overallConfidence)
                Text(
                    text = "- ${food.name}: ${confidence.first}% (${confidence.second})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScanSourceCard(scanSource: ScanResultSource) {
    val usedCloudCredits = scanSource !in setOf(ScanResultSource.BARCODE, ScanResultSource.OCR_LABEL)
    val badgeColor = if (usedCloudCredits) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    val supportText = if (usedCloudCredits) {
        "Cloud AI credits used for this scan."
    } else {
        "No cloud AI credits used for this scan."
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        elevation = ElevationLevel.Subtle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PremiumSectionChip(
                    text = "Scan source",
                    icon = DailyWellIcons.Analytics.Pattern
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = if (usedCloudCredits) "Cloud AI" else "On-device/free",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor
                    )
                }
            }
            Text(
                text = scanSource.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = scanSource.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = supportText,
                style = MaterialTheme.typography.labelMedium,
                color = badgeColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ScanRecoveryNoticeCard(message: String) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        elevation = ElevationLevel.Subtle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = DailyWellIcons.Actions.Edit,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun estimateFoodConfidence(food: FoodItem, overall: Int): Pair<Int, String> {
    var score = overall
    val reasons = mutableListOf<String>()

    if (food.weightGrams == null) {
        score -= 7
        reasons += "portion estimated"
    }
    if (food.macros.protein + food.macros.carbs + food.macros.fat <= 0) {
        score -= 10
        reasons += "macro data limited"
    }
    if (food.serving.contains("piece", ignoreCase = true) || food.serving.contains("cup", ignoreCase = true)) {
        score -= 4
        reasons += "serving inferred"
    }

    return score.coerceIn(35, 99) to if (reasons.isEmpty()) "strong match" else reasons.joinToString(", ")
}

@Composable
private fun MomentumRetentionCard(
    todayNutrition: DailyNutrition?,
    scannedCalories: Int
) {
    val completedMeals = todayNutrition?.meals?.size ?: 0
    val projectedMeals = completedMeals + 1

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        elevation = ElevationLevel.Subtle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PremiumSectionChip(
                text = "Momentum boost",
                icon = DailyWellIcons.Analytics.Streak
            )
            Text(
                text = "This becomes meal #$projectedMeals today (+$scannedCalories cal logged).",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Next move: scan your next meal later to keep your daily logging streak alive.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class MicronutrientTotals(
    val fiber: Int = 0,
    val sugar: Int = 0,
    val sodium: Int = 0,
    val vitaminC: Int = 0,
    val calcium: Int = 0,
    val iron: Int = 0
) {
    val hasData: Boolean
        get() = fiber > 0 || sugar > 0 || sodium > 0 || vitaminC > 0 || calcium > 0 || iron > 0
}

private fun calculateMicronutrientTotals(foods: List<FoodItem>): MicronutrientTotals {
    return foods.fold(MicronutrientTotals()) { acc, item ->
        val micro = item.micronutrients
        MicronutrientTotals(
            fiber = acc.fiber + (micro?.fiber ?: 0),
            sugar = acc.sugar + (micro?.sugar ?: 0),
            sodium = acc.sodium + (micro?.sodium ?: 0),
            vitaminC = acc.vitaminC + (micro?.vitaminC ?: 0),
            calcium = acc.calcium + (micro?.calcium ?: 0),
            iron = acc.iron + (micro?.iron ?: 0)
        )
    }
}

@Composable
private fun ScanSnapshotCard(
    mealSuggestion: String,
    calories: Int,
    macros: MacroNutrients,
    micronutrients: MicronutrientTotals,
    confidencePercent: Int,
    detectedItems: Int,
    portionLabel: String,
    nextAction: String
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        elevation = ElevationLevel.Medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PremiumSectionChip(
                text = "Result overview",
                icon = DailyWellIcons.Health.FoodScan
            )
            Text(
                text = mealSuggestion,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SnapshotMetric(
                    label = "Calories",
                    value = "$calories cal",
                    modifier = Modifier.weight(1f)
                )
                SnapshotMetric(
                    label = "Confidence",
                    value = "$confidencePercent%",
                    modifier = Modifier.weight(1f)
                )
                SnapshotMetric(
                    label = "Portion",
                    value = portionLabel,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "$detectedItems item${if (detectedItems == 1) "" else "s"} detected",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Text(
                text = "Macros",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SnapshotMetric(
                    label = "Protein",
                    value = "${macros.protein}g",
                    modifier = Modifier.weight(1f)
                )
                SnapshotMetric(
                    label = "Carbs",
                    value = "${macros.carbs}g",
                    modifier = Modifier.weight(1f)
                )
                SnapshotMetric(
                    label = "Fat",
                    value = "${macros.fat}g",
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "Micros",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (micronutrients.hasData) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SnapshotMetric("Fiber", "${micronutrients.fiber}g", Modifier.weight(1f))
                    SnapshotMetric("Sugar", "${micronutrients.sugar}g", Modifier.weight(1f))
                    SnapshotMetric("Sodium", "${micronutrients.sodium}mg", Modifier.weight(1f))
                }
            } else {
                Text(
                    text = "Micronutrient details were limited in this scan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Text(
                text = "Next action",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = nextAction,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun NextActionPlanCard(nextAction: String) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        elevation = ElevationLevel.Subtle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PremiumSectionChip(
                text = "Next best action",
                icon = DailyWellIcons.Actions.Check
            )
            Text(
                text = nextAction,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Save this scan now, then compare your next meal for streak momentum.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SnapshotMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MicronutrientsCard(micronutrients: MicronutrientTotals) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DailyWellIcons.Health.Nutrition,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Micronutrients",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!micronutrients.hasData) {
                Text(
                    text = "Micronutrient details were limited in this scan. You can still log and edit manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MicronutrientPill("Fiber", "${micronutrients.fiber}g", Modifier.weight(1f))
                    MicronutrientPill("Sugar", "${micronutrients.sugar}g", Modifier.weight(1f))
                    MicronutrientPill("Sodium", "${micronutrients.sodium}mg", Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MicronutrientPill("Vit C", "${micronutrients.vitaminC}mg", Modifier.weight(1f))
                    MicronutrientPill("Calcium", "${micronutrients.calcium}mg", Modifier.weight(1f))
                    MicronutrientPill("Iron", "${micronutrients.iron}mg", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MicronutrientPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FoodNutritionTable(foods: List<FoodItem>) {
    if (foods.isEmpty()) {
        Text(
            "No food items detected. Try retaking with better lighting.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NutritionTableHeaderCell("Food", 2.0f, TextAlign.Start)
                NutritionTableHeaderCell("Serving", 1.35f, TextAlign.Start)
                NutritionTableHeaderCell("Cal", 0.75f, TextAlign.Center)
                NutritionTableHeaderCell("P", 0.6f, TextAlign.Center)
                NutritionTableHeaderCell("C", 0.6f, TextAlign.Center)
                NutritionTableHeaderCell("F", 0.6f, TextAlign.Center)
            }
        }

        foods.forEachIndexed { index, food ->
            val rowColor = if (index % 2 == 0) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = rowColor
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(2.0f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(calorieDensityColor(food.autoCalorieType))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = food.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = food.serving,
                        modifier = Modifier.weight(1.35f),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    NutritionTableValueCell(food.calories.toString(), 0.75f, Color(0xFFFF6F00))
                    NutritionTableValueCell(food.macros.protein.toString(), 0.6f, Color(0xFF1E88E5))
                    NutritionTableValueCell(food.macros.carbs.toString(), 0.6f, Color(0xFF43A047))
                    NutritionTableValueCell(food.macros.fat.toString(), 0.6f, Color(0xFFE53935))
                }
            }
        }

        Text(
            "P/C/F = grams of Protein, Carbs, Fat",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RowScope.NutritionTableHeaderCell(
    text: String,
    cellWeight: Float,
    textAlign: TextAlign
) {
    Text(
        text = text,
        modifier = Modifier.weight(cellWeight),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign
    )
}

@Composable
private fun RowScope.NutritionTableValueCell(
    text: String,
    cellWeight: Float,
    color: Color
) {
    Text(
        text = text,
        modifier = Modifier.weight(cellWeight),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        textAlign = TextAlign.Center
    )
}

private fun calorieDensityColor(type: CalorieDensityType): Color = when (type) {
    CalorieDensityType.GREEN -> Color(0xFF4CAF50)
    CalorieDensityType.YELLOW -> Color(0xFFFBC02D)
    CalorieDensityType.RED -> Color(0xFFE53935)
}

@Composable
fun MacroResultItem(
    dotColor: Color,
    name: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    // Animated counting for the macro value
    val animatedValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 200,
            easing = EaseOutCubic
        ),
        label = "macroCount_$name"
    )

    Card(
        modifier = modifier.pressScale(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                name,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "$animatedValue",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "g",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Complete meal logging dialog (Noom-inspired)
 * Includes: Meal type, emotion tracking, hunger level
 */
@Composable
fun EmotionPickerDialog(
    selectedEmotion: String?,
    hungerLevel: Int,
    mealType: MealType,
    onEmotionSelected: (String) -> Unit,
    onHungerLevelChange: (Int) -> Unit,
    onMealTypeSelected: (MealType) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Before you log...", fontWeight = FontWeight.Bold)
                Text(
                    "Understanding WHY you eat is key to lasting change",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Meal type selection
                StaggeredItem(index = 0, baseDelay = 100L) {
                    Text(
                        "What type of meal is this?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                StaggeredItem(index = 1, baseDelay = 100L) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MealType.values().take(4).forEach { type ->
                            FilterChip(
                                selected = mealType == type,
                                onClick = { onMealTypeSelected(type) },
                                label = {
                                    Text(
                                        type.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Emotion selection
                StaggeredItem(index = 2, baseDelay = 100L) {
                    Text(
                        "Why are you eating right now?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                data class EmotionOption(val icon: ImageVector, val label: String)
                val emotions = listOf(
                    EmotionOption(DailyWellIcons.Mood.Great, "Physically Hungry"),
                    EmotionOption(DailyWellIcons.Mood.Struggling, "Stressed"),
                    EmotionOption(DailyWellIcons.Mood.Okay, "Bored"),
                    EmotionOption(DailyWellIcons.Mood.Low, "Sad/Emotional"),
                    EmotionOption(DailyWellIcons.Social.Cheer, "Celebrating"),
                    EmotionOption(DailyWellIcons.Social.People, "Social (others eating)"),
                    EmotionOption(DailyWellIcons.Misc.Time, "Habit (always eat now)")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    var staggerRowIndex = 3
                    emotions.chunked(2).forEach { row ->
                        val rowIdx = staggerRowIndex
                        staggerRowIndex++
                        StaggeredItem(index = rowIdx, baseDelay = 100L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { emotion ->
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .pressScale()
                                            .clickable { onEmotionSelected(emotion.label) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selectedEmotion == emotion.label)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (selectedEmotion == emotion.label)
                                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                        else null
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = emotion.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                emotion.label,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                                // Fill empty space if odd number
                                if (row.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Hunger scale
                StaggeredItem(index = 7, baseDelay = 100L) {
                    Column {
                        Text(
                            "How hungry are you? (1-10)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(Modifier.height(8.dp))

                        Slider(
                            value = hungerLevel.toFloat(),
                            onValueChange = { onHungerLevelChange(it.toInt()) },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Starving", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "$hungerLevel",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Stuffed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = selectedEmotion != null,
                modifier = Modifier.pressScale()
            ) {
                Text("Log Meal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip")
            }
        }
    )
}

/**
 * Color breakdown item for Noom-style food analysis
 * Progress bars animate from 0 to their target width
 */
@Composable
fun ColorBreakdownItem(
    label: String,
    percent: Int,
    color: Color,
    description: String
) {
    // Animate progress bar from 0 to target
    val animatedProgress by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 300,
            easing = EaseOutCubic
        ),
        label = "colorBreakdown_$label"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                "$percent%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        // Animated progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.LightGray.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }

        if (percent > 0) {
            Text(
                description,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
