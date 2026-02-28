package com.dailywell.app.ui.screens.rewards

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.data.model.RedemptionCategory
import com.dailywell.app.data.model.RewardItem
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import com.dailywell.app.ui.components.ShimmerLoadingScreen
import androidx.compose.ui.graphics.vector.ImageVector
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardStoreScreen(
    onBackClick: () -> Unit = {},
    viewModel: RewardStoreViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedCategory by remember { mutableStateOf<RedemptionCategory?>(null) }
    var showRedeemDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<RewardItem?>(null) }

    // Redeem confirmation dialog
    if (showRedeemDialog && selectedItem != null) {
        RedeemConfirmationDialog(
            item = selectedItem!!,
            currentBalance = uiState.coinBalance,
            onDismiss = {
                showRedeemDialog = false
                selectedItem = null
            },
            onConfirm = {
                viewModel.redeemItem(selectedItem!!.id)
                showRedeemDialog = false
                selectedItem = null
            }
        )
    }

    // Success toast
    if (uiState.redemptionSuccess) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            viewModel.dismissSuccess()
        }
    }

    GlassScreenWrapper {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Reward Store",
                    subtitle = "Redeem WellCoins",
                    onNavigationClick = onBackClick,
                    trailingActions = {
                        WellCoinBalanceChip(balance = uiState.coinBalance)
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Category tabs
                CategoryTabs(
                    selectedCategory = selectedCategory,
                    onCategorySelected = {
                        selectedCategory = if (selectedCategory == it) null else it
                    }
                )

                // Rewards list
                if (uiState.isLoading) {
                    ShimmerLoadingScreen(modifier = Modifier.fillMaxSize())
                } else {
                    val filteredRewards = if (selectedCategory != null) {
                        uiState.availableRewards.filter { it.category == selectedCategory }
                    } else {
                        uiState.availableRewards
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Success message
                        if (uiState.redemptionSuccess) {
                            item {
                                SuccessBanner(message = "Reward redeemed successfully! Check your email for details.")
                            }
                        }

                        // Error message
                        if (uiState.errorMessage != null) {
                            item {
                                ErrorBanner(message = uiState.errorMessage!!)
                            }
                        }

                        // How to earn coins section
                        item {
                            PremiumSectionChip(
                                text = "How to Earn Coins",
                                icon = DailyWellIcons.Gamification.Coin
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            HowToEarnCard()
                        }

                        item {
                            PremiumSectionChip(
                                text = "Rewards Catalog",
                                icon = DailyWellIcons.Gamification.Reward
                            )
                        }

                        // Rewards
                        items(
                            items = filteredRewards,
                            key = { it.id }
                        ) { item ->
                            RewardItemCard(
                                item = item,
                                currentBalance = uiState.coinBalance,
                                onClick = {
                                    selectedItem = item
                                    showRedeemDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WellCoinBalanceChip(balance: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = DailyWellIcons.Gamification.Coin,
                contentDescription = "WellCoins",
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = balance.toString(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun CategoryTabs(
    selectedCategory: RedemptionCategory?,
    onCategorySelected: (RedemptionCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RedemptionCategory.entries.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category.displayName(),
                        fontSize = 12.sp
                    )
                }
            )
        }
    }
}

@Composable
fun RewardItemCard(
    item: RewardItem,
    currentBalance: Int,
    onClick: () -> Unit
) {
    val canAfford = currentBalance >= item.coinCost

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canAfford && item.isAvailable) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (canAfford && item.isAvailable)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon and details
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Reward icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Gamification.Reward,
                        contentDescription = item.name,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = item.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = DailyWellIcons.Gamification.Coin,
                            contentDescription = "coins",
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${item.coinCost}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Redeem button or status
            if (!item.isAvailable) {
                Text(
                    text = "Sold Out",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (!canAfford) {
                Text(
                    text = "Need ${item.coinCost - currentBalance} more",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Button(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Redeem")
                }
            }
        }
    }
}

@Composable
fun HowToEarnCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DailyWellIcons.Gamification.Coin,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "How to Earn WellCoins",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            listOf(
                "Complete a habit: +5 coins",
                "Daily check-in: +10 coins",
                "Perfect day (all habits): +20 coins",
                "7-day streak: +50 coins",
                "30-day streak: +200 coins",
                "Voice chat with coach: +15 coins",
                "Complete daily challenge: +30 coins"
            ).forEach { rule ->
                Text(
                    text = "• $rule",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun RedeemConfirmationDialog(
    item: RewardItem,
    currentBalance: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = DailyWellIcons.Gamification.Reward,
                contentDescription = item.name,
                modifier = Modifier.size(40.dp)
            )
        },
        title = { Text("Redeem ${item.name}?") },
        text = {
            Column {
                Text(item.description)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cost: ${item.coinCost} coins",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Remaining: ${currentBalance - item.coinCost} coins",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm")
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
fun SuccessBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun ErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = DailyWellIcons.Status.Warning,
                contentDescription = "Warning",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

// Extension function for category display names
fun RedemptionCategory.displayName(): String = when (this) {
    RedemptionCategory.GIFT_CARDS -> "Gift Cards"
    RedemptionCategory.CHARITY_DONATION -> "Charity"
    RedemptionCategory.IN_APP_THEMES -> "Themes"
    RedemptionCategory.IN_APP_FEATURES -> "Features"
    RedemptionCategory.WELLNESS_DISCOUNTS -> "Discounts"
    RedemptionCategory.BADGE_UPGRADES -> "Badges"
}
