package com.dailywell.app.ui.screens.body

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dailywell.app.core.theme.LocalDailyWellColors
import com.dailywell.app.data.repository.BodyMetricsRepository
import com.dailywell.app.data.model.PhotoAngle
import com.dailywell.app.data.model.ProgressPhoto
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.DailyWellSprings
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PlatformPhotoImage
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import com.dailywell.app.ui.components.StaggeredItem
import com.dailywell.app.ui.components.pressScale
import com.dailywell.app.ui.components.rememberAnimatedGradientOffset
import com.dailywell.app.ui.components.rememberBreathingScale
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Progress Photos Screen - Visual Tracking
 *
 * PERFECTION MODE: Better than MyFitnessPal
 * - Photo gallery with categories
 * - Before/after comparison
 * - Privacy-first approach
 * - Timeline view
 * - Motivational progress insights
 *
 * Quality Standard: Apple Photos inspired design
 */

/** Sealed state for the main content area */
private sealed interface PhotoScreenState {
    data object Loading : PhotoScreenState
    data class Compare(
        val photos: List<ProgressPhoto>,
        val selectedPhotos: Pair<ProgressPhoto?, ProgressPhoto?>
    ) : PhotoScreenState
    data object Empty : PhotoScreenState
    data class Gallery(val photos: List<ProgressPhoto>) : PhotoScreenState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressPhotosScreen(
    userId: String,
    bodyMetricsRepository: BodyMetricsRepository,
    onBack: () -> Unit,
    onTakePhoto: (PhotoAngle, (String) -> Unit) -> Unit, // Callback to capture photo
    modifier: Modifier = Modifier
) {
    var photos by remember { mutableStateOf<List<ProgressPhoto>>(emptyList()) }
    var selectedPhotoType by remember { mutableStateOf<PhotoAngle?>(null) }
    var selectedPhoto by remember { mutableStateOf<ProgressPhoto?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCompareMode by remember { mutableStateOf(false) }
    var comparePhotos by remember { mutableStateOf<Pair<ProgressPhoto?, ProgressPhoto?>>(null to null) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load photos
    LaunchedEffect(userId, selectedPhotoType) {
        isLoading = true
        try {
            val result = bodyMetricsRepository.getProgressPhotos(userId, selectedPhotoType)
            result.fold(
                onSuccess = { photos = it },
                onFailure = { /* ignore */ }
            )
        } finally {
            isLoading = false
        }
    }

    // Derive the current screen state
    val screenState: PhotoScreenState = when {
        isLoading -> PhotoScreenState.Loading
        showCompareMode -> PhotoScreenState.Compare(photos, comparePhotos)
        photos.isEmpty() -> PhotoScreenState.Empty
        else -> PhotoScreenState.Gallery(photos)
    }

    GlassScreenWrapper {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            PremiumTopBar(
                title = "Progress Photos",
                subtitle = "Visual timeline and comparison",
                onNavigationClick = onBack,
                trailingActions = {
                    if (photos.size >= 2) {
                        IconButton(onClick = { showCompareMode = !showCompareMode }) {
                            if (showCompareMode) {
                                Icon(DailyWellIcons.Nav.Close, "Exit compare")
                            } else {
                                Icon(
                                    imageVector = DailyWellIcons.Analytics.Correlation,
                                    contentDescription = "Compare"
                                )
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!showCompareMode) {
                val breathingScale = rememberBreathingScale()
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.graphicsLayer {
                        scaleX = breathingScale
                        scaleY = breathingScale
                    }
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Misc.Camera,
                        contentDescription = "Take photo",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Chips with staggered entrance
            if (!showCompareMode) {
                PremiumSectionChip(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    text = "Photo timeline",
                    icon = DailyWellIcons.Health.Photos
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StaggeredItem(index = 0) {
                        FilterChip(
                            selected = selectedPhotoType == null,
                            onClick = { selectedPhotoType = null },
                            label = { Text("All") }
                        )
                    }
                    StaggeredItem(index = 1) {
                        FilterChip(
                            selected = selectedPhotoType == PhotoAngle.FRONT,
                            onClick = { selectedPhotoType = PhotoAngle.FRONT },
                            label = { Text("Front") }
                        )
                    }
                    StaggeredItem(index = 2) {
                        FilterChip(
                            selected = selectedPhotoType == PhotoAngle.SIDE,
                            onClick = { selectedPhotoType = PhotoAngle.SIDE },
                            label = { Text("Side") }
                        )
                    }
                    StaggeredItem(index = 3) {
                        FilterChip(
                            selected = selectedPhotoType == PhotoAngle.BACK,
                            onClick = { selectedPhotoType = PhotoAngle.BACK },
                            label = { Text("Back") }
                        )
                    }
                }
            }

            // Animated crossfade between screen states
            Crossfade(
                targetState = screenState,
                animationSpec = tween(durationMillis = 350, easing = EaseInOutCubic),
                label = "screenStateCrossfade",
                modifier = Modifier.weight(1f)
            ) { state ->
                when (state) {
                    is PhotoScreenState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is PhotoScreenState.Compare -> {
                        CompareView(
                            photos = state.photos,
                            selectedPhotos = state.selectedPhotos,
                            onPhotoSelected = { position, photo ->
                                comparePhotos = if (position == 0) {
                                    photo to comparePhotos.second
                                } else {
                                    comparePhotos.first to photo
                                }
                            }
                        )
                    }
                    is PhotoScreenState.Empty -> {
                        EmptyPhotosState(onAddPhoto = { showAddDialog = true })
                    }
                    is PhotoScreenState.Gallery -> {
                        PhotoGallery(
                            photos = state.photos,
                            onPhotoClick = { selectedPhoto = it },
                            onDeletePhoto = { photo ->
                                scope.launch {
                                    bodyMetricsRepository.deleteProgressPhoto(photo.id).fold(
                                        onSuccess = {
                                            photos = photos.filter { it.id != photo.id }
                                            snackbarHostState.showSnackbar("Photo deleted")
                                        },
                                        onFailure = { error ->
                                            snackbarHostState.showSnackbar(
                                                error.message ?: "Failed to delete photo"
                                            )
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // Add Photo Dialog
        if (showAddDialog) {
            AddPhotoDialog(
                onDismiss = { showAddDialog = false },
                onPhotoTypeSelected = { photoType ->
                    showAddDialog = false
                    onTakePhoto(photoType) { photoUrl ->
                        scope.launch {
                            val result = bodyMetricsRepository.saveProgressPhoto(
                                userId = userId,
                                photoUrl = photoUrl,
                                photoAngle = photoType,
                                date = Clock.System.now().toString()
                            )
                            result.fold(
                                onSuccess = {
                                    photos = listOf(it) + photos
                                    snackbarHostState.showSnackbar("Photo saved!")
                                },
                                onFailure = { error ->
                                    snackbarHostState.showSnackbar(
                                        error.message ?: "Failed to save photo"
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }

        // Photo Detail Dialog
        selectedPhoto?.let { photo ->
            PhotoDetailDialog(
                photo = photo,
                onDismiss = { selectedPhoto = null },
                onDelete = {
                    scope.launch {
                        bodyMetricsRepository.deleteProgressPhoto(photo.id).fold(
                            onSuccess = {
                                photos = photos.filter { it.id != photo.id }
                                selectedPhoto = null
                                snackbarHostState.showSnackbar("Photo deleted")
                            },
                            onFailure = { error ->
                                snackbarHostState.showSnackbar(
                                    error.message ?: "Failed to delete photo"
                                )
                            }
                        )
                    }
                }
            )
        }
    }
    }
}

/**
 * Empty Photos State with fade-in animation
 */
@Composable
private fun EmptyPhotosState(
    onAddPhoto: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 500, easing = EaseOutCubic)
        ) + slideInVertically(
            initialOffsetY = { it / 8 },
            animationSpec = tween(durationMillis = 500, easing = EaseOutCubic)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val breathingScale = rememberBreathingScale(
                minScale = 1f,
                maxScale = 1.05f,
                durationMs = 2500
            )

            Icon(
                imageVector = DailyWellIcons.Misc.Camera,
                contentDescription = "Camera",
                modifier = Modifier
                    .size(80.dp)
                    .padding(16.dp)
                    .graphicsLayer {
                        scaleX = breathingScale
                        scaleY = breathingScale
                    },
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Track Your Progress",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Take photos to visualize your transformation journey",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onAddPhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .pressScale()
            ) {
                Icon(
                    imageVector = DailyWellIcons.Misc.Camera,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Take First Photo")
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = DailyWellIcons.Onboarding.Philosophy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Tips for Best Results",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    listOf(
                        "Take photos in the same spot each time",
                        "Use consistent lighting",
                        "Take photos at the same time of day",
                        "Wear similar clothing for comparison"
                    ).forEach { tip ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("\u2022")
                            Text(
                                text = tip,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Photo Gallery Grid
 */
@Composable
private fun PhotoGallery(
    photos: List<ProgressPhoto>,
    onPhotoClick: (ProgressPhoto) -> Unit,
    onDeletePhoto: (ProgressPhoto) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(photos) { index, photo ->
            StaggeredItem(index = index, delayPerItem = 60L) {
                PhotoCard(
                    photo = photo,
                    onClick = { onPhotoClick(photo) }
                )
            }
        }

        // Add space for FAB
        item {
            Spacer(Modifier.height(80.dp))
        }
    }
}

/**
 * Photo Card with press scale feedback
 */
@Composable
private fun PhotoCard(
    photo: ProgressPhoto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(0.75f)
            .pressScale()
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            PhotoContent(photo = photo)

            // Photo info overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp)
            ) {
                Text(
                    text = photo.photoType.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatPhotoDate(photo.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Compare View
 */
@Composable
private fun CompareView(
    photos: List<ProgressPhoto>,
    selectedPhotos: Pair<ProgressPhoto?, ProgressPhoto?>,
    onPhotoSelected: (position: Int, ProgressPhoto) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Instructions
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "Select two photos to compare your progress",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Before Photo
            StaggeredItem(index = 0) {
                ComparePhotoSlot(
                    label = "Before",
                    photo = selectedPhotos.first,
                    photos = photos,
                    onPhotoSelected = { onPhotoSelected(0, it) },
                    modifier = Modifier.weight(1f)
                )
            }

            // After Photo
            StaggeredItem(index = 1) {
                ComparePhotoSlot(
                    label = "After",
                    photo = selectedPhotos.second,
                    photos = photos,
                    onPhotoSelected = { onPhotoSelected(1, it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Comparison Stats with animated visibility
        AnimatedVisibility(
            visible = selectedPhotos.first != null && selectedPhotos.second != null,
            enter = fadeIn(tween(300)) + slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = tween(400, easing = EaseOutCubic)
            ),
            exit = fadeOut(tween(200)) + slideOutVertically(
                targetOffsetY = { it / 4 },
                animationSpec = tween(200)
            )
        ) {
            if (selectedPhotos.first != null && selectedPhotos.second != null) {
                ComparisonStatsCard(
                    beforePhoto = selectedPhotos.first!!,
                    afterPhoto = selectedPhotos.second!!
                )
            }
        }
    }
}

/**
 * Compare Photo Slot
 */
@Composable
private fun ComparePhotoSlot(
    label: String,
    photo: ProgressPhoto?,
    photos: List<ProgressPhoto>,
    onPhotoSelected: (ProgressPhoto) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSelector by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxHeight()
            .pressScale()
            .clickable { showSelector = true }
    ) {
        Crossfade(
            targetState = photo,
            animationSpec = tween(300),
            label = "photoSlotCrossfade"
        ) { currentPhoto ->
            if (currentPhoto == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = DailyWellIcons.Actions.Add,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = DailyWellIcons.Misc.Camera,
                                contentDescription = "Add photo",
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Select $label Photo",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    PhotoContent(photo = currentPhoto)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatPhotoDate(currentPhoto.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }

    if (showSelector) {
        PhotoSelectorDialog(
            photos = photos,
            onPhotoSelected = {
                onPhotoSelected(it)
                showSelector = false
            },
            onDismiss = { showSelector = false }
        )
    }
}

/**
 * Comparison Stats Card with animated progress bar
 */
@Composable
private fun ComparisonStatsCard(
    beforePhoto: ProgressPhoto,
    afterPhoto: ProgressPhoto
) {
    val daysBetween = calculateDaysBetween(beforePhoto.date, afterPhoto.date)

    // Animated progress for the progress bar
    var progressVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        progressVisible = true
    }
    val animatedProgress by animateFloatAsState(
        targetValue = if (progressVisible) (daysBetween.coerceAtMost(90) / 90f) else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            delayMillis = 300,
            easing = EaseOutCubic
        ),
        label = "progressBar"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = DailyWellIcons.Analytics.TrendUp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Progress Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Animated progress bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Journey progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$daysBetween / 90 days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f)
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    label = "Days",
                    value = "$daysBetween"
                )
                StatItem(
                    label = "Weeks",
                    value = "${daysBetween / 7}"
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Keep up the amazing work! Your transformation is inspiring!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Stat Item
 */
@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * Photo Selector Dialog
 */
@Composable
private fun PhotoSelectorDialog(
    photos: List<ProgressPhoto>,
    onPhotoSelected: (ProgressPhoto) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Photo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(photos) { photo ->
                        PhotoCard(
                            photo = photo,
                            onClick = { onPhotoSelected(photo) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Add Photo Dialog with staggered photo angle items
 */
@Composable
private fun AddPhotoDialog(
    onDismiss: () -> Unit,
    onPhotoTypeSelected: (PhotoAngle) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Select Photo Type",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                PhotoAngle.values().forEachIndexed { index, angle ->
                    StaggeredItem(index = index, delayPerItem = 70L, baseDelay = 100L) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .pressScale()
                                .clickable { onPhotoTypeSelected(angle) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = DailyWellIcons.Misc.Camera,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = angle.name.replace("_", " "),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

/**
 * Photo Detail Dialog
 */
@Composable
private fun PhotoDetailDialog(
    photo: ProgressPhoto,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
                // Photo Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    PhotoContent(
                        photo = photo,
                        contentDescription = "Progress photo detail"
                    )
                }

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = photo.angle.name.replace("_", " "),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = formatPhotoDate(photo.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (photo.note.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = photo.note,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close")
                        }

                        Button(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444)
                            )
                        ) {
                            Icon(DailyWellIcons.Actions.Delete, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Photo?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PhotoContent(
    photo: ProgressPhoto,
    contentDescription: String = "Progress photo"
) {
    val source = resolvePhotoSource(photo)
    if (source != null) {
        PlatformPhotoImage(
            model = source,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = DailyWellIcons.Misc.Gallery,
            contentDescription = "Photo placeholder",
            modifier = Modifier
                .size(40.dp)
                .alpha(0.3f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun resolvePhotoSource(photo: ProgressPhoto): String? {
    val source = photo.photoUrl.ifBlank { photo.photoPath }.trim()
    return source.takeIf { it.isNotEmpty() }
}

/**
 * Helper: Format photo date
 */
private fun formatPhotoDate(dateString: String): String {
    return try {
        val date = dateString.take(10)
        val parts = date.split("-")
        if (parts.size == 3) {
            val monthNames = listOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            "${monthNames[parts[1].toInt() - 1]} ${parts[2]}, ${parts[0]}"
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}

/**
 * Helper: Calculate days between dates
 */
private fun calculateDaysBetween(date1: String, date2: String): Int {
    return try {
        val firstDate = LocalDate.parse(date1.take(10))
        val secondDate = LocalDate.parse(date2.take(10))
        abs(firstDate.daysUntil(secondDate))
    } catch (e: Exception) {
        0
    }
}
