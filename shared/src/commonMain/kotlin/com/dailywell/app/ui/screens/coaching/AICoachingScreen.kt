package com.dailywell.app.ui.screens.coaching

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.core.theme.PrimaryLight
import com.dailywell.app.core.theme.Secondary
import com.dailywell.app.core.theme.SecondaryLight
import com.dailywell.app.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICoachingScreen(
    viewModel: AICoachingViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Coach") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Coach avatar button
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryLight)
                            .clickable { viewModel.showCoachSelector(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.selectedCoach.avatar,
                            fontSize = 20.sp
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.activeSession != null) {
            ChatSessionView(
                session = uiState.activeSession!!,
                coach = uiState.selectedCoach,
                currentMessage = uiState.currentMessage,
                onMessageChange = viewModel::updateCurrentMessage,
                onSendMessage = viewModel::sendMessage,
                onQuickReply = viewModel::selectQuickReply,
                onEndSession = viewModel::endSession,
                modifier = Modifier.padding(padding)
            )
        } else {
            CoachingHomeView(
                uiState = uiState,
                onStartSession = viewModel::startSession,
                onShowSessionTypes = { viewModel.showSessionTypeSelector(true) },
                onCompleteAction = viewModel::completeSuggestedAction,
                onCompleteActionItem = viewModel::completeActionItem,
                onDismissActionItem = viewModel::dismissActionItem,
                modifier = Modifier.padding(padding)
            )
        }
    }

    // Coach selector dialog
    if (uiState.showCoachSelector) {
        CoachSelectorDialog(
            coaches = uiState.availableCoaches,
            selectedCoach = uiState.selectedCoach,
            onSelectCoach = viewModel::selectCoach,
            onDismiss = { viewModel.showCoachSelector(false) }
        )
    }

    // Session type selector dialog
    if (uiState.showSessionTypeSelector) {
        SessionTypeSelectorDialog(
            onSelectType = viewModel::startSession,
            onDismiss = { viewModel.showSessionTypeSelector(false) }
        )
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
            // Coach greeting
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = coach.avatar, fontSize = 24.sp)
                }

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
            Text(text = action.emoji, fontSize = 14.sp)
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
            emoji = "☀️",
            label = "Check-in",
            onClick = onStartCheckin,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            emoji = "🎯",
            label = "Coaching",
            onClick = onStartCoaching,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            emoji = "💪",
            label = "Motivate",
            onClick = onMotivationBoost,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    emoji: String,
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
            Text(text = emoji, fontSize = 24.sp)
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
                Text(
                    text = "📊 Weekly Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

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
                StatItem(
                    value = when (summary.streakStatus.streakTrend) {
                        TrendDirection.UP -> "📈"
                        TrendDirection.DOWN -> "📉"
                        TrendDirection.STABLE -> "➡️"
                    },
                    label = "Trend"
                )
            }

            HorizontalDivider()

            // Top win
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "🏆", fontSize = 16.sp)
                Text(
                    text = summary.topWin,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Pattern discovered
            summary.patternDiscovered?.let { pattern ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "💡", fontSize = 16.sp)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🎯", fontSize = 16.sp)
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
            Text(text = session.type.emoji, fontSize = 24.sp)

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

            Text(
                text = if (session.status == SessionStatus.COMPLETED) "✓" else "—",
                color = if (session.status == SessionStatus.COMPLETED)
                    Color(0xFF66BB6A) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun ChatSessionView(
    session: AICoachingSession,
    coach: CoachPersona,
    currentMessage: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onQuickReply: (String) -> Unit,
    onEndSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(session.messages.size) {
        if (session.messages.isNotEmpty()) {
            listState.animateScrollToItem(session.messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Session header
        Surface(
            color = PrimaryLight.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = session.type.emoji, fontSize = 20.sp)
                    Text(
                        text = session.title,
                        fontWeight = FontWeight.Medium
                    )
                }

                TextButton(onClick = onEndSession) {
                    Text("End Session")
                }
            }
        }

        // Messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(session.messages) { message ->
                ChatMessageBubble(
                    message = message,
                    coach = coach,
                    onQuickReply = onQuickReply
                )
            }
        }

        // Input area
        Surface(
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = currentMessage,
                    onValueChange = onMessageChange,
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSendMessage() }),
                    singleLine = true
                )

                IconButton(
                    onClick = onSendMessage,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Primary)
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
private fun ChatMessageBubble(
    message: CoachingMessage,
    coach: CoachPersona,
    onQuickReply: (String) -> Unit
) {
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
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = coach.avatar, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                color = if (isCoach) MaterialTheme.colorScheme.surface else Primary,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isCoach) 4.dp else 16.dp,
                    bottomEnd = if (isCoach) 16.dp else 4.dp
                ),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    color = if (isCoach) MaterialTheme.colorScheme.onSurface else Color.White
                )
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
                        color = Primary.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = suggestion,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachSelectorDialog(
    coaches: List<CoachPersona>,
    selectedCoach: CoachPersona,
    onSelectCoach: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Choose Your Coach",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                coaches.forEach { coach ->
                    val isSelected = coach.id == selectedCoach.id

                    Surface(
                        onClick = { onSelectCoach(coach.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PrimaryLight.copy(alpha = 0.3f)
                               else MaterialTheme.colorScheme.surface,
                        border = if (isSelected)
                            androidx.compose.foundation.BorderStroke(2.dp, Primary) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = coach.avatar, fontSize = 24.sp)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = coach.name,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = coach.style.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Primary
                                )
                                Text(
                                    text = coach.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Primary
                                )
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
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
                            Text(text = type.emoji, fontSize = 24.sp)
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
