package com.dailywell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.PremiumDesignTokens
import com.dailywell.app.core.theme.PremiumLayoutTokens
import androidx.compose.animation.core.*

/**
 * Shared premium tab header used across hub screens.
 */
@Composable
fun PremiumTabHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    includeStatusBarPadding: Boolean = false,
    leadingIcon: ImageVector? = null,
    onLeadingClick: (() -> Unit)? = null,
    trailingActions: @Composable RowScope.() -> Unit = {}
) {
    val glow = rememberInfiniteTransition(label = "premiumHeaderGlow")
        .animateFloat(
            initialValue = 0.22f,
            targetValue = 0.46f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "premiumHeaderGlow"
        )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (includeStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
            .padding(
                horizontal = PremiumLayoutTokens.headerOuterHorizontalPadding,
                vertical = if (includeStatusBarPadding) PremiumLayoutTokens.headerOuterVerticalPadding else 0.dp
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PremiumLayoutTokens.headerCornerRadius))
                .border(
                    1.dp,
                    PremiumDesignTokens.headerBorder,
                    RoundedCornerShape(PremiumLayoutTokens.headerCornerRadius)
                )
                .background(
                    Brush.horizontalGradient(
                        colors = PremiumDesignTokens.headerGradient
                    )
                )
                .padding(PremiumLayoutTokens.headerInnerPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (leadingIcon != null && onLeadingClick != null) {
                        Box(
                            modifier = Modifier
                                .size(PremiumLayoutTokens.headerIconContainer)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PremiumDesignTokens.headerIconBackdrop)
                                .clickable(onClick = onLeadingClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = "Navigate",
                                tint = PremiumDesignTokens.headerTitleText,
                                modifier = Modifier.size(PremiumLayoutTokens.headerIconSize)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column {
                        Text(
                            text = title,
                            color = PremiumDesignTokens.headerTitleText,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = PremiumDesignTokens.headerTitleText,
                                letterSpacing = (-0.5).sp,
                                shadow = Shadow(
                                    color = PremiumDesignTokens.headerTitleText.copy(alpha = glow.value),
                                    blurRadius = 30f
                                )
                            )
                        )
                        subtitle?.let {
                            Text(
                                text = it,
                                color = PremiumDesignTokens.headerTitleText.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = trailingActions
                )
            }
        }
    }
}

/**
 * Premium top bar wrapper for tab pages that need back navigation and actions.
 */
@Composable
fun PremiumTopBar(
    title: String,
    subtitle: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailingActions: @Composable RowScope.() -> Unit = {}
) {
    PremiumTabHeader(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        includeStatusBarPadding = true,
        leadingIcon = if (onNavigationClick != null) DailyWellIcons.Nav.Back else null,
        onLeadingClick = onNavigationClick,
        trailingActions = trailingActions
    )
}
