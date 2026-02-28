package com.dailywell.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Centralized Material Icon mapping for DailyWell
 *
 * Replaces ALL emoji usage throughout the app with Material Icons.
 * Organized by category for easy discovery and consistency.
 */
object DailyWellIcons {

    // ==================== NAVIGATION ====================
    object Nav {
        val Today = Icons.Filled.CheckCircle
        val Insights = Icons.Filled.Insights
        val Track = Icons.Filled.FitnessCenter
        val Coach = Icons.Filled.Psychology
        val You = Icons.Filled.Person
        val Back = Icons.AutoMirrored.Filled.ArrowBack
        val Close = Icons.Filled.Close
        val Settings = Icons.Filled.Settings
        val Menu = Icons.Filled.Menu
        val MoreVert = Icons.Filled.MoreVert
        val ExpandMore = Icons.Filled.ExpandMore
        val ExpandLess = Icons.Filled.ExpandLess
        val ChevronRight = Icons.Filled.ChevronRight
        val ArrowForward = Icons.Filled.ArrowForward
    }

    // ==================== HABITS ====================
    object Habits {
        val Sleep = Icons.Filled.Bedtime
        val Water = Icons.Filled.WaterDrop
        val Move = Icons.Filled.DirectionsRun
        val Nourish = Icons.Filled.Restaurant
        val Calm = Icons.Filled.SelfImprovement
        val Connect = Icons.Filled.Diversity3
        val Unplug = Icons.Filled.PhonelinkOff
        val Focus = Icons.Filled.CenterFocusStrong
        val Learn = Icons.Filled.AutoStories
        val Gratitude = Icons.Filled.VolunteerActivism
        val Nature = Icons.Filled.Park
        val Breathe = Icons.Filled.Air
        val Custom = Icons.Filled.Star
        val Add = Icons.Filled.AddCircle
        val HabitStacking = Icons.Filled.Link
        val Intentions = Icons.Filled.TrackChanges
        val SmartReminders = Icons.Filled.NotificationsActive
        val Recovery = Icons.Filled.Autorenew
    }

    // ==================== HEALTH & BODY ====================
    object Health {
        val Heart = Icons.Filled.Favorite
        val HeartOutline = Icons.Outlined.FavoriteBorder
        val Biometric = Icons.Filled.MonitorHeart
        val HealthConnect = Icons.Filled.HealthAndSafety
        val Body = Icons.Filled.Accessibility
        val Weight = Icons.Filled.Scale
        val Measurements = Icons.Filled.Straighten
        val Photos = Icons.Filled.CameraAlt
        val Temperature = Icons.Filled.Thermostat
        val Workout = Icons.Filled.FitnessCenter
        val Steps = Icons.Filled.DirectionsWalk
        val Nutrition = Icons.Filled.Restaurant
        val FoodScan = Icons.Filled.PhotoCamera
        val Calories = Icons.Filled.LocalFireDepartment
        val WaterDrop = Icons.Filled.WaterDrop
        val Protein = Icons.Filled.Egg
        val Family = Icons.Filled.FamilyRestroom
    }

    // ==================== SOCIAL ====================
    object Social {
        val Group = Icons.Filled.Groups
        val Person = Icons.Filled.Person
        val People = Icons.Filled.People
        val Partner = Icons.Filled.Handshake
        val HighFive = Icons.Filled.WavingHand
        val Contract = Icons.Filled.Description
        val Share = Icons.Filled.Share
        val Invite = Icons.Filled.PersonAdd
        val Leaderboard = Icons.Filled.Leaderboard
        val Cheer = Icons.Filled.Celebration
        val Referral = Icons.Filled.CardGiftcard
    }

    // ==================== COACHING & AI ====================
    object Coaching {
        val AICoach = Icons.Filled.Psychology
        val Audio = Icons.Filled.Headphones
        val Microphone = Icons.Filled.Mic
        val VoiceChat = Icons.Filled.RecordVoiceOver
        val Play = Icons.Filled.PlayArrow
        val Pause = Icons.Filled.Pause
        val Stop = Icons.Filled.Stop
        val Send = Icons.AutoMirrored.Filled.Send
        val Chat = Icons.AutoMirrored.Filled.Chat
        val SmartNotification = Icons.Filled.NotificationsActive
        val Lesson = Icons.Filled.MenuBook
        val Quiz = Icons.Filled.Quiz
        val Reflection = Icons.Filled.EditNote
    }

    // ==================== ANALYTICS & INSIGHTS ====================
    object Analytics {
        val Insights = Icons.Filled.Insights
        val TrendUp = Icons.AutoMirrored.Filled.TrendingUp
        val TrendDown = Icons.Filled.TrendingDown
        val TrendFlat = Icons.Filled.TrendingFlat
        val Calendar = Icons.Filled.CalendarMonth
        val BarChart = Icons.Filled.BarChart
        val PieChart = Icons.Filled.PieChart
        val Timeline = Icons.Filled.Timeline
        val Pattern = Icons.Filled.Hub
        val AtRisk = Icons.Filled.Warning
        val Correlation = Icons.Filled.CompareArrows
        val Score = Icons.Filled.Speed
        val Streak = Icons.Filled.LocalFireDepartment
    }

    // ==================== ACTIONS ====================
    object Actions {
        val Check = Icons.Filled.Check
        val CheckCircle = Icons.Filled.CheckCircle
        val Add = Icons.Filled.Add
        val Remove = Icons.Filled.Remove
        val Delete = Icons.Filled.Delete
        val Edit = Icons.Filled.Edit
        val Save = Icons.Filled.Save
        val Refresh = Icons.Filled.Refresh
        val Search = Icons.Filled.Search
        val Filter = Icons.Filled.FilterList
        val Sort = Icons.Filled.Sort
        val Download = Icons.Filled.Download
        val Upload = Icons.Filled.Upload
        val Copy = Icons.Filled.ContentCopy
        val Undo = Icons.Filled.Undo
        val Sync = Icons.Filled.Sync
    }

    // ==================== STATUS ====================
    object Status {
        val Success = Icons.Filled.CheckCircle
        val Warning = Icons.Filled.Warning
        val Error = Icons.Filled.Error
        val Info = Icons.Filled.Info
        val Lock = Icons.Filled.Lock
        val Unlock = Icons.Filled.LockOpen
        val Premium = Icons.Filled.WorkspacePremium
        val Star = Icons.Filled.Star
        val StarOutline = Icons.Outlined.Star
        val Verified = Icons.Filled.Verified
        val New = Icons.Filled.NewReleases
        val Notification = Icons.Filled.Notifications
        val NotificationOff = Icons.Filled.NotificationsOff
    }

    // ==================== GAMIFICATION ====================
    object Gamification {
        val Trophy = Icons.Filled.EmojiEvents
        val Badge = Icons.Filled.MilitaryTech
        val Medal = Icons.Filled.WorkspacePremium
        val Crown = Icons.Filled.AutoAwesome
        val Gift = Icons.Filled.CardGiftcard
        val Spin = Icons.Filled.Casino
        val Shield = Icons.Filled.Shield
        val XP = Icons.Filled.Bolt
        val Level = Icons.Filled.Upgrade
        val Challenge = Icons.Filled.Flag
        val Duel = Icons.Filled.SportsKabaddi
        val Reward = Icons.Filled.Redeem
        val Coin = Icons.Filled.MonetizationOn
    }

    // ==================== AUTH ====================
    object Auth {
        val SignOut = Icons.Filled.Logout
        val Delete = Icons.Filled.DeleteForever
        val PasswordChange = Icons.Filled.Lock
        val EditProfile = Icons.Filled.Edit
        val Google = Icons.Filled.AccountCircle
        val Verified = Icons.Filled.Verified
    }

    // ==================== MISC ====================
    object Misc {
        val Secure = Icons.Filled.Security
        val Time = Icons.Filled.Schedule
        val Timer = Icons.Filled.Timer
        val Location = Icons.Filled.LocationOn
        val Cloud = Icons.Filled.Cloud
        val Phone = Icons.Filled.PhoneAndroid
        val Camera = Icons.Filled.CameraAlt
        val Gallery = Icons.Filled.PhotoLibrary
        val Flash = Icons.Filled.FlashOn
        val FlashOff = Icons.Filled.FlashOff
        val Brightness = Icons.Filled.LightMode
        val DarkMode = Icons.Filled.DarkMode
        val Palette = Icons.Filled.Palette
        val Link = Icons.Filled.Link
        val OpenInNew = Icons.Filled.OpenInNew
        val Help = Icons.Filled.HelpOutline
        val Privacy = Icons.Filled.PrivacyTip
        val Sunrise = Icons.Filled.WbSunny
        val Night = Icons.Filled.NightsStay
        val Sparkle = Icons.Filled.AutoAwesome
    }

    // ==================== WEATHER ====================
    object Weather {
        val Sunny = Icons.Filled.WbSunny
        val Cloudy = Icons.Filled.Cloud
        val Rainy = Icons.Filled.Thunderstorm
        val Snowy = Icons.Filled.AcUnit
        val Hot = Icons.Filled.Whatshot
        val Cold = Icons.Filled.SevereCold
        val Storm = Icons.Filled.Thunderstorm
    }

    // ==================== MOOD ====================
    object Mood {
        val Great = Icons.Filled.SentimentVerySatisfied
        val Good = Icons.Filled.SentimentSatisfied
        val Okay = Icons.Filled.SentimentNeutral
        val Low = Icons.Filled.SentimentDissatisfied
        val Struggling = Icons.Filled.SentimentVeryDissatisfied
    }

    // ==================== TIME OF DAY ====================
    object TimeOfDayIcons {
        val Morning = Icons.Filled.WbSunny
        val Afternoon = Icons.Filled.LightMode
        val Evening = Icons.Filled.NightsStay
        val Anytime = Icons.Filled.Schedule
    }

    // ==================== ONBOARDING ====================
    object Onboarding {
        val Welcome = Icons.Filled.Spa
        val Philosophy = Icons.Filled.Lightbulb
        val SelectHabits = Icons.Filled.Checklist
        val Reminder = Icons.Filled.AlarmOn
        val Consistency = Icons.Filled.Verified
        val FirstWin = Icons.Filled.Celebration
        val Ready = Icons.Filled.RocketLaunch
    }

    /**
     * Get Material Icon for a habit by its ID
     */
    fun getHabitIcon(habitId: String): ImageVector {
        return when (habitId.lowercase()) {
            "sleep", "rest" -> Habits.Sleep
            "water", "hydrate" -> Habits.Water
            "move", "exercise" -> Habits.Move
            "vegetables", "nourish" -> Habits.Nourish
            "calm", "meditate", "mindfulness" -> Habits.Calm
            "connect", "social" -> Habits.Connect
            "unplug", "digital_detox" -> Habits.Unplug
            "focus" -> Habits.Focus
            "learn" -> Habits.Learn
            "gratitude" -> Habits.Gratitude
            "nature" -> Habits.Nature
            "breathe" -> Habits.Breathe
            else -> Habits.Custom
        }
    }

    /**
     * Get Material Icon for a coach by their ID
     */
    fun getCoachIcon(coachId: String): ImageVector {
        return when (coachId) {
            "coach_sam", "sam" -> Icons.Filled.School
            "coach_alex", "alex" -> Icons.Filled.Psychology
            "coach_dana", "dana" -> Icons.Filled.Science
            "coach_grace", "grace" -> Icons.Filled.HealthAndSafety
            "coach_mike", "mike" -> Icons.Filled.FitnessCenter
            else -> Icons.Filled.School
        }
    }

    /**
     * Get Material Icon for an achievement badge
     */
    fun getBadgeIcon(badgeName: String): ImageVector {
        return when (badgeName.lowercase()) {
            "streak_7", "week_streak" -> Icons.Filled.LocalFireDepartment
            "streak_30", "month_streak" -> Gamification.Medal
            "streak_100", "century_streak" -> Gamification.Trophy
            "first_habit", "first_completion" -> Status.Star
            "perfect_week" -> Gamification.Crown
            "early_bird" -> Misc.Sunrise
            "night_owl" -> Misc.Night
            "comeback", "comeback_champion" -> Habits.Recovery
            else -> Gamification.Badge
        }
    }

    /**
     * Get Material Icon for a feature category
     */
    fun getCategoryIcon(category: String): ImageVector {
        return when (category.uppercase()) {
            "BUILD_HABITS" -> Habits.HabitStacking
            "TRACK_ANALYZE" -> Analytics.Insights
            "AI_COACHING" -> Coaching.AICoach
            "SOCIAL_FUN" -> Social.Group
            "HEALTH" -> Health.Heart
            else -> Misc.Sparkle
        }
    }

    /**
     * Get Material Icon for a mood level
     */
    fun getMoodIcon(moodName: String): ImageVector {
        return when (moodName.uppercase()) {
            "GREAT" -> Mood.Great
            "GOOD" -> Mood.Good
            "OKAY" -> Mood.Okay
            "LOW" -> Mood.Low
            "STRUGGLING" -> Mood.Struggling
            else -> Mood.Okay
        }
    }

    /**
     * Get Material Icon for time of day
     */
    fun getTimeOfDayIcon(timeOfDay: String): ImageVector {
        return when (timeOfDay.uppercase()) {
            "MORNING" -> TimeOfDayIcons.Morning
            "AFTERNOON" -> TimeOfDayIcons.Afternoon
            "EVENING" -> TimeOfDayIcons.Evening
            "ANYTIME" -> TimeOfDayIcons.Anytime
            else -> TimeOfDayIcons.Anytime
        }
    }
}
