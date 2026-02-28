package com.dailywell.app.api

sealed class AuthState {
    data object Unknown : AuthState()
    data object SignedOut : AuthState()
    data class Anonymous(val uid: String) : AuthState()
    data class Authenticated(
        val uid: String,
        val email: String?,
        val displayName: String?,
        val photoUrl: String?,
        val isEmailVerified: Boolean,
        val providers: List<String>
    ) : AuthState()
}
