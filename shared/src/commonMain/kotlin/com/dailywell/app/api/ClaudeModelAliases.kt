package com.dailywell.app.api

/**
 * Centralized Claude model routing aliases.
 *
 * Keep model IDs in one place so upgrades are one-line changes.
 * `JOKER_LATEST` is a marker for future automation when Anthropic exposes
 * a stable wildcard selector for production model resolution.
 */
object ClaudeModelAliases {
    const val JOKER_LATEST = "*"

    // Chat/coach: fast and affordable, highest call volume.
    // Uses Anthropic's rolling latest alias for Haiku.
    const val COACH_HAIKU = "claude-3-5-haiku-latest"

    // Food scanner: higher reasoning/vision reliability.
    // Uses Sonnet 4 major-line alias for forward-compatible upgrades.
    const val SCANNER_SONNET = "claude-sonnet-4-0"

    // Weekly reports: best long-form synthesis quality.
    // Use Opus 4.1 alias so weekly reports automatically track latest 4.1 snapshot.
    const val WEEKLY_REPORT_OPUS = "claude-opus-4-1"
}
