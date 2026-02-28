package com.dailywell.app.ui.navigation

internal enum class NotificationDeepLinkTarget {
    TODAY,
    INSIGHTS,
    COACH,
    TRACK,
    YOU,
    ACHIEVEMENTS,
    UNKNOWN
}

internal fun parseNotificationDeepLink(deepLink: String?): NotificationDeepLinkTarget {
    val normalized = deepLink
        ?.trim()
        ?.lowercase()
        ?.substringAfter("dailywell://", missingDelimiterValue = "")
        ?.substringBefore("?")
        ?.substringBefore("#")
        .orEmpty()

    return when (normalized) {
        "today" -> NotificationDeepLinkTarget.TODAY
        "insights" -> NotificationDeepLinkTarget.INSIGHTS
        "coaching" -> NotificationDeepLinkTarget.COACH
        "track" -> NotificationDeepLinkTarget.TRACK
        "you", "social" -> NotificationDeepLinkTarget.YOU
        "achievements" -> NotificationDeepLinkTarget.ACHIEVEMENTS
        else -> NotificationDeepLinkTarget.UNKNOWN
    }
}
