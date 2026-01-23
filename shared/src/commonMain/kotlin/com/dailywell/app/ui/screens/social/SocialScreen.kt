package com.dailywell.app.ui.screens.social

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
fun SocialScreen(
    onBack: () -> Unit,
    viewModel: SocialViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showCreateContractDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "←", fontSize = 24.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            when (uiState.selectedTab) {
                SocialTab.GROUPS -> FloatingActionButton(
                    onClick = { showCreateGroupDialog = true },
                    containerColor = Secondary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Group")
                }
                SocialTab.CONTRACTS -> FloatingActionButton(
                    onClick = { showCreateContractDialog = true },
                    containerColor = Secondary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Contract")
                }
                else -> {}
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Privacy Notice
            PrivacyNoticeCard()

            // Tab Row
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                SocialTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (tab) {
                                        SocialTab.GROUPS -> "Groups"
                                        SocialTab.PARTNERS -> "Partners"
                                        SocialTab.HIGH_FIVES -> "High Fives"
                                        SocialTab.CONTRACTS -> "Contracts"
                                    }
                                )
                                if (tab == SocialTab.HIGH_FIVES && uiState.unreadHighFiveCount > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Badge(
                                        containerColor = Primary
                                    ) {
                                        Text("${uiState.unreadHighFiveCount}")
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Tab Content
            AnimatedContent(
                targetState = uiState.selectedTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }
            ) { tab ->
                when (tab) {
                    SocialTab.GROUPS -> GroupsContent(
                        groups = uiState.groups,
                        onLeaveGroup = { viewModel.leaveGroup(it) }
                    )
                    SocialTab.PARTNERS -> PartnersContent(
                        partners = uiState.partners,
                        onAccept = { viewModel.acceptPartnerRequest(it) },
                        onDecline = { viewModel.declinePartnerRequest(it) },
                        onRemove = { viewModel.removePartner(it) }
                    )
                    SocialTab.HIGH_FIVES -> HighFivesContent(
                        highFives = uiState.receivedHighFives,
                        onMarkAsRead = { viewModel.markHighFiveAsRead(it) }
                    )
                    SocialTab.CONTRACTS -> ContractsContent(
                        contracts = uiState.commitmentContracts,
                        onCancel = { viewModel.cancelContract(it) }
                    )
                }
            }
        }
    }

    // Create Group Dialog
    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, emoji, type ->
                viewModel.createGroup(name, emoji, type)
                showCreateGroupDialog = false
            }
        )
    }

    // Create Contract Dialog
    if (showCreateContractDialog) {
        CreateContractDialog(
            onDismiss = { showCreateContractDialog = false },
            onCreate = { habitId, habitName, commitment, targetDays, stakes ->
                viewModel.createCommitmentContract(
                    habitId = habitId,
                    habitName = habitName,
                    commitment = commitment,
                    targetDays = targetDays,
                    startDate = kotlinx.datetime.Clock.System.now().toString(),
                    endDate = "",  // Will be calculated
                    stakes = stakes
                )
                showCreateContractDialog = false
            }
        )
    }
}

@Composable
private fun PrivacyNoticeCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryLight.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔒",
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = SocialMessages.privacyReminder,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun GroupsContent(
    groups: List<AccountabilityGroup>,
    onLeaveGroup: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (groups.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "👥",
                    title = "No Groups Yet",
                    description = "Create or join a group to share accountability with others. Small groups (3-10 people) work best!"
                )
            }
        } else {
            items(groups) { group ->
                GroupCard(
                    group = group,
                    onLeave = { onLeaveGroup(group.id) }
                )
            }
        }
    }
}

@Composable
private fun GroupCard(
    group: AccountabilityGroup,
    onLeave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Secondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = group.emoji,
                            fontSize = 24.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${group.members.size}/${group.maxMembers} members • ${group.groupType.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                TextButton(onClick = onLeave) {
                    Text("Leave", color = Color(0xFFE57373))
                }
            }

            if (group.inviteCode != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Invite code: ${group.inviteCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { /* Copy to clipboard */ }
                    )
                }
            }

            // Member avatars
            if (group.members.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    group.members.take(5).forEachIndexed { index, member ->
                        Box(
                            modifier = Modifier
                                .offset(x = (-8 * index).dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(listOf(
                                    Color(0xFFE3F2FD),
                                    Color(0xFFE8F5E9),
                                    Color(0xFFFFF3E0),
                                    Color(0xFFF3E5F5),
                                    Color(0xFFFFEBEE)
                                )[index % 5]),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = member.profileEmoji,
                                fontSize = 16.sp
                            )
                        }
                    }
                    if (group.members.size > 5) {
                        Box(
                            modifier = Modifier
                                .offset(x = (-8 * 5).dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Gray.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${group.members.size - 5}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartnersContent(
    partners: List<AccountabilityPartner>,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (partners.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "🤝",
                    title = "No Partners Yet",
                    description = "Invite a friend to be your accountability partner. Research shows 1-on-1 partnerships are highly effective!"
                )
            }
        } else {
            items(partners) { partner ->
                PartnerCard(
                    partner = partner,
                    onAccept = { onAccept(partner.partnerId) },
                    onDecline = { onDecline(partner.partnerId) },
                    onRemove = { onRemove(partner.partnerId) }
                )
            }
        }
    }
}

@Composable
private fun PartnerCard(
    partner: AccountabilityPartner,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Secondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = partner.profileEmoji,
                        fontSize = 24.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = partner.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (partner.status) {
                            PartnerStatus.ACTIVE -> "🤝 Active partner"
                            PartnerStatus.PENDING_SENT -> "⏳ Waiting for response"
                            PartnerStatus.PENDING_RECEIVED -> "📬 Wants to connect"
                            else -> partner.status.name
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            when (partner.status) {
                PartnerStatus.PENDING_RECEIVED -> Row {
                    TextButton(onClick = onDecline) {
                        Text("Decline", color = Color.Gray)
                    }
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                    ) {
                        Text("Accept")
                    }
                }
                PartnerStatus.ACTIVE -> TextButton(onClick = onRemove) {
                    Text("Remove", color = Color(0xFFE57373))
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun HighFivesContent(
    highFives: List<HighFive>,
    onMarkAsRead: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (highFives.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "✋",
                    title = "No High Fives Yet",
                    description = "High fives are simple encouragements from your accountability partners and groups. Send some love!"
                )
            }
        } else {
            items(highFives) { highFive ->
                HighFiveCard(
                    highFive = highFive,
                    onMarkAsRead = { onMarkAsRead(highFive.id) }
                )
            }
        }
    }
}

@Composable
private fun HighFiveCard(
    highFive: HighFive,
    onMarkAsRead: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMarkAsRead() },
        colors = CardDefaults.cardColors(
            containerColor = if (!highFive.isRead)
                Color(0xFFFFF3E0)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Secondary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = highFive.fromEmoji,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = highFive.fromDisplayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = highFive.reason.emoji,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = highFive.message ?: SocialMessages.getHighFiveMessage(highFive.reason, highFive.habitName),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (highFive.habitName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "for ${highFive.habitName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Secondary
                    )
                }
            }

            if (!highFive.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Primary)
                )
            }
        }
    }
}

@Composable
private fun ContractsContent(
    contracts: List<CommitmentContract>,
    onCancel: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📜 About Commitment Contracts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = SocialMessages.getCommitmentPrompt(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        if (contracts.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "📝",
                    title = "No Contracts Yet",
                    description = "Make a public commitment to boost your chances of success!"
                )
            }
        } else {
            items(contracts) { contract ->
                ContractCard(
                    contract = contract,
                    onCancel = { onCancel(contract.id) }
                )
            }
        }
    }
}

@Composable
private fun ContractCard(
    contract: CommitmentContract,
    onCancel: () -> Unit
) {
    val progress = contract.currentProgress.toFloat() / contract.targetDays.toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contract.habitName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                when (contract.status) {
                    ContractStatus.COMPLETED -> Text(
                        text = "✅ Completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary
                    )
                    ContractStatus.FAILED -> Text(
                        text = "❌ Not completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE57373)
                    )
                    ContractStatus.ACTIVE -> TextButton(onClick = onCancel) {
                        Text("Cancel", color = Color.Gray, fontSize = 12.sp)
                    }
                    else -> {}
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "\"${contract.commitment}\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${contract.currentProgress}/${contract.targetDays} days",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Secondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Secondary,
                    trackColor = Color.Gray.copy(alpha = 0.2f)
                )
            }

            if (contract.stakes != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Stakes: ${contract.stakes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF8A65)
                )
            }
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
            Text(
                text = emoji,
                fontSize = 48.sp
            )

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
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, emoji: String, type: GroupType) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🎯") }
    var selectedType by remember { mutableStateOf(GroupType.GENERAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Group Type",
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Group type selection
                GroupType.entries.take(4).forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedType = type
                                selectedEmoji = type.emoji
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                selectedEmoji = type.emoji
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${type.emoji} ${type.label}")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, selectedEmoji, selectedType) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Secondary)
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
private fun CreateContractDialog(
    onDismiss: () -> Unit,
    onCreate: (habitId: String, habitName: String, commitment: String, targetDays: Int, stakes: String?) -> Unit
) {
    var selectedHabit by remember { mutableStateOf("move") }
    var commitment by remember { mutableStateOf("") }
    var targetDays by remember { mutableStateOf("30") }
    var stakes by remember { mutableStateOf("") }

    val habits = listOf(
        "sleep" to "🌙 Rest",
        "water" to "💧 Hydrate",
        "move" to "🏃 Move",
        "vegetables" to "🥗 Nourish",
        "calm" to "🧘 Calm",
        "connect" to "💬 Connect",
        "unplug" to "📴 Unplug"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Commitment") },
        text = {
            Column {
                Text(
                    text = "Select Habit",
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                habits.forEach { (id, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedHabit = id }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedHabit == id,
                            onClick = { selectedHabit = id }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = commitment,
                    onValueChange = { commitment = it },
                    label = { Text("Your commitment") },
                    placeholder = { Text("I will exercise for 30 minutes daily") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetDays,
                    onValueChange = { targetDays = it },
                    label = { Text("Number of days") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = stakes,
                    onValueChange = { stakes = it },
                    label = { Text("Stakes (optional)") },
                    placeholder = { Text("If I fail, I'll donate $20") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val habitName = habits.find { it.first == selectedHabit }?.second?.substringAfter(" ") ?: selectedHabit
                    onCreate(
                        selectedHabit,
                        habitName,
                        commitment,
                        targetDays.toIntOrNull() ?: 30,
                        stakes.takeIf { it.isNotBlank() }
                    )
                },
                enabled = commitment.isNotBlank() && targetDays.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Secondary)
            ) {
                Text("Commit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
