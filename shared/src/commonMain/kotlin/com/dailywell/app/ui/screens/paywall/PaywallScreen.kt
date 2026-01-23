package com.dailywell.app.ui.screens.paywall

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.Success
import org.koin.compose.viewmodel.koinViewModel

/**
 * Premium Paywall Screen with psychology-optimized pricing
 * Uses: Anchoring, Decoy Effect, Center-Stage, Loss Aversion
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    onPurchaseSuccess: () -> Unit,
    onPurchaseProduct: (productId: String) -> Unit,
    onRestorePurchases: () -> Unit,
    onStartTrial: (() -> Unit)? = null,
    viewModel: PaywallViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle purchase success
    LaunchedEffect(uiState.purchaseSuccess) {
        if (uiState.purchaseSuccess) {
            onPurchaseSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Text(text = "✕", fontSize = 24.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Premium star badge
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFD700),
                                    Color(0xFFFFA500)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⭐", fontSize = 40.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Trial banner (if applicable)
                if (onStartTrial != null && !uiState.isTrialActive) {
                    TrialBanner(onStartTrial = onStartTrial)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "Unlock Your Full Potential",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Join thousands building better habits",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Premium features with checkmarks
                PremiumFeaturesList()

                Spacer(modifier = Modifier.height(28.dp))

                // PRICING SECTION - Psychology optimized
                // Order: Lifetime (anchor) | Annual (target) | Monthly (decoy)
                Text(
                    text = "CHOOSE YOUR PLAN",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Three-tier pricing with annual highlighted
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // LIFETIME - Anchor (makes annual look cheap)
                    PricingTier(
                        modifier = Modifier.weight(1f),
                        title = "Lifetime",
                        price = uiState.lifetimePrice,
                        subtitle = "one-time",
                        badge = "Save ${uiState.lifetimeSavingsPercent}%",
                        badgeColor = Color(0xFF9C27B0),
                        isSelected = uiState.selectedProductId == PRODUCT_LIFETIME,
                        isPopular = false,
                        onClick = { viewModel.selectProduct(PRODUCT_LIFETIME) }
                    )

                    // ANNUAL - Target (center-stage, highlighted)
                    PricingTier(
                        modifier = Modifier.weight(1f),
                        title = "Annual",
                        price = uiState.annualPrice,
                        subtitle = "${uiState.annualMonthlyEquivalent}/mo",
                        badge = "BEST VALUE",
                        badgeColor = Success,
                        isSelected = uiState.selectedProductId == PRODUCT_ANNUAL,
                        isPopular = true,
                        onClick = { viewModel.selectProduct(PRODUCT_ANNUAL) }
                    )

                    // MONTHLY - Decoy (makes annual look better)
                    PricingTier(
                        modifier = Modifier.weight(1f),
                        title = "Monthly",
                        price = uiState.monthlyPrice,
                        subtitle = "/month",
                        badge = null,
                        badgeColor = null,
                        isSelected = uiState.selectedProductId == PRODUCT_MONTHLY,
                        isPopular = false,
                        onClick = { viewModel.selectProduct(PRODUCT_MONTHLY) }
                    )
                }

                // Error message
                uiState.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Purchase button
                Button(
                    onClick = {
                        viewModel.setPurchasing(true)
                        onPurchaseProduct(viewModel.getSelectedProductId())
                    },
                    enabled = !uiState.isPurchasing && !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Success
                    )
                ) {
                    if (uiState.isPurchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = getPurchaseButtonText(uiState),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Restore purchase link
                Text(
                    text = "Restore Purchase",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(
                        enabled = !uiState.isLoading && !uiState.isPurchasing
                    ) {
                        viewModel.setRestoring(true)
                        onRestorePurchases()
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Trust signals
                TrustSignals()

                Spacer(modifier = Modifier.height(16.dp))

                // Legal text
                Text(
                    text = "Cancel anytime. Subscriptions renew automatically unless cancelled at least 24 hours before the end of the current period.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }

            // Loading overlay
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun TrialBanner(onStartTrial: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStartTrial() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🎁", fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Try Premium Free for 14 Days",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "No credit card required",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "→",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PremiumFeaturesList() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FeatureRow(emoji = "✓", text = "All 7 core habits unlocked")
            FeatureRow(emoji = "✓", text = "Up to 5 custom habits")
            FeatureRow(emoji = "✓", text = "Health Connect auto-tracking")
            FeatureRow(emoji = "✓", text = "Full history & patterns")
            FeatureRow(emoji = "✓", text = "Smart adaptive reminders")
            FeatureRow(emoji = "✓", text = "Streak protection (2 freezes/month)")
            FeatureRow(emoji = "✓", text = "Weekly insights & reflections")
            FeatureRow(emoji = "✓", text = "Home screen widgets")
        }
    }
}

@Composable
private fun FeatureRow(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = emoji,
            fontSize = 16.sp,
            color = Success
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PricingTier(
    modifier: Modifier = Modifier,
    title: String,
    price: String,
    subtitle: String,
    badge: String?,
    badgeColor: Color?,
    isSelected: Boolean,
    isPopular: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, Success, RoundedCornerShape(16.dp))
                } else if (isPopular) {
                    Modifier.border(1.dp, Success.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> Success.copy(alpha = 0.1f)
                isPopular -> Success.copy(alpha = 0.05f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge
            if (badge != null && badgeColor != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isPopular) FontWeight.Bold else FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = price,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected || isPopular) Success else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Selection indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) Success else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .then(
                        if (!isSelected) Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(10.dp)
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TrustSignals() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrustItem(text = "🔒 Secure")
        Spacer(modifier = Modifier.width(16.dp))
        TrustItem(text = "↩️ Cancel anytime")
        Spacer(modifier = Modifier.width(16.dp))
        TrustItem(text = "💳 No hidden fees")
    }
}

@Composable
private fun TrustItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun getPurchaseButtonText(state: PaywallUiState): String {
    return when (state.selectedProductId) {
        PRODUCT_MONTHLY -> "Subscribe for ${state.monthlyPrice}/month"
        PRODUCT_ANNUAL -> "Get Annual for ${state.annualPrice}"
        PRODUCT_LIFETIME -> "Get Lifetime for ${state.lifetimePrice}"
        else -> "Continue"
    }
}
