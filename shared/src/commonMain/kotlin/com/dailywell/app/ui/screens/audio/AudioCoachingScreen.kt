package com.dailywell.app.ui.screens.audio

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.data.model.*
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.core.theme.PrimaryLight
import com.dailywell.app.core.theme.Secondary
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioCoachingScreen(
    onBack: () -> Unit,
    isPremium: Boolean = false,
    viewModel: AudioCoachingViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    GlassScreenWrapper {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    PremiumTopBar(
                        title = "Audio Coaching",
                        subtitle = "Guided sessions for focus and recovery",
                        onNavigationClick = onBack
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Header
                item {
                    AudioCoachingHeader(
                        totalListenTime = uiState.totalListenTime,
                        completedCount = uiState.completedTrackIds.size
                    )
                }

                // Recommended Track
                uiState.recommendedTrack?.let { track ->
                    item {
                        RecommendedTrackCard(
                            track = track,
                            isLocked = !isPremium && track.isPremium,
                            onPlay = { viewModel.playTrack(track) }
                        )
                    }
                }

                // Category Filters
                item {
                    CategoryFilters(
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = { viewModel.selectCategory(it) }
                    )
                }

                // Playlists Section
                    if (uiState.selectedCategory == null) {
                        item {
                            PremiumSectionChip(
                                text = "Playlists",
                                icon = DailyWellIcons.Coaching.Audio
                            )
                        }

                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.playlists) { playlist ->
                                    PlaylistCard(
                                        playlist = playlist,
                                        isLocked = !isPremium && playlist.isPremium
                                    )
                                }
                            }
                        }
                    }

                    // Tracks Section
                    item {
                        PremiumSectionChip(
                            text = if (uiState.selectedCategory != null)
                                uiState.selectedCategory!!.label
                            else
                                "All tracks",
                            icon = DailyWellIcons.Analytics.Pattern
                        )
                    }

                    items(uiState.tracks) { track ->
                        AudioTrackCard(
                            track = track,
                            isCompleted = track.id in uiState.completedTrackIds,
                            isFavorite = track.id in uiState.favoriteTrackIds,
                            isLocked = !isPremium && track.isPremium,
                            isPlaying = uiState.currentPlayingTrack?.id == track.id && uiState.isPlaying,
                            onPlay = { viewModel.playTrack(track) },
                            onFavoriteToggle = { viewModel.toggleFavorite(track.id) }
                        )
                    }

                    // Premium Upsell
                    if (!isPremium) {
                        item {
                            PremiumUpsellCard()
                        }
                    }
                }
            }

            // Mini Player
            AnimatedVisibility(
                visible = uiState.currentPlayingTrack != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                uiState.currentPlayingTrack?.let { track ->
                    MiniPlayer(
                        track = track,
                        isPlaying = uiState.isPlaying,
                        progress = uiState.playbackProgress,
                        onPlayPause = {
                            if (uiState.isPlaying) viewModel.pauseTrack() else viewModel.resumeTrack()
                        },
                        onClose = { viewModel.stopTrack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioCoachingHeader(
    totalListenTime: Int,
    completedCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PrimaryLight.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = DailyWellIcons.Coaching.Audio,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Micro-Coaching",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Expert guidance in 2-3 minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = AudioLibrary.formatDuration(totalListenTime),
                    label = "Listen time"
                )
                StatItem(
                    value = "$completedCount",
                    label = "Completed"
                )
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Secondary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun RecommendedTrackCard(
    track: AudioTrack,
    isLocked: Boolean,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked) { onPlay() },
        colors = CardDefaults.cardColors(containerColor = Secondary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = DailyWellIcons.Misc.Sparkle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recommended for you",
                    style = MaterialTheme.typography.labelMedium,
                    color = Secondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Secondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getAudioCategoryIcon(track.category),
                        contentDescription = track.category.label,
                        modifier = Modifier.size(32.dp),
                        tint = Secondary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = track.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = AudioLibrary.formatDuration(track.durationSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = Secondary
                    )
                }

                if (isLocked) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Premium",
                        tint = Color.Gray
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = DailyWellIcons.Coaching.Play,
                            contentDescription = "Play",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilters(
    selectedCategory: AudioCategory?,
    onCategorySelected: (AudioCategory?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") }
            )
        }
        items(AudioCategory.entries.take(6)) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = {
                    onCategorySelected(if (selectedCategory == category) null else category)
                },
                leadingIcon = {
                    Icon(
                        imageVector = getAudioCategoryIcon(category),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = { Text(category.label) }
            )
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: AudioPlaylist,
    isLocked: Boolean
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(enabled = !isLocked) { },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Secondary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getPlaylistIcon(playlist.id),
                    contentDescription = playlist.name,
                    modifier = Modifier.size(40.dp),
                    tint = Secondary
                )
                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Premium",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${playlist.trackIds.size} tracks - ${AudioLibrary.formatDuration(playlist.totalDuration)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun AudioTrackCard(
    track: AudioTrack,
    isCompleted: Boolean,
    isFavorite: Boolean,
    isLocked: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked) { onPlay() },
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying)
                Secondary.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isPlaying) Secondary.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    // Playing animation placeholder
                    Icon(
                        imageVector = DailyWellIcons.Coaching.Audio,
                        contentDescription = "Playing",
                        modifier = Modifier.size(22.dp),
                        tint = Secondary
                    )
                } else {
                    Icon(
                        imageVector = getAudioCategoryIcon(track.category),
                        contentDescription = track.category.label,
                        modifier = Modifier.size(24.dp),
                        tint = Primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Track info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCompleted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = DailyWellIcons.Actions.CheckCircle,
                            contentDescription = "Completed",
                            modifier = Modifier.size(16.dp),
                            tint = Primary
                        )
                    }
                    if (track.isNew) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "NEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = track.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AudioLibrary.formatDuration(track.durationSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = Secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "-",
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = track.category.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Actions
            if (isLocked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Premium",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) Color(0xFFE57373) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    track: AudioTrack,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        Column {
            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Secondary,
                trackColor = Color.Gray.copy(alpha = 0.2f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Track info
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Secondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getAudioCategoryIcon(track.category),
                        contentDescription = track.category.label,
                        modifier = Modifier.size(22.dp),
                        tint = Secondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = AudioLibrary.formatDuration(track.durationSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Play/Pause button
                IconButton(onClick = onPlayPause) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) DailyWellIcons.Coaching.Pause else DailyWellIcons.Coaching.Play,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                }

                // Close button
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumUpsellCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = DailyWellIcons.Coaching.Audio,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Secondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Unlock All Audio Coaching",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Get unlimited access to expert-guided sessions, morning mindset tracks, sleep stories, and more.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* Navigate to paywall */ },
                colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Go Premium")
            }
        }
    }
}

/**
 * Maps AudioCategory to the appropriate Material Icon
 */
private fun getAudioCategoryIcon(category: AudioCategory): ImageVector = when (category) {
    AudioCategory.MORNING_MINDSET -> DailyWellIcons.Misc.Sunrise
    AudioCategory.EVENING_WINDDOWN -> DailyWellIcons.Misc.Night
    AudioCategory.HABIT_SCIENCE -> DailyWellIcons.Coaching.Lesson
    AudioCategory.MOTIVATION -> DailyWellIcons.Health.Workout
    AudioCategory.BREATHING -> DailyWellIcons.Habits.Calm
    AudioCategory.FOCUS -> DailyWellIcons.Habits.Intentions
    AudioCategory.STRESS_RELIEF -> DailyWellIcons.Habits.Calm
    AudioCategory.SLEEP_STORIES -> DailyWellIcons.Habits.Sleep
    AudioCategory.CELEBRATION -> DailyWellIcons.Social.Cheer
    AudioCategory.COMEBACK -> DailyWellIcons.Habits.Recovery
}

/**
 * Maps playlist ID to the appropriate Material Icon
 */
private fun getPlaylistIcon(playlistId: String): ImageVector = when (playlistId) {
    "morning_essentials" -> DailyWellIcons.Misc.Sunrise
    "habit_academy" -> DailyWellIcons.Coaching.Lesson
    "motivation_boost" -> DailyWellIcons.Gamification.XP
    "evening_routine" -> DailyWellIcons.Misc.Night
    else -> DailyWellIcons.Coaching.Audio
}

