package com.dailywell.app.ui.screens.leaderboard

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.*
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onBack: () -> Unit
) {
    val viewModel: LeaderboardViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Social & Rankings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Friend requests badge
                    if (uiState.friendRequests.isNotEmpty()) {
                        BadgedBox(
                            badge = {
                                Badge { Text("${uiState.friendRequests.size}") }
                            }
                        ) {
                            IconButton(onClick = { viewModel.showAddFriendDialog() }) {
                                Text("👥", fontSize = 20.sp)
                            }
                        }
                    } else {
                        IconButton(onClick = { viewModel.showAddFriendDialog() }) {
                            Text("➕👤", fontSize = 18.sp)
                        }
                    }

                    // Unread cheers badge
                    if (uiState.unreadCheersCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge { Text("${uiState.unreadCheersCount}") }
                            }
                        ) {
                            IconButton(onClick = { viewModel.selectTab(LeaderboardTab.CHEERS) }) {
                                Text("🎉", fontSize = 20.sp)
                            }
                        }
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
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                edgePadding = 16.dp,
                divider = { }
            ) {
                LeaderboardTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = when (tab) {
                                        LeaderboardTab.FRIENDS -> "👥"
                                        LeaderboardTab.GLOBAL -> "🌍"
                                        LeaderboardTab.HABITS -> "📊"
                                        LeaderboardTab.ACTIVITY -> "📰"
                                        LeaderboardTab.CHEERS -> "🎉"
                                        LeaderboardTab.REFERRALS -> "🎁"
                                    },
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = when (tab) {
                                        LeaderboardTab.FRIENDS -> "Friends"
                                        LeaderboardTab.GLOBAL -> "Global"
                                        LeaderboardTab.HABITS -> "Habits"
                                        LeaderboardTab.ACTIVITY -> "Activity"
                                        LeaderboardTab.CHEERS -> "Cheers"
                                        LeaderboardTab.REFERRALS -> "Referrals"
                                    }
                                )
                            }
                        }
                    )
                }
            }

            // Content
            when (uiState.selectedTab) {
                LeaderboardTab.FRIENDS -> FriendsLeaderboardContent(
                    entries = uiState.friendsLeaderboard,
                    currentUserRank = uiState.currentUserRank,
                    selectedMetric = uiState.selectedMetric,
                    onMetricChange = { viewModel.selectMetric(it) },
                    onSendCheer = { userId, userName -> viewModel.showCheerDialog(userId, userName) },
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refreshLeaderboards() }
                )
                LeaderboardTab.GLOBAL -> GlobalLeaderboardContent(
                    entries = uiState.globalLeaderboard,
                    currentUserRank = uiState.currentUserRank,
                    selectedMetric = uiState.selectedMetric,
                    onMetricChange = { viewModel.selectMetric(it) },
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refreshLeaderboards() }
                )
                LeaderboardTab.HABITS -> HabitLeaderboardContent(
                    entries = uiState.habitLeaderboard,
                    selectedHabitType = uiState.selectedHabitType,
                    onHabitTypeChange = { viewModel.selectHabitType(it) }
                )
                LeaderboardTab.ACTIVITY -> ActivityFeedContent(
                    activityFeed = if (uiState.showFriendsOnlyFeed) uiState.friendsActivityFeed else uiState.activityFeed,
                    showFriendsOnly = uiState.showFriendsOnlyFeed,
                    onToggleFriendsOnly = { viewModel.toggleFriendsOnlyFeed() },
                    onReact = { activityId, reactionType -> viewModel.addReaction(activityId, reactionType) },
                    onSendCheer = { userId, userName -> viewModel.showCheerDialog(userId, userName) },
                    onHide = { viewModel.hideActivityItem(it) }
                )
                LeaderboardTab.CHEERS -> CheersContent(
                    receivedCheers = uiState.receivedCheers,
                    sentCheers = uiState.sentCheers,
                    onMarkAsRead = { viewModel.markCheerAsRead(it) },
                    onMarkAllAsRead = { viewModel.markAllCheersAsRead() },
                    onSendCheer = { userId, userName -> viewModel.showCheerDialog(userId, userName) },
                    friends = uiState.friends
                )
                LeaderboardTab.REFERRALS -> ReferralsContent(
                    referralCode = uiState.referralCode,
                    referralStats = uiState.referralStats,
                    onGenerateCode = { viewModel.generateReferralCode() },
                    onApplyCode = { viewModel.applyReferralCode(it) },
                    error = uiState.error,
                    onClearError = { viewModel.clearError() }
                )
            }
        }

        // Dialogs
        if (uiState.showCheerDialog && uiState.selectedUserForCheer != null) {
            SendCheerDialog(
                userName = uiState.selectedUserForCheer!!.second,
                onDismiss = { viewModel.dismissCheerDialog() },
                onSendCheer = { cheerType, message -> viewModel.sendCheer(cheerType, message) }
            )
        }

        if (uiState.showAddFriendDialog) {
            AddFriendDialog(
                searchQuery = uiState.searchQuery,
                searchResults = uiState.searchResults,
                friendRequests = uiState.friendRequests,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onSendRequest = { userId, userName -> viewModel.sendFriendRequest(userId, userName) },
                onAcceptRequest = { viewModel.acceptFriendRequest(it) },
                onDeclineRequest = { viewModel.declineFriendRequest(it) },
                onDismiss = { viewModel.dismissAddFriendDialog() }
            )
        }
    }
}

// ============== FRIENDS LEADERBOARD ==============

@Composable
private fun FriendsLeaderboardContent(
    entries: List<LeaderboardEntry>,
    currentUserRank: LeaderboardEntry?,
    selectedMetric: LeaderboardMetric,
    onMetricChange: (LeaderboardMetric) -> Unit,
    onSendCheer: (String, String) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Metric Selector
        MetricSelector(selectedMetric = selectedMetric, onMetricChange = onMetricChange)

        if (entries.isEmpty()) {
            EmptyLeaderboardState(
                emoji = "👥",
                title = "No Friends Yet",
                subtitle = "Add friends to see how you compare!"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Current user highlight
                currentUserRank?.let { rank ->
                    item {
                        CurrentUserRankCard(rank = rank, metricLabel = selectedMetric.displayName())
                    }
                }

                items(entries) { entry ->
                    LeaderboardEntryCard(
                        entry = entry,
                        metricLabel = selectedMetric.displayName(),
                        showCheerButton = !entry.isCurrentUser,
                        onSendCheer = { onSendCheer(entry.userId, entry.displayName) }
                    )
                }
            }
        }
    }
}

// ============== GLOBAL LEADERBOARD ==============

@Composable
private fun GlobalLeaderboardContent(
    entries: List<LeaderboardEntry>,
    currentUserRank: LeaderboardEntry?,
    selectedMetric: LeaderboardMetric,
    onMetricChange: (LeaderboardMetric) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MetricSelector(selectedMetric = selectedMetric, onMetricChange = onMetricChange)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top 3 podium
            if (entries.size >= 3) {
                item {
                    PodiumSection(
                        first = entries[0],
                        second = entries[1],
                        third = entries[2]
                    )
                }
            }

            // Current user rank if not in top 3
            currentUserRank?.let { rank ->
                if (rank.rank > 3) {
                    item {
                        CurrentUserRankCard(rank = rank, metricLabel = selectedMetric.displayName())
                    }
                }
            }

            // Rest of leaderboard
            items(entries.drop(3)) { entry ->
                LeaderboardEntryCard(
                    entry = entry,
                    metricLabel = selectedMetric.displayName(),
                    showCheerButton = false,
                    onSendCheer = {}
                )
            }
        }
    }
}

@Composable
private fun PodiumSection(
    first: LeaderboardEntry,
    second: LeaderboardEntry,
    third: LeaderboardEntry
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // 2nd place
            PodiumEntry(entry = second, rank = 2, height = 80.dp)

            // 1st place
            PodiumEntry(entry = first, rank = 1, height = 100.dp)

            // 3rd place
            PodiumEntry(entry = third, rank = 3, height = 60.dp)
        }
    }
}

@Composable
private fun PodiumEntry(
    entry: LeaderboardEntry,
    rank: Int,
    height: androidx.compose.ui.unit.Dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Text(
            text = entry.avatarEmoji,
            fontSize = 32.sp
        )
        Text(
            text = entry.displayName,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${entry.score}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Podium stand
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(
                    when (rank) {
                        1 -> Color(0xFFFFD700) // Gold
                        2 -> Color(0xFFC0C0C0) // Silver
                        else -> Color(0xFFCD7F32) // Bronze
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    else -> "🥉"
                },
                fontSize = 24.sp
            )
        }
    }
}

// ============== HABIT LEADERBOARDS ==============

@Composable
private fun HabitLeaderboardContent(
    entries: List<LeaderboardEntry>,
    selectedHabitType: String,
    onHabitTypeChange: (String) -> Unit
) {
    val habitTypes = listOf(
        "sleep" to "😴 Sleep",
        "water" to "💧 Water",
        "exercise" to "🏃 Exercise",
        "meditation" to "🧘 Meditation",
        "reading" to "📚 Reading"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Habit type selector
        ScrollableTabRow(
            selectedTabIndex = habitTypes.indexOfFirst { it.first == selectedHabitType }.coerceAtLeast(0),
            edgePadding = 16.dp,
            divider = { }
        ) {
            habitTypes.forEach { (type, label) ->
                Tab(
                    selected = selectedHabitType == type,
                    onClick = { onHabitTypeChange(type) },
                    text = { Text(label) }
                )
            }
        }

        if (entries.isEmpty()) {
            EmptyLeaderboardState(
                emoji = "📊",
                title = "No Rankings Yet",
                subtitle = "Complete this habit to appear on the leaderboard!"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries) { entry ->
                    LeaderboardEntryCard(
                        entry = entry,
                        metricLabel = "completions",
                        showCheerButton = false,
                        onSendCheer = {}
                    )
                }
            }
        }
    }
}

// ============== ACTIVITY FEED ==============

@Composable
private fun ActivityFeedContent(
    activityFeed: List<ActivityFeedItem>,
    showFriendsOnly: Boolean,
    onToggleFriendsOnly: () -> Unit,
    onReact: (String, ReactionType) -> Unit,
    onSendCheer: (String, String) -> Unit,
    onHide: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Filter toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showFriendsOnly) "Friends Activity" else "Global Activity",
                style = MaterialTheme.typography.titleMedium
            )
            FilterChip(
                selected = showFriendsOnly,
                onClick = onToggleFriendsOnly,
                label = { Text("Friends Only") },
                leadingIcon = if (showFriendsOnly) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null
            )
        }

        if (activityFeed.isEmpty()) {
            EmptyLeaderboardState(
                emoji = "📰",
                title = "No Activity Yet",
                subtitle = "Complete habits to see activity here!"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activityFeed) { item ->
                    ActivityFeedCard(
                        item = item,
                        onReact = { reactionType -> onReact(item.id, reactionType) },
                        onSendCheer = { onSendCheer(item.userId, item.userName) },
                        onHide = { onHide(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityFeedCard(
    item: ActivityFeedItem,
    onReact: (ReactionType) -> Unit,
    onSendCheer: () -> Unit,
    onHide: () -> Unit
) {
    var showReactionPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = item.userEmoji, fontSize = 28.sp)
                    Column {
                        Text(
                            text = item.userName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.timestamp,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Activity type icon
                Text(
                    text = when (item.type) {
                        ActivityType.HABIT_COMPLETED -> "✅"
                        ActivityType.STREAK_MILESTONE -> "🔥"
                        ActivityType.LEVEL_UP -> "⬆️"
                        ActivityType.BADGE_EARNED -> "🏆"
                        ActivityType.CHALLENGE_COMPLETED -> "🎯"
                        ActivityType.CHALLENGE_JOINED -> "🤝"
                        ActivityType.PERFECT_DAY -> "⭐"
                        ActivityType.FRIEND_JOINED -> "👋"
                        ActivityType.DUEL_WON -> "🏅"
                        ActivityType.DUEL_STARTED -> "⚔️"
                        ActivityType.COMEBACK -> "🔄"
                    },
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content
            Text(
                text = getActivityDescription(item),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Reactions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Existing reactions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.reactions.groupBy { it.type }.forEach { (type, reactions) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { onReact(type) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = type.emoji, fontSize = 14.sp)
                                if (reactions.size > 1) {
                                    Text(
                                        text = " ${reactions.size}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Add reaction button
                    Box {
                        IconButton(
                            onClick = { showReactionPicker = !showReactionPicker },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("😊", fontSize = 18.sp)
                        }

                        DropdownMenu(
                            expanded = showReactionPicker,
                            onDismissRequest = { showReactionPicker = false }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ReactionType.entries.forEach { reaction ->
                                    TextButton(
                                        onClick = {
                                            onReact(reaction)
                                            showReactionPicker = false
                                        }
                                    ) {
                                        Text(reaction.emoji, fontSize = 20.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Send cheer button
                    IconButton(
                        onClick = onSendCheer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("🎉", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

private fun getActivityDescription(item: ActivityFeedItem): String {
    return when (val content = item.content) {
        is ActivityContent.HabitCompleted -> "${item.userName} completed ${content.habitName}"
        is ActivityContent.StreakMilestone -> "${item.userName} reached a ${content.days}-day streak! 🔥"
        is ActivityContent.LevelUp -> "${item.userName} leveled up to Level ${content.newLevel}! ⬆️"
        is ActivityContent.BadgeEarned -> "${item.userName} unlocked: ${content.badgeName}"
        is ActivityContent.ChallengeCompleted -> "${item.userName} completed the \"${content.challengeName}\" challenge!"
        is ActivityContent.ChallengeJoined -> "${item.userName} joined the \"${content.challengeName}\" challenge"
        is ActivityContent.PerfectDay -> "${item.userName} achieved a perfect day! (Day ${content.dayNumber}) ⭐"
        is ActivityContent.FriendJoined -> "${item.userName} became friends with ${content.friendName}"
        is ActivityContent.DuelWon -> "${item.userName} won a duel against ${content.opponentName}! 🏆"
        is ActivityContent.DuelStarted -> "${item.userName} started a duel with ${content.opponentName}"
        is ActivityContent.Comeback -> "${item.userName} is back after ${content.daysAway} days! 🔄"
    }
}

// ============== CHEERS ==============

@Composable
private fun CheersContent(
    receivedCheers: List<Cheer>,
    sentCheers: List<Cheer>,
    onMarkAsRead: (String) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onSendCheer: (String, String) -> Unit,
    friends: List<Friend>
) {
    var showReceivedTab by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = showReceivedTab,
                onClick = { showReceivedTab = true },
                label = { Text("Received (${receivedCheers.size})") }
            )
            FilterChip(
                selected = !showReceivedTab,
                onClick = { showReceivedTab = false },
                label = { Text("Sent (${sentCheers.size})") }
            )

            Spacer(modifier = Modifier.weight(1f))

            if (showReceivedTab && receivedCheers.any { !it.isRead }) {
                TextButton(onClick = onMarkAllAsRead) {
                    Text("Mark All Read")
                }
            }
        }

        val cheers = if (showReceivedTab) receivedCheers else sentCheers

        if (cheers.isEmpty()) {
            EmptyLeaderboardState(
                emoji = "🎉",
                title = if (showReceivedTab) "No Cheers Yet" else "No Cheers Sent",
                subtitle = if (showReceivedTab) "Complete habits to receive cheers from friends!" else "Send cheers to encourage your friends!"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cheers) { cheer ->
                    CheerCard(
                        cheer = cheer,
                        isReceived = showReceivedTab,
                        onMarkAsRead = { onMarkAsRead(cheer.id) }
                    )
                }
            }
        }

        // Quick send to friends
        if (!showReceivedTab && friends.isNotEmpty()) {
            Divider()
            Text(
                text = "Quick Send",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(16.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(friends) { friend ->
                    ElevatedAssistChip(
                        onClick = { onSendCheer(friend.odUserId, friend.displayName) },
                        label = { Text(friend.displayName) },
                        leadingIcon = { Text(friend.avatarEmoji) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CheerCard(
    cheer: Cheer,
    isReceived: Boolean,
    onMarkAsRead: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isReceived && !cheer.isRead) {
                    Modifier.clickable { onMarkAsRead() }
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isReceived && !cheer.isRead) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cheer type emoji
            Text(
                text = cheer.cheerType.emoji,
                fontSize = 32.sp
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isReceived) {
                        "From ${cheer.fromUserName}"
                    } else {
                        "To someone"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (cheer.message.isNotEmpty()) {
                    Text(
                        text = "\"${cheer.message}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }

                Text(
                    text = cheer.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isReceived && !cheer.isRead) {
                Badge { }
            }
        }
    }
}

// ============== REFERRALS ==============

@Composable
private fun ReferralsContent(
    referralCode: ReferralCode?,
    referralStats: ReferralStats?,
    onGenerateCode: () -> Unit,
    onApplyCode: (String) -> Unit,
    error: String?,
    onClearError: () -> Unit
) {
    var codeToApply by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Your referral code
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎁", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your Referral Code",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (referralCode != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = referralCode.code,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Share this code with friends!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            FilledTonalButton(onClick = { /* Copy to clipboard */ }) {
                                Text("📋", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy")
                            }
                            FilledTonalButton(onClick = { /* Share */ }) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share")
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onGenerateCode) {
                            Text("Generate Code")
                        }
                    }
                }
            }
        }

        // Referral stats
        referralStats?.let { stats ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Your Referral Stats",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                value = stats.totalReferrals.toString(),
                                label = "Total\nReferrals"
                            )
                            StatItem(
                                value = stats.successfulReferrals.toString(),
                                label = "Active\nUsers"
                            )
                            StatItem(
                                value = "${stats.totalXpEarned}",
                                label = "XP\nEarned"
                            )
                        }

                        if (stats.currentTier != ReferralTier.BRONZE) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (stats.currentTier) {
                                        ReferralTier.BRONZE -> "🥉"
                                        ReferralTier.SILVER -> "🥈"
                                        ReferralTier.GOLD -> "🥇"
                                        ReferralTier.PLATINUM -> "💎"
                                        ReferralTier.DIAMOND -> "👑"
                                    },
                                    fontSize = 24.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${stats.currentTier.name} Referrer",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Apply a code
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Have a Referral Code?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = codeToApply,
                        onValueChange = { codeToApply = it.uppercase() },
                        label = { Text("Enter code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = error != null
                    )

                    if (error != null) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            onApplyCode(codeToApply)
                            codeToApply = ""
                        },
                        enabled = codeToApply.length >= 6,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Code")
                    }
                }
            }
        }

        // Rewards info
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Referral Rewards",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    RewardInfoRow(emoji = "🎁", text = "Both you and your friend get 500 XP")
                    RewardInfoRow(emoji = "🛡️", text = "Both get 1 Streak Shield")
                    RewardInfoRow(emoji = "⭐", text = "Unlock exclusive referrer badges")
                    RewardInfoRow(emoji = "💎", text = "Refer 10+ friends for Premium trial")
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
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RewardInfoRow(emoji: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 20.sp)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

// ============== COMMON COMPONENTS ==============

@Composable
private fun MetricSelector(
    selectedMetric: LeaderboardMetric,
    onMetricChange: (LeaderboardMetric) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = LeaderboardMetric.entries.indexOf(selectedMetric),
        edgePadding = 16.dp,
        divider = { }
    ) {
        LeaderboardMetric.entries.forEach { metric ->
            FilterChip(
                selected = selectedMetric == metric,
                onClick = { onMetricChange(metric) },
                label = { Text(metric.displayName()) },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

private fun LeaderboardMetric.displayName(): String = when (this) {
    LeaderboardMetric.TOTAL_XP -> "Total XP"
    LeaderboardMetric.WEEKLY_XP -> "Weekly XP"
    LeaderboardMetric.MONTHLY_XP -> "Monthly XP"
    LeaderboardMetric.CURRENT_STREAK -> "Streak"
    LeaderboardMetric.LONGEST_STREAK -> "Best Streak"
    LeaderboardMetric.PERFECT_DAYS -> "Perfect Days"
    LeaderboardMetric.HABITS_COMPLETED -> "Habits"
    LeaderboardMetric.CHALLENGES_WON -> "Challenges"
    LeaderboardMetric.DUELS_WON -> "Duels Won"
}

@Composable
private fun CurrentUserRankCard(
    rank: LeaderboardEntry,
    metricLabel: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "#${rank.rank}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(rank.avatarEmoji, fontSize = 32.sp)
                Column {
                    Text(
                        text = "You",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Level ${rank.level}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${rank.score}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = metricLabel,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun LeaderboardEntryCard(
    entry: LeaderboardEntry,
    metricLabel: String,
    showCheerButton: Boolean,
    onSendCheer: () -> Unit
) {
    val rankColor = when (entry.rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isCurrentUser) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rank
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(rankColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (entry.rank <= 3) {
                            when (entry.rank) {
                                1 -> "🥇"
                                2 -> "🥈"
                                else -> "🥉"
                            }
                        } else {
                            "#${entry.rank}"
                        },
                        style = if (entry.rank <= 3) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.labelMedium
                        },
                        fontWeight = FontWeight.Bold,
                        color = rankColor
                    )
                }

                // Avatar
                Text(entry.avatarEmoji, fontSize = 28.sp)

                // Name and level
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = entry.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (entry.isFriend) {
                            Text("👥", fontSize = 12.sp)
                        }
                    }
                    Text(
                        text = "Lvl ${entry.level} • ${entry.streak}🔥",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Rank change indicator
                entry.previousRank?.let { prevRank ->
                    val change = prevRank - entry.rank
                    if (change != 0) {
                        Text(
                            text = if (change > 0) "↑$change" else "↓${-change}",
                            color = if (change > 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Score
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${entry.score}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Cheer button
                if (showCheerButton) {
                    IconButton(
                        onClick = onSendCheer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("🎉", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLeaderboardState(
    emoji: String,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============== DIALOGS ==============

@Composable
private fun SendCheerDialog(
    userName: String,
    onDismiss: () -> Unit,
    onSendCheer: (CheerType, String?) -> Unit
) {
    var selectedType by remember { mutableStateOf(CheerType.ENCOURAGEMENT) }
    var message by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Send Cheer to $userName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Choose a cheer type:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Cheer type grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(CheerType.entries.toList()) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Text(type.emoji, fontSize = 24.sp)
                                    Text(
                                        text = type.name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " "),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            },
                            modifier = Modifier.height(70.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Optional message
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Add a message (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSendCheer(selectedType, message.ifBlank { null })
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Send ${selectedType.emoji}")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFriendDialog(
    searchQuery: String,
    searchResults: List<UserSearchResult>,
    friendRequests: List<FriendRequest>,
    onSearchQueryChange: (String) -> Unit,
    onSendRequest: (String, String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit,
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
                    text = "Friends",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Pending requests
                if (friendRequests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Friend Requests",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    friendRequests.forEach { request ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(request.fromUserEmoji, fontSize = 24.sp)
                                    Text(
                                        text = request.fromUserName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row {
                                    IconButton(onClick = { onAcceptRequest(request.id) }) {
                                        Text("✅", fontSize = 20.sp)
                                    }
                                    IconButton(onClick = { onDeclineRequest(request.id) }) {
                                        Text("❌", fontSize = 20.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Search users") },
                    placeholder = { Text("Enter username...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Search results
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(searchResults) { result ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(result.avatarEmoji, fontSize = 28.sp)
                                    Column {
                                        Text(
                                            text = result.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Level ${result.level}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                when {
                                    result.isFriend -> {
                                        AssistChip(
                                            onClick = { },
                                            label = { Text("Friend") },
                                            leadingIcon = { Text("✓") }
                                        )
                                    }
                                    result.hasPendingRequest -> {
                                        AssistChip(
                                            onClick = { },
                                            label = { Text("Pending") }
                                        )
                                    }
                                    else -> {
                                        FilledTonalButton(
                                            onClick = { onSendRequest(result.userId, result.displayName) }
                                        ) {
                                            Text("Add")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
