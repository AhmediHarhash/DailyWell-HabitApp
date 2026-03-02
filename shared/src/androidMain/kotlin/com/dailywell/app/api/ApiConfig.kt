package com.dailywell.app.api

import com.dailywell.shared.BuildConfig

/**
 * API Configuration for external services
 * API keys are stored securely in local.properties (not committed to git)
 * and accessed via BuildConfig fields
 */
object ApiConfig {
    // Claude API (Anthropic)
    // Key is injected from local.properties at build time
    val CLAUDE_API_KEY: String
        get() = BuildConfig.CLAUDE_API_KEY

    const val CLAUDE_API_URL = "https://api.anthropic.com/v1/messages"
    const val CLAUDE_MODEL = ClaudeModelAliases.SCANNER_SONNET
    const val CLAUDE_OPUS_MODEL = ClaudeModelAliases.WEEKLY_REPORT_OPUS
    const val CLAUDE_API_VERSION = "2023-06-01"

    // Task-specific model routing
    const val CLAUDE_COACH_MODEL = ClaudeModelAliases.COACH_HAIKU
    const val CLAUDE_SCANNER_MODEL = ClaudeModelAliases.SCANNER_SONNET
    const val CLAUDE_WEEKLY_REPORT_MODEL = ClaudeModelAliases.WEEKLY_REPORT_OPUS

    // TTS is now FREE using Android's built-in TextToSpeech engine
    // No external API required - works offline with neural voices on Android 11+

    // Firebase Project
    const val FIREBASE_PROJECT_ID = "dailywell-habit"

    // Open Food Facts API (FREE - no API key required!)
    // 4+ million products database
    const val OPEN_FOOD_FACTS_API_URL = "https://world.openfoodfacts.org/api/v2/product"
    const val OPEN_FOOD_FACTS_SEARCH_URL = "https://world.openfoodfacts.org/cgi/search.pl"
    const val OPEN_FOOD_FACTS_USER_AGENT = "DailyWell/1.0 (Android) - habit tracking app"

    // Claude Haiku for coaching chat (high-frequency interactive flow)
    const val CLAUDE_HAIKU_MODEL = ClaudeModelAliases.COACH_HAIKU
}
