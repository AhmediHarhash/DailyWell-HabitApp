package com.dailywell.app.ui.screens.nutrition

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.dailywell.app.core.theme.LocalDailyWellColors
import com.dailywell.app.data.model.MealType
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.ElevationLevel
import com.dailywell.app.ui.components.GlassCard
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

/**
 * Android implementation of CameraView using CameraX
 * PRODUCTION-READY with full photo capture and compression
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun CameraView(
    onBack: () -> Unit,
    onCapture: (
        imageBytes: ByteArray,
        mealType: MealType?,
        inputMode: ScanInputMode,
        labelOcrDraft: LabelOcrDraft?
    ) -> Unit,
    onBarcodeDetected: (String) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var cameraPermissionRequested by rememberSaveable { mutableStateOf(false) }
    var selectedMealType by remember { mutableStateOf<MealType?>(null) }
    var showMealTypePicker by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var selectedMode by rememberSaveable { mutableStateOf(ScanInputMode.FOOD_CAMERA) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var captureInFlight by remember { mutableStateOf(false) }
    var autoCaptureArmed by rememberSaveable { mutableStateOf(false) }
    var autoCaptureCountdown by remember { mutableStateOf<Int?>(null) }
    var lastDetectedBarcode by remember { mutableStateOf<String?>(null) }
    var lastBarcodeDetectionTs by remember { mutableStateOf(0L) }
    val dailyWellColors = LocalDailyWellColors.current

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream) ?: return@use
                val compressed = compressBitmapToBytes(bitmap, maxSizeKb = 1024)
                onCapture(compressed, selectedMealType, ScanInputMode.LIBRARY, null)
            }
        } catch (_: Exception) {
            // Ignore gallery failures silently; user can retry.
        }
    }

    // Request camera permission if not granted
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionRequested = true
            cameraPermissionState.launchPermissionRequest()
        }
    }

    val deniedStatus = cameraPermissionState.status as? PermissionStatus.Denied
    val isPermanentlyDenied = deniedStatus != null &&
        !deniedStatus.shouldShowRationale &&
        cameraPermissionRequested

    // Capture photo function with compression
    fun capturePhoto() {
        if (captureInFlight) return
        if (selectedMode == ScanInputMode.BARCODE) return
        val capture = imageCapture ?: return

        captureInFlight = true
        autoCaptureArmed = false
        autoCaptureCountdown = null

        capture.takePicture(
            Executors.newSingleThreadExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    var finished = false
                    fun finishCapture() {
                        if (finished) return
                        finished = true
                        ContextCompat.getMainExecutor(context).execute {
                            captureInFlight = false
                        }
                    }

                    try {
                        // Convert ImageProxy to Bitmap
                        val bitmap = imageProxyToBitmap(imageProxy)

                        // Compress to < 1MB
                        val compressedBytes = compressBitmapToBytes(bitmap, maxSizeKb = 1024)

                        val mode = when (selectedMode) {
                            ScanInputMode.FOOD_LABEL -> ScanInputMode.FOOD_LABEL
                            ScanInputMode.LIBRARY -> ScanInputMode.LIBRARY
                            ScanInputMode.BARCODE -> ScanInputMode.BARCODE
                            else -> ScanInputMode.FOOD_CAMERA
                        }

                        if (mode == ScanInputMode.FOOD_LABEL) {
                            extractNutritionFromLabelBitmap(bitmap) { labelDraft ->
                                onCapture(
                                    compressedBytes,
                                    selectedMealType,
                                    mode,
                                    labelDraft
                                )
                                finishCapture()
                            }
                        } else {
                            onCapture(
                                compressedBytes,
                                selectedMealType,
                                mode,
                                null
                            )
                            finishCapture()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        finishCapture()
                    } finally {
                        imageProxy.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    ContextCompat.getMainExecutor(context).execute {
                        captureInFlight = false
                    }
                    exception.printStackTrace()
                }
            }
        )
    }

    LaunchedEffect(autoCaptureArmed, selectedMode) {
        if (!autoCaptureArmed) return@LaunchedEffect
        if (selectedMode == ScanInputMode.BARCODE || selectedMode == ScanInputMode.LIBRARY) {
            autoCaptureArmed = false
            autoCaptureCountdown = null
            return@LaunchedEffect
        }
        if (imageCapture == null || captureInFlight) {
            autoCaptureArmed = false
            autoCaptureCountdown = null
            return@LaunchedEffect
        }

        for (second in 3 downTo 1) {
            autoCaptureCountdown = second
            delay(700L)
        }

        autoCaptureCountdown = null
        capturePhoto()
    }

    if (!cameraPermissionState.status.isGranted) {
        // Camera permission request UI
        CameraPermissionRequest(
            onBack = onBack,
            onRequestPermission = {
                cameraPermissionRequested = true
                cameraPermissionState.launchPermissionRequest()
            },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            },
            showOpenSettings = isPermanentlyDenied
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        // Preview use case
                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                        // Image capture use case
                        val imageCaptureUseCase = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .setTargetRotation(previewView.display.rotation)
                            .setFlashMode(
                                if (flashEnabled) ImageCapture.FLASH_MODE_ON
                                else ImageCapture.FLASH_MODE_OFF
                            )
                            .build()

                        imageCapture = imageCaptureUseCase

                        // Select back camera
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            val useCases = mutableListOf<UseCase>(preview, imageCaptureUseCase)

                            if (selectedMode == ScanInputMode.BARCODE) {
                                val barcodeAnalyzer = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analysis ->
                                        analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                            processBarcodeImage(imageProxy) { barcode ->
                                                val now = System.currentTimeMillis()
                                                val shouldEmit = barcode != lastDetectedBarcode ||
                                                    (now - lastBarcodeDetectionTs) > 1500L
                                                if (shouldEmit) {
                                                    lastDetectedBarcode = barcode
                                                    lastBarcodeDetectionTs = now
                                                    ContextCompat.getMainExecutor(context).execute {
                                                        onBarcodeDetected(barcode)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                useCases += barcodeAnalyzer
                            }

                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                *useCases.toTypedArray()
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )

            CameraFramingGuide(
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(
                        if (selectedMode == ScanInputMode.BARCODE) {
                            Modifier.size(width = 300.dp, height = 180.dp)
                        } else {
                            Modifier.size(250.dp)
                        }
                    )
            )

            autoCaptureCountdown?.let { secondsLeft ->
                GlassCard(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 110.dp),
                    cornerRadius = 18.dp,
                    elevation = ElevationLevel.Subtle
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$secondsLeft",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Auto capture",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Camera scrims improve readability while keeping live preview visible.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.64f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.76f)
                            )
                        )
                    )
            )

            // Top bar with controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back button
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = dailyWellColors.glassSheet.copy(alpha = 0.72f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Nav.Back,
                        "Back",
                        tint = Color.White
                    )
                }

                // Flash toggle
                IconButton(
                    onClick = { flashEnabled = !flashEnabled },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (flashEnabled) {
                            dailyWellColors.timeAccent.copy(alpha = 0.30f)
                        } else {
                            dailyWellColors.glassSheet.copy(alpha = 0.72f)
                        },
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (flashEnabled) {
                            DailyWellIcons.Misc.Flash
                        } else {
                            DailyWellIcons.Misc.FlashOff
                        },
                        contentDescription = if (flashEnabled) "Flash on" else "Flash off",
                        modifier = Modifier.size(20.dp),
                        tint = if (flashEnabled) Color.Yellow else Color.White
                    )
                }
            }

            // Bottom UI
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScanModePill(
                        label = "Scan food",
                        selected = selectedMode == ScanInputMode.FOOD_CAMERA,
                        enabled = true,
                        onClick = { selectedMode = ScanInputMode.FOOD_CAMERA }
                    )
                    ScanModePill(
                        label = "Barcode",
                        selected = selectedMode == ScanInputMode.BARCODE,
                        enabled = true,
                        onClick = {
                            showMealTypePicker = false
                            selectedMode = ScanInputMode.BARCODE
                        }
                    )
                    ScanModePill(
                        label = "Food label",
                        selected = selectedMode == ScanInputMode.FOOD_LABEL,
                        enabled = true,
                        onClick = { selectedMode = ScanInputMode.FOOD_LABEL }
                    )
                    ScanModePill(
                        label = "Library",
                        selected = selectedMode == ScanInputMode.LIBRARY,
                        enabled = true,
                        onClick = {
                            showMealTypePicker = false
                            selectedMode = ScanInputMode.LIBRARY
                            galleryPicker.launch("image/*")
                        }
                    )
                }

                PremiumSectionChip(
                    text = when (selectedMode) {
                        ScanInputMode.BARCODE -> "Barcode mode"
                        ScanInputMode.FOOD_LABEL -> "Label mode"
                        ScanInputMode.LIBRARY -> "Library import"
                        else -> selectedMealType?.name?.lowercase()?.replaceFirstChar { it.uppercase() }
                            ?: "Smart meal scan"
                    },
                    icon = DailyWellIcons.Health.FoodScan
                )

                // Meal type selector (optional)
                if (showMealTypePicker) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp,
                        elevation = ElevationLevel.Subtle
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MealType.values().take(4).forEach { type ->
                                FilterChip(
                                    selected = selectedMealType == type,
                                    onClick = { selectedMealType = type },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = dailyWellColors.timeAccent.copy(alpha = 0.24f),
                                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    label = {
                                        Text(
                                            type.name.lowercase().replaceFirstChar { it.uppercase() },
                                            fontSize = 12.sp
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Prompt card
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (selectedMode != ScanInputMode.BARCODE) {
                                showMealTypePicker = !showMealTypePicker
                            }
                        },
                    cornerRadius = 20.dp,
                    elevation = ElevationLevel.Subtle
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = when (selectedMode) {
                                ScanInputMode.BARCODE -> "Center barcode in frame for instant lookup."
                                ScanInputMode.FOOD_LABEL -> "Frame nutrition label clearly, then capture."
                                ScanInputMode.LIBRARY -> "Choose a photo from library to analyze."
                                else -> "Center your plate. Use Auto 3s or tap shutter."
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = when (selectedMode) {
                                ScanInputMode.BARCODE -> "Uses free barcode database first"
                                ScanInputMode.FOOD_LABEL -> "Runs on-device OCR first, then cloud fallback only if needed"
                                ScanInputMode.LIBRARY -> "Uses cloud AI credits for gallery photo analysis"
                                else -> "Uses cloud AI credits. Meal type is optional"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (autoCaptureArmed) {
                                autoCaptureArmed = false
                                autoCaptureCountdown = null
                            } else {
                                autoCaptureArmed = true
                            }
                        },
                        enabled = imageCapture != null &&
                            !captureInFlight &&
                            selectedMode != ScanInputMode.BARCODE &&
                            selectedMode != ScanInputMode.LIBRARY,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.16f),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f))
                    ) {
                        Text(
                            text = when {
                                selectedMode == ScanInputMode.BARCODE -> "Auto off in barcode"
                                selectedMode == ScanInputMode.LIBRARY -> "Pick from gallery"
                                autoCaptureCountdown != null -> "Auto in ${autoCaptureCountdown}s"
                                autoCaptureArmed -> "Cancel auto"
                                else -> "Auto 3s"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.36f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
                    ) {
                        Text(
                            text = when {
                                selectedMode == ScanInputMode.BARCODE -> "Live barcode"
                                captureInFlight -> "Capturing..."
                                else -> "Manual"
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }

                // Capture button
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    dailyWellColors.timeAccent.copy(alpha = 0.90f),
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.78f), CircleShape)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.96f))
                        .clickable {
                            if (selectedMode == ScanInputMode.BARCODE) return@clickable
                            if (selectedMode == ScanInputMode.LIBRARY) {
                                galleryPicker.launch("image/*")
                                return@clickable
                            }
                            autoCaptureArmed = false
                            autoCaptureCountdown = null
                            capturePhoto()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (captureInFlight) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = when (selectedMode) {
                                ScanInputMode.BARCODE -> DailyWellIcons.Health.Nutrition
                                ScanInputMode.LIBRARY -> DailyWellIcons.Misc.Gallery
                                else -> DailyWellIcons.Health.FoodScan
                            },
                            contentDescription = when (selectedMode) {
                                ScanInputMode.BARCODE -> "Barcode mode active"
                                ScanInputMode.LIBRARY -> "Open gallery"
                                else -> "Capture meal"
                            },
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraFramingGuide(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = 0.58f),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        val cornerColor = Color.White.copy(alpha = 0.94f)
        CameraGuideCorner(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            color = cornerColor
        )
        CameraGuideCorner(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            color = cornerColor,
            mirrorX = true
        )
        CameraGuideCorner(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            color = cornerColor,
            mirrorY = true
        )
        CameraGuideCorner(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            color = cornerColor,
            mirrorX = true,
            mirrorY = true
        )
    }
}

@Composable
private fun CameraGuideCorner(
    modifier: Modifier = Modifier,
    color: Color,
    mirrorX: Boolean = false,
    mirrorY: Boolean = false
) {
    val cornerAlignment = when {
        mirrorX && mirrorY -> Alignment.BottomEnd
        mirrorX -> Alignment.TopEnd
        mirrorY -> Alignment.BottomStart
        else -> Alignment.TopStart
    }

    Box(
        modifier = modifier
            .size(28.dp)
    ) {
        Box(
            modifier = Modifier
                .align(cornerAlignment)
                .height(3.dp)
                .width(24.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Box(
            modifier = Modifier
                .align(cornerAlignment)
                .width(3.dp)
                .height(24.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun ScanModePill(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) {
            Color.White.copy(alpha = 0.92f)
        } else {
            Color.Black.copy(alpha = 0.42f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                Color.White.copy(alpha = 0.94f)
            } else {
                Color.White.copy(alpha = 0.26f)
            }
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) {
                Color(0xFF1F3A52)
            } else {
                Color.White.copy(alpha = if (enabled) 0.85f else 0.45f)
            }
        )
    }
}

@Composable
private fun CameraPermissionRequest(
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    showOpenSettings: Boolean
) {
    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Smart Scan",
                    subtitle = "Camera access needed",
                    onNavigationClick = onBack
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = ElevationLevel.Prominent,
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PremiumSectionChip(
                            text = "Permission",
                            icon = DailyWellIcons.Health.FoodScan
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Icon(
                            imageVector = DailyWellIcons.Health.FoodScan,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Camera Permission Required",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "To scan your meals with AI, we need camera access. Photos are analyzed and never stored as raw images.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        if (showOpenSettings) {
                            Text(
                                text = "Camera access is blocked. Open app settings and allow Camera permission.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Button(
                            onClick = if (showOpenSettings) onOpenSettings else onRequestPermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (showOpenSettings) "Open Settings" else "Enable Camera",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * On-device OCR extraction for nutrition labels (cost-free fallback before cloud AI).
 */
private fun extractNutritionFromLabelBitmap(
    bitmap: Bitmap,
    onResult: (LabelOcrDraft?) -> Unit
) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val image = InputImage.fromBitmap(bitmap, 0)

    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            onResult(parseLabelTextToDraft(visionText.text))
        }
        .addOnFailureListener {
            onResult(null)
        }
        .addOnCompleteListener {
            recognizer.close()
        }
}

private fun parseLabelTextToDraft(rawText: String): LabelOcrDraft? {
    val normalized = rawText
        .replace("\u00A0", " ")
        .replace("\r", "\n")
    if (normalized.isBlank()) return null

    fun extractFloat(patterns: List<Regex>): Float? {
        patterns.forEach { pattern ->
            val value = pattern.find(normalized)
                ?.groupValues
                ?.getOrNull(1)
                ?.replace(",", ".")
                ?.toFloatOrNull()
            if (value != null) return value
        }
        return null
    }

    fun extractInt(patterns: List<Regex>): Int? {
        return extractFloat(patterns)?.toInt()
    }

    fun extractServing(): String? {
        val servingPatterns = listOf(
            Regex("(?i)serving\\s*size\\s*[:]?\\s*([\\w\\s\\./()%,-]+)"),
            Regex("(?i)per\\s+serving\\s*[:]?\\s*([\\w\\s\\./()%,-]+)")
        )
        return servingPatterns
            .firstNotNullOfOrNull { pattern ->
                pattern.find(normalized)?.groupValues?.getOrNull(1)?.trim()
            }
            ?.takeIf { it.isNotBlank() }
            ?.take(36)
    }

    fun inferProductName(): String? {
        return normalized.lineSequence()
            .map { it.trim() }
            .firstOrNull { line ->
                line.length >= 3 &&
                    line.length <= 52 &&
                    !line.contains("nutrition", ignoreCase = true) &&
                    !line.contains("facts", ignoreCase = true) &&
                    !line.contains("serving", ignoreCase = true) &&
                    !line.contains("ingredients", ignoreCase = true) &&
                    !line.matches(Regex("^[0-9\\s%.,mgkcalg/:-]+$"))
            }
    }

    val calories = extractInt(
        listOf(
            Regex("(?i)cal(?:orie|ories|0ries)?\\s*[:]?\\s*(\\d{2,4})"),
            Regex("(?i)energy\\s*[:]?\\s*(\\d{2,4})\\s*kcal")
        )
    ) ?: run {
        val energyKj = extractFloat(
            listOf(
                Regex("(?i)energy\\s*[:]?\\s*(\\d{3,5})\\s*kj")
            )
        )
        energyKj?.let { (it / 4.184f).toInt() }
    }

    val protein = extractInt(
        listOf(
            Regex("(?i)protein\\s*[:]?\\s*(\\d+(?:[\\.,]\\d+)?)\\s*g"),
            Regex("(?i)protem\\s*[:]?\\s*(\\d+(?:[\\.,]\\d+)?)\\s*g")
        )
    )
    val carbs = extractInt(
        listOf(
            Regex("(?i)(?:total\\s*)?carbo(?:hydrate|hydrates|hydrat[e3]s?)\\s*[:]?\\s*(\\d+(?:[\\.,]\\d+)?)\\s*g"),
            Regex("(?i)carbs?\\s*[:]?\\s*(\\d+(?:[\\.,]\\d+)?)\\s*g")
        )
    )
    val fat = extractInt(
        listOf(
            Regex("(?i)(?:total\\s*)?fat\\s*[:]?\\s*(\\d+(?:[\\.,]\\d+)?)\\s*g"),
            Regex("(?i)lipids?\\s*[:]?\\s*(\\d+(?:[\\.,]\\d+)?)\\s*g")
        )
    )
    val sugar = extractInt(
        listOf(
            Regex("(?i)(?:total\\s*)?sugars?\\s*[:]?\\s*(\\d+(?:[\\.,]\\d+)?)\\s*g"),
            Regex("(?i)added\\s+sugars?\\s*[:]?\\s*(\\d+(?:[\\.,]\\d+)?)\\s*g")
        )
    )
    val fiber = extractInt(
        listOf(
            Regex("(?i)(?:dietary\\s*)?fib(?:er|re)\\s*[:]?\\s*(\\d+(?:[\\.,]\\d+)?)\\s*g")
        )
    )
    val sodiumFromSalt = extractFloat(
        listOf(
            Regex("(?i)salt\\s*[:]?\\s*(\\d+(?:[\\.,]\\d+)?)\\s*g")
        )
    )?.let { (it * 393f).toInt() }
    val sodium = extractInt(
        listOf(
            Regex("(?i)sodium\\s*[:]?\\s*(\\d+(?:[\\.,]\\d+)?)\\s*mg")
        )
    ) ?: sodiumFromSalt

    val coreFields = listOf(calories, protein, carbs, fat).count { it != null }
    val confidence = when {
        coreFields >= 4 -> 0.92f
        coreFields == 3 -> 0.78f
        coreFields == 2 && calories != null -> 0.66f
        coreFields == 2 -> 0.58f
        else -> 0.35f
    }

    if (coreFields < 2 && calories == null) return null

    return LabelOcrDraft(
        productName = inferProductName(),
        servingText = extractServing(),
        calories = calories,
        proteinGrams = protein,
        carbsGrams = carbs,
        fatGrams = fat,
        sugarGrams = sugar,
        fiberGrams = fiber,
        sodiumMg = sodium,
        confidence = confidence,
        extractedText = normalized
    )
}

/**
 * Barcode detector for hybrid low-cost scan pipeline.
 */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processBarcodeImage(
    imageProxy: ImageProxy,
    onBarcodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    val scanner = BarcodeScanning.getClient()
    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            barcodes.firstOrNull { barcode ->
                barcode.format == Barcode.FORMAT_EAN_13 ||
                    barcode.format == Barcode.FORMAT_UPC_A ||
                    barcode.format == Barcode.FORMAT_EAN_8
            }?.rawValue?.takeIf { it.isNotBlank() }?.let(onBarcodeDetected)
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

/**
 * Converts ImageProxy to Bitmap
 */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
    val image = imageProxy.image ?: throw IllegalStateException("Image is null")
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    // Decode the ByteArray to Bitmap
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    // Apply rotation based on image metadata
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    return if (rotationDegrees != 0) {
        rotateBitmap(bitmap, rotationDegrees.toFloat())
    } else {
        bitmap
    }
}

/**
 * Rotates bitmap by specified degrees
 */
private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/**
 * Compresses bitmap to ByteArray with target max size
 * Ensures image is < 1MB for fast API upload
 */
private fun compressBitmapToBytes(bitmap: Bitmap, maxSizeKb: Int = 1024): ByteArray {
    val maxSizeBytes = maxSizeKb * 1024
    var quality = 90

    // First resize if image is too large
    var currentBitmap = bitmap
    val maxDimension = 1920
    if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
        val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        currentBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    // Compress with decreasing quality until size < maxSizeBytes
    var outputStream = ByteArrayOutputStream()
    currentBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

    while (outputStream.size() > maxSizeBytes && quality > 10) {
        outputStream = ByteArrayOutputStream()
        quality -= 10
        currentBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    }

    return outputStream.toByteArray()
}


