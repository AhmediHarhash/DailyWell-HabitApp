package com.dailywell.app.ui.screens.paywall

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.*
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import kotlin.math.round
import org.koin.compose.viewmodel.koinViewModel

// ═══════════════════════════════════════════════════════════
// WARM WELLNESS PAYWALL (2026 Redesign)
// Matches DailyWell's cream/sage aesthetic
// ═══════════════════════════════════════════════════════════

private data class PlanInfo(
    val id: String,
    val label: String,
    val price: String,
    val period: String,
    val monthlyEquivalent: String?,
    val badgeText: String?,
    val savings: String?,
    val comparisonText: String?,
    val features: List<String>
)

private data class PricingComparison(
    val annualPerMonthLabel: String,
    val annualSavingsText: String?,
    val annualComparisonText: String
)

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
    val pricingComparison = remember(uiState.monthlyPrice, uiState.annualPrice) {
        buildPricingComparison(uiState.monthlyPrice, uiState.annualPrice)
    }

    val plans = remember(uiState.monthlyPrice, uiState.annualPrice, pricingComparison) {
        listOf(
            PlanInfo(
                id = PRODUCT_ANNUAL,
                label = "Annual",
                price = uiState.annualPrice,
                period = "/year",
                monthlyEquivalent = pricingComparison?.annualPerMonthLabel ?: "Best value each month",
                badgeText = "BEST VALUE",
                savings = pricingComparison?.annualSavingsText ?: "Best annual savings",
                comparisonText = pricingComparison?.annualComparisonText,
                features = listOf(
                    "Everything in Premium unlocked",
                    "AI scan + coach + insight features",
                    "Priority support and early updates"
                )
            ),
            PlanInfo(
                id = PRODUCT_MONTHLY,
                label = "Monthly",
                price = uiState.monthlyPrice,
                period = "/month",
                monthlyEquivalent = "Billed month-to-month",
                badgeText = null,
                savings = null,
                comparisonText = null,
                features = listOf(
                    "Same full Premium feature set",
                    "Switch or cancel anytime",
                    "Best for short-term flexibility"
                )
            )
        )
    }

    var selectedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedIndex) {
        viewModel.selectProduct(plans[selectedIndex].id)
    }

    LaunchedEffect(uiState.purchaseSuccess) {
        if (uiState.purchaseSuccess) onPurchaseSuccess()
    }

    val selectedPlan = plans[selectedIndex]

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "DailyWell Premium",
                    subtitle = "Build lasting habits with the full toolkit",
                    onNavigationClick = onDismiss
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(BackgroundLight.copy(alpha = 0.82f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Premium icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = DailyWellIcons.Status.Premium,
                            contentDescription = "Premium",
                            modifier = Modifier.size(36.dp),
                            tint = Primary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Unlock DailyWell Premium",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Build lasting habits with the full toolkit",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondaryLight,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    PremiumSectionChip(
                        text = "Choose your plan",
                        icon = DailyWellIcons.Status.Premium
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Trial banner
                    if (onStartTrial != null && !uiState.isTrialActive) {
                        WarmTrialBanner(onStartTrial = onStartTrial)
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Pill selector: Annual | Monthly
                    PillSelector(
                        options = plans.map { it.label },
                        selectedIndex = selectedIndex,
                        onSelect = { selectedIndex = it }
                    )

                    if (selectedPlan.id == PRODUCT_ANNUAL && pricingComparison?.annualSavingsText != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AnnualSavingsCallout(text = pricingComparison.annualSavingsText)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Plan card
                    WarmPlanCard(plan = selectedPlan)

                    Spacer(modifier = Modifier.height(28.dp))

                    // Error message
                    uiState.errorMessage?.let { error ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            colors = CardDefaults.cardColors(containerColor = ErrorLight),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = Error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(14.dp).fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // CTA button
                    Button(
                        onClick = {
                            viewModel.setPurchasing(true)
                            onPurchaseProduct(viewModel.getSelectedProductId())
                        },
                        enabled = !uiState.isPurchasing && !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        if (uiState.isPurchasing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = getCtaText(selectedPlan),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Restore link
                    TextButton(
                        onClick = {
                            viewModel.setRestoring(true)
                            onRestorePurchases()
                        },
                        enabled = !uiState.isLoading && !uiState.isPurchasing
                    ) {
                        Text(
                            text = "Restore Purchase",
                            color = TextSecondaryLight,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Trust badges
                    WarmTrustBadges()

                    Spacer(modifier = Modifier.height(16.dp))

                    // Legal
                    Text(
                        text = "Cancel anytime. Subscriptions auto-renew unless cancelled 24h before period end.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(48.dp))
                }

                // Loading overlay
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BackgroundLight.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// PILL SELECTOR
// ═══════════════════════════════════════════════════════════

@Composable
private fun PillSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceSubtle)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color.Transparent
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) TextPrimaryLight else TextSecondaryLight
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(bgColor)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// WARM PLAN CARD (GlassCard style)
// ═══════════════════════════════════════════════════════════

@Composable
private fun WarmPlanCard(plan: PlanInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge
            plan.badgeText?.let { badge ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Primary.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        letterSpacing = 0.8.sp
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Price
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = plan.price,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight
                )
                Text(
                    text = plan.period,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryLight,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                )
            }

            plan.monthlyEquivalent?.let { monthly ->
                Text(
                    text = monthly,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )
            }

            // Savings badge
            plan.savings?.let { savings ->
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Success.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = savings,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Success
                    )
                }
            }

            plan.comparisonText?.let { comparison ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = comparison,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Divider
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.6f),
                color = DividerLight
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Features
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                plan.features.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = DailyWellIcons.Actions.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryLight
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnualSavingsCallout(text: String) {
    Row(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Success.copy(alpha = 0.12f))
            .border(width = 1.dp, color = Success.copy(alpha = 0.28f), shape = RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = DailyWellIcons.Gamification.XP,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Success
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$text vs monthly billing",
            style = MaterialTheme.typography.labelMedium,
            color = Success,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ═══════════════════════════════════════════════════════════
// TRIAL BANNER (warm style)
// ═══════════════════════════════════════════════════════════

@Composable
private fun WarmTrialBanner(onStartTrial: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable { onStartTrial() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = DailyWellIcons.Gamification.Gift,
                    contentDescription = "Gift",
                    modifier = Modifier.size(24.dp),
                    tint = Primary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Start 14-Day Free Trial",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryDark
                )
                Text(
                    text = "Full access \u2022 No credit card required",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )
            }

            Icon(
                imageVector = DailyWellIcons.Nav.ChevronRight,
                contentDescription = "Start trial",
                modifier = Modifier.size(20.dp),
                tint = Primary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// TRUST BADGES (warm)
// ═══════════════════════════════════════════════════════════

@Composable
private fun WarmTrustBadges() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        WarmTrustBadge(DailyWellIcons.Misc.Secure, "Secure\nPayment")
        WarmTrustBadge(DailyWellIcons.Habits.Recovery, "Cancel\nAnytime")
        WarmTrustBadge(DailyWellIcons.Gamification.XP, "Instant\nAccess")
    }
}

@Composable
private fun WarmTrustBadge(icon: ImageVector, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceSubtle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(20.dp),
                tint = Primary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondaryLight,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════

private fun getCtaText(plan: PlanInfo): String {
    return when (plan.id) {
        PRODUCT_MONTHLY -> "Subscribe \u2022 ${plan.price}/mo"
        PRODUCT_ANNUAL -> "Get Annual \u2022 ${plan.price}"
        else -> "Continue"
    }
}

private fun buildPricingComparison(monthlyPrice: String, annualPrice: String): PricingComparison? {
    val monthly = parsePriceAmount(monthlyPrice) ?: return null
    val annual = parsePriceAmount(annualPrice) ?: return null
    if (monthly <= 0.0 || annual <= 0.0) return null

    val currency = detectCurrencySymbol(monthlyPrice, annualPrice)
    val monthlyYearly = monthly * 12.0
    val annualPerMonth = annual / 12.0
    val savingsAmount = (monthlyYearly - annual).coerceAtLeast(0.0)
    val savingsPercent = if (monthlyYearly > 0.0) {
        round((savingsAmount / monthlyYearly) * 100.0).toInt()
    } else {
        0
    }

    return PricingComparison(
        annualPerMonthLabel = "$currency${formatMoney(annualPerMonth)}/mo",
        annualSavingsText = if (savingsAmount > 0.0) {
            "Save $currency${formatMoney(savingsAmount)} ($savingsPercent%)"
        } else {
            null
        },
        annualComparisonText = "Monthly would total $currency${formatMoney(monthlyYearly)}/year"
    )
}

private fun detectCurrencySymbol(vararg priceLabels: String): String {
    priceLabels.forEach { label ->
        val trimmed = label.trim()
        val prefix = trimmed.takeWhile { !it.isDigit() && it != '-' && it != '+' }.trim()
        if (prefix.isNotEmpty()) return prefix

        val suffix = trimmed.takeLastWhile { !it.isDigit() && it != '.' && it != ',' }.trim()
        if (suffix.isNotEmpty()) return suffix
    }
    return "$"
}

private fun parsePriceAmount(priceLabel: String): Double? {
    val numeric = priceLabel.filter { it.isDigit() || it == ',' || it == '.' }
    if (numeric.isEmpty()) return null

    val lastComma = numeric.lastIndexOf(',')
    val lastDot = numeric.lastIndexOf('.')
    val normalized = when {
        lastComma >= 0 && lastDot >= 0 -> {
            if (lastDot > lastComma) numeric.replace(",", "") else numeric.replace(".", "").replace(',', '.')
        }
        lastComma >= 0 -> {
            val chunks = numeric.split(',')
            if (chunks.size == 2 && chunks[1].length in 1..2) numeric.replace(',', '.') else numeric.replace(",", "")
        }
        else -> numeric
    }
    return normalized.toDoubleOrNull()
}

private fun formatMoney(amount: Double): String {
    val rounded = round(amount * 100.0) / 100.0
    val asLong = rounded.toLong()
    return if (asLong.toDouble() == rounded) {
        asLong.toString()
    } else {
        val text = rounded.toString()
        val decimals = text.substringAfter('.', "")
        when (decimals.length) {
            0 -> "$text.00"
            1 -> "$text" + "0"
            else -> text
        }
    }
}
