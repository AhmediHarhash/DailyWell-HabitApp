package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

/**
 * Audio Micro-Coaching
 * Short audio tips and guidance for habit formation
 *
 * Fabulous charges $40/yr for this - high perceived value
 * Focus: 2-3 minute daily tips, motivational messages, expert guidance
 */
@Serializable
data class AudioCoachingData(
    val unlockedTracks: List<String> = emptyList(),
    val completedTracks: List<String> = emptyList(),
    val favoritesTracks: List<String> = emptyList(),
    val currentStreak: Int = 0,
    val lastListenedAt: String? = null,
    val totalListenTime: Int = 0,  // in seconds
    val preferences: AudioPreferences = AudioPreferences()
)

@Serializable
data class AudioPreferences(
    val autoPlayMorning: Boolean = false,
    val autoPlayEvening: Boolean = false,
    val preferredVoice: VoiceType = VoiceType.CALM,
    val playbackSpeed: Float = 1.0f,
    val enableBackgroundPlay: Boolean = true
)

@Serializable
enum class VoiceType(val label: String, val description: String) {
    CALM("Calm", "Soothing, meditative tone"),
    ENERGETIC("Energetic", "Upbeat, motivating tone"),
    WARM("Warm", "Friendly, supportive tone"),
    DIRECT("Direct", "Clear, no-nonsense tone")
}

/**
 * Audio Track - a single piece of audio content
 */
@Serializable
data class AudioTrack(
    val id: String,
    val title: String,
    val description: String,
    val category: AudioCategory,
    val habitId: String? = null,           // null = general
    val durationSeconds: Int,
    val transcript: String? = null,        // For accessibility
    val isPremium: Boolean = true,
    val isNew: Boolean = false,
    val releaseDate: String,
    val tags: List<String> = emptyList(),
    val voiceType: VoiceType = VoiceType.CALM,
    val scienceReference: String? = null   // Research backing the advice
)

@Serializable
enum class AudioCategory(val label: String, val emoji: String, val description: String) {
    MORNING_MINDSET("Morning Mindset", "🌅", "Start your day with intention"),
    EVENING_WINDDOWN("Evening Wind-down", "🌙", "Reflect and prepare for rest"),
    HABIT_SCIENCE("Habit Science", "🧠", "Understand the science of change"),
    MOTIVATION("Motivation", "💪", "When you need a boost"),
    BREATHING("Breathing", "🌬️", "Quick breathing exercises"),
    FOCUS("Focus", "🎯", "Sharpen your concentration"),
    STRESS_RELIEF("Stress Relief", "🧘", "Calm your mind"),
    SLEEP_STORIES("Sleep Stories", "😴", "Drift off peacefully"),
    CELEBRATION("Celebration", "🎉", "Celebrate your wins"),
    COMEBACK("Comeback", "🔥", "Get back on track")
}

/**
 * Audio Playlist - curated collection of tracks
 */
@Serializable
data class AudioPlaylist(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val trackIds: List<String>,
    val totalDuration: Int,               // in seconds
    val isPremium: Boolean = true,
    val isSequential: Boolean = true      // Play in order vs shuffle
)

/**
 * Pre-built audio content library
 * Note: In production, these would link to actual audio files
 */
object AudioLibrary {

    val tracks = listOf(
        // Morning Mindset
        AudioTrack(
            id = "morning_intention_1",
            title = "Set Your Intention",
            description = "A 2-minute guide to starting your day with purpose",
            category = AudioCategory.MORNING_MINDSET,
            durationSeconds = 120,
            transcript = """
                Good morning. Take a deep breath.
                Today is a new opportunity to become the person you want to be.
                As you breathe out, release yesterday's imperfections.
                Now, set your intention: What ONE habit matters most today?
                Picture yourself completing it. How does it feel?
                Hold that feeling. You've already taken the first step.
                Now go make it real.
            """.trimIndent(),
            isPremium = false,  // Free sample
            releaseDate = "2024-01-01",
            tags = listOf("morning", "intention", "mindset"),
            scienceReference = "Research shows morning intention-setting increases follow-through by 40%"
        ),
        AudioTrack(
            id = "morning_energy_1",
            title = "Energy Activation",
            description = "Quick energizing routine to shake off grogginess",
            category = AudioCategory.MORNING_MINDSET,
            durationSeconds = 180,
            transcript = """
                Let's wake up your body and mind.
                Stand up if you can. If not, sit tall.
                Roll your shoulders back three times.
                Now take three quick breaths - in through nose, out through mouth.
                Shake your hands like you're flicking water off them.
                Smile. Yes, even if you don't feel like it.
                Your brain doesn't know the difference - it releases the same happy chemicals.
                Feel that? That's you activating your best self.
                Now, what's the first habit you'll conquer today?
            """.trimIndent(),
            releaseDate = "2024-01-01",
            tags = listOf("morning", "energy", "activation")
        ),

        // Habit Science
        AudioTrack(
            id = "habit_science_1",
            title = "The 66-Day Myth",
            description = "The truth about how long habits really take to form",
            category = AudioCategory.HABIT_SCIENCE,
            durationSeconds = 150,
            transcript = """
                You've heard it takes 21 days to form a habit. That's a myth.
                Research from University College London found it actually takes 66 days on average.
                But here's the key insight: 'on average' hides huge variation.
                Simple habits like drinking water? Maybe 18 days.
                Complex habits like daily exercise? Could be 254 days.
                Don't focus on the number. Focus on repetition and consistency.
                Missing one day doesn't reset your progress.
                What matters is getting back on track the very next day.
                Your brain is literally rewiring itself each time you repeat the behavior.
                Be patient with yourself. You're building neural pathways.
            """.trimIndent(),
            releaseDate = "2024-01-05",
            tags = listOf("science", "habit formation", "patience"),
            scienceReference = "Phillippa Lally, UCL Study 2009"
        ),
        AudioTrack(
            id = "habit_science_2",
            title = "The Cue-Routine-Reward Loop",
            description = "Understanding the brain's habit mechanism",
            category = AudioCategory.HABIT_SCIENCE,
            durationSeconds = 180,
            transcript = """
                Every habit follows the same pattern: Cue, Routine, Reward.
                The cue triggers your brain to start the behavior.
                The routine is the habit itself.
                The reward is what your brain gets out of it.
                Here's the secret: you can't eliminate bad habits, only replace them.
                Keep the same cue and reward, but change the routine.
                Stressed and reaching for chips? The cue is stress, the reward is relief.
                Keep the cue, swap the routine - maybe deep breaths instead.
                Same cue, same reward, healthier behavior.
                What habit loop could you redesign today?
            """.trimIndent(),
            releaseDate = "2024-01-10",
            tags = listOf("science", "habit loop", "behavior change"),
            scienceReference = "Charles Duhigg, The Power of Habit"
        ),

        // Motivation
        AudioTrack(
            id = "motivation_slump_1",
            title = "When Motivation Disappears",
            description = "What to do when you just don't feel like it",
            category = AudioCategory.MOTIVATION,
            durationSeconds = 120,
            transcript = """
                You don't feel motivated. That's okay.
                Here's the secret: motivation follows action, not the other way around.
                Waiting to feel motivated is like waiting to feel hungry before learning to cook.
                Just start. The smallest possible version of your habit.
                One push-up. One sip of water. One minute of stillness.
                Your brain rewards completion, not intention.
                Once you start, momentum builds.
                You don't have to feel like doing it. You just have to do it.
                Feel like it later.
            """.trimIndent(),
            isPremium = false,  // Free sample
            releaseDate = "2024-01-15",
            tags = listOf("motivation", "action", "momentum")
        ),

        // Breathing
        AudioTrack(
            id = "breathing_calm_1",
            title = "Box Breathing Reset",
            description = "Navy SEAL technique for instant calm",
            category = AudioCategory.BREATHING,
            habitId = "calm",
            durationSeconds = 180,
            transcript = """
                Let's do box breathing - used by Navy SEALs to stay calm under pressure.
                Breathe in for 4 counts... 1, 2, 3, 4.
                Hold for 4 counts... 1, 2, 3, 4.
                Breathe out for 4 counts... 1, 2, 3, 4.
                Hold empty for 4 counts... 1, 2, 3, 4.
                Again. In... 1, 2, 3, 4.
                Hold... 1, 2, 3, 4.
                Out... 1, 2, 3, 4.
                Hold... 1, 2, 3, 4.
                One more round on your own.
                Notice how your heart rate has slowed.
                You've just activated your parasympathetic nervous system.
                You can use this anytime, anywhere.
            """.trimIndent(),
            releaseDate = "2024-01-20",
            tags = listOf("breathing", "calm", "stress relief"),
            scienceReference = "Box breathing activates the vagus nerve, reducing cortisol"
        ),

        // Evening Wind-down
        AudioTrack(
            id = "evening_reflection_1",
            title = "Daily Wins Review",
            description = "End your day by acknowledging progress",
            category = AudioCategory.EVENING_WINDDOWN,
            durationSeconds = 150,
            transcript = """
                As this day ends, let's celebrate what went right.
                Think of one habit you completed today. Just one.
                Even if it was small, you showed up.
                That matters more than you know.
                Your brain is 3x more likely to repeat a behavior when you acknowledge it.
                So say it out loud or in your mind: I did _____ today.
                Now, think of one thing you're grateful for.
                And one thing you're looking forward to tomorrow.
                That's it. You've just primed your brain for success.
                Sleep well. Tomorrow is another opportunity.
            """.trimIndent(),
            habitId = "sleep",
            releaseDate = "2024-01-25",
            tags = listOf("evening", "reflection", "gratitude")
        ),

        // Comeback
        AudioTrack(
            id = "comeback_restart_1",
            title = "The Restart Button",
            description = "When you've fallen off track",
            category = AudioCategory.COMEBACK,
            durationSeconds = 120,
            transcript = """
                You missed some days. Maybe a lot of days.
                And now there's a voice saying 'what's the point?'
                Here's what I want you to know:
                Every master was once a disaster.
                Every streak started at day one.
                The difference between those who succeed and those who don't?
                It's not perfection. It's resilience.
                It's the willingness to restart, again and again.
                Right now, in this moment, you can begin again.
                Not tomorrow. Not Monday. Now.
                What's one tiny thing you can do in the next 60 seconds?
                Do that. You've already turned the tide.
            """.trimIndent(),
            isPremium = false,  // Free for recovery
            releaseDate = "2024-02-01",
            tags = listOf("comeback", "restart", "resilience")
        ),

        // Celebration
        AudioTrack(
            id = "celebration_streak_7",
            title = "One Week Wonder",
            description = "Celebrating your 7-day streak",
            category = AudioCategory.CELEBRATION,
            durationSeconds = 90,
            transcript = """
                Seven days. One whole week of showing up.
                Do you know what that means?
                Your brain has started to expect this behavior.
                You're no longer fighting against yourself - you're building momentum.
                Research shows that after 7 days, the mental effort required drops by half.
                You did the hard part. Now it gets easier.
                Take a moment to feel proud. You earned this.
                Let's keep going.
            """.trimIndent(),
            releaseDate = "2024-02-05",
            tags = listOf("celebration", "streak", "milestone"),
            scienceReference = "Cognitive load decreases 50% after 7 consecutive repetitions"
        )
    )

    val playlists = listOf(
        AudioPlaylist(
            id = "morning_essentials",
            name = "Morning Essentials",
            description = "The perfect way to start your day",
            emoji = "🌅",
            trackIds = listOf("morning_intention_1", "morning_energy_1", "breathing_calm_1"),
            totalDuration = 480,
            isPremium = true
        ),
        AudioPlaylist(
            id = "habit_academy",
            name = "Habit Academy",
            description = "Learn the science of lasting change",
            emoji = "🎓",
            trackIds = listOf("habit_science_1", "habit_science_2"),
            totalDuration = 330,
            isPremium = true
        ),
        AudioPlaylist(
            id = "motivation_boost",
            name = "Need a Boost",
            description = "When you need that extra push",
            emoji = "⚡",
            trackIds = listOf("motivation_slump_1", "comeback_restart_1"),
            totalDuration = 240,
            isPremium = false
        ),
        AudioPlaylist(
            id = "evening_routine",
            name = "Evening Routine",
            description = "Wind down and reflect",
            emoji = "🌙",
            trackIds = listOf("evening_reflection_1"),
            totalDuration = 150,
            isPremium = true
        )
    )

    fun getTrackById(id: String): AudioTrack? = tracks.find { it.id == id }

    fun getTracksByCategory(category: AudioCategory): List<AudioTrack> =
        tracks.filter { it.category == category }

    fun getTracksByHabit(habitId: String): List<AudioTrack> =
        tracks.filter { it.habitId == habitId }

    fun getFreeTracks(): List<AudioTrack> =
        tracks.filter { !it.isPremium }

    fun getPlaylistById(id: String): AudioPlaylist? = playlists.find { it.id == id }

    fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (remainingSeconds == 0) "${minutes}m" else "${minutes}m ${remainingSeconds}s"
    }
}
