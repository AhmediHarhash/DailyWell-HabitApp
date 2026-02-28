package com.dailywell.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import com.dailywell.app.core.theme.*
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumTopBar
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

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (isFromSettings) {
                    PremiumTopBar(
                        title = when {
                            uiState.showForgotPassword -> "Reset Password"
                            uiState.isSignUp -> "Create Account"
                            else -> "Account"
                        },
                        subtitle = when {
                            uiState.showForgotPassword -> "Recover access to your account"
                            uiState.isSignUp -> "Create your synced DailyWell account"
                            else -> "Sign in to sync data across devices"
                        },
                        onNavigationClick = onBack
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight.copy(alpha = 0.82f))
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp)
                    .padding(top = if (isFromSettings) 12.dp else 48.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // App icon - leaf in sage circle
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = DailyWellIcons.Onboarding.Welcome,
                    contentDescription = "DailyWell",
                    modifier = Modifier.size(36.dp),
                    tint = Primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = when {
                    uiState.showForgotPassword -> "Reset Password"
                    uiState.isSignUp -> "Create Account"
                    else -> "Welcome to DailyWell"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = when {
                    uiState.showForgotPassword -> "Enter your email to receive a reset link"
                    uiState.isSignUp -> "Sign up to save your progress across devices"
                    else -> "Sign in to sync your habits"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Forgot password mode
            if (uiState.showForgotPassword) {
                ForgotPasswordContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onForgotPassword = onForgotPassword
                )
            } else {
                // Google Sign-In button
                if (onGoogleSignIn != null) {
                    OutlinedButton(
                        onClick = {
                            viewModel.signInWithGoogle(
                                onGoogleSignIn = onGoogleSignIn,
                                onComplete = onComplete
                            )
                        },
                        enabled = !uiState.isGoogleLoading && !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimaryLight
                        )
                    ) {
                        if (uiState.isGoogleLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = DailyWellIcons.Auth.Google,
                                contentDescription = "Google",
                                modifier = Modifier.size(20.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = TextSecondaryLight.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "  or continue with email  ",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight.copy(alpha = 0.6f)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = TextSecondaryLight.copy(alpha = 0.3f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Display name field (sign up only)
                if (uiState.isSignUp) {
                    OutlinedTextField(
                        value = uiState.displayName,
                        onValueChange = { viewModel.setDisplayName(it) },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Email field
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.setEmail(it) },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password field
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = { viewModel.setPassword(it) },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) DailyWellIcons.Status.Unlock
                                else DailyWellIcons.Status.Lock,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = TextSecondaryLight
                            )
                        }
                    }
                )

                // Forgot password link (sign-in mode only)
                if (!uiState.isSignUp && onForgotPassword != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Forgot password?",
                            style = MaterialTheme.typography.bodySmall,
                            color = Primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { viewModel.showForgotPassword() }
                        )
                    }
                }

                // Error message
                uiState.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = Error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Success message
                uiState.successMessage?.let { success ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = success,
                        style = MaterialTheme.typography.bodySmall,
                        color = Success,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit button
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
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (uiState.isSignUp) "Create Account" else "Sign In",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle sign up / sign in
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (uiState.isSignUp) "Already have an account? " else "Don't have an account? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryLight
                    )
                    Text(
                        text = if (uiState.isSignUp) "Sign In" else "Sign Up",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary,
                        modifier = Modifier.clickable { viewModel.toggleSignUp() }
                    )
                }

                // Skip option (onboarding mode only)
                if (!isFromSettings) {
                    Spacer(modifier = Modifier.height(28.dp))

                    TextButton(onClick = { viewModel.skip(onComplete) }) {
                        Text(
                            text = "Skip for now",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryLight.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "You can sign in later from Settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Terms & Privacy links
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { uriHandler.openUri("https://dailywell.hekax.com/privacy/") }
                )
                Text(
                    text = "  |  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight.copy(alpha = 0.3f)
                )
                Text(
                    text = "Terms of Service",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { uriHandler.openUri("https://dailywell.hekax.com/terms/") }
                )
            }
        }
    }
}
}

@Composable
private fun ForgotPasswordContent(
    uiState: AuthUiState,
    viewModel: AuthViewModel,
    onForgotPassword: (suspend (String) -> Result<Unit>)?
) {
    OutlinedTextField(
        value = uiState.email,
        onValueChange = { viewModel.setEmail(it) },
        label = { Text("Email") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done
        )
    )

    // Error
    uiState.errorMessage?.let { error ->
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = Error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Send reset link button
    Button(
        onClick = {
            if (onForgotPassword != null) {
                viewModel.submitForgotPassword(onResetPassword = onForgotPassword)
            }
        },
        enabled = !uiState.isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Primary)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "Send Reset Link",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    TextButton(onClick = { viewModel.hideForgotPassword() }) {
        Text(
            text = "Back to Sign In",
            style = MaterialTheme.typography.bodyMedium,
            color = Primary
        )
    }
}


