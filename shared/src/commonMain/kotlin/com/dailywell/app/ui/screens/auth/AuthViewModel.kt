package com.dailywell.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isSignUp: Boolean = false,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSuccess: Boolean = false,
    val showForgotPassword: Boolean = false,
    val forgotPasswordSent: Boolean = false
)

class AuthViewModel(
    private val settingsRepository: SettingsRepository,
    private val getFirebaseUid: () -> String? = { null },
    private val getFirebaseDisplayName: () -> String? = { null },
    private val getFirebaseEmail: () -> String? = { null }
) : ViewModel() {

    private val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    private fun isValidEmail(email: String): Boolean = emailPattern.matches(email.trim())

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun setEmail(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun setPassword(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun setDisplayName(name: String) {
        _uiState.update { it.copy(displayName = name, errorMessage = null) }
    }

    fun toggleSignUp() {
        _uiState.update {
            it.copy(
                isSignUp = !it.isSignUp,
                errorMessage = null,
                successMessage = null,
                showForgotPassword = false,
                forgotPasswordSent = false
            )
        }
    }

    fun showForgotPassword() {
        _uiState.update { it.copy(showForgotPassword = true, errorMessage = null, successMessage = null) }
    }

    fun hideForgotPassword() {
        _uiState.update { it.copy(showForgotPassword = false, errorMessage = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun submitForgotPassword(
        onResetPassword: suspend (String) -> Result<Unit>
    ) {
        val state = _uiState.value
        val email = state.email.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email address") }
            return
        }
        if (!isValidEmail(email)) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = onResetPassword(email)
                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                forgotPasswordSent = true,
                                showForgotPassword = false,
                                successMessage = "Password reset email sent to $email",
                                errorMessage = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = error.message ?: "Failed to send reset email")
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Something went wrong")
                }
            }
        }
    }

    fun signInWithGoogle(
        onGoogleSignIn: suspend () -> Result<Unit>,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGoogleLoading = true, errorMessage = null) }
            try {
                val result = onGoogleSignIn()
                result.fold(
                    onSuccess = {
                        val settings = settingsRepository.getSettings().first()
                        _uiState.update { it.copy(isGoogleLoading = false, isSuccess = true) }
                        runCatching {
                            settingsRepository.updateSettings(
                                settings.copy(
                                    hasCompletedAuth = true,
                                    authProvider = "google",
                                    firebaseUid = getFirebaseUid(),
                                    displayName = getFirebaseDisplayName() ?: settings.displayName,
                                    userEmail = getFirebaseEmail() ?: settings.userEmail
                                )
                            )
                        }.onFailure { error ->
                            println("Auth success but local auth settings update failed: ${error.message}")
                        }
                        onComplete()
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(isGoogleLoading = false, errorMessage = error.message ?: "Google sign-in failed")
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isGoogleLoading = false, errorMessage = e.message ?: "Something went wrong")
                }
            }
        }
    }

    fun submit(
        onSignIn: suspend (email: String, password: String) -> Result<Unit>,
        onSignUp: suspend (email: String, password: String, displayName: String) -> Result<Unit>,
        onComplete: () -> Unit
    ) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in all fields") }
            return
        }
        if (!isValidEmail(state.email)) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address") }
            return
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
            return
        }
        if (state.isSignUp && state.displayName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your name") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val result = if (state.isSignUp) {
                    onSignUp(state.email.trim(), state.password, state.displayName.ifBlank { "User" })
                } else {
                    onSignIn(state.email.trim(), state.password)
                }

                result.fold(
                    onSuccess = {
                        val settings = settingsRepository.getSettings().first()
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                        runCatching {
                            settingsRepository.updateSettings(
                                settings.copy(
                                    hasCompletedAuth = true,
                                    userEmail = state.email.trim(),
                                    authProvider = "email",
                                    firebaseUid = getFirebaseUid(),
                                    displayName = if (state.isSignUp) state.displayName.ifBlank { null } else (getFirebaseDisplayName() ?: settings.displayName)
                                )
                            )
                        }.onFailure { error ->
                            println("Auth success but local auth settings update failed: ${error.message}")
                        }
                        onComplete()
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Authentication failed"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Something went wrong")
                }
            }
        }
    }

    fun skip(onComplete: () -> Unit) {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings().first()
            settingsRepository.updateSettings(
                settings.copy(authSkipped = true)
            )
            onComplete()
        }
    }
}
