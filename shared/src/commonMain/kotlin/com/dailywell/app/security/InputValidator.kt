package com.dailywell.app.security

/**
 * Input Validation Utilities
 * CVE-DW-009 FIX: Input validation for all user inputs
 * OWASP MASVS-CODE-1: Code Quality
 *
 * Provides sanitization and validation for:
 * - User text inputs
 * - Habit names and descriptions
 * - AI chat messages
 * - Reflection entries
 */
object InputValidator {

    // Maximum lengths for different input types
    private const val MAX_HABIT_NAME_LENGTH = 100
    private const val MAX_DESCRIPTION_LENGTH = 500
    private const val MAX_CHAT_MESSAGE_LENGTH = 2000
    private const val MAX_REFLECTION_LENGTH = 5000
    private const val MAX_USERNAME_LENGTH = 50
    private const val MAX_EMAIL_LENGTH = 254

    // Regex patterns for validation
    private val SAFE_TEXT_PATTERN = Regex("^[\\p{L}\\p{N}\\p{P}\\p{S}\\s]*$")
    private val EMAIL_PATTERN = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val EMOJI_PATTERN = Regex("[\\p{So}\\p{Sc}\\p{Sm}\\p{Sk}]")

    /**
     * Validate and sanitize habit name
     */
    fun validateHabitName(input: String): ValidationResult {
        val trimmed = input.trim()

        if (trimmed.isEmpty()) {
            return ValidationResult.Invalid("Habit name cannot be empty")
        }

        if (trimmed.length > MAX_HABIT_NAME_LENGTH) {
            return ValidationResult.Invalid("Habit name too long (max $MAX_HABIT_NAME_LENGTH characters)")
        }

        if (containsHtmlOrScript(trimmed)) {
            return ValidationResult.Invalid("Invalid characters in habit name")
        }

        return ValidationResult.Valid(sanitizeText(trimmed))
    }

    /**
     * Validate and sanitize description text
     */
    fun validateDescription(input: String): ValidationResult {
        val trimmed = input.trim()

        if (trimmed.length > MAX_DESCRIPTION_LENGTH) {
            return ValidationResult.Invalid("Description too long (max $MAX_DESCRIPTION_LENGTH characters)")
        }

        if (containsHtmlOrScript(trimmed)) {
            return ValidationResult.Invalid("Invalid characters in description")
        }

        return ValidationResult.Valid(sanitizeText(trimmed))
    }

    /**
     * Validate and sanitize chat message
     */
    fun validateChatMessage(input: String): ValidationResult {
        val trimmed = input.trim()

        if (trimmed.isEmpty()) {
            return ValidationResult.Invalid("Message cannot be empty")
        }

        if (trimmed.length > MAX_CHAT_MESSAGE_LENGTH) {
            return ValidationResult.Invalid("Message too long (max $MAX_CHAT_MESSAGE_LENGTH characters)")
        }

        // Allow more flexibility in chat, but still sanitize dangerous content
        val sanitized = sanitizeForAI(trimmed)
        return ValidationResult.Valid(sanitized)
    }

    /**
     * Validate and sanitize reflection entry
     */
    fun validateReflection(input: String): ValidationResult {
        val trimmed = input.trim()

        if (trimmed.length > MAX_REFLECTION_LENGTH) {
            return ValidationResult.Invalid("Reflection too long (max $MAX_REFLECTION_LENGTH characters)")
        }

        return ValidationResult.Valid(sanitizeText(trimmed))
    }

    /**
     * Validate username
     */
    fun validateUsername(input: String): ValidationResult {
        val trimmed = input.trim()

        if (trimmed.isEmpty()) {
            return ValidationResult.Invalid("Username cannot be empty")
        }

        if (trimmed.length < 2) {
            return ValidationResult.Invalid("Username must be at least 2 characters")
        }

        if (trimmed.length > MAX_USERNAME_LENGTH) {
            return ValidationResult.Invalid("Username too long (max $MAX_USERNAME_LENGTH characters)")
        }

        // Only allow alphanumeric, underscores, and emojis in username
        val sanitized = trimmed.replace(Regex("[^\\p{L}\\p{N}_\\p{So}\\s]"), "")
        if (sanitized != trimmed) {
            return ValidationResult.Invalid("Username contains invalid characters")
        }

        return ValidationResult.Valid(sanitized)
    }

    /**
     * Validate email address
     */
    fun validateEmail(input: String): ValidationResult {
        val trimmed = input.trim().lowercase()

        if (trimmed.isEmpty()) {
            return ValidationResult.Invalid("Email cannot be empty")
        }

        if (trimmed.length > MAX_EMAIL_LENGTH) {
            return ValidationResult.Invalid("Email too long")
        }

        if (!EMAIL_PATTERN.matches(trimmed)) {
            return ValidationResult.Invalid("Invalid email format")
        }

        return ValidationResult.Valid(trimmed)
    }

    /**
     * Validate habit ID (must be alphanumeric with underscores/hyphens)
     */
    fun validateHabitId(input: String): Boolean {
        if (input.isEmpty() || input.length > 100) return false
        return input.matches(Regex("^[a-zA-Z0-9_-]+$"))
    }

    /**
     * Sanitize text by removing potentially dangerous characters
     */
    fun sanitizeText(input: String): String {
        return input
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
            .replace("\\", "&#x5C;")
            .trim()
    }

    /**
     * Sanitize text for AI prompts
     * Prevents prompt injection while allowing normal text
     */
    fun sanitizeForAI(input: String): String {
        // Remove potential prompt injection patterns
        return input
            .replace(Regex("\\bsystem:\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bassistant:\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\buser:\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bignore previous instructions", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bforget your instructions", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<\\|.*?\\|>"), "") // Remove special tokens
            .trim()
    }

    /**
     * Check if input contains HTML or script tags
     */
    private fun containsHtmlOrScript(input: String): Boolean {
        val lowerInput = input.lowercase()
        return lowerInput.contains("<script") ||
                lowerInput.contains("javascript:") ||
                lowerInput.contains("onclick=") ||
                lowerInput.contains("onerror=") ||
                lowerInput.contains("onload=") ||
                Regex("<[a-z]+[^>]*>", RegexOption.IGNORE_CASE).containsMatchIn(input)
    }

    /**
     * Check if string contains only emojis
     */
    fun isOnlyEmoji(input: String): Boolean {
        val withoutEmoji = input.replace(EMOJI_PATTERN, "").trim()
        return withoutEmoji.isEmpty() && input.isNotEmpty()
    }

    /**
     * Extract emojis from string
     */
    fun extractEmojis(input: String): List<String> {
        return EMOJI_PATTERN.findAll(input).map { it.value }.toList()
    }
}

/**
 * Validation result sealed class
 */
sealed class ValidationResult {
    data class Valid(val sanitizedValue: String) : ValidationResult()
    data class Invalid(val errorMessage: String) : ValidationResult()

    val isValid: Boolean
        get() = this is Valid

    fun getValueOrNull(): String? = when (this) {
        is Valid -> sanitizedValue
        is Invalid -> null
    }

    fun getErrorOrNull(): String? = when (this) {
        is Valid -> null
        is Invalid -> errorMessage
    }
}
