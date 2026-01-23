package com.dailywell.app.ui.screens.challenges

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.data.model.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(
    onBack: () -> Unit,
    viewModel: ChallengeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Challenges", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            ChallengeTabRow(
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.selectTab(it) }
            )

            // Content based on selected tab
            when (uiState.selectedTab) {
                ChallengeTab.SOLO -> SoloChallengesContent(
                    availableChallenges = viewModel.getFilteredChallenges(),
                    activeChallenges = uiState.activeChallenges,
                    completedChallenges = uiState.completedChallenges,
                    selectedDifficulty = uiState.selectedDifficulty,
                    onFilterChange = { viewModel.filterByDifficulty(it) },
                    onJoinChallenge = { viewModel.joinChallenge(it) },
                    onAbandonChallenge = { viewModel.abandonChallenge(it) },
                    onClaimReward = { viewModel.claimReward(it) },
                    onShowDetail = { viewModel.showChallengeDetail(it) },
                    isLoading = uiState.isLoading
                )
                ChallengeTab.DUELS -> DuelsContent(
                    pendingInvitations = uiState.pendingInvitations,
                    activeDuels = uiState.activeDuels,
                    duelHistory = uiState.duelHistory,
                    onAcceptDuel = { viewModel.acceptDuel(it) },
                    onDeclineDuel = { viewModel.declineDuel(it) },
                    onShowDuelDetail = { viewModel.showDuelDetail(it) },
                    onCreateDuel = { viewModel.showCreateDuel() }
                )
                ChallengeTab.COMMUNITY -> CommunityContent(
                    challenge = uiState.communityChallenge,
                    userProgress = uiState.userCommunityProgress,
                    leaderboard = uiState.communityLeaderboard,
                    onJoin = { viewModel.joinCommunityChallenge() }
                )
                ChallengeTab.SEASONAL -> SeasonalContent(
                    event = uiState.seasonalEvent,
                    progress = uiState.seasonalProgress,
                    onJoin = { viewModel.joinSeasonalEvent() },
                    onClaimReward = { viewModel.claimSeasonalReward(it) }
                )
                ChallengeTab.CREATE -> CreateChallengeContent(
                    customChallenges = uiState.customChallenges,
                    onCreateChallenge = { viewModel.showCreateChallenge() },
                    onDeleteChallenge = { viewModel.deleteCustomChallenge(it) },
                    onShareChallenge = { viewModel.shareCustomChallenge(it, emptyList()) }
                )
            }
        }
    }

    // Dialogs
    uiState.showChallengeDetail?.let { challenge ->
        ChallengeDetailDialog(
            challenge = challenge,
            onDismiss = { viewModel.hideChallengeDetail() },
            onJoin = { viewModel.joinChallenge(challenge.id) }
        )
    }

    uiState.showDuelDetail?.let { duel ->
        DuelDetailDialog(
            duel = duel,
            onDismiss = { viewModel.hideDuelDetail() }
        )
    }

    if (uiState.showCreateChallenge) {
        CreateChallengeDialog(
            onDismiss = { viewModel.hideCreateChallenge() },
            onCreate = { title, desc, emoji, goal, duration, difficulty ->
                viewModel.createCustomChallenge(title, desc, emoji, goal, duration, difficulty)
            }
        )
    }

    uiState.rewardClaimed?.let { rewards ->
        RewardClaimedDialog(
            rewards = rewards,
            onDismiss = { viewModel.dismissRewardClaimed() }
        )
    }
}

@Composable
private fun ChallengeTabRow(
    selectedTab: ChallengeTab,
    onTabSelected: (ChallengeTab) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        ChallengeTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = when (tab) {
                            ChallengeTab.SOLO -> "🎯 Solo"
                            ChallengeTab.DUELS -> "⚔️ Duels"
                            ChallengeTab.COMMUNITY -> "🌍 Community"
                            ChallengeTab.SEASONAL -> "🎄 Seasonal"
                            ChallengeTab.CREATE -> "✏️ Create"
                        },
                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

// ============== SOLO CHALLENGES ==============

@Composable
private fun SoloChallengesContent(
    availableChallenges: List<Challenge>,
    activeChallenges: List<ActiveChallenge>,
    completedChallenges: List<ActiveChallenge>,
    selectedDifficulty: ChallengeDifficulty?,
    onFilterChange: (ChallengeDifficulty?) -> Unit,
    onJoinChallenge: (String) -> Unit,
    onAbandonChallenge: (String) -> Unit,
    onClaimReward: (String) -> Unit,
    onShowDetail: (Challenge) -> Unit,
    isLoading: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Difficulty filter
        item {
            DifficultyFilter(
                selected = selectedDifficulty,
                onSelect = onFilterChange
            )
        }

        // Active challenges section
        if (activeChallenges.isNotEmpty()) {
            item {
                Text(
                    text = "Active Challenges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(activeChallenges) { active ->
                ActiveChallengeCard(
                    activeChallenge = active,
                    onAbandon = { onAbandonChallenge(active.challenge.id) },
                    onClaimReward = { onClaimReward(active.challenge.id) }
                )
            }
        }

        // Available challenges section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Available Challenges",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (availableChallenges.isEmpty()) {
            item {
                EmptyState(
                    emoji = "🎯",
                    title = "No challenges available",
                    subtitle = "You've joined all available challenges!"
                )
            }
        } else {
            items(availableChallenges) { challenge ->
                ChallengeCard(
                    challenge = challenge,
                    onJoin = { onJoinChallenge(challenge.id) },
                    onClick = { onShowDetail(challenge) }
                )
            }
        }

        // Completed challenges section
        if (completedChallenges.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Completed (${completedChallenges.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(completedChallenges.take(3)) { completed ->
                CompletedChallengeCard(activeChallenge = completed)
            }
        }
    }
}

@Composable
private fun DifficultyFilter(
    selected: ChallengeDifficulty?,
    onSelect: (ChallengeDifficulty?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("All") }
        )
        ChallengeDifficulty.entries.forEach { difficulty ->
            FilterChip(
                selected = selected == difficulty,
                onClick = { onSelect(difficulty) },
                label = {
                    Text(
                        text = when (difficulty) {
                            ChallengeDifficulty.EASY -> "🟢 Easy"
                            ChallengeDifficulty.MEDIUM -> "🟡 Medium"
                            ChallengeDifficulty.HARD -> "🟠 Hard"
                            ChallengeDifficulty.EXTREME -> "🔴 Extreme"
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun ChallengeCard(
    challenge: Challenge,
    onJoin: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = challenge.emoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = challenge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DifficultyBadge(difficulty = challenge.difficulty)
                    Text(
                        text = "${challenge.duration.days} days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "+${challenge.rewards.xp} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Join button
            Button(
                onClick = onJoin,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text("Join")
            }
        }
    }
}

@Composable
private fun ActiveChallengeCard(
    activeChallenge: ActiveChallenge,
    onAbandon: () -> Unit,
    onClaimReward: () -> Unit
) {
    val progress = activeChallenge.userProgress
    val isComplete = progress.currentValue >= progress.targetValue

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = activeChallenge.challenge.emoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeChallenge.challenge.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${progress.currentValue}/${progress.targetValue}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (isComplete) {
                    Button(onClick = onClaimReward) {
                        Text("Claim 🎁")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress.progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isComplete) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
            )

            if (!isComplete) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onAbandon,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Abandon Challenge")
                }
            }
        }
    }
}

@Composable
private fun CompletedChallengeCard(activeChallenge: ActiveChallenge) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = activeChallenge.challenge.emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = activeChallenge.challenge.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "✅",
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: ChallengeDifficulty) {
    val (color, text) = when (difficulty) {
        ChallengeDifficulty.EASY -> Color(0xFF4CAF50) to "Easy"
        ChallengeDifficulty.MEDIUM -> Color(0xFFFFC107) to "Medium"
        ChallengeDifficulty.HARD -> Color(0xFFFF9800) to "Hard"
        ChallengeDifficulty.EXTREME -> Color(0xFFF44336) to "Extreme"
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ============== DUELS ==============

@Composable
private fun DuelsContent(
    pendingInvitations: List<DuelInvitation>,
    activeDuels: List<Duel>,
    duelHistory: List<Duel>,
    onAcceptDuel: (String) -> Unit,
    onDeclineDuel: (String) -> Unit,
    onShowDuelDetail: (Duel) -> Unit,
    onCreateDuel: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Create duel button
        item {
            Button(
                onClick = onCreateDuel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("⚔️ Challenge a Friend")
            }
        }

        // Pending invitations
        if (pendingInvitations.isNotEmpty()) {
            item {
                Text(
                    text = "Pending Invitations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(pendingInvitations) { invitation ->
                DuelInvitationCard(
                    invitation = invitation,
                    onAccept = { onAcceptDuel(invitation.duelId) },
                    onDecline = { onDeclineDuel(invitation.duelId) }
                )
            }
        }

        // Active duels
        if (activeDuels.isNotEmpty()) {
            item {
                Text(
                    text = "Active Duels",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(activeDuels) { duel ->
                DuelCard(
                    duel = duel,
                    onClick = { onShowDuelDetail(duel) }
                )
            }
        }

        // Empty state
        if (pendingInvitations.isEmpty() && activeDuels.isEmpty()) {
            item {
                EmptyState(
                    emoji = "⚔️",
                    title = "No active duels",
                    subtitle = "Challenge a friend to a duel!"
                )
            }
        }

        // Duel history
        if (duelHistory.isNotEmpty()) {
            item {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(duelHistory.take(5)) { duel ->
                DuelHistoryCard(duel = duel)
            }
        }
    }
}

@Composable
private fun DuelInvitationCard(
    invitation: DuelInvitation,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = invitation.challengerEmoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${invitation.challengerName} challenges you!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${invitation.duration.days} day challenge",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Decline")
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Accept ⚔️")
                }
            }
        }
    }
}

@Composable
private fun DuelCard(
    duel: Duel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // VS Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = duel.challengerEmoji, fontSize = 32.sp)
                    Text(
                        text = duel.challengerName,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${duel.challengerProgress}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "VS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = duel.opponentEmoji, fontSize = 32.sp)
                    Text(
                        text = duel.opponentName,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${duel.opponentProgress}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ends: ${duel.endsAt?.substringBefore("T") ?: "N/A"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun DuelHistoryCard(duel: Duel) {
    val isWin = duel.winnerId == duel.challengerId
    val isTie = duel.winnerId == null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isWin -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                isTie -> MaterialTheme.colorScheme.surfaceVariant
                else -> Color(0xFFF44336).copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    isWin -> "🏆"
                    isTie -> "🤝"
                    else -> "😔"
                },
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "vs ${duel.opponentName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${duel.challengerProgress} - ${duel.opponentProgress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = when {
                    isWin -> "Won!"
                    isTie -> "Tie"
                    else -> "Lost"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    isWin -> Color(0xFF4CAF50)
                    isTie -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> Color(0xFFF44336)
                }
            )
        }
    }
}

// ============== COMMUNITY ==============

@Composable
private fun CommunityContent(
    challenge: CommunityChallenge?,
    userProgress: UserCommunityProgress?,
    leaderboard: List<ChallengeParticipant>,
    onJoin: () -> Unit
) {
    if (challenge == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                emoji = "🌍",
                title = "No active community challenge",
                subtitle = "Check back soon!"
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Challenge header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = challenge.emoji, fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = challenge.description,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Global progress
                    val progress = challenge.globalProgress.toFloat() / challenge.globalTarget
                    Text(
                        text = "${(progress * 100).toInt()}% Complete",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatNumber(challenge.globalProgress)} / ${formatNumber(challenge.globalTarget)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatBox(label = "Participants", value = formatNumber(challenge.totalParticipants.toLong()))
                        StatBox(label = "Reward", value = "+${challenge.rewards.baseXp} XP")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (userProgress == null) {
                        Button(onClick = onJoin) {
                            Text("Join Challenge 🌍")
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Your contribution: ${userProgress.contribution}",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Tiers
        item {
            Text(
                text = "Reward Tiers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(challenge.tiers) { tier ->
            TierCard(
                tier = tier,
                isUnlocked = challenge.globalProgress >= (challenge.globalTarget * tier.threshold).toLong()
            )
        }

        // Leaderboard
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Top Contributors",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        itemsIndexed(leaderboard) { index, participant ->
            LeaderboardRow(rank = index + 1, participant = participant)
        }
    }
}

@Composable
private fun TierCard(tier: CommunityTier, isUnlocked: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked)
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isUnlocked) "✅" else "🔒",
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tier.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(tier.threshold * 100).toInt()}% of goal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "+${tier.xpBonus} XP",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isUnlocked) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, participant: ChallengeParticipant) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "#$rank"
            },
            fontSize = if (rank <= 3) 24.sp else 16.sp,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = participant.avatarEmoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = participant.displayName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${participant.currentValue}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ============== SEASONAL ==============

@Composable
private fun SeasonalContent(
    event: SeasonalEvent?,
    progress: UserSeasonalProgress?,
    onJoin: () -> Unit,
    onClaimReward: (String) -> Unit
) {
    if (event == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                emoji = "🎄",
                title = "No active seasonal event",
                subtitle = "Check back during special occasions!"
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Event header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = event.emoji, fontSize = 48.sp)
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${event.startDate} - ${event.endDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )

                    if (progress == null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onJoin) {
                            Text("Join Event ${event.emoji}")
                        }
                    }
                }
            }
        }

        // Challenges
        item {
            Text(
                text = "Event Challenges",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(event.challenges) { challenge ->
            val isCompleted = progress?.challengesCompleted?.contains(challenge.id) == true
            SeasonalChallengeCard(
                challenge = challenge,
                isCompleted = isCompleted
            )
        }

        // Exclusive rewards
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Exclusive Rewards",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(event.exclusiveRewards) { reward ->
            val isEarned = progress?.rewardsEarned?.contains(reward.id) == true
            SeasonalRewardCard(
                reward = reward,
                isEarned = isEarned,
                onClaim = { onClaimReward(reward.id) }
            )
        }
    }
}

@Composable
private fun SeasonalChallengeCard(challenge: Challenge, isCompleted: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = challenge.emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = challenge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isCompleted) {
                Text(text = "✅", fontSize = 24.sp)
            } else {
                Text(
                    text = "+${challenge.rewards.xp}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SeasonalRewardCard(
    reward: SeasonalReward,
    isEarned: Boolean,
    onClaim: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEarned)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = reward.emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reward.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = reward.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isEarned) {
                Text(text = "🎁", fontSize = 24.sp)
            } else {
                Text(
                    text = "🔒",
                    fontSize = 24.sp
                )
            }
        }
    }
}

// ============== CREATE CHALLENGE ==============

@Composable
private fun CreateChallengeContent(
    customChallenges: List<CustomChallengeTemplate>,
    onCreateChallenge: () -> Unit,
    onDeleteChallenge: (String) -> Unit,
    onShareChallenge: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Button(
                onClick = onCreateChallenge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("✏️ Create New Challenge")
            }
        }

        if (customChallenges.isEmpty()) {
            item {
                EmptyState(
                    emoji = "✏️",
                    title = "No custom challenges",
                    subtitle = "Create your own challenges to share with friends!"
                )
            }
        } else {
            item {
                Text(
                    text = "My Challenges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(customChallenges) { template ->
                CustomChallengeCard(
                    template = template,
                    onDelete = { onDeleteChallenge(template.id) },
                    onShare = { onShareChallenge(template.id) }
                )
            }
        }
    }
}

@Composable
private fun CustomChallengeCard(
    template: CustomChallengeTemplate,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = template.emoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = template.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DifficultyBadge(difficulty = template.difficulty)
                Text(
                    text = "${template.duration.days} days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (template.isPublic) {
                    Text(
                        text = "🌐 Public",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete")
                }
                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Share 📤")
                }
            }
        }
    }
}

// ============== DIALOGS ==============

@Composable
private fun ChallengeDetailDialog(
    challenge: Challenge,
    onDismiss: () -> Unit,
    onJoin: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text(text = challenge.emoji, fontSize = 48.sp) },
        title = {
            Text(
                text = challenge.title,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column {
                Text(text = challenge.description)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Duration", style = MaterialTheme.typography.labelSmall)
                        Text("${challenge.duration.days} days", fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Difficulty", style = MaterialTheme.typography.labelSmall)
                        DifficultyBadge(difficulty = challenge.difficulty)
                    }
                    Column {
                        Text("Reward", style = MaterialTheme.typography.labelSmall)
                        Text("+${challenge.rewards.xp} XP", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onJoin(); onDismiss() }) {
                Text("Join Challenge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DuelDetailDialog(
    duel: Duel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Duel Details") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = duel.challengerEmoji, fontSize = 48.sp)
                        Text(text = duel.challengerName)
                        Text(
                            text = "${duel.challengerProgress}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = duel.opponentEmoji, fontSize = 48.sp)
                        Text(text = duel.opponentName)
                        Text(
                            text = "${duel.opponentProgress}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Status: ${duel.status.name}",
                    style = MaterialTheme.typography.bodyMedium
                )
                duel.endsAt?.let {
                    Text(
                        text = "Ends: ${it.substringBefore("T")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CreateChallengeDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, ChallengeGoal, ChallengeDuration, ChallengeDifficulty) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🎯") }
    var days by remember { mutableStateOf("7") }
    var targetValue by remember { mutableStateOf("10") }
    var difficulty by remember { mutableStateOf(ChallengeDifficulty.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Challenge") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it.take(2) },
                        label = { Text("Emoji") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = days,
                        onValueChange = { days = it.filter { c -> c.isDigit() } },
                        label = { Text("Days") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = targetValue,
                    onValueChange = { targetValue = it.filter { c -> c.isDigit() } },
                    label = { Text("Target (habits to complete)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Difficulty", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ChallengeDifficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = { difficulty = diff },
                            label = {
                                Text(
                                    text = when (diff) {
                                        ChallengeDifficulty.EASY -> "Easy"
                                        ChallengeDifficulty.MEDIUM -> "Med"
                                        ChallengeDifficulty.HARD -> "Hard"
                                        ChallengeDifficulty.EXTREME -> "Extreme"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        onCreate(
                            title,
                            description,
                            emoji,
                            ChallengeGoal.TotalHabits(targetValue.toIntOrNull() ?: 10),
                            ChallengeDuration(days.toIntOrNull() ?: 7),
                            difficulty
                        )
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RewardClaimedDialog(
    rewards: ChallengeRewards,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text(text = "🎉", fontSize = 48.sp) },
        title = {
            Text(
                text = "Challenge Complete!",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "+${rewards.xp} XP",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (rewards.streakShields > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "+${rewards.streakShields} 🛡️ Streak Shield(s)")
                }
                rewards.badge?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "🏆 New Badge Unlocked!")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Awesome!")
            }
        }
    )
}

// ============== HELPERS ==============

@Composable
private fun EmptyState(emoji: String, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 48.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

private fun formatNumber(num: Long): String {
    return when {
        num >= 1_000_000 -> "${num / 1_000_000}M"
        num >= 1_000 -> "${num / 1_000}K"
        else -> num.toString()
    }
}
