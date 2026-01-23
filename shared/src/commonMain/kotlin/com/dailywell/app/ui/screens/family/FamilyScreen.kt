package com.dailywell.app.ui.screens.family

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.data.model.*
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.core.theme.PrimaryLight
import com.dailywell.app.core.theme.Secondary
import com.dailywell.app.core.theme.SecondaryLight
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    onBack: () -> Unit,
    isPremium: Boolean = true,
    viewModel: FamilyViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Family Plan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "←", fontSize = 24.sp)
                    }
                },
                actions = {
                    if (uiState.familyData.familyId != null) {
                        IconButton(onClick = { viewModel.showInviteDialog(true) }) {
                            Text("➕", fontSize = 20.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Secondary)
            }
        } else if (uiState.familyData.familyId == null) {
            // No family - show create/join options
            NoFamilyScreen(
                onCreateFamily = { viewModel.createFamily("My Family") },
                onJoinFamily = { viewModel.showJoinDialog(true) }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tab selector
                TabRow(
                    selectedTabIndex = FamilyTab.entries.indexOf(uiState.selectedTab),
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    FamilyTab.entries.forEach { tab ->
                        Tab(
                            selected = uiState.selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(tab.emoji)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tab.title, fontSize = 12.sp)
                                }
                            }
                        )
                    }
                }

                // Content based on selected tab
                AnimatedContent(
                    targetState = uiState.selectedTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    }
                ) { tab ->
                    when (tab) {
                        FamilyTab.OVERVIEW -> OverviewTab(
                            familyData = uiState.familyData,
                            viewModel = viewModel
                        )
                        FamilyTab.CHALLENGES -> ChallengesTab(
                            challenges = uiState.familyData.sharedChallenges,
                            members = uiState.familyData.members,
                            viewModel = viewModel,
                            onCreateChallenge = { viewModel.showCreateChallengeDialog(true) }
                        )
                        FamilyTab.ACTIVITY -> ActivityTab(
                            activityFeed = uiState.activityFeed,
                            onHighFive = { viewModel.sendHighFive(it) }
                        )
                        FamilyTab.SETTINGS -> SettingsTab(
                            familyData = uiState.familyData,
                            viewModel = viewModel,
                            onLeaveFamily = { viewModel.leaveFamily() }
                        )
                    }
                }
            }
        }

        // Dialogs
        if (uiState.showInviteDialog) {
            InviteDialog(
                inviteCode = uiState.familyData.inviteCode ?: "",
                onRegenerateCode = { viewModel.regenerateInviteCode() },
                onDismiss = { viewModel.showInviteDialog(false) }
            )
        }

        if (uiState.showJoinDialog) {
            JoinFamilyDialog(
                code = uiState.joinCode,
                onCodeChange = { viewModel.updateJoinCode(it) },
                onJoin = { viewModel.joinFamily() },
                onDismiss = { viewModel.showJoinDialog(false) }
            )
        }

        if (uiState.showCreateChallengeDialog) {
            CreateChallengeDialog(
                onCreateFromTemplate = { viewModel.createTemplateChallenge(it) },
                onDismiss = { viewModel.showCreateChallengeDialog(false) }
            )
        }
    }
}

@Composable
private fun NoFamilyScreen(
    onCreateFamily: () -> Unit,
    onJoinFamily: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("👨‍👩‍👧‍👦", fontSize = 72.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Family Plan",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Share premium features with up to 6 family members",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onCreateFamily,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Secondary)
        ) {
            Text("Create Family", modifier = Modifier.padding(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onJoinFamily,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Join with Invite Code", modifier = Modifier.padding(8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PrimaryLight.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Benefits include:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                BenefitItem("✅", "Up to 6 members")
                BenefitItem("✅", "Family challenges")
                BenefitItem("✅", "Shared milestones")
                BenefitItem("✅", "Activity feed")
                BenefitItem("✅", "$8.33/person/year")
            }
        }
    }
}

@Composable
private fun BenefitItem(emoji: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun OverviewTab(
    familyData: FamilyPlanData,
    viewModel: FamilyViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Family members
        item {
            Text(
                text = "Family Members (${familyData.members.size}/${familyData.maxMembers})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(familyData.members) { member ->
            FamilyMemberCard(member = member)
        }

        // Active challenge
        val activeChallenge = familyData.sharedChallenges.firstOrNull {
            it.status == ChallengeStatus.ACTIVE
        }

        if (activeChallenge != null) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Active Challenge",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                ActiveChallengeCard(
                    challenge = activeChallenge,
                    members = familyData.members,
                    viewModel = viewModel
                )
            }
        }

        // Recent milestones
        if (familyData.sharedMilestones.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recent Milestones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(familyData.sharedMilestones.take(3)) { milestone ->
                MilestoneCard(milestone = milestone)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FamilyMemberCard(member: FamilyMember) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryLight.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(member.avatar, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (member.role == FamilyRole.OWNER) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "👑",
                            fontSize = 14.sp
                        )
                    }
                }
                Text(
                    text = "${member.todayCompletedHabits} habits today • ${member.currentStreak} day streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Weekly score
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${member.weeklyScore}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        member.weeklyScore >= 80 -> Secondary
                        member.weeklyScore >= 60 -> Color(0xFFFFB74D)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = "weekly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActiveChallengeCard(
    challenge: FamilyChallenge,
    members: List<FamilyMember>,
    viewModel: FamilyViewModel
) {
    val totalProgress = viewModel.getTotalChallengeProgress(challenge)
    val progressPercent = viewModel.getChallengeProgressPercent(challenge)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(challenge.emoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = challenge.type.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$totalProgress / ${challenge.targetValue}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(progressPercent * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Secondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Secondary,
                    trackColor = Color.White
                )
            }

            // Member contributions
            if (challenge.type == FamilyChallengeType.COLLABORATIVE) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(members) { member ->
                        val contribution = challenge.currentProgress[member.id] ?: 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(member.avatar, fontSize = 20.sp)
                            Text(
                                text = "$contribution",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Reward
            challenge.reward?.let { reward ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "🎁 Reward: $reward",
                    style = MaterialTheme.typography.bodySmall,
                    color = Secondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MilestoneCard(milestone: FamilyMilestone) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(milestone.emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = milestone.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = milestone.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChallengesTab(
    challenges: List<FamilyChallenge>,
    members: List<FamilyMember>,
    viewModel: FamilyViewModel,
    onCreateChallenge: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Button(
                onClick = onCreateChallenge,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Secondary)
            ) {
                Text("🏆 Create New Challenge")
            }
        }

        val activeChallenges = challenges.filter { it.status == ChallengeStatus.ACTIVE }
        val completedChallenges = challenges.filter { it.status == ChallengeStatus.COMPLETED }

        if (activeChallenges.isNotEmpty()) {
            item {
                Text(
                    text = "Active Challenges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(activeChallenges) { challenge ->
                ActiveChallengeCard(
                    challenge = challenge,
                    members = members,
                    viewModel = viewModel
                )
            }
        }

        if (completedChallenges.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Completed Challenges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(completedChallenges.take(5)) { challenge ->
                CompletedChallengeCard(
                    challenge = challenge,
                    members = members
                )
            }
        }

        if (challenges.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "🏆",
                    title = "No Challenges Yet",
                    description = "Create a challenge to motivate your family!"
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CompletedChallengeCard(
    challenge: FamilyChallenge,
    members: List<FamilyMember>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(challenge.emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (challenge.winnerId != null) {
                    val winner = members.find { it.id == challenge.winnerId }
                    Text(
                        text = "🏆 Winner: ${winner?.name ?: "Unknown"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Secondary
                    )
                } else {
                    Text(
                        text = "✅ Completed!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityTab(
    activityFeed: List<FamilyActivity>,
    onHighFive: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (activityFeed.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "📱",
                    title = "No Activity Yet",
                    description = "Family activity will appear here when members complete habits."
                )
            }
        } else {
            items(activityFeed) { activity ->
                ActivityCard(
                    activity = activity,
                    onHighFive = { onHighFive(activity.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActivityCard(
    activity: FamilyActivity,
    onHighFive: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryLight.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(activity.memberAvatar, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        text = activity.memberName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " ${activity.message}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = activity.timestamp.substringBefore("T"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // High five button
            if (activity.canHighFive) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onHighFive) {
                        Text(
                            text = if (activity.highFives.isEmpty()) "✋" else "🙌",
                            fontSize = 24.sp
                        )
                    }
                    if (activity.highFives.isNotEmpty()) {
                        Text(
                            text = "${activity.highFives.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    familyData: FamilyPlanData,
    viewModel: FamilyViewModel,
    onLeaveFamily: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Invite code
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Invite Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = familyData.inviteCode ?: "---",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Secondary
                        )
                        TextButton(onClick = { viewModel.regenerateInviteCode() }) {
                            Text("Regenerate")
                        }
                    }
                    Text(
                        text = "Share this code with family members to let them join.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Members management (for owner)
        if (familyData.isOwner) {
            item {
                Text(
                    text = "Manage Members",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(familyData.members.filter { it.role != FamilyRole.OWNER }) { member ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(member.avatar, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = member.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = member.role.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(
                            onClick = { viewModel.removeMember(member.id) },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE57373))
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
        }

        // Leave family
        item {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onLeaveFamily,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373))
            ) {
                Text(if (familyData.isOwner) "Delete Family" else "Leave Family")
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EmptyStateCard(
    emoji: String,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Dialogs

@Composable
private fun InviteDialog(
    inviteCode: String,
    onRegenerateCode: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Family Members") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Share this code:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = inviteCode,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Secondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Family members can enter this code to join your family plan and share premium features.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onRegenerateCode) {
                Text("New Code")
            }
        }
    )
}

@Composable
private fun JoinFamilyDialog(
    code: String,
    onCodeChange: (String) -> Unit,
    onJoin: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Family") },
        text = {
            Column {
                Text(
                    text = "Enter the invite code shared by your family member:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) onCodeChange(it) },
                    label = { Text("Invite Code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onJoin,
                enabled = code.length == 6
            ) {
                Text("Join")
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
private fun CreateChallengeDialog(
    onCreateFromTemplate: (ChallengeTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Challenge") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Choose a challenge template:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                ChallengeTemplateItem(
                    emoji = "👨‍👩‍👧‍👦",
                    title = "Family Streak Week",
                    description = "Everyone completes all habits daily",
                    onClick = { onCreateFromTemplate(ChallengeTemplate.FAMILY_STREAK) }
                )

                ChallengeTemplateItem(
                    emoji = "💧",
                    title = "Team Hydration",
                    description = "Collective water drinking goal",
                    onClick = { onCreateFromTemplate(ChallengeTemplate.HYDRATION) }
                )

                ChallengeTemplateItem(
                    emoji = "👟",
                    title = "Step Competition",
                    description = "Who gets the most steps?",
                    onClick = { onCreateFromTemplate(ChallengeTemplate.STEP_CHALLENGE) }
                )

                ChallengeTemplateItem(
                    emoji = "📵",
                    title = "Screen-Free Evenings",
                    description = "No screens after 8pm",
                    onClick = { onCreateFromTemplate(ChallengeTemplate.SCREEN_FREE) }
                )

                ChallengeTemplateItem(
                    emoji = "🧘",
                    title = "Mindfulness Marathon",
                    description = "Longest mindfulness streak wins",
                    onClick = { onCreateFromTemplate(ChallengeTemplate.MINDFULNESS) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ChallengeTemplateItem(
    emoji: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
