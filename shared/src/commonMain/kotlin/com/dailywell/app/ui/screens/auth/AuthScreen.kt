package com.dailywell.app.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailywell.app.core.theme.AccentIndigo
import com.dailywell.app.core.theme.AccentSky
import com.dailywell.app.core.theme.Error
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.core.theme.Success
import com.dailywell.app.ui.components.DailyWellIcons
import habithealth.shared.generated.resources.Res
import habithealth.shared.generated.resources.onboarding_hero
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthScreen(
    onSignIn: suspend (email: String, password: String) -> Result<Unit>,
    onSignUp: suspend (email: String, password: String, displayName: String) -> Result<Unit>,
    onGoogleSignIn: (suspend () -> Result<Unit>)? = null,
    onForgotPassword: (suspend (String) -> Result<Unit>)? = null,
    onComplete: () -> Unit,
    isFromSettings: Boolean = false,
    onBack: (() -> Unit)? = null,
    viewModel: AuthViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current
    var passwordVisible by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F7))
    ) {
        val wideLayout = maxWidth >= 860.dp

        Column(modifier = Modifier.fillMaxSize()) {
            if (isFromSettings) {
                SettingsHeader(onBack = onBack)
            } else {
                AuthStageStrip()
            }

            if (wideLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(0.44f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        tonalElevation = 0.dp,
                        shadowElevation = 4.dp
                    ) {
                        AuthFormPanel(
                            uiState = uiState,
                            passwordVisible = passwordVisible,
                            onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
                            onSignIn = onSignIn,
                            onSignUp = onSignUp,
                            onGoogleSignIn = onGoogleSignIn,
                            onForgotPassword = onForgotPassword,
                            onComplete = onComplete,
                            isFromSettings = isFromSettings,
                            viewModel = viewModel,
                            onPrivacyClick = { uriHandler.openUri("https://dailywell.hekax.com/privacy/") },
                            onTermsClick = { uriHandler.openUri("https://dailywell.hekax.com/terms/") }
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .weight(0.56f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF0F121B),
                        tonalElevation = 0.dp,
                        shadowElevation = 4.dp
                    ) {
                        AuthHeroPanel()
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!isFromSettings) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF0F121B),
                            tonalElevation = 0.dp
                        ) {
                            AuthHeroCompact()
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        tonalElevation = 0.dp,
                        shadowElevation = 2.dp
                    ) {
                        AuthFormPanel(
                            uiState = uiState,
                            passwordVisible = passwordVisible,
                            onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
                            onSignIn = onSignIn,
                            onSignUp = onSignUp,
                            onGoogleSignIn = onGoogleSignIn,
                            onForgotPassword = onForgotPassword,
                            onComplete = onComplete,
                            isFromSettings = isFromSettings,
                            viewModel = viewModel,
                            onPrivacyClick = { uriHandler.openUri("https://dailywell.hekax.com/privacy/") },
                            onTermsClick = { uriHandler.openUri("https://dailywell.hekax.com/terms/") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(onBack: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = { onBack?.invoke() }) {
            Icon(
                imageVector = DailyWellIcons.Nav.Back,
                contentDescription = "Back"
            )
        }
        Column {
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manage sign in and sync",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6E7380)
            )
        }
    }
}

@Composable
private fun AuthStageStrip() {
    val items = listOf("Sign In", "Permissions", "Set Up", "Learn")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, label ->
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (index == 0) FontWeight.Bold else FontWeight.SemiBold,
                color = if (index == 0) Color(0xFF1B1F2A) else Color(0xFF7D8390)
            )
            if (index < items.lastIndex) {
                Text(
                    text = "  >  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFB1B6C0)
                )
            }
        }
    }
}

@Composable
private fun AuthHeroPanel() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.onboarding_hero),
            contentDescription = "DailyWell preview",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x330E111A),
                            Color(0xBB0E111A)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Works across your whole health system",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Coach, tracking, and insights stay synced with one account.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun AuthHeroCompact() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        Image(
            painter = painterResource(Res.drawable.onboarding_hero),
            contentDescription = "DailyWell preview",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xD9151821)
                        )
                    )
                )
        )
        Text(
            text = "DailyWell account sync",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun AuthFormPanel(
    uiState: AuthUiState,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: () -> Unit,
    onSignIn: suspend (String, String) -> Result<Unit>,
    onSignUp: suspend (String, String, String) -> Result<Unit>,
    onGoogleSignIn: (suspend () -> Result<Unit>)?,
    onForgotPassword: (suspend (String) -> Result<Unit>)?,
    onComplete: () -> Unit,
    isFromSettings: Boolean,
    viewModel: AuthViewModel,
    onPrivacyClick: () -> Unit,
    onTermsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (uiState.showForgotPassword) {
                "Reset your password"
            } else if (uiState.isSignUp) {
                "Create your DailyWell account"
            } else {
                "Get started with DailyWell"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF181C26)
        )

        Text(
            text = if (uiState.showForgotPassword) {
                "Enter your email and we will send a reset link."
            } else if (uiState.isSignUp) {
                "Save progress, coaching, and insights across devices."
            } else {
                "Sign in once to keep your habits and AI history synced."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF656B77)
        )

        if (!uiState.showForgotPassword) {
            ProviderGrid(
                onGoogleClick = if (onGoogleSignIn != null) {
                    {
                        viewModel.signInWithGoogle(
                            onGoogleSignIn = onGoogleSignIn,
                            onComplete = onComplete
                        )
                    }
                } else null,
                googleLoading = uiState.isGoogleLoading,
                enabled = !uiState.isLoading
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE3E6ED))
                Text(
                    text = "OR",
                    modifier = Modifier.padding(horizontal = 10.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8D93A0)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE3E6ED))
            }
        }

        if (uiState.showForgotPassword) {
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::setEmail,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                label = { Text("Email address") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                )
            )

            Button(
                onClick = {
                    if (onForgotPassword != null) {
                        viewModel.submitForgotPassword(onResetPassword = onForgotPassword)
                    }
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14161D))
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Send reset link")
                }
            }

            TextButton(onClick = viewModel::hideForgotPassword) {
                Text("Back to sign in", color = AccentSky)
            }
        } else {
            if (uiState.isSignUp) {
                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = viewModel::setDisplayName,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    label = { Text("Display name") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
            }

            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::setEmail,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                label = { Text("Email address") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::setPassword,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                label = { Text("Password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = onPasswordVisibilityChange) {
                        Icon(
                            imageVector = if (passwordVisible) DailyWellIcons.Status.Unlock else DailyWellIcons.Status.Lock,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color(0xFF7A8090)
                        )
                    }
                }
            )

            if (!uiState.isSignUp && onForgotPassword != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot password?",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentSky,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { viewModel.showForgotPassword() }
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.submit(
                        onSignIn = onSignIn,
                        onSignUp = onSignUp,
                        onComplete = onComplete
                    )
                },
                enabled = !uiState.isLoading && !uiState.isGoogleLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10131C))
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (uiState.isSignUp) "Create Account" else "Continue with Email",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isSignUp) "Already have an account?" else "New here?",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A8090)
                )
                Text(
                    text = if (uiState.isSignUp) " Sign In" else " Create account",
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.toggleSignUp() }
                )
            }
        }

        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = Error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        uiState.successMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Success,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (!isFromSettings && !uiState.showForgotPassword) {
            TextButton(
                onClick = { viewModel.skip(onComplete) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now", color = Color(0xFF7A8090))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8D93A0),
                modifier = Modifier.clickable(onClick = onPrivacyClick)
            )
            Text(
                text = "  |  ",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFBDC2CD)
            )
            Text(
                text = "Terms of Service",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8D93A0),
                modifier = Modifier.clickable(onClick = onTermsClick)
            )
        }
    }
}

@Composable
private fun ProviderGrid(
    onGoogleClick: (() -> Unit)?,
    googleLoading: Boolean,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProviderButton(
                label = "Google",
                brandMark = "G",
                modifier = Modifier.weight(1f),
                isEnabled = enabled && onGoogleClick != null,
                onClick = { onGoogleClick?.invoke() },
                loading = googleLoading,
                activeGradient = listOf(AccentSky, AccentIndigo)
            )
            ProviderButton(
                label = "Microsoft",
                brandMark = "M",
                modifier = Modifier.weight(1f),
                isEnabled = false,
                onClick = {}
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProviderButton(
                label = "Apple",
                brandMark = "A",
                modifier = Modifier.weight(1f),
                isEnabled = false,
                onClick = {}
            )
            ProviderButton(
                label = "SSO",
                brandMark = "S",
                modifier = Modifier.weight(1f),
                isEnabled = false,
                onClick = {}
            )
        }
    }
}

@Composable
private fun ProviderButton(
    label: String,
    brandMark: String,
    modifier: Modifier = Modifier,
    isEnabled: Boolean,
    onClick: () -> Unit,
    loading: Boolean = false,
    activeGradient: List<Color> = listOf(Color(0xFFEEF2FF), Color(0xFFE8F0FF))
) {
    val container = if (isEnabled) {
        Brush.horizontalGradient(activeGradient.map { it.copy(alpha = 0.18f) })
    } else {
        Brush.horizontalGradient(listOf(Color(0xFFF4F6FA), Color(0xFFF4F6FA)))
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .border(
                width = 1.dp,
                color = if (isEnabled) Color(0xFFC9D2E8) else Color(0xFFE5E8EF),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = isEnabled && !loading, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = AccentSky
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isEnabled) Color.White else Color(0xFFE2E5EB)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = brandMark,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isEnabled) Color(0xFF202737) else Color(0xFF8D93A0),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isEnabled) Color(0xFF1C2230) else Color(0xFF8B92A0),
            fontWeight = FontWeight.SemiBold
        )
    }
}
