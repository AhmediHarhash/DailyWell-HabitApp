package com.dailywell.app.ui.screens.scan

import android.Manifest
import android.content.Intent
import android.util.Size
import android.view.ViewGroup
import android.net.Uri
import android.provider.Settings
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.dailywell.app.data.model.*
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumTopBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.koin.compose.viewmodel.koinViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
actual fun FoodScanScreen(
    viewModel: FoodScanViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var cameraPermissionRequested by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        viewModel.setCameraPermission(cameraPermissionState.status.isGranted)
    }

    val deniedStatus = cameraPermissionState.status as? PermissionStatus.Denied
    val isPermanentlyDenied = deniedStatus != null &&
        !deniedStatus.shouldShowRationale &&
        cameraPermissionRequested

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Smart Scan",
                    subtitle = if (uiState.isLoading) "Analyzing barcode..." else "Point camera at barcode",
                    onNavigationClick = onNavigateBack
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    !cameraPermissionState.status.isGranted -> {
                        CameraPermissionRequest(
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
                    }
                    uiState.showResults && uiState.scannedFood != null -> {
                        ScanResultsView(
                            food = uiState.scannedFood!!,
                            onScanAgain = { viewModel.closeResults() }
                        )
                    }
                    else -> {
                        BarcodeScannerView(
                            onBarcodeDetected = { barcode ->
                                viewModel.onBarcodeDetected(barcode)
                            },
                            isLoading = uiState.isLoading,
                            error = uiState.error,
                            onDismissError = { viewModel.dismissError() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionRequest(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    showOpenSettings: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📷",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Camera Permission Needed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "To scan food products, we need access to your camera. Your camera is only used for scanning and photos are not stored.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (showOpenSettings) {
            Text(
                text = "Camera access is blocked. Open app settings and allow Camera permission.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Button(
            onClick = if (showOpenSettings) onOpenSettings else onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showOpenSettings) "Open Settings" else "Enable Camera")
        }
    }
}

@Composable
private fun BarcodeScannerView(
    onBarcodeDetected: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    onDismissError: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
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

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(
                                Executors.newSingleThreadExecutor()
                            ) { imageProxy ->
                                processBarcode(imageProxy) { barcode ->
                                    onBarcodeDetected(barcode)
                                }
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        // Handle exception
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // Scan frame overlay
        ScanFrameOverlay()

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Looking up product...",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Error snackbar
        error?.let { errorMessage ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = onDismissError) {
                        Text("Dismiss", color = Color.White)
                    }
                }
            ) {
                Text(errorMessage)
            }
        }

        // Instructions
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Point camera at a barcode",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Product info appears automatically",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ScanFrameOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(280.dp, 180.dp)
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                )
        )
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processBarcode(
    imageProxy: ImageProxy,
    onBarcodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { value ->
                        // Filter for food product barcodes (EAN-13, UPC-A)
                        if (barcode.format == Barcode.FORMAT_EAN_13 ||
                            barcode.format == Barcode.FORMAT_UPC_A ||
                            barcode.format == Barcode.FORMAT_EAN_8
                        ) {
                            onBarcodeDetected(value)
                        }
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

@Composable
private fun ScanResultsView(
    food: ScannedFood,
    onScanAgain: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Health Score Badge (Hero section)
        item {
            HealthScoreHero(
                food = food
            )
        }

        // Product Info
        item {
            ProductInfoCard(food = food)
        }

        // NOVA Processing
        item {
            NovaProcessingCard(novaGroup = food.novaLevel)
        }

        // Eco-Score (Environmental Impact)
        if (food.ecoScore.isNotEmpty()) {
            item {
                EcoScoreCard(ecoScore = food.ecoScore)
            }
        }

        // Nutrients
        item {
            NutrientsCard(nutrients = food.nutrients)
        }

        // Additives (if any)
        if (food.additives.isNotEmpty()) {
            item {
                AdditivesCard(additives = food.additives)
            }
        }

        // Healthier Alternatives (if any)
        if (food.alternatives.isNotEmpty()) {
            item {
                AlternativesCard(alternatives = food.alternatives)
            }
        }

        // Tips
        if (food.tips.isNotEmpty()) {
            item {
                TipsCard(tips = food.tips)
            }
        }

        // Scan Again Button
        item {
            Button(
                onClick = onScanAgain,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Scan Another Product")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HealthScoreHero(food: ScannedFood) {
    val scoreColor = when {
        food.healthScore >= 75 -> Color(0xFF4CAF50)  // Green
        food.healthScore >= 50 -> Color(0xFFFFA726)  // Orange
        else -> Color(0xFFFF7043)                     // Deep Orange
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = scoreColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Score Circle
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(scoreColor),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${food.healthScore}",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "/100",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Friendly Message
            Text(
                text = food.friendlyMessage,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scoreColor
            )

            // Grade badge if available
            if (food.healthGradeStr.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nutri-Score: ${food.healthGradeStr}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProductInfoCard(food: ScannedFood) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (food.brand.isNotEmpty()) {
                Text(
                    text = food.brand,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (food.quantity.isNotEmpty()) {
                Text(
                    text = food.quantity,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NovaProcessingCard(novaGroup: NovaGroup) {
    val color = when (novaGroup.level) {
        1 -> Color(0xFF4CAF50)
        2 -> Color(0xFF8BC34A)
        3 -> Color(0xFFFFA726)
        else -> Color(0xFFFF7043)
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${novaGroup.level}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "NOVA ${novaGroup.level}: ${novaGroup.displayName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = novaGroup.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EcoScoreCard(ecoScore: String) {
    val grade = ecoScore.uppercase()
    val (color, label, description) = when (grade) {
        "A" -> Triple(Color(0xFF1E8F4E), "Very Low Impact", "Minimal environmental footprint")
        "B" -> Triple(Color(0xFF4CAF50), "Low Impact", "Below average environmental impact")
        "C" -> Triple(Color(0xFFFFA726), "Moderate Impact", "Average environmental footprint")
        "D" -> Triple(Color(0xFFFF7043), "Higher Impact", "Above average environmental impact")
        "E" -> Triple(Color(0xFFE53935), "High Impact", "Consider more eco-friendly options")
        else -> Triple(Color.Gray, "Unknown", "Environmental data not available")
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Eco badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = grade,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Eco-Score: ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NutrientsCard(nutrients: NutrientInfo) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Nutrition (per ${nutrients.servingSize})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            NutrientRow("Calories", "${nutrients.calories} kcal", null)
            NutrientRow("Fat", "${nutrients.fat}g", nutrients.fatPercent)
            NutrientRow("Saturated Fat", "${nutrients.saturatedFat}g", nutrients.saturatedFatPercent)
            NutrientRow("Sugars", "${nutrients.sugars}g", nutrients.sugarsPercent)
            NutrientRow("Fiber", "${nutrients.fiber}g", nutrients.fiberPercent)
            NutrientRow("Protein", "${nutrients.protein}g", nutrients.proteinPercent)
            NutrientRow("Sodium", "${nutrients.sodium.toInt()}mg", nutrients.sodiumPercent)
        }
    }
}

@Composable
private fun NutrientRow(name: String, value: String, percent: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            percent?.let {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "($it%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AdditivesCard(additives: List<Additive>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Additives (${additives.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            additives.forEach { additive ->
                AdditiveItem(additive = additive)
                if (additive != additives.last()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun AdditiveItem(additive: Additive) {
    val riskColor = Color(additive.risk.color)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .offset(y = 6.dp)
                .clip(CircleShape)
                .background(riskColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${additive.code} - ${additive.name}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = additive.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TipsCard(tips: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Tips",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            tips.forEach { tip ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(text = "💡", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun AlternativesCard(alternatives: List<FoodAlternative>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🔄", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Healthier Alternatives",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            alternatives.forEachIndexed { index, alternative ->
                AlternativeItem(alternative = alternative)
                if (index < alternatives.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun AlternativeItem(alternative: FoodAlternative) {
    val scoreColor = when {
        alternative.healthScore >= 75 -> Color(0xFF4CAF50)  // Green
        alternative.healthScore >= 50 -> Color(0xFFFFA726)  // Orange
        else -> Color(0xFFFF7043)  // Deep Orange
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Health Score Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(scoreColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${alternative.healthScore}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alternative.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (alternative.brand.isNotBlank()) {
                    Text(
                        text = alternative.brand,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (alternative.reason.isNotBlank()) {
                    Text(
                        text = alternative.reason,
                        style = MaterialTheme.typography.labelSmall,
                        color = scoreColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
