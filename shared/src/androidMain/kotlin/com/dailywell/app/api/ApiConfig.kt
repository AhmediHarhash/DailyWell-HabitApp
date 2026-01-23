package com.dailywell.app.api

import com.dailywell.shared.BuildConfig

/**
 * API Configuration for external services
 * API keys are stored securely in local.properties (not committed to git)
 * and accessed via BuildConfig fields
 */
object ApiConfig {
    // Claude API (Anthropic) - using claude-sonnet-4-20250514 (latest 2026)
    // Key is injected from local.properties at build time
    val CLAUDE_API_KEY: String
        get() = BuildConfig.CLAUDE_API_KEY

    const val CLAUDE_API_URL = "https://api.anthropic.com/v1/messages"
    const val CLAUDE_MODEL = "claude-sonnet-4-20250514"
    const val CLAUDE_API_VERSION = "2023-06-01"

    // TTS is now FREE using Android's built-in TextToSpeech engine
    // No external API required - works offline with neural voices on Android 11+

    // Firebase Project
    const val FIREBASE_PROJECT_ID = "dailywell-habit"
}
