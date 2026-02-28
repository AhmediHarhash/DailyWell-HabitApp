package com.dailywell.app.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationDeepLinkTest {

    @Test
    fun parse_todayLink_routesToToday() {
        val target = parseNotificationDeepLink("dailywell://today")
        assertEquals(NotificationDeepLinkTarget.TODAY, target)
    }

    @Test
    fun parse_insightsLinkWithQuery_routesToInsights() {
        val target = parseNotificationDeepLink("dailywell://insights?from=push")
        assertEquals(NotificationDeepLinkTarget.INSIGHTS, target)
    }

    @Test
    fun parse_coachingLinkWithFragment_routesToCoach() {
        val target = parseNotificationDeepLink("dailywell://coaching#thread")
        assertEquals(NotificationDeepLinkTarget.COACH, target)
    }

    @Test
    fun parse_socialAlias_routesToYou() {
        val target = parseNotificationDeepLink("  DAILYWELL://social  ")
        assertEquals(NotificationDeepLinkTarget.YOU, target)
    }

    @Test
    fun parse_achievements_routesToAchievements() {
        val target = parseNotificationDeepLink("dailywell://achievements")
        assertEquals(NotificationDeepLinkTarget.ACHIEVEMENTS, target)
    }

    @Test
    fun parse_unknownOrNull_returnsUnknown() {
        assertEquals(NotificationDeepLinkTarget.UNKNOWN, parseNotificationDeepLink(null))
        assertEquals(NotificationDeepLinkTarget.UNKNOWN, parseNotificationDeepLink("dailywell://unknown"))
        assertEquals(NotificationDeepLinkTarget.UNKNOWN, parseNotificationDeepLink("https://dailywell.app/insights"))
    }
}
