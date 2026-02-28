package com.dailywell.app.ui.screens.coaching

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dailywell.app.ai.SLMDownloadProgress
import com.dailywell.app.core.theme.LocalDailyWellColors
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.core.theme.PrimaryLight
import com.dailywell.app.core.theme.Success
import com.dailywell.app.core.theme.Secondary
import com.dailywell.app.core.theme.SecondaryLight
import com.dailywell.app.core.theme.Error
import com.dailywell.app.core.theme.Warning
import com.dailywell.app.core.theme.WarningLight
import com.dailywell.app.data.model.*
import com.dailywell.app.speech.SpeechRecognitionState
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PlatformCoachAvatar
import com.dailywell.app.ui.components.VoiceInputButton
import com.dailywell.app.ui.components.VoiceInputStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICoachingScreen(
    viewModel: AICoachingViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val speechState by viewModel.speechState.collectAsState()
    val speechSettings by viewModel.speechSettings.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var bootstrapped by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!bootstrapped) {
            bootstrapped = true
            viewModel.onCoachScreenOpened()
        }
    }

    GlassScreenWrapper {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(320.dp)
                ) {
                    CoachHistoryDrawer(
                        activeSession = uiState.activeSession,
                        sessionHistory = uiState.sessionHistory,
                        onNewChat = {
                            viewModel.startNewChat()
                            scope.launch { drawerState.close() }
                        },
                        onSelectSession = { sessionId ->
                            viewModel.resumeSession(sessionId)
                            scope.launch { drawerState.close() }
                        },
                        onClose = { scope.launch { drawerState.close() } }
                    )
                }
            }
        ) {
            Scaffold(
                containerColor = Color.Transparent
            ) { padding ->
                if (uiState.activeSession != null) {
                    ChatSessionView(
                        session = uiState.activeSession!!,
                        coach = uiState.selectedCoach,
                        currentMessage = uiState.currentMessage,
                        pendingUserMessage = uiState.pendingUserMessage,
                        isGeneratingReply = uiState.isGeneratingReply,
                        onMessageChange = viewModel::updateCurrentMessage,
                        onSendMessage = viewModel::sendMessage,
                        onQuickReply = viewModel::selectQuickReply,
                        onEndSession = viewModel::endSession,
                        onOpenHistory = { scope.launch { drawerState.open() } },
                        onStartNewChat = viewModel::startNewChat,
                        onBack = onBack,
                        speechState = speechState,
                        speechSettings = speechSettings,
                        isVoiceAvailable = viewModel.isVoiceAvailable,
                        onStartVoiceInput = viewModel::startVoiceInput,
                        onStopVoiceInput = viewModel::stopVoiceInput,
                        onCancelVoiceInput = viewModel::cancelVoiceInput,
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    // Upgrade prompt dialog (when AI credits depleted)
    if (uiState.showUpgradePrompt) {
        UpgradePromptDialog(
            message = uiState.aiLimitMessage ?: "You've reached your AI coaching limit for this month.",
            onDismiss = viewModel::dismissUpgradePrompt,
            onUpgrade = {
                // Navigate to upgrade screen - handled by parent
                viewModel.dismissUpgradePrompt()
            }
        )
    }

    // Show limit message as a snackbar-like banner
    uiState.aiLimitMessage?.let { message ->
        if (!uiState.showUpgradePrompt) {
            LimitMessageBanner(
                message = message,
                onDismiss = viewModel::clearAILimitMessage
            )
        }
    }
}

@Composable
private fun CoachHistoryDrawer(
    activeSession: AICoachingSession?,
    sessionHistory: List<AICoachingSession>,
    onNewChat: () -> Unit,
    onSelectSession: (String) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val sessions = remember(activeSession, sessionHistory) {
        buildList {
            activeSession?.let { add(it) }
            addAll(sessionHistory)
        }.distinctBy { it.id }
    }
    val filteredSessions = remember(sessions, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            sessions
        } else {
            sessions.filter { session ->
                val lastMessage = session.messages.lastOrNull()?.content.orEmpty()
                session.title.contains(query, ignoreCase = true) ||
                    session.description.contains(query, ignoreCase = true) ||
                    session.type.displayName.contains(query, ignoreCase = true) ||
                    lastMessage.contains(query, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Chat History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Button(
            onClick = onNewChat,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("New Chat")
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("Search sessions") },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            }
        )

        if (filteredSessions.isEmpty()) {
            Text(
                if (sessions.isEmpty()) "No previous sessions yet." else "No sessions match your search.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSessions) { session ->
                    val lastMessage = session.messages.lastOrNull()?.content ?: ""
                    val isActive = activeSession?.id == session.id

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelectSession(session.id) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isActive) {
                            PrimaryLight.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                session.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (lastMessage.isNotBlank()) {
                                Text(
                                    lastMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (isActive) "Active" else session.status.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isActive) Primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = { onSelectSession(session.id) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(if (isActive) "Open" else "Resume")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachingHomeView(
    uiState: AICoachingUiState,
    onStartSession: (CoachingSessionType) -> Unit,
    onShowSessionTypes: () -> Unit,
    onCompleteAction: (String) -> Unit,
    onCompleteActionItem: (String) -> Unit,
    onDismissActionItem: (String) -> Unit,
    onStartSLMDownload: () -> Unit,
    onDismissSLMDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Daily insight card
        uiState.dailyInsight?.let { insight ->
            item {
                DailyInsightCard(
                    insight = insight,
                    coach = uiState.selectedCoach,
                    onActionComplete = onCompleteAction
                )
            }
        }

        // Quick actions
        item {
            QuickActionsRow(
                onStartCheckin = { onStartSession(CoachingSessionType.DAILY_CHECKIN) },
                onStartCoaching = onShowSessionTypes,
                onMotivationBoost = { onStartSession(CoachingSessionType.MOTIVATION_BOOST) }
            )
        }

        // SLM model download progress (for offline AI coach)
        when (val progress = uiState.slmDownloadProgress) {
            is SLMDownloadProgress.NotStarted,
            is SLMDownloadProgress.Downloading,
            is SLMDownloadProgress.Failed,
            is SLMDownloadProgress.NeedsStorage,
            is SLMDownloadProgress.WaitingForWifi -> item {
                SLMDownloadStatusCard(
                    progress = progress,
                    onStartDownload = onStartSLMDownload,
                    onDismiss = onDismissSLMDownload
                )
            }
            else -> Unit
        }

        // AI Credits indicator
        item {
            AICreditsCard(
                creditsPercent = uiState.aiCreditsPercent,
                planType = uiState.aiUsage?.planType?.displayName ?: "Free",
                canUseAI = uiState.canUseAI
            )
        }

        // Action items
        if (uiState.actionItems.isNotEmpty()) {
            item {
                Text(
                    text = "Your Action Items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(uiState.actionItems) { item ->
                ActionItemCard(
                    item = item,
                    onComplete = { onCompleteActionItem(item.id) },
                    onDismiss = { onDismissActionItem(item.id) }
                )
            }
        }

        // Weekly summary
        uiState.weeklySummary?.let { summary ->
            item {
                WeeklySummaryCard(summary = summary)
            }
        }

        // Session history
        if (uiState.sessionHistory.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(uiState.sessionHistory.take(5)) { session ->
                SessionHistoryCard(session = session)
            }
        }
    }
}

@Composable
private fun SLMDownloadStatusCard(
    progress: SLMDownloadProgress,
    onStartDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val isStorageWarning = progress is SLMDownloadProgress.NeedsStorage

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isStorageWarning) WarningLight else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (progress) {
                        is SLMDownloadProgress.NotStarted -> "Offline AI Coach"
                        is SLMDownloadProgress.Downloading -> "Downloading AI Coach"
                        is SLMDownloadProgress.Failed -> "Download failed"
                        is SLMDownloadProgress.NeedsStorage -> "Storage needed"
                        is SLMDownloadProgress.WaitingForWifi -> "Waiting for WiFi"
                        else -> "AI Coach"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isStorageWarning) Warning else MaterialTheme.colorScheme.onSurface
                )

                if (progress is SLMDownloadProgress.NotStarted ||
                    progress is SLMDownloadProgress.Failed ||
                    progress is SLMDownloadProgress.WaitingForWifi
                ) {
                    Text(
                        text = "Later",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            when (progress) {
                is SLMDownloadProgress.NotStarted -> {
                    Text(
                        "Download about 380MB once for faster, offline AI coaching.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onStartDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Download")
                    }
                }
                is SLMDownloadProgress.Downloading -> {
                    val percent = (progress.progress * 100).toInt()
                    Text(
                        "Downloading... $percent%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { progress.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                is SLMDownloadProgress.Failed -> {
                    Text(
                        "Could not download the model. Retry on WiFi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedButton(
                        onClick = onStartDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Retry")
                    }
                }
                is SLMDownloadProgress.NeedsStorage -> {
                    val needMb = progress.needBytes / (1024 * 1024)
                    Text(
                        "Need about ${needMb}MB free to download the offline AI model.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Warning
                    )
                    Button(
                        onClick = onStartDownload,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Warning)
                    ) {
                        Text("Check Again", color = Color.White)
                    }
                }
                is SLMDownloadProgress.WaitingForWifi -> {
                    Text(
                        "Connect to WiFi to start model download.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onStartDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Check Connection")
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun DailyInsightCard(
    insight: DailyCoachingInsight,
    coach: CoachPersona,
    onActionComplete: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryLight.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Coach greeting with AI avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlatformCoachAvatar(
                    coachId = coach.id,
                    size = 48.dp,
                    isActive = true
                )

                Column {
                    Text(
                        text = coach.name,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = coach.style.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Greeting
            Text(
                text = insight.greeting,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            // Main message
            Text(
                text = insight.mainMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            // Celebration note
            insight.celebrationNote?.let { note ->
                Surface(
                    color = Secondary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = note,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Motivational quote
            insight.motivationalQuote?.let { quote ->
                Text(
                    text = quote,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Suggested actions
            if (insight.suggestedActions.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(insight.suggestedActions) { action ->
                        SuggestedActionChip(
                            action = action,
                            onClick = { onActionComplete(action.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestedActionChip(
    action: SuggestedAction,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Primary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getSuggestedActionIcon(action.actionType),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Primary
            )
            Text(
                text = action.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Primary
            )
        }
    }
}

@Composable
private fun QuickActionsRow(
    onStartCheckin: () -> Unit,
    onStartCoaching: () -> Unit,
    onMotivationBoost: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionButton(
            icon = DailyWellIcons.Misc.Sunrise,
            label = "Check-in",
            onClick = onStartCheckin,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = DailyWellIcons.Habits.Intentions,
            label = "Coaching",
            onClick = onStartCoaching,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = DailyWellIcons.Health.Workout,
            label = "Motivate",
            onClick = onMotivationBoost,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = Primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ActionItemCard(
    item: CoachingActionItem,
    onComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Priority indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when (item.priority) {
                            ActionPriority.HIGH -> Color(0xFFE53935)
                            ActionPriority.MEDIUM -> Color(0xFFFFA726)
                            ActionPriority.LOW -> Color(0xFF66BB6A)
                        }
                    )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            IconButton(onClick = onComplete) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Complete",
                    tint = Color(0xFF66BB6A)
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun WeeklySummaryCard(summary: WeeklyCoachingSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SecondaryLight.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Analytics.BarChart,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Primary
                    )
                    Text(
                        text = "Weekly Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Score badge
                Surface(
                    color = Secondary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${summary.overallScore}%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = "${(summary.completionRate * 100).toInt()}%",
                    label = "Completion"
                )
                StatItem(
                    value = "${summary.streakStatus.currentStreak}",
                    label = "Streak"
                )
                TrendIconItem(
                    trend = summary.streakStatus.streakTrend,
                    label = "Trend"
                )
            }

            HorizontalDivider()

            // Top win
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = DailyWellIcons.Gamification.Trophy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Primary
                )
                Text(
                    text = summary.topWin,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Pattern discovered
            summary.patternDiscovered?.let { pattern ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Onboarding.Philosophy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Primary
                    )
                    Text(
                        text = pattern,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Next week focus
            Surface(
                color = Primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Habits.Intentions,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Primary
                    )
                    Column {
                        Text(
                            text = "Next Week Focus",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = summary.nextWeekFocus,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun SessionHistoryCard(session: AICoachingSession) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = getSessionTypeIcon(session.type),
                contentDescription = session.type.displayName,
                modifier = Modifier.size(24.dp),
                tint = Primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${session.messages.size} messages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (session.status == SessionStatus.COMPLETED) {
                Icon(
                    imageVector = DailyWellIcons.Actions.CheckCircle,
                    contentDescription = "Completed",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF66BB6A)
                )
            }
        }
    }
}

@Composable
private fun ChatSessionView(
    session: AICoachingSession,
    coach: CoachPersona,
    currentMessage: String,
    pendingUserMessage: CoachingMessage?,
    isGeneratingReply: Boolean,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onQuickReply: (String) -> Unit,
    onEndSession: () -> Unit,
    onOpenHistory: () -> Unit,
    onStartNewChat: () -> Unit,
    onBack: () -> Unit,
    speechState: SpeechRecognitionState,
    speechSettings: com.dailywell.app.speech.SpeechSettings,
    isVoiceAvailable: Boolean,
    onStartVoiceInput: () -> Unit,
    onStopVoiceInput: () -> Unit,
    onCancelVoiceInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dailyWellColors = LocalDailyWellColors.current
    val listState = rememberLazyListState()
    val displayMessages = remember(session.messages, pendingUserMessage) {
        buildList {
            addAll(session.messages)
            if (pendingUserMessage != null) {
                val alreadyPersisted = session.messages.any {
                    it.role == MessageRole.USER && it.content == pendingUserMessage.content
                }
                if (!alreadyPersisted) add(pendingUserMessage)
            }
        }
    }
    val totalListItems = displayMessages.size + if (isGeneratingReply) 1 else 0

    LaunchedEffect(totalListItems) {
        if (totalListItems > 0) {
            listState.animateScrollToItem(totalListItems - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dailyWellColors.timeGradientStart.copy(alpha = 0.24f),
                        dailyWellColors.timeGradientEnd.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        CoachSessionActionsBar(
            session = session,
            coach = coach,
            onEndSession = onEndSession,
            onOpenHistory = onOpenHistory,
            onStartNewChat = onStartNewChat,
            onBack = onBack,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
        )

        // Messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(displayMessages, key = { it.id }) { message ->
                ChatMessageBubble(
                    message = message,
                    coach = coach,
                    onQuickReply = onQuickReply
                )
            }
            if (isGeneratingReply) {
                item(key = "analyzing_indicator") {
                    AnalyzingMessageBubble(coach = coach)
                }
            }
        }

        // Voice input status (shows transcription progress)
        val isListening = speechState is SpeechRecognitionState.Listening ||
                         speechState is SpeechRecognitionState.PartialResult

        if (isListening || speechState is SpeechRecognitionState.Error) {
            VoiceInputStatus(
                state = speechState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // Input area
        Surface(
            color = dailyWellColors.glassSheet.copy(alpha = 0.88f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = BorderStroke(1.dp, dailyWellColors.glassBorder.copy(alpha = 0.34f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                dailyWellColors.glassBackground.copy(alpha = 0.38f),
                                dailyWellColors.glassPrimary.copy(alpha = 0.28f)
                            )
                        )
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice input button
                if (isVoiceAvailable) {
                    VoiceInputButton(
                        state = speechState,
                        inputMode = speechSettings.inputMode,
                        onStartListening = onStartVoiceInput,
                        onStopListening = onStopVoiceInput,
                        onCancelListening = onCancelVoiceInput,
                        enabled = !isListening || speechState !is SpeechRecognitionState.Processing
                    )
                }

                OutlinedTextField(
                    value = currentMessage,
                    onValueChange = onMessageChange,
                    placeholder = {
                        Text(
                            if (isVoiceAvailable) "Type or tap mic..." else "Type a message..."
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSendMessage() }),
                    singleLine = true,
                    enabled = !isListening,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = dailyWellColors.glassBackground.copy(alpha = 0.55f),
                        unfocusedContainerColor = dailyWellColors.glassBackground.copy(alpha = 0.35f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        unfocusedBorderColor = dailyWellColors.glassBorder.copy(alpha = 0.45f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                IconButton(
                    onClick = onSendMessage,
                    enabled = currentMessage.isNotBlank() && !isListening && !isGeneratingReply,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentMessage.isNotBlank() && !isListening && !isGeneratingReply) {
                                Brush.horizontalGradient(
                                    listOf(
                                        Primary,
                                        Secondary
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        Primary.copy(alpha = 0.5f),
                                        Secondary.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachSessionActionsBar(
    session: AICoachingSession,
    coach: CoachPersona,
    onEndSession: () -> Unit,
    onOpenHistory: () -> Unit,
    onStartNewChat: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("Back")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                PlatformCoachAvatar(
                    coachId = coach.id,
                    size = 28.dp,
                    isActive = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Coach chat",
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${session.messages.size} messages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(onClick = onOpenHistory) { Text("History") }
                TextButton(onClick = onStartNewChat) { Text("New") }
                TextButton(onClick = onEndSession) { Text("End") }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: CoachingMessage,
    coach: CoachPersona,
    onQuickReply: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isCoach = message.role == MessageRole.COACH

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCoach) Alignment.Start else Alignment.End
    ) {
        Row(
            horizontalArrangement = if (isCoach) Arrangement.Start else Arrangement.End,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isCoach) {
                PlatformCoachAvatar(
                    coachId = coach.id,
                    size = 32.dp,
                    isActive = false
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isCoach) 4.dp else 16.dp,
                    bottomEnd = if (isCoach) 16.dp else 4.dp
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isCoach) {
                        colorScheme.primary.copy(alpha = 0.34f)
                    } else {
                        Color.White.copy(alpha = 0.28f)
                    }
                ),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isCoach) {
                                Brush.horizontalGradient(
                                    listOf(
                                        colorScheme.primaryContainer.copy(alpha = 0.96f),
                                        colorScheme.secondaryContainer.copy(alpha = 0.90f)
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        Primary,
                                        Secondary
                                    )
                                )
                            }
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = message.content,
                        color = if (isCoach) colorScheme.onPrimaryContainer else Color.White
                    )
                }
            }
        }

        // Quick reply suggestions
        if (isCoach && message.suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 40.dp)
            ) {
                items(message.suggestions) { suggestion ->
                    Surface(
                        onClick = { onQuickReply(suggestion) },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.36f))
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            colorScheme.surface.copy(alpha = 0.96f),
                                            colorScheme.surfaceVariant.copy(alpha = 0.88f)
                                        )
                                    )
                                )
                        ) {
                            Text(
                                text = suggestion,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun AnalyzingMessageBubble(coach: CoachPersona) {
    val dailyWellColors = LocalDailyWellColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth()
        ) {
            PlatformCoachAvatar(
                coachId = coach.id,
                size = 32.dp,
                isActive = false
            )
            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = dailyWellColors.glassPrimary.copy(alpha = 0.88f),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 16.dp
                ),
                border = BorderStroke(1.dp, dailyWellColors.glassBorder.copy(alpha = 0.44f)),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Planning your best next step...",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionTypeSelectorDialog(
    onSelectType: (CoachingSessionType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Start a Session",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                CoachingSessionType.entries.forEach { type ->
                    Surface(
                        onClick = { onSelectType(type) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getSessionTypeIcon(type),
                                contentDescription = type.displayName,
                                modifier = Modifier.size(24.dp),
                                tint = Primary
                            )
                            Text(
                                text = type.displayName,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

/**
 * AI Credits indicator card - shows remaining AI credits as a progress bar
 */
@Composable
private fun AICreditsCard(
    creditsPercent: Float,
    planType: String,
    canUseAI: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = creditsPercent / 100f,
        label = "credits_progress"
    )

    // Color changes based on remaining credits
    val progressColor by animateColorAsState(
        targetValue = when {
            creditsPercent > 50f -> Success
            creditsPercent > 20f -> Warning
            else -> Error
        },
        label = "credits_color"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Credits",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Surface(
                        color = Primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = planType,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary
                        )
                    }
                }

                Text(
                    text = "${creditsPercent.toInt()}% remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = progressColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Warning message when low
            if (!canUseAI) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Error
                    )
                    Text(
                        text = "Credits depleted. Upgrade for more AI coaching!",
                        style = MaterialTheme.typography.labelSmall,
                        color = Error
                    )
                }
            } else if (creditsPercent <= 20f) {
                Text(
                    text = "Running low on credits. Consider upgrading!",
                    style = MaterialTheme.typography.labelSmall,
                    color = Warning
                )
            }
        }
    }
}

/**
 * Dialog shown when AI credits are depleted
 */
@Composable
private fun UpgradePromptDialog(
    message: String,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon
                Surface(
                    color = Warning.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(32.dp),
                        tint = Warning
                    )
                }

                Text(
                    text = "AI Credits Limit Reached",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Upgrade benefits
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryLight.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Premium Benefits:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        BenefitRow("Unlimited AI coaching sessions")
                        BenefitRow("Personalized advice & insights")
                        BenefitRow("Priority response times")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Maybe Later")
                    }

                    Button(
                        onClick = onUpgrade,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Upgrade")
                    }
                }
            }
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Banner shown when user hits AI limit (but not showing upgrade dialog)
 */
@Composable
private fun LimitMessageBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Warning.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Warning
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Warning
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(16.dp),
                    tint = Warning
                )
            }
        }
    }
}

/**
 * Maps CoachingSessionType to the appropriate Material Icon
 */
private fun getSessionTypeIcon(type: CoachingSessionType): ImageVector = when (type) {
    CoachingSessionType.DAILY_CHECKIN -> DailyWellIcons.Misc.Sunrise
    CoachingSessionType.WEEKLY_REVIEW -> DailyWellIcons.Analytics.BarChart
    CoachingSessionType.HABIT_COACHING -> DailyWellIcons.Habits.Intentions
    CoachingSessionType.MOTIVATION_BOOST -> DailyWellIcons.Health.Workout
    CoachingSessionType.OBSTACLE_SOLVING -> DailyWellIcons.Analytics.Pattern
    CoachingSessionType.CELEBRATION -> DailyWellIcons.Social.Cheer
    CoachingSessionType.RECOVERY_SUPPORT -> DailyWellIcons.Habits.Recovery
}

/**
 * Maps SuggestedActionType to the appropriate Material Icon
 */
private fun getSuggestedActionIcon(type: SuggestedActionType): ImageVector = when (type) {
    SuggestedActionType.COMPLETE_HABIT -> DailyWellIcons.Actions.CheckCircle
    SuggestedActionType.STACK_HABITS -> DailyWellIcons.Habits.HabitStacking
    SuggestedActionType.SET_INTENTION -> DailyWellIcons.Habits.Intentions
    SuggestedActionType.REFLECT -> DailyWellIcons.Coaching.Reflection
    SuggestedActionType.REST -> DailyWellIcons.Habits.Sleep
    SuggestedActionType.CELEBRATE -> DailyWellIcons.Social.Cheer
}

/**
 * Trend icon item that renders an Icon instead of emoji text
 */
@Composable
private fun TrendIconItem(trend: TrendDirection, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = when (trend) {
                TrendDirection.UP -> DailyWellIcons.Analytics.TrendUp
                TrendDirection.DOWN -> DailyWellIcons.Analytics.TrendDown
                TrendDirection.STABLE -> DailyWellIcons.Analytics.TrendFlat
            },
            contentDescription = trend.name,
            modifier = Modifier.size(28.dp),
            tint = Primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
