package com.dailywell.app.data.content

/**
 * ReflectionPromptsDatabase - 120 Unique Weekly Reflection Prompts
 *
 * PRODUCTION-READY: No repetition for 6+ months of weekly reflections.
 * Organized by theme and journey stage for contextual relevance.
 *
 * Themes (6 × 20 prompts = 120):
 * 1. Gratitude & Appreciation (20)
 * 2. Challenges & Growth (20)
 * 3. Progress & Wins (20)
 * 4. Future Planning (20)
 * 5. Self-Discovery (20)
 * 6. Relationships & Connection (20)
 */
object ReflectionPromptsDatabase {

    // ==================== DATA MODELS ====================

    data class ReflectionPrompt(
        val id: String,
        val theme: ReflectionTheme,
        val question: String,
        val subPrompt: String? = null,
        val journeyStage: JourneyStage = JourneyStage.ALL,
        val difficulty: PromptDifficulty = PromptDifficulty.MEDIUM
    )

    enum class ReflectionTheme {
        GRATITUDE,
        CHALLENGES,
        PROGRESS,
        FUTURE,
        SELF_DISCOVERY,
        RELATIONSHIPS
    }

    enum class JourneyStage {
        BEGINNER,      // First 1-2 weeks
        BUILDING,      // Weeks 3-8
        MAINTAINING,   // Weeks 9+
        RECOVERING,    // After a break
        ALL            // Suitable for any stage
    }

    enum class PromptDifficulty {
        EASY,          // Surface-level reflection
        MEDIUM,        // Moderate introspection
        DEEP           // Profound self-examination
    }

    // ==================== GRATITUDE & APPRECIATION (20) ====================

    private val gratitudePrompts = listOf(
        ReflectionPrompt(
            "gr001", ReflectionTheme.GRATITUDE,
            "What's one small thing that happened this week that you're grateful for?",
            "It doesn't have to be profound—a good meal, a kind word, a moment of peace all count.",
            JourneyStage.BEGINNER, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "gr002", ReflectionTheme.GRATITUDE,
            "Who supported your habits this week, directly or indirectly?",
            "Think about people who made space for your growth or encouraged you.",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "gr003", ReflectionTheme.GRATITUDE,
            "What ability or skill allowed you to pursue your habits this week?",
            "Consider physical abilities, mental capacities, or resources you might take for granted.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "gr004", ReflectionTheme.GRATITUDE,
            "Looking back at where you started, what progress deserves appreciation?",
            "Compare today's you to the you who first began this journey.",
            JourneyStage.BUILDING, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "gr005", ReflectionTheme.GRATITUDE,
            "What unexpected gift or opportunity arose from maintaining your habits?",
            "Sometimes benefits appear in surprising ways—new connections, insights, or opportunities.",
            JourneyStage.MAINTAINING, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "gr006", ReflectionTheme.GRATITUDE,
            "What would past-you think about your current habits and progress?",
            "Imagine the you from 6 months or a year ago seeing you now.",
            JourneyStage.MAINTAINING, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "gr007", ReflectionTheme.GRATITUDE,
            "What challenge this week are you now grateful for, even though it was hard?",
            "Difficulties often teach us the most valuable lessons.",
            JourneyStage.ALL, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "gr008", ReflectionTheme.GRATITUDE,
            "Which habit has become easier over time? Take a moment to appreciate that growth.",
            "What once required effort now flows more naturally.",
            JourneyStage.BUILDING, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "gr009", ReflectionTheme.GRATITUDE,
            "What aspect of your health or wellbeing are you most grateful for this week?",
            "Physical, mental, emotional, or spiritual wellbeing all count.",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "gr010", ReflectionTheme.GRATITUDE,
            "How has the ability to restart after a slip been a gift?",
            "The power to begin again is precious.",
            JourneyStage.RECOVERING, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "gr011", ReflectionTheme.GRATITUDE,
            "What environment or circumstance made your habits possible this week?",
            "Consider your living situation, work flexibility, or access to resources.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "gr012", ReflectionTheme.GRATITUDE,
            "What lesson from a mistake or failure are you now thankful for?",
            "Errors often contain the seeds of wisdom.",
            JourneyStage.ALL, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "gr013", ReflectionTheme.GRATITUDE,
            "Who in your life would you thank for influencing your desire to improve?",
            "Sometimes inspiration comes from unexpected sources.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "gr014", ReflectionTheme.GRATITUDE,
            "What simple pleasure enhanced your week?",
            "Morning coffee, a sunset, a favorite song—what brought you joy?",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "gr015", ReflectionTheme.GRATITUDE,
            "How has technology supported your habit journey?",
            "Apps, reminders, communities, or content that helped you.",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "gr016", ReflectionTheme.GRATITUDE,
            "What strength in yourself are you grateful to have discovered?",
            "Habits reveal our capabilities in surprising ways.",
            JourneyStage.BUILDING, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "gr017", ReflectionTheme.GRATITUDE,
            "What would your life look like without your current positive habits?",
            "Contrast often highlights appreciation.",
            JourneyStage.MAINTAINING, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "gr018", ReflectionTheme.GRATITUDE,
            "What's one thing about your body you're grateful it can do?",
            "Movement, sensing, healing—what does your body do for you?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "gr019", ReflectionTheme.GRATITUDE,
            "What peaceful or calm moment did you experience this week?",
            "Even in chaos, there are usually moments of stillness.",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "gr020", ReflectionTheme.GRATITUDE,
            "What are you most grateful about your future self for doing today?",
            "How are today's habits gifts to tomorrow's you?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        )
    )

    // ==================== CHALLENGES & GROWTH (20) ====================

    private val challengePrompts = listOf(
        ReflectionPrompt(
            "ch001", ReflectionTheme.CHALLENGES,
            "What was your biggest obstacle to habit completion this week?",
            "Identify the barrier without judgment—just observe.",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "ch002", ReflectionTheme.CHALLENGES,
            "How did you respond when things didn't go as planned?",
            "What was your internal dialogue? Your emotional response?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ch003", ReflectionTheme.CHALLENGES,
            "What excuse almost derailed you, and how did you handle it?",
            "Our minds are creative excuse-generators. What did yours produce?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ch004", ReflectionTheme.CHALLENGES,
            "What uncomfortable truth about yourself did your habits reveal this week?",
            "Habits are mirrors—what did you see?",
            JourneyStage.BUILDING, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "ch005", ReflectionTheme.CHALLENGES,
            "Where did you feel most resistant, and what might that resistance mean?",
            "Resistance often points to important growth opportunities.",
            JourneyStage.ALL, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "ch006", ReflectionTheme.CHALLENGES,
            "What trigger caused you to slip or struggle?",
            "Time of day? Emotion? Environment? Person?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ch007", ReflectionTheme.CHALLENGES,
            "How could you prepare differently for next week's challenges?",
            "What strategy might help with predictable obstacles?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ch008", ReflectionTheme.CHALLENGES,
            "What did you learn about your limits this week?",
            "Physical, emotional, or time limits—what did you discover?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ch009", ReflectionTheme.CHALLENGES,
            "Where did perfectionism get in the way of progress?",
            "Did 'all or nothing' thinking affect your week?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ch010", ReflectionTheme.CHALLENGES,
            "What story are you telling yourself that might not be true?",
            "We all have narratives—some help us, some limit us.",
            JourneyStage.BUILDING, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "ch011", ReflectionTheme.CHALLENGES,
            "What fear affected your habit execution this week?",
            "Fear of failure? Success? Judgment? Change?",
            JourneyStage.ALL, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "ch012", ReflectionTheme.CHALLENGES,
            "How did stress affect your habits, and what helped (or would help)?",
            "Stress and habits have a complex relationship.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ch013", ReflectionTheme.CHALLENGES,
            "What comparison to others affected you this week?",
            "Did you compare your journey to someone else's?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ch014", ReflectionTheme.CHALLENGES,
            "What energy drain made habits harder?",
            "Poor sleep? Difficult relationships? Work stress?",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "ch015", ReflectionTheme.CHALLENGES,
            "Where did you give up too easily, and where did you push through?",
            "Both patterns contain lessons.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ch016", ReflectionTheme.CHALLENGES,
            "What belief about yourself might be holding you back?",
            "Limiting beliefs often operate unconsciously.",
            JourneyStage.BUILDING, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "ch017", ReflectionTheme.CHALLENGES,
            "How did your environment work against you this week?",
            "What in your surroundings made habits harder?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ch018", ReflectionTheme.CHALLENGES,
            "What would you do differently if you could restart this week?",
            "Hindsight offers valuable insights.",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "ch019", ReflectionTheme.CHALLENGES,
            "What aspect of habit-building still confuses or frustrates you?",
            "Naming the confusion is the first step to clarity.",
            JourneyStage.BEGINNER, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "ch020", ReflectionTheme.CHALLENGES,
            "What sacrifice felt hardest this week, and was it worth it?",
            "Change requires giving up some things for others.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        )
    )

    // ==================== PROGRESS & WINS (20) ====================

    private val progressPrompts = listOf(
        ReflectionPrompt(
            "pg001", ReflectionTheme.PROGRESS,
            "What's one habit win you're proud of this week, big or small?",
            "No win is too small to celebrate.",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "pg002", ReflectionTheme.PROGRESS,
            "Where did you show up when you didn't feel like it?",
            "Consistency despite resistance is a major victory.",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "pg003", ReflectionTheme.PROGRESS,
            "What habit is becoming more automatic?",
            "What required willpower before now flows more easily?",
            JourneyStage.BUILDING, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "pg004", ReflectionTheme.PROGRESS,
            "How have your habits affected your energy or mood this week?",
            "What changes have you noticed in how you feel?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "pg005", ReflectionTheme.PROGRESS,
            "What unexpected benefit have your habits created?",
            "Side effects of positive habits are often surprising.",
            JourneyStage.BUILDING, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "pg006", ReflectionTheme.PROGRESS,
            "Where did you exceed your own expectations?",
            "Did you surprise yourself with what you accomplished?",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "pg007", ReflectionTheme.PROGRESS,
            "What new personal record did you set this week?",
            "Longest streak? Best performance? Most consistent effort?",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "pg008", ReflectionTheme.PROGRESS,
            "How have your habits influenced other areas of your life?",
            "Has discipline in one area spread to others?",
            JourneyStage.BUILDING, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "pg009", ReflectionTheme.PROGRESS,
            "What compliment or recognition did you receive (or should give yourself)?",
            "External validation matters, but so does self-recognition.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "pg010", ReflectionTheme.PROGRESS,
            "What evidence shows you're becoming the person you want to be?",
            "Identity shifts manifest in daily actions.",
            JourneyStage.BUILDING, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "pg011", ReflectionTheme.PROGRESS,
            "What did you learn that will help you next week?",
            "Each week teaches something for the next.",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "pg012", ReflectionTheme.PROGRESS,
            "How did you bounce back from a setback?",
            "Recovery speed is its own metric of progress.",
            JourneyStage.RECOVERING, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "pg013", ReflectionTheme.PROGRESS,
            "What milestone are you approaching, and how does that feel?",
            "The approach to a goal has its own energy.",
            JourneyStage.BUILDING, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "pg014", ReflectionTheme.PROGRESS,
            "Where did your preparation pay off?",
            "When did past effort make this week easier?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "pg015", ReflectionTheme.PROGRESS,
            "How has your self-talk about habits improved?",
            "The way we speak to ourselves matters.",
            JourneyStage.BUILDING, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "pg016", ReflectionTheme.PROGRESS,
            "What habit felt genuinely enjoyable this week?",
            "When did the habit itself become the reward?",
            JourneyStage.MAINTAINING, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "pg017", ReflectionTheme.PROGRESS,
            "How has your relationship with yourself improved through habits?",
            "Self-trust grows with consistent action.",
            JourneyStage.BUILDING, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "pg018", ReflectionTheme.PROGRESS,
            "What would you tell someone just starting what you're now doing?",
            "Your experience contains wisdom for beginners.",
            JourneyStage.MAINTAINING, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "pg019", ReflectionTheme.PROGRESS,
            "What internal resistance have you overcome recently?",
            "Inner battles won are significant victories.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "pg020", ReflectionTheme.PROGRESS,
            "How would you rate this week on a scale of 1-10, and why?",
            "Overall assessment with reasoning.",
            JourneyStage.ALL, PromptDifficulty.EASY
        )
    )

    // ==================== FUTURE PLANNING (20) ====================

    private val futurePrompts = listOf(
        ReflectionPrompt(
            "ft001", ReflectionTheme.FUTURE,
            "What's one thing you'll do differently next week?",
            "A single focused change is more effective than many.",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "ft002", ReflectionTheme.FUTURE,
            "What obstacle can you anticipate and prepare for?",
            "What predictable challenge is coming?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ft003", ReflectionTheme.FUTURE,
            "What support do you need to ask for?",
            "Who could help you, and how?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ft004", ReflectionTheme.FUTURE,
            "What environment change would make your habits easier?",
            "What could you add, remove, or rearrange?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ft005", ReflectionTheme.FUTURE,
            "Where do you want to be with your habits in one month?",
            "Set a clear vision for the near future.",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "ft006", ReflectionTheme.FUTURE,
            "What habit would you like to add once your current ones are solid?",
            "Plan for expansion when the time is right.",
            JourneyStage.BUILDING, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "ft007", ReflectionTheme.FUTURE,
            "What would future-you want you to prioritize this coming week?",
            "Speak from the perspective of your future self.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ft008", ReflectionTheme.FUTURE,
            "What fear about the future can you address through your habits?",
            "Habits can be a form of preparation.",
            JourneyStage.ALL, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "ft009", ReflectionTheme.FUTURE,
            "How can you make your best days more frequent?",
            "What conditions create your best habit days?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ft010", ReflectionTheme.FUTURE,
            "What's the next level of your current habit?",
            "How might you increase difficulty or depth?",
            JourneyStage.MAINTAINING, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ft011", ReflectionTheme.FUTURE,
            "What would make next week a 10 out of 10?",
            "Define your ideal week in concrete terms.",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "ft012", ReflectionTheme.FUTURE,
            "What commitment are you ready to make to yourself?",
            "A promise you can keep to yourself.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ft013", ReflectionTheme.FUTURE,
            "How might your habits evolve over the next year?",
            "What's the trajectory you're on?",
            JourneyStage.MAINTAINING, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "ft014", ReflectionTheme.FUTURE,
            "What routine adjustment would have the biggest impact?",
            "Which small change would yield big results?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ft015", ReflectionTheme.FUTURE,
            "What do you need to let go of to move forward?",
            "Sometimes progress requires release.",
            JourneyStage.ALL, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "ft016", ReflectionTheme.FUTURE,
            "What's your intention for this coming week in one sentence?",
            "A guiding principle for the week ahead.",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "ft017", ReflectionTheme.FUTURE,
            "How will you protect your habit time this week?",
            "What boundaries will you set?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ft018", ReflectionTheme.FUTURE,
            "What would you like to have written in next week's reflection?",
            "Write the story before it happens.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "ft019", ReflectionTheme.FUTURE,
            "What's one way you can be kinder to yourself next week?",
            "Self-compassion as a planned practice.",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "ft020", ReflectionTheme.FUTURE,
            "What legacy do you want your habits to create?",
            "The long-term impact of your daily choices.",
            JourneyStage.ALL, PromptDifficulty.DEEP
        )
    )

    // ==================== SELF-DISCOVERY (20) ====================

    private val selfDiscoveryPrompts = listOf(
        ReflectionPrompt(
            "sd001", ReflectionTheme.SELF_DISCOVERY,
            "What did you learn about yourself through your habits this week?",
            "Habits reveal who we are.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "sd002", ReflectionTheme.SELF_DISCOVERY,
            "What core value did you honor this week?",
            "How did your habits align with what matters most?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "sd003", ReflectionTheme.SELF_DISCOVERY,
            "What pattern in your behavior did you notice?",
            "Repeating behaviors often contain messages.",
            JourneyStage.BUILDING, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "sd004", ReflectionTheme.SELF_DISCOVERY,
            "What does your strongest habit say about who you are?",
            "Your consistent actions define your identity.",
            JourneyStage.MAINTAINING, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "sd005", ReflectionTheme.SELF_DISCOVERY,
            "What surprised you about your own reactions this week?",
            "We don't always know ourselves as well as we think.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "sd006", ReflectionTheme.SELF_DISCOVERY,
            "What part of yourself emerged when you were challenged?",
            "Difficulty reveals hidden aspects of ourselves.",
            JourneyStage.ALL, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "sd007", ReflectionTheme.SELF_DISCOVERY,
            "What do you now believe is possible for you?",
            "How has your sense of potential shifted?",
            JourneyStage.BUILDING, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "sd008", ReflectionTheme.SELF_DISCOVERY,
            "What emotional need might your habits be addressing?",
            "We seek certain states through our behaviors.",
            JourneyStage.ALL, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "sd009", ReflectionTheme.SELF_DISCOVERY,
            "What's one thing about yourself you've come to accept?",
            "Acceptance often precedes genuine change.",
            JourneyStage.ALL, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "sd010", ReflectionTheme.SELF_DISCOVERY,
            "When do you feel most like yourself?",
            "Which habits connect you to your authentic self?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "sd011", ReflectionTheme.SELF_DISCOVERY,
            "What inner critic voice did you hear this week?",
            "Name the voice to understand it better.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "sd012", ReflectionTheme.SELF_DISCOVERY,
            "What motivates you at a deep level?",
            "Beyond the surface—what drives you?",
            JourneyStage.ALL, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "sd013", ReflectionTheme.SELF_DISCOVERY,
            "How is your identity shifting through this journey?",
            "Who are you becoming?",
            JourneyStage.BUILDING, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "sd014", ReflectionTheme.SELF_DISCOVERY,
            "What brings you genuine joy in your habit practice?",
            "Where is the joy, not just the achievement?",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "sd015", ReflectionTheme.SELF_DISCOVERY,
            "What fear did you face or avoid this week?",
            "Our relationship with fear shapes our growth.",
            JourneyStage.ALL, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "sd016", ReflectionTheme.SELF_DISCOVERY,
            "What permission do you need to give yourself?",
            "What are you waiting for approval for?",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "sd017", ReflectionTheme.SELF_DISCOVERY,
            "What would you tell yourself if you were your own best friend?",
            "Advice from a place of unconditional support.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "sd018", ReflectionTheme.SELF_DISCOVERY,
            "What dream or aspiration are your habits serving?",
            "The bigger picture your daily actions feed into.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "sd019", ReflectionTheme.SELF_DISCOVERY,
            "What's one thing you've proven to yourself recently?",
            "Evidence of your own capability.",
            JourneyStage.BUILDING, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "sd020", ReflectionTheme.SELF_DISCOVERY,
            "What question about yourself are you still trying to answer?",
            "The ongoing inquiry of self-understanding.",
            JourneyStage.ALL, PromptDifficulty.DEEP
        )
    )

    // ==================== RELATIONSHIPS & CONNECTION (20) ====================

    private val relationshipPrompts = listOf(
        ReflectionPrompt(
            "rl001", ReflectionTheme.RELATIONSHIPS,
            "How have your habits affected your relationships this week?",
            "Positive habits often ripple outward to others.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "rl002", ReflectionTheme.RELATIONSHIPS,
            "Who encouraged you this week, and how did it feel?",
            "The impact of support on your journey.",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "rl003", ReflectionTheme.RELATIONSHIPS,
            "Who did you encourage or inspire through your habits?",
            "Your influence on others, intended or not.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "rl004", ReflectionTheme.RELATIONSHIPS,
            "How has someone challenged or doubted you, and how did you respond?",
            "External skepticism and your reaction to it.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "rl005", ReflectionTheme.RELATIONSHIPS,
            "What relationship would benefit from more of your energy?",
            "Habits affect the time and presence we have for others.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "rl006", ReflectionTheme.RELATIONSHIPS,
            "How have your habits made you more present with others?",
            "Quality of attention you bring to relationships.",
            JourneyStage.BUILDING, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "rl007", ReflectionTheme.RELATIONSHIPS,
            "What boundary did you set or need to set for your habits?",
            "Protecting your growth while honoring relationships.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "rl008", ReflectionTheme.RELATIONSHIPS,
            "Who would you like to share your habit journey with?",
            "Connection through shared growth.",
            JourneyStage.ALL, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "rl009", ReflectionTheme.RELATIONSHIPS,
            "How have your habits affected your family dynamics?",
            "Family relationships and personal growth.",
            JourneyStage.ALL, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "rl010", ReflectionTheme.RELATIONSHIPS,
            "What did you learn from observing someone else's habits?",
            "Others as mirrors and teachers.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "rl011", ReflectionTheme.RELATIONSHIPS,
            "How might your habits be affecting someone without you realizing?",
            "The unseen impact of our behaviors.",
            JourneyStage.MAINTAINING, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "rl012", ReflectionTheme.RELATIONSHIPS,
            "What conversation about habits would you like to have with someone?",
            "Unspoken dialogues about growth and change.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "rl013", ReflectionTheme.RELATIONSHIPS,
            "How has your improved wellbeing affected how you treat others?",
            "The connection between self-care and care for others.",
            JourneyStage.BUILDING, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "rl014", ReflectionTheme.RELATIONSHIPS,
            "What role does community play in your habit journey?",
            "The importance of belonging and shared purpose.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "rl015", ReflectionTheme.RELATIONSHIPS,
            "Who from your past would be proud of your current habits?",
            "Ancestral or historical pride in your choices.",
            JourneyStage.ALL, PromptDifficulty.DEEP
        ),
        ReflectionPrompt(
            "rl016", ReflectionTheme.RELATIONSHIPS,
            "What support could you offer someone else on their journey?",
            "Giving back from your experience.",
            JourneyStage.MAINTAINING, PromptDifficulty.EASY
        ),
        ReflectionPrompt(
            "rl017", ReflectionTheme.RELATIONSHIPS,
            "How have your habits affected your work relationships?",
            "Professional impact of personal discipline.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "rl018", ReflectionTheme.RELATIONSHIPS,
            "What would you like others to understand about your habit journey?",
            "Misunderstandings and desired understanding.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "rl019", ReflectionTheme.RELATIONSHIPS,
            "How can you include loved ones in your habits without forcing them?",
            "Invitation versus imposition.",
            JourneyStage.ALL, PromptDifficulty.MEDIUM
        ),
        ReflectionPrompt(
            "rl020", ReflectionTheme.RELATIONSHIPS,
            "What gratitude do you have for the people supporting your journey?",
            "Appreciation for your support network.",
            JourneyStage.ALL, PromptDifficulty.EASY
        )
    )

    // ==================== PUBLIC API ====================

    val allPrompts: List<ReflectionPrompt> by lazy {
        gratitudePrompts + challengePrompts + progressPrompts +
                futurePrompts + selfDiscoveryPrompts + relationshipPrompts
    }

    /**
     * Get a weekly prompt set (one from each theme)
     */
    fun getWeeklyPromptSet(): List<ReflectionPrompt> {
        return listOf(
            gratitudePrompts.random(),
            challengePrompts.random(),
            progressPrompts.random(),
            futurePrompts.random(),
            selfDiscoveryPrompts.random(),
            relationshipPrompts.random()
        )
    }

    /**
     * Get prompt for a specific theme and week number
     * This ensures deterministic selection based on week
     */
    fun getPromptForTheme(theme: ReflectionTheme, week: Int): ReflectionPrompt {
        val themePrompts = getPromptsByTheme(theme)
        val index = (week - 1) % themePrompts.size
        return themePrompts[index]
    }

    /**
     * Get prompts by theme
     */
    fun getPromptsByTheme(theme: ReflectionTheme): List<ReflectionPrompt> {
        return when (theme) {
            ReflectionTheme.GRATITUDE -> gratitudePrompts
            ReflectionTheme.CHALLENGES -> challengePrompts
            ReflectionTheme.PROGRESS -> progressPrompts
            ReflectionTheme.FUTURE -> futurePrompts
            ReflectionTheme.SELF_DISCOVERY -> selfDiscoveryPrompts
            ReflectionTheme.RELATIONSHIPS -> relationshipPrompts
        }
    }

    /**
     * Get prompts suitable for a specific journey stage
     */
    fun getPromptsForStage(stage: JourneyStage): List<ReflectionPrompt> {
        return allPrompts.filter {
            it.journeyStage == stage || it.journeyStage == JourneyStage.ALL
        }
    }

    /**
     * Get prompts by difficulty level
     */
    fun getPromptsByDifficulty(difficulty: PromptDifficulty): List<ReflectionPrompt> {
        return allPrompts.filter { it.difficulty == difficulty }
    }

    /**
     * Get total prompt count (should be 120)
     */
    fun getTotalCount(): Int = allPrompts.size

    /**
     * Get a deterministic prompt for a specific day of year
     */
    fun getPromptForDay(dayOfYear: Int): ReflectionPrompt {
        val adjustedDay = ((dayOfYear - 1) % allPrompts.size).coerceIn(0, allPrompts.size - 1)
        return allPrompts[adjustedDay]
    }

    /**
     * Search prompts by keyword
     */
    fun searchPrompts(query: String): List<ReflectionPrompt> {
        val lowerQuery = query.lowercase()
        return allPrompts.filter {
            it.question.lowercase().contains(lowerQuery) ||
            (it.subPrompt?.lowercase()?.contains(lowerQuery) == true)
        }
    }

    /**
     * Get a contextual prompt based on user state
     */
    fun getContextualPrompt(
        hasRecentBreak: Boolean,
        currentStreak: Int,
        moodLow: Boolean
    ): ReflectionPrompt {
        return when {
            hasRecentBreak -> challengePrompts.filter {
                it.journeyStage == JourneyStage.RECOVERING || it.journeyStage == JourneyStage.ALL
            }.randomOrNull() ?: challengePrompts.random()

            moodLow -> gratitudePrompts.random()

            currentStreak < 7 -> allPrompts.filter {
                it.journeyStage == JourneyStage.BEGINNER || it.journeyStage == JourneyStage.ALL
            }.random()

            currentStreak > 30 -> selfDiscoveryPrompts.filter {
                it.difficulty == PromptDifficulty.DEEP
            }.random()

            else -> allPrompts.random()
        }
    }
}
