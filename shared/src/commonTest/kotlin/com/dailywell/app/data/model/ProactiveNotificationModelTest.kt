package com.dailywell.app.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProactiveNotificationModelTest {

    @Test
    fun valueScoreComputesTotalAndThreshold() {
        val passing = NotificationValueScore(risk = 20, readiness = 20, novelty = 10, impact = 10, trust = 10)
        val failing = NotificationValueScore(risk = 10, readiness = 10, novelty = 10, impact = 10, trust = 10)

        assertEquals(70, passing.total)
        assertTrue(passing.passes)
        assertEquals(50, failing.total)
        assertFalse(failing.passes)
    }

    @Test
    fun guardrailsDetectAndSanitizeBannedLanguage() {
        val original = "Don't forget your streak. Hurry, time is running out."
        val sanitized = NotificationGuardrails.sanitize(original)

        assertTrue(NotificationGuardrails.containsBannedPhrase(original))
        assertFalse(NotificationGuardrails.containsBannedPhrase(sanitized))
    }

    @Test
    fun behaviorCategoryMappingMatchesExpectedBuckets() {
        assertEquals(
            NotificationBehaviorCategory.CELEBRATION,
            NotificationBehaviorCategory.fromType(ProactiveNotificationType.ACHIEVEMENT_UNLOCKED)
        )
        assertEquals(
            NotificationBehaviorCategory.STREAK_SHIELD,
            NotificationBehaviorCategory.fromType(ProactiveNotificationType.STREAK_AT_RISK)
        )
        assertEquals(
            NotificationBehaviorCategory.SOCIAL_WHISPER,
            NotificationBehaviorCategory.fromType(ProactiveNotificationType.COACH_OUTREACH)
        )
    }

    @Test
    fun templatesAndTitlesExistForAllTypes() {
        ProactiveNotificationType.entries.forEach { type ->
            val template = ProactiveNotificationTemplates.getTemplate(type)
            val title = ProactiveNotificationTemplates.getTitle(type, coachName = "Coach Sam")

            assertTrue(template.isNotBlank(), "Template should not be blank for $type")
            assertTrue(title.isNotBlank(), "Title should not be blank for $type")
        }

        assertEquals(
            "Coach Sam Says...",
            ProactiveNotificationTemplates.getTitle(ProactiveNotificationType.COACH_OUTREACH, "Coach Sam")
        )
    }
}
