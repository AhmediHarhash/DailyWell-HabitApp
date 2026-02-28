package com.dailywell.app.data.content

/**
 * DailyInsightsDatabase - 365 Unique Daily Insights
 *
 * PRODUCTION-READY: Every user sees a different insight each day for an entire year.
 * No repetition for 365 days. Research-backed psychology and neuroscience.
 *
 * Categories (7 × 52+ insights = 365):
 * 1. Habit Psychology (52)
 * 2. Neuroscience Facts (52)
 * 3. Behavioral Triggers (52)
 * 4. Progress Mindset (52)
 * 5. Social Psychology (52)
 * 6. Recovery & Compassion (52)
 * 7. Advanced Techniques (53)
 */
object DailyInsightsDatabase {

    // ==================== DATA MODELS ====================

    data class Insight(
        val id: String,
        val category: InsightCategory,
        val title: String,
        val content: String,
        val source: String? = null,
        val contextTags: List<String> = emptyList()
    )

    enum class InsightCategory {
        HABIT_PSYCHOLOGY,
        NEUROSCIENCE,
        BEHAVIORAL_TRIGGERS,
        PROGRESS_MINDSET,
        SOCIAL_PSYCHOLOGY,
        RECOVERY_COMPASSION,
        ADVANCED_TECHNIQUES
    }

    // ==================== HABIT PSYCHOLOGY (52 Insights) ====================

    private val habitPsychology = listOf(
        Insight("hp001", InsightCategory.HABIT_PSYCHOLOGY, "The 21-Day Myth", "Research shows habit formation takes 18-254 days, with an average of 66 days. Don't get discouraged if it takes longer than three weeks.", "Phillippa Lally, European Journal of Social Psychology", listOf("beginner", "streak")),
        Insight("hp002", InsightCategory.HABIT_PSYCHOLOGY, "Tiny Habits Win", "BJ Fogg's research proves that making habits incredibly small (2 minutes or less) is more effective than willpower-based approaches.", "BJ Fogg, Stanford Behavior Design Lab", listOf("beginner", "morning")),
        Insight("hp003", InsightCategory.HABIT_PSYCHOLOGY, "The Habit Loop", "Every habit follows a loop: Cue → Routine → Reward. Identify your cue to change any behavior.", "Charles Duhigg, The Power of Habit", listOf("learning")),
        Insight("hp004", InsightCategory.HABIT_PSYCHOLOGY, "Identity Trumps Goals", "Instead of 'I want to run,' say 'I am a runner.' Identity-based habits are 3x more likely to stick.", "James Clear, Atomic Habits", listOf("motivation", "identity")),
        Insight("hp005", InsightCategory.HABIT_PSYCHOLOGY, "Environment Beats Motivation", "You're more likely to eat an apple if it's on your counter than if it's in your fridge. Design your environment for success.", "Wendy Wood, Good Habits Bad Habits", listOf("environment", "setup")),
        Insight("hp006", InsightCategory.HABIT_PSYCHOLOGY, "The Plateau of Latent Potential", "Progress is often invisible until a breakthrough moment. Trust the process even when results aren't visible yet.", "James Clear, Atomic Habits", listOf("patience", "long-term")),
        Insight("hp007", InsightCategory.HABIT_PSYCHOLOGY, "Habit Stacking Power", "Link new habits to existing ones: 'After I [CURRENT HABIT], I will [NEW HABIT].' This uses neural pathways already formed.", "BJ Fogg, Tiny Habits", listOf("stacking", "technique")),
        Insight("hp008", InsightCategory.HABIT_PSYCHOLOGY, "The Two-Minute Rule", "When starting a new habit, it should take less than two minutes. 'Read before bed' becomes 'Read one page.'", "James Clear", listOf("beginner", "starting")),
        Insight("hp009", InsightCategory.HABIT_PSYCHOLOGY, "Friction Matters", "Every extra second of friction reduces habit execution by up to 20%. Remove barriers ruthlessly.", "Behavioral Economics Research", listOf("environment", "friction")),
        Insight("hp010", InsightCategory.HABIT_PSYCHOLOGY, "The Goldilocks Zone", "Habits stick best when they're challenging enough to be engaging but not so hard they're discouraging.", "Flow Theory, Mihaly Csikszentmihalyi", listOf("challenge", "balance")),
        Insight("hp011", InsightCategory.HABIT_PSYCHOLOGY, "Temptation Bundling", "Pair something you need to do with something you want to do. Listen to podcasts only while exercising.", "Katy Milkman, University of Pennsylvania", listOf("motivation", "technique")),
        Insight("hp012", InsightCategory.HABIT_PSYCHOLOGY, "Implementation Intentions", "People who write 'I will exercise at [TIME] in [PLACE]' are 2-3x more likely to follow through.", "Peter Gollwitzer", listOf("planning", "specificity")),
        Insight("hp013", InsightCategory.HABIT_PSYCHOLOGY, "The Fresh Start Effect", "Mondays, new months, and birthdays feel like new beginnings. Use these temporal landmarks to start habits.", "Katy Milkman, The Wharton School", listOf("timing", "fresh-start")),
        Insight("hp014", InsightCategory.HABIT_PSYCHOLOGY, "Keystone Habits", "Some habits trigger a cascade of other positive changes. Exercise often leads to better eating and sleeping.", "Charles Duhigg", listOf("keystone", "cascade")),
        Insight("hp015", InsightCategory.HABIT_PSYCHOLOGY, "Variable Rewards", "Unpredictable rewards are more motivating than predictable ones. This is why slot machines are addictive.", "B.F. Skinner, Behavioral Psychology", listOf("rewards", "gamification")),
        Insight("hp016", InsightCategory.HABIT_PSYCHOLOGY, "The Compound Effect", "Improving by just 1% each day leads to being 37 times better after one year. Small gains compound.", "Darren Hardy", listOf("compounding", "patience")),
        Insight("hp017", InsightCategory.HABIT_PSYCHOLOGY, "Willpower Is Finite", "Self-control depletes throughout the day. Schedule important habits early when willpower is strongest.", "Roy Baumeister", listOf("willpower", "timing")),
        Insight("hp018", InsightCategory.HABIT_PSYCHOLOGY, "The Zeigarnik Effect", "Incomplete tasks occupy mental space. Starting a habit, even briefly, creates a pull to finish it.", "Bluma Zeigarnik", listOf("psychology", "starting")),
        Insight("hp019", InsightCategory.HABIT_PSYCHOLOGY, "Commitment Devices", "Publicly committing to a habit increases success rates by up to 65%. Tell someone your goal.", "Dan Ariely, Predictably Irrational", listOf("commitment", "social")),
        Insight("hp020", InsightCategory.HABIT_PSYCHOLOGY, "The Diderot Effect", "One purchase often leads to more purchases. Similarly, one positive habit often sparks others.", "Juliet Schor", listOf("cascade", "momentum")),
        Insight("hp021", InsightCategory.HABIT_PSYCHOLOGY, "Reward Substitution", "When breaking a bad habit, replace the reward rather than eliminating it. Find a healthier way to meet the same need.", "Charles Duhigg", listOf("breaking", "substitution")),
        Insight("hp022", InsightCategory.HABIT_PSYCHOLOGY, "Context-Dependent Memory", "Habits are tied to specific contexts. Changing location or time can disrupt unwanted habits.", "Endel Tulving", listOf("context", "change")),
        Insight("hp023", InsightCategory.HABIT_PSYCHOLOGY, "The Mere Exposure Effect", "Repeated exposure increases liking. The more you do a habit, the more you'll enjoy it.", "Robert Zajonc", listOf("repetition", "enjoyment")),
        Insight("hp024", InsightCategory.HABIT_PSYCHOLOGY, "Self-Efficacy Theory", "Believing you can succeed is crucial. Past small wins build confidence for bigger challenges.", "Albert Bandura", listOf("confidence", "wins")),
        Insight("hp025", InsightCategory.HABIT_PSYCHOLOGY, "Intention-Action Gap", "90% of people intend to exercise but only 50% actually do. Implementation intentions bridge this gap.", "Sheeran & Webb", listOf("intention", "action")),
        Insight("hp026", InsightCategory.HABIT_PSYCHOLOGY, "The What-The-Hell Effect", "One slip often leads to abandonment: 'I already messed up, might as well give up.' Recognize and resist this pattern.", "Janet Polivy", listOf("recovery", "slip")),
        Insight("hp027", InsightCategory.HABIT_PSYCHOLOGY, "Habit Reversal Training", "To break a habit: 1) Notice the urge, 2) Do a competing response, 3) Celebrate the win.", "Nathan Azrin", listOf("breaking", "technique")),
        Insight("hp028", InsightCategory.HABIT_PSYCHOLOGY, "The Peak-End Rule", "We judge experiences by their peak moment and ending. Make habit endings positive and memorable.", "Daniel Kahneman", listOf("experience", "ending")),
        Insight("hp029", InsightCategory.HABIT_PSYCHOLOGY, "Automaticity", "When a habit becomes automatic, it moves from conscious effort to unconscious execution. That's the goal.", "John Bargh", listOf("automation", "mastery")),
        Insight("hp030", InsightCategory.HABIT_PSYCHOLOGY, "Decision Fatigue", "Every decision depletes mental energy. Automate habits to preserve brainpower for important choices.", "Roy Baumeister", listOf("decisions", "energy")),
        Insight("hp031", InsightCategory.HABIT_PSYCHOLOGY, "The Sunk Cost Fallacy", "Past effort shouldn't dictate future choices. If a habit isn't serving you, it's okay to change it.", "Richard Thaler", listOf("flexibility", "change")),
        Insight("hp032", InsightCategory.HABIT_PSYCHOLOGY, "Cognitive Dissonance", "When actions don't match beliefs, we change beliefs to match. Act like the person you want to become.", "Leon Festinger", listOf("identity", "action")),
        Insight("hp033", InsightCategory.HABIT_PSYCHOLOGY, "The Premack Principle", "Use preferred activities to reinforce less preferred ones. 'After vegetables, dessert.'", "David Premack", listOf("rewards", "sequencing")),
        Insight("hp034", InsightCategory.HABIT_PSYCHOLOGY, "Present Bias", "We overvalue immediate rewards over future benefits. Combat this by making future rewards tangible now.", "Ted O'Donoghue", listOf("bias", "future")),
        Insight("hp035", InsightCategory.HABIT_PSYCHOLOGY, "Ego Depletion Debate", "While willpower may be limited, believing it's unlimited actually increases persistence.", "Carol Dweck", listOf("willpower", "mindset")),
        Insight("hp036", InsightCategory.HABIT_PSYCHOLOGY, "Social Proof", "We look to others for behavioral cues. Surround yourself with people who have the habits you want.", "Robert Cialdini", listOf("social", "influence")),
        Insight("hp037", InsightCategory.HABIT_PSYCHOLOGY, "Loss Aversion", "People fear losing more than they value gaining. Frame habits as 'keeping your streak' rather than 'building' one.", "Amos Tversky", listOf("framing", "motivation")),
        Insight("hp038", InsightCategory.HABIT_PSYCHOLOGY, "The Planning Fallacy", "We underestimate how long things take. Build buffer time into habit planning.", "Daniel Kahneman", listOf("planning", "time")),
        Insight("hp039", InsightCategory.HABIT_PSYCHOLOGY, "Construal Level Theory", "Distant goals feel abstract; near goals feel concrete. Break yearly goals into daily actions.", "Yaacov Trope", listOf("goals", "breakdown")),
        Insight("hp040", InsightCategory.HABIT_PSYCHOLOGY, "Affective Forecasting", "We're bad at predicting how future events will make us feel. Habits often feel better than expected once started.", "Dan Gilbert", listOf("feeling", "prediction")),
        Insight("hp041", InsightCategory.HABIT_PSYCHOLOGY, "Implementation Hierarchy", "Identity → Process → Outcome. Focus on who you're becoming, not just what you're achieving.", "James Clear", listOf("identity", "process")),
        Insight("hp042", InsightCategory.HABIT_PSYCHOLOGY, "Chunking", "Breaking complex behaviors into smaller chunks makes them easier to learn and automate.", "George Miller", listOf("learning", "breakdown")),
        Insight("hp043", InsightCategory.HABIT_PSYCHOLOGY, "Behavioral Activation", "Action often precedes motivation, not the other way around. Start moving, motivation follows.", "CBT Research", listOf("action", "motivation")),
        Insight("hp044", InsightCategory.HABIT_PSYCHOLOGY, "The Endowed Progress Effect", "People work harder when they feel they've already started. Give yourself credit for past efforts.", "Joseph Nunes", listOf("progress", "motivation")),
        Insight("hp045", InsightCategory.HABIT_PSYCHOLOGY, "Habit Extinction", "Unused neural pathways weaken over time. Old habits don't disappear, but new ones can become stronger.", "Neuroscience", listOf("breaking", "neural")),
        Insight("hp046", InsightCategory.HABIT_PSYCHOLOGY, "Self-Determination Theory", "Habits stick when they satisfy autonomy, competence, and relatedness needs.", "Ryan & Deci", listOf("needs", "motivation")),
        Insight("hp047", InsightCategory.HABIT_PSYCHOLOGY, "Behavioral Momentum", "Completing easy tasks builds momentum for harder ones. Start with your simplest habit.", "John Nevin", listOf("momentum", "starting")),
        Insight("hp048", InsightCategory.HABIT_PSYCHOLOGY, "Goal Gradient Effect", "Motivation increases as we approach our goal. Visible progress accelerates effort.", "Clark Hull", listOf("progress", "visualization")),
        Insight("hp049", InsightCategory.HABIT_PSYCHOLOGY, "Operant Conditioning", "Behaviors followed by positive consequences are repeated. Celebrate every habit completion.", "B.F. Skinner", listOf("rewards", "celebration")),
        Insight("hp050", InsightCategory.HABIT_PSYCHOLOGY, "Habit Stacking Research", "Studies show that stacked habits have a 74% higher adherence rate than standalone habits.", "Behavioral Research", listOf("stacking", "effectiveness")),
        Insight("hp051", InsightCategory.HABIT_PSYCHOLOGY, "The Fogg Behavior Model", "Behavior = Motivation × Ability × Prompt. If any element is missing, the behavior won't happen.", "BJ Fogg", listOf("model", "framework")),
        Insight("hp052", InsightCategory.HABIT_PSYCHOLOGY, "Metacognition", "Thinking about your thinking improves habit formation. Notice your patterns and adjust.", "John Flavell", listOf("awareness", "reflection"))
    )

    // ==================== NEUROSCIENCE FACTS (52 Insights) ====================

    private val neuroscienceFacts = listOf(
        Insight("ns001", InsightCategory.NEUROSCIENCE, "Dopamine Anticipation", "Dopamine spikes happen in anticipation of reward, not just upon receiving it. The pursuit is the pleasure.", "Wolfram Schultz", listOf("dopamine", "motivation")),
        Insight("ns002", InsightCategory.NEUROSCIENCE, "Neuroplasticity Window", "Your brain physically changes with every repetition. Each time you perform a habit, neural connections strengthen.", "Michael Merzenich", listOf("brain", "change")),
        Insight("ns003", InsightCategory.NEUROSCIENCE, "The Basal Ganglia", "Habits are stored in the basal ganglia, separate from conscious decision-making. This is why habits feel automatic.", "Ann Graybiel, MIT", listOf("brain", "automation")),
        Insight("ns004", InsightCategory.NEUROSCIENCE, "Morning Cortisol", "Cortisol peaks in the morning, enhancing focus and learning. This is ideal for habit formation.", "Circadian Research", listOf("morning", "timing")),
        Insight("ns005", InsightCategory.NEUROSCIENCE, "Synaptic Pruning", "The brain eliminates unused connections. 'Use it or lose it' applies to habits too.", "Neuroscience Research", listOf("brain", "practice")),
        Insight("ns006", InsightCategory.NEUROSCIENCE, "The Prefrontal Cortex", "New habits require prefrontal cortex effort. Once automatic, they shift to more efficient brain regions.", "Earl Miller, MIT", listOf("brain", "effort")),
        Insight("ns007", InsightCategory.NEUROSCIENCE, "Sleep and Memory", "Sleep consolidates habit learning. Your brain literally replays and strengthens new patterns overnight.", "Matthew Walker", listOf("sleep", "memory")),
        Insight("ns008", InsightCategory.NEUROSCIENCE, "Myelin Formation", "Practice wraps neurons in myelin, making signals faster. This is why habits become effortless.", "Douglas Fields", listOf("practice", "speed")),
        Insight("ns009", InsightCategory.NEUROSCIENCE, "The Amygdala's Role", "Emotional significance helps habits stick. Attach positive emotions to your habit practice.", "Joseph LeDoux", listOf("emotions", "memory")),
        Insight("ns010", InsightCategory.NEUROSCIENCE, "Hebbian Learning", "'Neurons that fire together, wire together.' Consistent habit timing creates stronger neural pathways.", "Donald Hebb", listOf("timing", "neural")),
        Insight("ns011", InsightCategory.NEUROSCIENCE, "Default Mode Network", "The brain's 'autopilot' mode handles habits. This frees mental resources for other tasks.", "Marcus Raichle", listOf("autopilot", "efficiency")),
        Insight("ns012", InsightCategory.NEUROSCIENCE, "Nucleus Accumbens", "This reward center releases dopamine when habits are completed. It's your brain's 'feel good' signal.", "Reward Pathway Research", listOf("reward", "dopamine")),
        Insight("ns013", InsightCategory.NEUROSCIENCE, "Hippocampal Encoding", "New habits involve the hippocampus for conscious learning before becoming automatic.", "Howard Eichenbaum", listOf("learning", "memory")),
        Insight("ns014", InsightCategory.NEUROSCIENCE, "Mirror Neurons", "Watching others perform habits activates your own motor neurons. Observation aids learning.", "Giacomo Rizzolatti", listOf("social", "learning")),
        Insight("ns015", InsightCategory.NEUROSCIENCE, "BDNF and Exercise", "Exercise releases BDNF, which enhances learning and habit formation across all domains.", "Carl Cotman", listOf("exercise", "brain")),
        Insight("ns016", InsightCategory.NEUROSCIENCE, "The Striatum", "This brain region stores procedural memories, including habits. It's your behavioral library.", "Neuroscience Research", listOf("brain", "memory")),
        Insight("ns017", InsightCategory.NEUROSCIENCE, "Glial Cell Support", "Glial cells support neurons and are crucial for habit formation. They need proper sleep to function.", "R. Douglas Fields", listOf("brain", "sleep")),
        Insight("ns018", InsightCategory.NEUROSCIENCE, "Norepinephrine", "This neurotransmitter enhances alertness and focus. Morning light exposure triggers its release.", "Neurotransmitter Research", listOf("focus", "morning")),
        Insight("ns019", InsightCategory.NEUROSCIENCE, "The Motor Cortex", "Physical habits are mapped in the motor cortex. Repetition creates precise movement patterns.", "Motor Learning Research", listOf("physical", "precision")),
        Insight("ns020", InsightCategory.NEUROSCIENCE, "Serotonin and Habits", "Serotonin regulates mood and well-being. Consistent habits help stabilize serotonin levels.", "Neurotransmitter Research", listOf("mood", "consistency")),
        Insight("ns021", InsightCategory.NEUROSCIENCE, "Acetylcholine Focus", "This neurotransmitter enhances focus and learning. It's highest when you're fully engaged.", "Neuroscience", listOf("focus", "engagement")),
        Insight("ns022", InsightCategory.NEUROSCIENCE, "Long-Term Potentiation", "Repeated use strengthens synaptic connections permanently. This is the biological basis of habit.", "Eric Kandel", listOf("biology", "permanence")),
        Insight("ns023", InsightCategory.NEUROSCIENCE, "The Cerebellum", "This brain region coordinates automatic movements and procedural learning, including habits.", "James Bower", listOf("brain", "coordination")),
        Insight("ns024", InsightCategory.NEUROSCIENCE, "Stress Hormones", "Chronic stress impairs the prefrontal cortex, making it harder to form new habits. Manage stress first.", "Robert Sapolsky", listOf("stress", "cortex")),
        Insight("ns025", InsightCategory.NEUROSCIENCE, "Oxytocin Bonding", "Social habits release oxytocin, the bonding hormone. This makes group habits especially sticky.", "Paul Zak", listOf("social", "bonding")),
        Insight("ns026", InsightCategory.NEUROSCIENCE, "The Anterior Cingulate", "This region monitors conflict and errors. It's active when habits need adjustment.", "Michael Posner", listOf("brain", "adjustment")),
        Insight("ns027", InsightCategory.NEUROSCIENCE, "Endorphin Release", "Exercise and achievement habits trigger endorphin release, creating natural pain relief and pleasure.", "Candace Pert", listOf("exercise", "pleasure")),
        Insight("ns028", InsightCategory.NEUROSCIENCE, "Circadian Rhythm", "Your brain has natural energy fluctuations. Align habits with your chronotype for best results.", "Matthew Walker", listOf("timing", "energy")),
        Insight("ns029", InsightCategory.NEUROSCIENCE, "Neural Efficiency", "Experts use less brain energy for habitual tasks. Efficiency is the goal of automaticity.", "Cognitive Neuroscience", listOf("efficiency", "mastery")),
        Insight("ns030", InsightCategory.NEUROSCIENCE, "The Insula", "This brain region processes bodily sensations and gut feelings. It helps you 'feel' when a habit is due.", "Antonio Damasio", listOf("intuition", "body")),
        Insight("ns031", InsightCategory.NEUROSCIENCE, "GABA and Relaxation", "This calming neurotransmitter increases with meditation habits, reducing anxiety and improving focus.", "Neurotransmitter Research", listOf("calm", "meditation")),
        Insight("ns032", InsightCategory.NEUROSCIENCE, "Thalamic Gateway", "The thalamus filters sensory information. Habits bypass this conscious filter for automatic execution.", "Brain Research", listOf("automation", "brain")),
        Insight("ns033", InsightCategory.NEUROSCIENCE, "Epigenetic Changes", "Consistent habits can actually change gene expression, influencing health outcomes for years.", "Epigenetic Research", listOf("genes", "long-term")),
        Insight("ns034", InsightCategory.NEUROSCIENCE, "The Ventral Striatum", "This reward region responds to both anticipation and receipt of habit rewards.", "Reward Research", listOf("reward", "anticipation")),
        Insight("ns035", InsightCategory.NEUROSCIENCE, "White Matter Integrity", "Regular habits improve white matter connections, enhancing overall brain communication.", "DTI Studies", listOf("brain", "connection")),
        Insight("ns036", InsightCategory.NEUROSCIENCE, "Orexin and Wakefulness", "This neuropeptide regulates wakefulness and motivation. Morning light exposure activates it.", "Sleep Research", listOf("wakefulness", "morning")),
        Insight("ns037", InsightCategory.NEUROSCIENCE, "Glutamate Learning", "This excitatory neurotransmitter is crucial for learning. It's most active during focused practice.", "Neuroscience", listOf("learning", "focus")),
        Insight("ns038", InsightCategory.NEUROSCIENCE, "Dendritic Sprouting", "New neural branches grow with learning. Each habit adds to your brain's physical structure.", "Neuroplasticity Research", listOf("growth", "brain")),
        Insight("ns039", InsightCategory.NEUROSCIENCE, "The Reticular Activating System", "This brain region filters important information. It's why you notice things related to your habits.", "Neuroscience", listOf("attention", "awareness")),
        Insight("ns040", InsightCategory.NEUROSCIENCE, "Adenosine and Sleep Pressure", "Adenosine builds throughout the day, creating sleep pressure. Regular sleep habits keep it balanced.", "Sleep Research", listOf("sleep", "timing")),
        Insight("ns041", InsightCategory.NEUROSCIENCE, "Cognitive Reserve", "Consistent mental habits build cognitive reserve, protecting against age-related decline.", "Yaakov Stern", listOf("protection", "aging")),
        Insight("ns042", InsightCategory.NEUROSCIENCE, "The Habenula", "This tiny brain region processes disappointment and negative prediction errors. Learn from habit failures.", "Neuroscience", listOf("learning", "failure")),
        Insight("ns043", InsightCategory.NEUROSCIENCE, "Melatonin Rhythm", "Evening habits that support melatonin (dim lights, less screens) improve sleep and next-day habits.", "Circadian Research", listOf("evening", "sleep")),
        Insight("ns044", InsightCategory.NEUROSCIENCE, "The Claustrum", "This brain structure may coordinate consciousness and attention, linking awareness to habit execution.", "Francis Crick", listOf("consciousness", "attention")),
        Insight("ns045", InsightCategory.NEUROSCIENCE, "Dopamine Fasting Myth", "You can't truly 'fast' dopamine, but reducing high-dopamine activities increases sensitivity to subtle rewards.", "Neuroscience", listOf("dopamine", "sensitivity")),
        Insight("ns046", InsightCategory.NEUROSCIENCE, "The Substantia Nigra", "This region produces dopamine for movement and reward. Parkinson's research shows its importance for habits.", "Neuroscience", listOf("dopamine", "movement")),
        Insight("ns047", InsightCategory.NEUROSCIENCE, "Cortical Thinning", "The brain becomes more efficient through selective thinning. Expertise means less, not more, brain activity.", "Neuroscience", listOf("efficiency", "expertise")),
        Insight("ns048", InsightCategory.NEUROSCIENCE, "The Locus Coeruleus", "This region releases norepinephrine for alertness. Cold exposure and exercise activate it.", "Neuroscience", listOf("alertness", "activation")),
        Insight("ns049", InsightCategory.NEUROSCIENCE, "Reward Prediction Error", "The brain learns from the difference between expected and actual rewards. Surprise enhances learning.", "Wolfram Schultz", listOf("learning", "surprise")),
        Insight("ns050", InsightCategory.NEUROSCIENCE, "The Vagus Nerve", "This nerve connects brain and body. Breathing habits directly influence heart rate and calm.", "Stephen Porges", listOf("breathing", "calm")),
        Insight("ns051", InsightCategory.NEUROSCIENCE, "Temporal Lobe Memory", "The temporal lobe stores episodic memories of habit contexts. Location and time cues activate it.", "Larry Squire", listOf("memory", "context")),
        Insight("ns052", InsightCategory.NEUROSCIENCE, "Brain-Derived Neurotrophic Factor", "BDNF is your brain's fertilizer. Exercise habits significantly boost its production.", "Carl Cotman", listOf("exercise", "growth"))
    )

    // ==================== BEHAVIORAL TRIGGERS (52 Insights) ====================

    private val behavioralTriggers = listOf(
        Insight("bt001", InsightCategory.BEHAVIORAL_TRIGGERS, "Location as Trigger", "Your brain associates specific places with specific behaviors. Create a 'habit zone' for your practice.", "Context Research", listOf("location", "environment")),
        Insight("bt002", InsightCategory.BEHAVIORAL_TRIGGERS, "Time-Based Cues", "Linking habits to specific times creates automatic triggers. 'When it's 7 AM' works better than 'morning.'", "Implementation Intentions", listOf("time", "specificity")),
        Insight("bt003", InsightCategory.BEHAVIORAL_TRIGGERS, "Emotional Triggers", "Emotions can cue habits both positive and negative. Notice which feelings precede your habits.", "Emotional Eating Research", listOf("emotions", "awareness")),
        Insight("bt004", InsightCategory.BEHAVIORAL_TRIGGERS, "Visual Cues", "Objects in plain sight act as powerful triggers. Put your running shoes by the door.", "Visual Cueing Research", listOf("visual", "environment")),
        Insight("bt005", InsightCategory.BEHAVIORAL_TRIGGERS, "Preceding Actions", "The habit stack: one behavior triggers the next. 'After I brush my teeth, I meditate.'", "BJ Fogg", listOf("stacking", "sequence")),
        Insight("bt006", InsightCategory.BEHAVIORAL_TRIGGERS, "Social Triggers", "Other people's behaviors prompt ours. Seeing others exercise makes you more likely to move.", "Social Influence Research", listOf("social", "influence")),
        Insight("bt007", InsightCategory.BEHAVIORAL_TRIGGERS, "Digital Triggers", "Phone notifications and app icons are powerful cues. Curate them intentionally.", "Digital Behavior Research", listOf("digital", "notifications")),
        Insight("bt008", InsightCategory.BEHAVIORAL_TRIGGERS, "Sensory Triggers", "Sounds, smells, and textures can trigger habits. Use a specific playlist for workouts.", "Sensory Research", listOf("sensory", "association")),
        Insight("bt009", InsightCategory.BEHAVIORAL_TRIGGERS, "Transition Moments", "The spaces between activities are natural trigger points. Commutes, meals, and bedtime are ideal.", "Transition Research", listOf("transitions", "timing")),
        Insight("bt010", InsightCategory.BEHAVIORAL_TRIGGERS, "Energy States", "High energy and low energy states trigger different habits. Match habits to energy levels.", "Energy Management", listOf("energy", "matching")),
        Insight("bt011", InsightCategory.BEHAVIORAL_TRIGGERS, "Hunger and Thirst", "Physical needs can override habit intentions. Address basic needs before attempting complex habits.", "Maslow's Hierarchy", listOf("needs", "basics")),
        Insight("bt012", InsightCategory.BEHAVIORAL_TRIGGERS, "Weather Patterns", "Weather affects mood and behavior. Have indoor alternatives for outdoor habits.", "Weather Psychology", listOf("weather", "adaptation")),
        Insight("bt013", InsightCategory.BEHAVIORAL_TRIGGERS, "Monday Effect", "Week beginnings feel like fresh starts. Use Mondays for launching new habits.", "Fresh Start Effect", listOf("monday", "timing")),
        Insight("bt014", InsightCategory.BEHAVIORAL_TRIGGERS, "Completion Triggers", "Finishing one task naturally triggers consideration of the next. Link habits in sequence.", "Task Completion", listOf("completion", "sequence")),
        Insight("bt015", InsightCategory.BEHAVIORAL_TRIGGERS, "Body Position", "Standing versus sitting triggers different mental states. Use posture as a habit cue.", "Embodied Cognition", listOf("posture", "body")),
        Insight("bt016", InsightCategory.BEHAVIORAL_TRIGGERS, "Clothing Changes", "Changing clothes signals activity change. Use workout clothes as an exercise trigger.", "Enclothed Cognition", listOf("clothing", "signal")),
        Insight("bt017", InsightCategory.BEHAVIORAL_TRIGGERS, "Alarm Sounds", "Specific sounds can become powerful triggers. Use distinct sounds for different habits.", "Classical Conditioning", listOf("sounds", "alerts")),
        Insight("bt018", InsightCategory.BEHAVIORAL_TRIGGERS, "Calendar Events", "Scheduled events create anticipation. Put habits on your calendar with reminders.", "Scheduling Research", listOf("calendar", "scheduling")),
        Insight("bt019", InsightCategory.BEHAVIORAL_TRIGGERS, "Caffeine Timing", "Coffee creates an alert state that can trigger focused habits. Time it strategically.", "Caffeine Research", listOf("caffeine", "focus")),
        Insight("bt020", InsightCategory.BEHAVIORAL_TRIGGERS, "Post-Meal Windows", "After eating, energy is available for activity. Post-meal walks leverage this naturally.", "Digestion Research", listOf("meals", "timing")),
        Insight("bt021", InsightCategory.BEHAVIORAL_TRIGGERS, "Device Locations", "Where you charge your phone influences behavior. Charging away from the bedroom aids sleep habits.", "Digital Wellbeing", listOf("devices", "location")),
        Insight("bt022", InsightCategory.BEHAVIORAL_TRIGGERS, "Light Exposure", "Morning light triggers wakefulness. Bright light exposure reinforces morning habits.", "Circadian Research", listOf("light", "morning")),
        Insight("bt023", InsightCategory.BEHAVIORAL_TRIGGERS, "Verbal Prompts", "Speaking your intention aloud increases follow-through. 'I am about to exercise.'", "Self-Talk Research", listOf("verbal", "intention")),
        Insight("bt024", InsightCategory.BEHAVIORAL_TRIGGERS, "Written Reminders", "Sticky notes and visible lists serve as constant triggers. Place them strategically.", "Memory Research", listOf("visual", "reminders")),
        Insight("bt025", InsightCategory.BEHAVIORAL_TRIGGERS, "Accountability Partners", "Knowing someone is waiting for you creates social pressure to show up.", "Social Commitment", listOf("social", "accountability")),
        Insight("bt026", InsightCategory.BEHAVIORAL_TRIGGERS, "Streak Visualization", "Seeing your streak creates psychological investment. Don't break the chain.", "Jerry Seinfeld Method", listOf("streaks", "visual")),
        Insight("bt027", InsightCategory.BEHAVIORAL_TRIGGERS, "Physical Objects", "Specific objects can become habit triggers. A meditation cushion, a journal, a yoga mat.", "Object Association", listOf("objects", "association")),
        Insight("bt028", InsightCategory.BEHAVIORAL_TRIGGERS, "Routine Interruptions", "Vacations and holidays disrupt triggers. Plan in advance for maintaining habits.", "Routine Research", listOf("disruption", "planning")),
        Insight("bt029", InsightCategory.BEHAVIORAL_TRIGGERS, "Stress Response", "Stress often triggers comfort habits. Create healthy alternatives before stress hits.", "Stress Research", listOf("stress", "alternatives")),
        Insight("bt030", InsightCategory.BEHAVIORAL_TRIGGERS, "Boredom Triggers", "Boredom often triggers unhealthy habits. Have planned responses to idle moments.", "Boredom Research", listOf("boredom", "planning")),
        Insight("bt031", InsightCategory.BEHAVIORAL_TRIGGERS, "Reward Proximity", "Visible rewards enhance motivation. Keep the reward in sight during habit execution.", "Reward Research", listOf("rewards", "visibility")),
        Insight("bt032", InsightCategory.BEHAVIORAL_TRIGGERS, "Social Media Cues", "Seeing fitness posts can trigger exercise motivation. Curate your feed intentionally.", "Social Media Research", listOf("social-media", "influence")),
        Insight("bt033", InsightCategory.BEHAVIORAL_TRIGGERS, "Season Changes", "Seasonal shifts affect behavior naturally. Adapt habits to seasonal rhythms.", "Seasonal Research", listOf("seasons", "adaptation")),
        Insight("bt034", InsightCategory.BEHAVIORAL_TRIGGERS, "Energy Drinks Effect", "Caffeine and sugar create temporary energy. Be aware of the crash that follows.", "Energy Research", listOf("energy", "awareness")),
        Insight("bt035", InsightCategory.BEHAVIORAL_TRIGGERS, "Room Temperature", "Comfortable temperature facilitates habit execution. Extreme temperatures disrupt routines.", "Environment Research", listOf("temperature", "comfort")),
        Insight("bt036", InsightCategory.BEHAVIORAL_TRIGGERS, "Music as Primer", "Specific songs can prime specific moods and behaviors. Create habit playlists.", "Music Psychology", listOf("music", "priming")),
        Insight("bt037", InsightCategory.BEHAVIORAL_TRIGGERS, "First Thing Advantage", "Habits done first have highest compliance. Put priorities before distractions.", "Priority Research", listOf("morning", "priority")),
        Insight("bt038", InsightCategory.BEHAVIORAL_TRIGGERS, "Shower Transitions", "Showers mark transitions between states. Use them to trigger evening or morning routines.", "Transition Research", listOf("shower", "transition")),
        Insight("bt039", InsightCategory.BEHAVIORAL_TRIGGERS, "Weekend Patterns", "Weekend triggers differ from weekdays. Create weekend-specific habit cues.", "Weekly Patterns", listOf("weekend", "adaptation")),
        Insight("bt040", InsightCategory.BEHAVIORAL_TRIGGERS, "Fatigue Awareness", "Tiredness often triggers unhealthy comfort behaviors. Recognize when rest is the real need.", "Fatigue Research", listOf("fatigue", "awareness")),
        Insight("bt041", InsightCategory.BEHAVIORAL_TRIGGERS, "Celebration Moments", "Completing a habit deserves immediate celebration. This creates positive association.", "Reward Research", listOf("celebration", "positive")),
        Insight("bt042", InsightCategory.BEHAVIORAL_TRIGGERS, "Commute Triggers", "Travel time is underutilized. Transform commutes into habit opportunities.", "Commute Research", listOf("commute", "opportunity")),
        Insight("bt043", InsightCategory.BEHAVIORAL_TRIGGERS, "Before Bed Routine", "The hour before sleep shapes sleep quality. Create a consistent wind-down sequence.", "Sleep Hygiene", listOf("evening", "routine")),
        Insight("bt044", InsightCategory.BEHAVIORAL_TRIGGERS, "Wallet/Keys/Phone", "Essential items create daily touchpoints. Associate habits with picking them up.", "Daily Objects", listOf("objects", "daily")),
        Insight("bt045", InsightCategory.BEHAVIORAL_TRIGGERS, "Work Breaks", "Scheduled breaks are natural habit windows. Use them for micro-habits.", "Break Research", listOf("breaks", "work")),
        Insight("bt046", InsightCategory.BEHAVIORAL_TRIGGERS, "Social Eating Cues", "Eating with others influences portion sizes and choices. Be mindful of social eating.", "Social Eating", listOf("social", "eating")),
        Insight("bt047", InsightCategory.BEHAVIORAL_TRIGGERS, "Exercise Equipment Visibility", "Visible equipment serves as a constant cue. Don't hide your workout gear.", "Visual Cues", listOf("equipment", "visible")),
        Insight("bt048", InsightCategory.BEHAVIORAL_TRIGGERS, "App Opening Habits", "The first app you open sets the tone. Make it a productive one.", "Digital Habits", listOf("apps", "first")),
        Insight("bt049", InsightCategory.BEHAVIORAL_TRIGGERS, "Anticipatory Triggers", "Thinking about a future event can trigger preparation habits. Visualize your day.", "Anticipation Research", listOf("anticipation", "planning")),
        Insight("bt050", InsightCategory.BEHAVIORAL_TRIGGERS, "Pain Points", "Moments of frustration can trigger reflection habits. Use difficulty as a cue for growth.", "Growth Mindset", listOf("difficulty", "growth")),
        Insight("bt051", InsightCategory.BEHAVIORAL_TRIGGERS, "Sunset/Sunrise", "Natural light changes mark the day. Use them as triggers for morning and evening routines.", "Natural Rhythms", listOf("light", "natural")),
        Insight("bt052", InsightCategory.BEHAVIORAL_TRIGGERS, "Device Unlocking", "Each phone unlock is a trigger point. Consider what habit you want it to prompt.", "Digital Behavior", listOf("phone", "trigger"))
    )

    // ==================== PROGRESS MINDSET (52 Insights) ====================

    private val progressMindset = listOf(
        Insight("pm001", InsightCategory.PROGRESS_MINDSET, "Progress Over Perfection", "Done is better than perfect. A 70% workout is infinitely better than a 0% one.", "Mindset Research", listOf("perfection", "action")),
        Insight("pm002", InsightCategory.PROGRESS_MINDSET, "The 1% Rule", "Improving 1% daily means you'll be 37x better in a year. Small gains compound exponentially.", "Compounding", listOf("small-steps", "compounding")),
        Insight("pm003", InsightCategory.PROGRESS_MINDSET, "Celebrating Small Wins", "Every small victory releases dopamine. Celebrate completing your habit, no matter how small.", "Reward Research", listOf("celebration", "wins")),
        Insight("pm004", InsightCategory.PROGRESS_MINDSET, "Growth vs. Fixed Mindset", "Believing abilities can grow leads to greater effort and resilience. Abilities are not fixed.", "Carol Dweck", listOf("growth", "belief")),
        Insight("pm005", InsightCategory.PROGRESS_MINDSET, "The Comparison Trap", "Compare yourself only to your past self. Others' journeys are incomparable to yours.", "Comparison Research", listOf("comparison", "self")),
        Insight("pm006", InsightCategory.PROGRESS_MINDSET, "Embracing the Plateau", "Plateaus are not failures—they're consolidation phases. Your brain is integrating learning.", "Learning Research", listOf("plateau", "patience")),
        Insight("pm007", InsightCategory.PROGRESS_MINDSET, "Non-Linear Progress", "Progress is rarely a straight line. Expect setbacks, plateaus, and surprising breakthroughs.", "Progress Research", listOf("setbacks", "reality")),
        Insight("pm008", InsightCategory.PROGRESS_MINDSET, "Process Goals", "Focus on the process, not the outcome. 'I will run 3x this week' beats 'I will lose 10 pounds.'", "Goal Setting", listOf("process", "focus")),
        Insight("pm009", InsightCategory.PROGRESS_MINDSET, "The Seinfeld Strategy", "Don't break the chain. A visual streak creates psychological investment in continuing.", "Jerry Seinfeld", listOf("streaks", "visual")),
        Insight("pm010", InsightCategory.PROGRESS_MINDSET, "Minimum Viable Effort", "On hard days, do the absolute minimum. Some effort maintains the habit pathway.", "Habit Maintenance", listOf("minimum", "maintenance")),
        Insight("pm011", InsightCategory.PROGRESS_MINDSET, "Reframing Failure", "Failure is data, not verdict. Each slip teaches you something about your triggers.", "Growth Mindset", listOf("failure", "learning")),
        Insight("pm012", InsightCategory.PROGRESS_MINDSET, "Intrinsic Motivation", "Long-term habits require intrinsic motivation. Find personal meaning in your practice.", "Self-Determination Theory", listOf("motivation", "meaning")),
        Insight("pm013", InsightCategory.PROGRESS_MINDSET, "The Two-Day Rule", "Never miss twice in a row. One miss is an accident; two is the start of a new habit.", "Matt D'Avella", listOf("recovery", "rule")),
        Insight("pm014", InsightCategory.PROGRESS_MINDSET, "Delayed Gratification", "The ability to delay gratification predicts success. Practice waiting for rewards.", "Marshmallow Test", listOf("patience", "discipline")),
        Insight("pm015", InsightCategory.PROGRESS_MINDSET, "Identity Reinforcement", "Every habit completion is a vote for your new identity. You're becoming that person.", "James Clear", listOf("identity", "votes")),
        Insight("pm016", InsightCategory.PROGRESS_MINDSET, "Measuring What Matters", "Track the right metrics. Effort and consistency matter more than outcomes initially.", "Measurement", listOf("tracking", "metrics")),
        Insight("pm017", InsightCategory.PROGRESS_MINDSET, "The Aggregation of Marginal Gains", "British Cycling won by improving everything by 1%. Micro-improvements add up.", "Dave Brailsford", listOf("marginal-gains", "details")),
        Insight("pm018", InsightCategory.PROGRESS_MINDSET, "Patience Is a Strategy", "Sustainable change takes time. Patience is not passive—it's strategic.", "Long-term Thinking", listOf("patience", "strategy")),
        Insight("pm019", InsightCategory.PROGRESS_MINDSET, "The Learning Zone", "Growth happens when you're slightly uncomfortable. Seek the edge of your ability.", "Learning Research", listOf("discomfort", "growth")),
        Insight("pm020", InsightCategory.PROGRESS_MINDSET, "Self-Compassion", "Treating yourself kindly after setbacks increases resilience. Harsh self-criticism backfires.", "Kristin Neff", listOf("compassion", "resilience")),
        Insight("pm021", InsightCategory.PROGRESS_MINDSET, "Visualizing Success", "Mental rehearsal activates the same brain regions as physical practice. See yourself succeeding.", "Visualization Research", listOf("visualization", "mental")),
        Insight("pm022", InsightCategory.PROGRESS_MINDSET, "The Effort Paradox", "What feels hard now will feel easy later. Difficulty is temporary; skill is permanent.", "Skill Acquisition", listOf("effort", "skill")),
        Insight("pm023", InsightCategory.PROGRESS_MINDSET, "Avoiding All-or-Nothing", "Perfect compliance isn't required. 80% consistency beats 100% for two weeks then quitting.", "Sustainability", listOf("consistency", "realistic")),
        Insight("pm024", InsightCategory.PROGRESS_MINDSET, "Finding Your Why", "Deep purpose sustains habits through difficulty. Know why this matters to you.", "Purpose Research", listOf("purpose", "motivation")),
        Insight("pm025", InsightCategory.PROGRESS_MINDSET, "Accepting Impermanence", "Energy, motivation, and circumstances fluctuate. Flexible consistency beats rigid perfection.", "Flexibility", listOf("flexibility", "adaptation")),
        Insight("pm026", InsightCategory.PROGRESS_MINDSET, "The Power of Yet", "'I can't do this' becomes 'I can't do this yet.' Language shapes possibility.", "Carol Dweck", listOf("language", "possibility")),
        Insight("pm027", InsightCategory.PROGRESS_MINDSET, "Micro-Progress", "If you can't do the full habit, do 10%. Micro-progress maintains momentum.", "Minimum Viable", listOf("micro", "momentum")),
        Insight("pm028", InsightCategory.PROGRESS_MINDSET, "Gratitude for Ability", "Be grateful you can attempt your habit. Many cannot. Gratitude fuels motivation.", "Gratitude Research", listOf("gratitude", "ability")),
        Insight("pm029", InsightCategory.PROGRESS_MINDSET, "Long-Term Thinking", "Ask: 'Will this matter in 5 years?' If yes, it deserves daily attention now.", "Long-term Perspective", listOf("perspective", "future")),
        Insight("pm030", InsightCategory.PROGRESS_MINDSET, "Effort-Based Praise", "Praise your effort, not your talent. 'I worked hard' beats 'I'm naturally good.'", "Carol Dweck", listOf("effort", "praise")),
        Insight("pm031", InsightCategory.PROGRESS_MINDSET, "The Feedback Loop", "Track, measure, and adjust. What gets measured gets managed.", "Feedback Research", listOf("tracking", "feedback")),
        Insight("pm032", InsightCategory.PROGRESS_MINDSET, "Embracing Beginner's Mind", "Being a beginner is a temporary stage. Everyone who is great was once terrible.", "Beginner Mindset", listOf("beginner", "growth")),
        Insight("pm033", InsightCategory.PROGRESS_MINDSET, "Systems vs. Goals", "Goals are about the destination; systems are about the journey. Focus on systems.", "James Clear", listOf("systems", "process")),
        Insight("pm034", InsightCategory.PROGRESS_MINDSET, "The Obstacle Is The Way", "Difficulties aren't blocking your progress—they are your progress. Embrace challenges.", "Ryan Holiday", listOf("obstacles", "growth")),
        Insight("pm035", InsightCategory.PROGRESS_MINDSET, "Consistency Compounds", "Showing up daily matters more than intensity. Consistent average beats sporadic excellence.", "Consistency Research", listOf("consistency", "daily")),
        Insight("pm036", InsightCategory.PROGRESS_MINDSET, "Positive Self-Talk", "How you talk to yourself matters. 'I get to exercise' beats 'I have to exercise.'", "Self-Talk Research", listOf("language", "positive")),
        Insight("pm037", InsightCategory.PROGRESS_MINDSET, "The Momentum Effect", "Objects in motion stay in motion. Getting started is the hardest part.", "Physics Metaphor", listOf("momentum", "starting")),
        Insight("pm038", InsightCategory.PROGRESS_MINDSET, "Anticipating Obstacles", "Mentally prepare for challenges. 'If obstacle X occurs, I will do Y.'", "Implementation Intentions", listOf("obstacles", "planning")),
        Insight("pm039", InsightCategory.PROGRESS_MINDSET, "Energy Management", "Protect your energy for important habits. Say no to draining activities.", "Energy Research", listOf("energy", "protection")),
        Insight("pm040", InsightCategory.PROGRESS_MINDSET, "The Dip", "Every worthwhile pursuit has a difficult middle phase. Push through the dip.", "Seth Godin", listOf("persistence", "middle")),
        Insight("pm041", InsightCategory.PROGRESS_MINDSET, "Enjoying the Process", "If you only enjoy the outcome, you're suffering 99% of the time. Learn to love the work.", "Process Enjoyment", listOf("enjoyment", "process")),
        Insight("pm042", InsightCategory.PROGRESS_MINDSET, "Negative Visualization", "Imagine losing your ability to practice. This creates gratitude for current opportunities.", "Stoic Practice", listOf("gratitude", "perspective")),
        Insight("pm043", InsightCategory.PROGRESS_MINDSET, "Rest as Progress", "Recovery is part of progress, not a break from it. Rest allows growth.", "Recovery Research", listOf("rest", "recovery")),
        Insight("pm044", InsightCategory.PROGRESS_MINDSET, "The Comparison Fallacy", "You see others' highlights, not their struggles. Everyone has unseen battles.", "Social Comparison", listOf("comparison", "reality")),
        Insight("pm045", InsightCategory.PROGRESS_MINDSET, "Intentional Practice", "Mindless repetition isn't practice. Deliberate focus creates real improvement.", "Anders Ericsson", listOf("deliberate", "focus")),
        Insight("pm046", InsightCategory.PROGRESS_MINDSET, "The Finish Line Illusion", "Achieving a goal doesn't end the journey. Maintenance is a lifelong practice.", "Long-term View", listOf("maintenance", "ongoing")),
        Insight("pm047", InsightCategory.PROGRESS_MINDSET, "Time In vs. Outcomes", "Early on, focus on time spent rather than results. Skills take time to develop.", "Skill Development", listOf("time", "patience")),
        Insight("pm048", InsightCategory.PROGRESS_MINDSET, "Experimentation Mindset", "Treat habits as experiments, not commitments. This reduces pressure and increases learning.", "Experimentation", listOf("experiment", "learning")),
        Insight("pm049", InsightCategory.PROGRESS_MINDSET, "The 10-Year Rule", "Mastery takes about 10 years or 10,000 hours. Embrace the long road.", "Anders Ericsson", listOf("mastery", "long-term")),
        Insight("pm050", InsightCategory.PROGRESS_MINDSET, "Reflection Practice", "Regular reflection accelerates growth. What worked? What didn't? What will you try next?", "Reflection Research", listOf("reflection", "learning")),
        Insight("pm051", InsightCategory.PROGRESS_MINDSET, "The Showing Up Victory", "On your worst days, just showing up is a win. Presence counts.", "Minimum Viable", listOf("showing-up", "presence")),
        Insight("pm052", InsightCategory.PROGRESS_MINDSET, "Future Self Connection", "Your habits are a gift to your future self. What will you thank yourself for?", "Future Self Research", listOf("future", "gift"))
    )

    // ==================== SOCIAL PSYCHOLOGY (52 Insights) ====================

    private val socialPsychology = listOf(
        Insight("sp001", InsightCategory.SOCIAL_PSYCHOLOGY, "Social Proof Power", "We look to others for behavior cues. Surround yourself with people who have your desired habits.", "Robert Cialdini", listOf("social-proof", "environment")),
        Insight("sp002", InsightCategory.SOCIAL_PSYCHOLOGY, "The Five People Rule", "You're the average of the five people you spend the most time with. Choose wisely.", "Jim Rohn", listOf("influence", "circle")),
        Insight("sp003", InsightCategory.SOCIAL_PSYCHOLOGY, "Accountability Multiplier", "People with accountability partners have 65% higher success rates.", "ASTD Research", listOf("accountability", "partners")),
        Insight("sp004", InsightCategory.SOCIAL_PSYCHOLOGY, "Public Commitment Effect", "Public commitments increase follow-through. Tell someone your intention.", "Commitment Research", listOf("public", "commitment")),
        Insight("sp005", InsightCategory.SOCIAL_PSYCHOLOGY, "The Köhler Effect", "People work harder in groups than alone when they're the weakest link. Find a slightly better group.", "Otto Köhler", listOf("groups", "motivation")),
        Insight("sp006", InsightCategory.SOCIAL_PSYCHOLOGY, "Mirroring Behavior", "We unconsciously mimic those around us. Be aware of what you're absorbing.", "Mirror Neurons", listOf("mirroring", "awareness")),
        Insight("sp007", InsightCategory.SOCIAL_PSYCHOLOGY, "Supportive vs. Sabotaging", "Some relationships support habits, others sabotage them. Identify and address saboteurs.", "Relationship Research", listOf("relationships", "support")),
        Insight("sp008", InsightCategory.SOCIAL_PSYCHOLOGY, "Social Facilitation", "Simple tasks improve with an audience; complex tasks may suffer. Choose when to share.", "Social Psychology", listOf("audience", "performance")),
        Insight("sp009", InsightCategory.SOCIAL_PSYCHOLOGY, "Conformity Pressure", "Groups can pressure us toward both good and bad habits. Choose groups aligned with your goals.", "Solomon Asch", listOf("conformity", "groups")),
        Insight("sp010", InsightCategory.SOCIAL_PSYCHOLOGY, "The Cheerleader Effect", "We perceive individuals as more attractive in groups. Similarly, group habits feel more achievable.", "Social Psychology", listOf("groups", "perception")),
        Insight("sp011", InsightCategory.SOCIAL_PSYCHOLOGY, "Teaching to Learn", "Explaining a habit to others reinforces your own understanding and commitment.", "Feynman Technique", listOf("teaching", "learning")),
        Insight("sp012", InsightCategory.SOCIAL_PSYCHOLOGY, "Healthy Competition", "Friendly competition can boost motivation. Just ensure it stays healthy, not obsessive.", "Competition Research", listOf("competition", "motivation")),
        Insight("sp013", InsightCategory.SOCIAL_PSYCHOLOGY, "Social Contagion", "Behaviors spread through networks like viruses. Your habits influence three degrees of separation.", "Nicholas Christakis", listOf("contagion", "influence")),
        Insight("sp014", InsightCategory.SOCIAL_PSYCHOLOGY, "The Observer Effect", "Knowing you'll report to someone changes behavior. Create gentle accountability structures.", "Observer Effect", listOf("observation", "accountability")),
        Insight("sp015", InsightCategory.SOCIAL_PSYCHOLOGY, "Shared Identity", "Groups with shared identity have stronger commitment. 'We are people who...'", "Social Identity", listOf("identity", "groups")),
        Insight("sp016", InsightCategory.SOCIAL_PSYCHOLOGY, "Mentorship Value", "Having a mentor accelerates learning. Find someone who's where you want to be.", "Mentorship Research", listOf("mentors", "learning")),
        Insight("sp017", InsightCategory.SOCIAL_PSYCHOLOGY, "Reciprocity Norm", "When others support us, we feel obligated to reciprocate. This strengthens mutual accountability.", "Robert Cialdini", listOf("reciprocity", "support")),
        Insight("sp018", InsightCategory.SOCIAL_PSYCHOLOGY, "Social Exclusion Pain", "Being excluded activates the same brain regions as physical pain. Choose inclusive communities.", "Naomi Eisenberger", listOf("exclusion", "belonging")),
        Insight("sp019", InsightCategory.SOCIAL_PSYCHOLOGY, "Role Models", "Visible role models make goals seem more achievable. Find people who represent your aspirations.", "Role Model Research", listOf("role-models", "inspiration")),
        Insight("sp020", InsightCategory.SOCIAL_PSYCHOLOGY, "Peer Pressure Positive", "Peer pressure can be positive. Join communities where healthy habits are the norm.", "Peer Influence", listOf("peer-pressure", "positive")),
        Insight("sp021", InsightCategory.SOCIAL_PSYCHOLOGY, "Family Influence", "Family habits are deeply ingrained. Acknowledge this without letting it define your future.", "Family Systems", listOf("family", "awareness")),
        Insight("sp022", InsightCategory.SOCIAL_PSYCHOLOGY, "The Spotlight Effect", "We overestimate how much others notice us. People are less focused on your failures than you think.", "Thomas Gilovich", listOf("spotlight", "perception")),
        Insight("sp023", InsightCategory.SOCIAL_PSYCHOLOGY, "Community Belonging", "Feeling part of a community satisfies a basic human need. Find your tribe.", "Belongingness", listOf("community", "belonging")),
        Insight("sp024", InsightCategory.SOCIAL_PSYCHOLOGY, "Constructive Criticism", "Feedback from trusted others accelerates improvement. Seek honest input.", "Feedback Research", listOf("feedback", "growth")),
        Insight("sp025", InsightCategory.SOCIAL_PSYCHOLOGY, "Emotional Contagion", "Emotions spread between people. Surround yourself with positive, motivated individuals.", "Emotional Contagion", listOf("emotions", "environment")),
        Insight("sp026", InsightCategory.SOCIAL_PSYCHOLOGY, "Celebration Together", "Shared celebrations amplify positive emotions. Celebrate wins with your community.", "Shared Joy", listOf("celebration", "together")),
        Insight("sp027", InsightCategory.SOCIAL_PSYCHOLOGY, "Social Learning Theory", "We learn by observing others. Watch people who have mastered what you're learning.", "Albert Bandura", listOf("observation", "learning")),
        Insight("sp028", InsightCategory.SOCIAL_PSYCHOLOGY, "Habit Buddy Benefits", "Exercising with a friend increases enjoyment and commitment. Find your habit buddy.", "Social Exercise", listOf("buddies", "exercise")),
        Insight("sp029", InsightCategory.SOCIAL_PSYCHOLOGY, "Online Communities", "Digital communities can provide the same support as physical ones. Find your online tribe.", "Online Support", listOf("online", "community")),
        Insight("sp030", InsightCategory.SOCIAL_PSYCHOLOGY, "The Herd Instinct", "We feel safety in numbers. This is why group fitness classes have high retention.", "Herd Behavior", listOf("groups", "safety")),
        Insight("sp031", InsightCategory.SOCIAL_PSYCHOLOGY, "Shame vs. Guilt", "Shame says 'I am bad'; guilt says 'I did bad.' Guilt motivates change; shame paralyzes.", "Brené Brown", listOf("shame", "guilt")),
        Insight("sp032", InsightCategory.SOCIAL_PSYCHOLOGY, "Social Comparison", "Upward comparison can motivate or discourage. Compare to people slightly ahead, not far ahead.", "Social Comparison", listOf("comparison", "motivation")),
        Insight("sp033", InsightCategory.SOCIAL_PSYCHOLOGY, "Relational Self", "Our sense of self is partly defined by our relationships. Healthy habits strengthen relationships.", "Self-Concept", listOf("relationships", "self")),
        Insight("sp034", InsightCategory.SOCIAL_PSYCHOLOGY, "Network Effects", "The value of a habit increases when more people in your network share it.", "Network Theory", listOf("networks", "value")),
        Insight("sp035", InsightCategory.SOCIAL_PSYCHOLOGY, "Couple Goals", "Couples who pursue goals together have stronger relationships. Make habits a team effort.", "Couples Research", listOf("couples", "together")),
        Insight("sp036", InsightCategory.SOCIAL_PSYCHOLOGY, "Family Habit Cascade", "Your habits influence your family, especially children. You're modeling for others.", "Family Influence", listOf("family", "modeling")),
        Insight("sp037", InsightCategory.SOCIAL_PSYCHOLOGY, "Support Group Power", "Formal support groups create structured accountability. Consider joining one.", "Support Groups", listOf("support", "structure")),
        Insight("sp038", InsightCategory.SOCIAL_PSYCHOLOGY, "Social Capital", "Healthy habits increase your social capital—people trust reliable individuals.", "Social Capital", listOf("trust", "reliability")),
        Insight("sp039", InsightCategory.SOCIAL_PSYCHOLOGY, "Interpersonal Accountability", "Being accountable to another person is more motivating than being accountable to yourself.", "Accountability Research", listOf("interpersonal", "motivation")),
        Insight("sp040", InsightCategory.SOCIAL_PSYCHOLOGY, "The Halo Effect", "People perceived as disciplined in one area are assumed to be disciplined in others.", "Edward Thorndike", listOf("perception", "discipline")),
        Insight("sp041", InsightCategory.SOCIAL_PSYCHOLOGY, "Altruistic Motivation", "Helping others with their habits strengthens your own. Become a mentor.", "Altruism Research", listOf("helping", "strength")),
        Insight("sp042", InsightCategory.SOCIAL_PSYCHOLOGY, "Social Rewards", "Social recognition is a powerful reward. Share your wins appropriately.", "Social Recognition", listOf("recognition", "sharing")),
        Insight("sp043", InsightCategory.SOCIAL_PSYCHOLOGY, "Friendship and Fitness", "Friends who exercise together tend to be healthier overall. Active friendships matter.", "Friendship Research", listOf("friends", "fitness")),
        Insight("sp044", InsightCategory.SOCIAL_PSYCHOLOGY, "Cultural Norms", "Cultural context shapes which habits are valued. Recognize cultural influences on your goals.", "Cultural Psychology", listOf("culture", "norms")),
        Insight("sp045", InsightCategory.SOCIAL_PSYCHOLOGY, "Generation Transmission", "Habits pass through generations. You can be the one to break negative cycles.", "Intergenerational", listOf("generations", "change")),
        Insight("sp046", InsightCategory.SOCIAL_PSYCHOLOGY, "Social Media Curating", "What you follow shapes you. Curate feeds that support your habit goals.", "Social Media", listOf("social-media", "curation")),
        Insight("sp047", InsightCategory.SOCIAL_PSYCHOLOGY, "Prosocial Habits", "Habits that benefit others are easier to maintain. Find ways your habits serve beyond yourself.", "Prosocial Behavior", listOf("others", "purpose")),
        Insight("sp048", InsightCategory.SOCIAL_PSYCHOLOGY, "Rejection Sensitivity", "Fear of rejection can prevent asking for support. Most people want to help.", "Rejection Research", listOf("asking", "support")),
        Insight("sp049", InsightCategory.SOCIAL_PSYCHOLOGY, "Collective Efficacy", "Groups that believe they can succeed together are more likely to do so.", "Albert Bandura", listOf("groups", "belief")),
        Insight("sp050", InsightCategory.SOCIAL_PSYCHOLOGY, "The Ripple Effect", "Your positive changes create ripples affecting people you'll never meet.", "Systems Thinking", listOf("ripples", "impact")),
        Insight("sp051", InsightCategory.SOCIAL_PSYCHOLOGY, "Expressing Vulnerability", "Sharing struggles creates connection and elicits support. It's okay to not be okay.", "Brené Brown", listOf("vulnerability", "connection")),
        Insight("sp052", InsightCategory.SOCIAL_PSYCHOLOGY, "Celebratory Rituals", "Shared rituals mark achievements and create bonds. Create celebration rituals for milestones.", "Rituals Research", listOf("rituals", "celebration"))
    )

    // ==================== RECOVERY & COMPASSION (52 Insights) ====================

    private val recoveryCompassion = listOf(
        Insight("rc001", InsightCategory.RECOVERY_COMPASSION, "The Self-Compassion Advantage", "Self-compassion after setbacks increases future success rates by 40%. Be kind to yourself.", "Kristin Neff", listOf("compassion", "setbacks")),
        Insight("rc002", InsightCategory.RECOVERY_COMPASSION, "Failure Is Feedback", "Every slip provides data. Ask 'What triggered this?' instead of 'What's wrong with me?'", "Growth Mindset", listOf("failure", "learning")),
        Insight("rc003", InsightCategory.RECOVERY_COMPASSION, "The Fresh Start Mindset", "Each day is a new beginning. Yesterday's slip doesn't define today's choices.", "Fresh Start Effect", listOf("fresh-start", "present")),
        Insight("rc004", InsightCategory.RECOVERY_COMPASSION, "Rest Is Productive", "Rest isn't laziness—it's recovery. Your brain consolidates learning during rest.", "Recovery Research", listOf("rest", "productive")),
        Insight("rc005", InsightCategory.RECOVERY_COMPASSION, "Perfectionism Is the Enemy", "Perfectionism leads to procrastination and burnout. Done is better than perfect.", "Perfectionism Research", listOf("perfectionism", "progress")),
        Insight("rc006", InsightCategory.RECOVERY_COMPASSION, "The Setback Opportunity", "Setbacks reveal vulnerabilities you can now address. They make your system stronger.", "Antifragility", listOf("setbacks", "growth")),
        Insight("rc007", InsightCategory.RECOVERY_COMPASSION, "Realistic Expectations", "Expecting perfection guarantees disappointment. Aim for consistency, not perfection.", "Expectations Research", listOf("realistic", "consistency")),
        Insight("rc008", InsightCategory.RECOVERY_COMPASSION, "Human, Not Machine", "You're not a robot. Energy fluctuates, motivation wanes, life happens. This is normal.", "Humanity", listOf("human", "normal")),
        Insight("rc009", InsightCategory.RECOVERY_COMPASSION, "The Recovery Ratio", "For every hour of intense effort, you need recovery time. Build rest into your schedule.", "Performance Research", listOf("recovery", "scheduling")),
        Insight("rc010", InsightCategory.RECOVERY_COMPASSION, "Treating Yourself Like a Friend", "Would you berate a friend for slipping? Extend the same kindness to yourself.", "Self-Compassion", listOf("friendship", "kindness")),
        Insight("rc011", InsightCategory.RECOVERY_COMPASSION, "Acknowledging Progress Made", "Even after a setback, you haven't lost all progress. Skills and neural pathways remain.", "Progress Persistence", listOf("progress", "persistence")),
        Insight("rc012", InsightCategory.RECOVERY_COMPASSION, "Flexibility Over Rigidity", "Rigid rules break under pressure. Flexible guidelines bend and survive.", "Flexibility", listOf("flexibility", "survival")),
        Insight("rc013", InsightCategory.RECOVERY_COMPASSION, "The Bounce-Back Ability", "Resilience isn't avoiding falls—it's getting back up quickly. Focus on quick recovery.", "Resilience Research", listOf("resilience", "recovery")),
        Insight("rc014", InsightCategory.RECOVERY_COMPASSION, "Normalization of Struggle", "Everyone struggles with habits. You're not uniquely flawed—you're perfectly human.", "Common Humanity", listOf("struggle", "normal")),
        Insight("rc015", InsightCategory.RECOVERY_COMPASSION, "Learning from Lapses", "What time did you slip? What were you feeling? Where were you? Analyze without judgment.", "Analysis", listOf("learning", "analysis")),
        Insight("rc016", InsightCategory.RECOVERY_COMPASSION, "The 80% Rule", "Aim for 80% consistency, not 100%. This is sustainable and allows for life's unpredictability.", "Sustainability", listOf("80-percent", "sustainable")),
        Insight("rc017", InsightCategory.RECOVERY_COMPASSION, "Temporary States", "Feelings of failure are temporary states, not permanent truths. This too shall pass.", "Impermanence", listOf("temporary", "feelings")),
        Insight("rc018", InsightCategory.RECOVERY_COMPASSION, "Self-Forgiveness Practice", "Holding onto guilt consumes energy needed for change. Forgive yourself and redirect.", "Forgiveness", listOf("forgiveness", "energy")),
        Insight("rc019", InsightCategory.RECOVERY_COMPASSION, "The Restart Button", "Every moment offers a chance to restart. You don't need to wait for Monday.", "Fresh Start", listOf("restart", "now")),
        Insight("rc020", InsightCategory.RECOVERY_COMPASSION, "Redefining Success", "Success isn't never failing—it's continuing despite failures. Persistence is success.", "Success Definition", listOf("success", "persistence")),
        Insight("rc021", InsightCategory.RECOVERY_COMPASSION, "Energy Conservation", "Some days, survival mode is enough. Protect your energy for true recovery.", "Energy Management", listOf("energy", "survival")),
        Insight("rc022", InsightCategory.RECOVERY_COMPASSION, "Sleep as Foundation", "Poor sleep undermines all other habits. Prioritize sleep during recovery phases.", "Sleep Priority", listOf("sleep", "foundation")),
        Insight("rc023", InsightCategory.RECOVERY_COMPASSION, "Stress Recognition", "Recognize when stress is making habits harder. Address the stress, not just the habit.", "Stress Awareness", listOf("stress", "recognition")),
        Insight("rc024", InsightCategory.RECOVERY_COMPASSION, "The Progress Illusion", "Setbacks feel bigger than they are. A week of slips doesn't erase months of progress.", "Perspective", listOf("perspective", "setbacks")),
        Insight("rc025", InsightCategory.RECOVERY_COMPASSION, "Emotional First Aid", "Attend to emotional wounds before pushing forward. Healing enables sustained effort.", "Emotional Care", listOf("emotions", "healing")),
        Insight("rc026", InsightCategory.RECOVERY_COMPASSION, "The Permission Slip", "Give yourself permission to be imperfect. Permission reduces resistance and guilt.", "Permission", listOf("permission", "imperfect")),
        Insight("rc027", InsightCategory.RECOVERY_COMPASSION, "Lowering the Bar", "During difficult times, lower the bar dramatically. Some movement is better than none.", "Adjustment", listOf("lowering", "difficult-times")),
        Insight("rc028", InsightCategory.RECOVERY_COMPASSION, "Celebrating Attempts", "Celebrate attempts, not just completions. Trying in the face of difficulty is brave.", "Attempts", listOf("celebration", "attempts")),
        Insight("rc029", InsightCategory.RECOVERY_COMPASSION, "The Comparison Pause", "Stop comparing your recovery to others' highlights. Your journey is unique.", "Comparison", listOf("comparison", "pause")),
        Insight("rc030", InsightCategory.RECOVERY_COMPASSION, "Physical Recovery Needs", "Sometimes the body needs rest more than the mind wants to push. Listen to your body.", "Body Wisdom", listOf("body", "listening")),
        Insight("rc031", InsightCategory.RECOVERY_COMPASSION, "Mental Health Priority", "Mental health affects habit capacity. Address mental health as a foundational habit.", "Mental Health", listOf("mental-health", "foundation")),
        Insight("rc032", InsightCategory.RECOVERY_COMPASSION, "The Non-Judgment Zone", "Practice observing slips without judgment. 'I notice I skipped' instead of 'I'm a failure.'", "Mindfulness", listOf("observation", "non-judgment")),
        Insight("rc033", InsightCategory.RECOVERY_COMPASSION, "Support Seeking", "Asking for help is strength, not weakness. Reach out when you're struggling.", "Support", listOf("help", "strength")),
        Insight("rc034", InsightCategory.RECOVERY_COMPASSION, "The Spiral Up", "Just as negative spirals exist, so do positive ones. One good choice leads to another.", "Positive Spiral", listOf("positive", "choices")),
        Insight("rc035", InsightCategory.RECOVERY_COMPASSION, "Burnout Prevention", "Burnout is real. Build recovery into your habit schedule before it's forced upon you.", "Burnout", listOf("prevention", "scheduling")),
        Insight("rc036", InsightCategory.RECOVERY_COMPASSION, "Seasons of Life", "Life has seasons—some are for building, some for maintaining, some for rest. Honor the season.", "Seasons", listOf("seasons", "honoring")),
        Insight("rc037", InsightCategory.RECOVERY_COMPASSION, "The Long View", "One bad day, week, or month doesn't define your journey. Take the long view.", "Perspective", listOf("long-view", "journey")),
        Insight("rc038", InsightCategory.RECOVERY_COMPASSION, "Acceptance Before Change", "Accept where you are before trying to change. Resistance wastes energy.", "Acceptance", listOf("acceptance", "change")),
        Insight("rc039", InsightCategory.RECOVERY_COMPASSION, "Self-Care Priority", "Self-care isn't selfish—it's foundational. You can't pour from an empty cup.", "Self-Care", listOf("self-care", "foundation")),
        Insight("rc040", InsightCategory.RECOVERY_COMPASSION, "The Comeback Story", "Every great story has low points. Your setback is setting up your comeback.", "Narrative", listOf("comeback", "story")),
        Insight("rc041", InsightCategory.RECOVERY_COMPASSION, "Celebrating Resilience", "The fact that you're still trying after setbacks is remarkable. Celebrate your resilience.", "Resilience", listOf("celebration", "resilience")),
        Insight("rc042", InsightCategory.RECOVERY_COMPASSION, "The Minimum Dose", "During recovery, focus on the minimum effective dose. Just enough to maintain.", "Minimum", listOf("minimum", "maintaining")),
        Insight("rc043", InsightCategory.RECOVERY_COMPASSION, "Grief for Lost Streaks", "It's okay to grieve a broken streak. Acknowledge the loss, then begin again.", "Grief", listOf("grief", "beginning")),
        Insight("rc044", InsightCategory.RECOVERY_COMPASSION, "Illness Allowance", "Being sick is a valid reason to pause habits. Recovery requires energy.", "Illness", listOf("illness", "pause")),
        Insight("rc045", InsightCategory.RECOVERY_COMPASSION, "Life Events Impact", "Major life events—moves, breakups, job changes—affect habit capacity. Adjust expectations.", "Life Events", listOf("adjusting", "events")),
        Insight("rc046", InsightCategory.RECOVERY_COMPASSION, "The Second Chance", "Every moment is a second chance. Don't wait for tomorrow to start again.", "Second Chance", listOf("now", "starting")),
        Insight("rc047", InsightCategory.RECOVERY_COMPASSION, "Kindness Cascade", "Being kind to yourself makes you kinder to others. Self-compassion benefits everyone.", "Kindness", listOf("kindness", "others")),
        Insight("rc048", InsightCategory.RECOVERY_COMPASSION, "The 'Good Enough' Standard", "Sometimes good enough is perfect. Let go of the ideal and embrace the real.", "Good Enough", listOf("standards", "real")),
        Insight("rc049", InsightCategory.RECOVERY_COMPASSION, "Rebuilding Trust", "After setbacks, slowly rebuild self-trust through small wins. Trust is earned.", "Trust", listOf("trust", "small-wins")),
        Insight("rc050", InsightCategory.RECOVERY_COMPASSION, "The Forgetting Curve", "Your brain naturally forgets struggles. Future you will barely remember today's slip.", "Memory", listOf("forgetting", "future")),
        Insight("rc051", InsightCategory.RECOVERY_COMPASSION, "Compassion as Fuel", "Self-criticism drains energy; self-compassion restores it. Choose your fuel wisely.", "Fuel", listOf("compassion", "energy")),
        Insight("rc052", InsightCategory.RECOVERY_COMPASSION, "Tomorrow Is Unwritten", "No matter what happened today, tomorrow's page is blank. What will you write?", "Tomorrow", listOf("future", "possibility"))
    )

    // ==================== ADVANCED TECHNIQUES (53 Insights) ====================

    private val advancedTechniques = listOf(
        Insight("at001", InsightCategory.ADVANCED_TECHNIQUES, "Environment Design", "Don't rely on willpower—design your environment for success. Make good choices the easy choices.", "James Clear", listOf("environment", "design")),
        Insight("at002", InsightCategory.ADVANCED_TECHNIQUES, "Precommitment Devices", "Lock in future behavior by removing options now. Put money on the line if needed.", "Dan Ariely", listOf("precommitment", "stakes")),
        Insight("at003", InsightCategory.ADVANCED_TECHNIQUES, "Habit Shaping", "Don't aim for the final version immediately. Shape habits gradually toward the goal.", "B.F. Skinner", listOf("shaping", "gradual")),
        Insight("at004", InsightCategory.ADVANCED_TECHNIQUES, "Time Blocking", "Schedule habits like appointments. What's scheduled gets done.", "Cal Newport", listOf("scheduling", "blocking")),
        Insight("at005", InsightCategory.ADVANCED_TECHNIQUES, "Implementation Intentions", "'If X happens, I will do Y' doubles success rates. Plan for specific scenarios.", "Peter Gollwitzer", listOf("planning", "if-then")),
        Insight("at006", InsightCategory.ADVANCED_TECHNIQUES, "Batching Similar Tasks", "Group similar habits together. This reduces transition costs and improves efficiency.", "Task Batching", listOf("batching", "efficiency")),
        Insight("at007", InsightCategory.ADVANCED_TECHNIQUES, "Habit Linking", "Chain new habits to existing strong ones. Leverage established neural pathways.", "Habit Stacking", listOf("linking", "established")),
        Insight("at008", InsightCategory.ADVANCED_TECHNIQUES, "Visual Progress Tracking", "Make progress visible with calendars, charts, or physical markers. Seeing is motivating.", "Visual Tracking", listOf("visual", "tracking")),
        Insight("at009", InsightCategory.ADVANCED_TECHNIQUES, "The Ulysses Contract", "Tie yourself to the mast like Ulysses. Create situations where you can't fail.", "Precommitment", listOf("commitment", "forced")),
        Insight("at010", InsightCategory.ADVANCED_TECHNIQUES, "Friction Engineering", "Add friction to bad habits, remove friction from good ones. Make it easy or hard.", "Friction Design", listOf("friction", "engineering")),
        Insight("at011", InsightCategory.ADVANCED_TECHNIQUES, "Morning Routines", "Win the morning, win the day. Front-load important habits when willpower is highest.", "Morning Mastery", listOf("morning", "willpower")),
        Insight("at012", InsightCategory.ADVANCED_TECHNIQUES, "Evening Preparation", "Prepare for tomorrow tonight. Lay out clothes, pack bags, set intentions.", "Evening Prep", listOf("evening", "preparation")),
        Insight("at013", InsightCategory.ADVANCED_TECHNIQUES, "The Pomodoro Technique", "Work in focused 25-minute blocks with 5-minute breaks. This maintains concentration.", "Francesco Cirillo", listOf("pomodoro", "focus")),
        Insight("at014", InsightCategory.ADVANCED_TECHNIQUES, "Habit Mapping", "Map your entire habit ecosystem. See how habits interact and influence each other.", "Systems Thinking", listOf("mapping", "ecosystem")),
        Insight("at015", InsightCategory.ADVANCED_TECHNIQUES, "Keystone Identification", "Identify your keystone habits—the ones that trigger cascades of other positive behaviors.", "Charles Duhigg", listOf("keystone", "cascade")),
        Insight("at016", InsightCategory.ADVANCED_TECHNIQUES, "Reward Scheduling", "Schedule rewards at variable intervals. Unpredictable rewards are more motivating.", "Variable Reward", listOf("rewards", "variable")),
        Insight("at017", InsightCategory.ADVANCED_TECHNIQUES, "Habit Contracts", "Write a formal contract with yourself or others specifying consequences for missing habits.", "Contracts", listOf("contracts", "formal")),
        Insight("at018", InsightCategory.ADVANCED_TECHNIQUES, "The 3-2-1 Method", "For morning habits: prepare 3 things tonight, wake 2 hours before obligations, spend 1 hour on you.", "Morning Method", listOf("morning", "method")),
        Insight("at019", InsightCategory.ADVANCED_TECHNIQUES, "Atomic Habits Framework", "Make it obvious, attractive, easy, and satisfying. The four laws of behavior change.", "James Clear", listOf("framework", "laws")),
        Insight("at020", InsightCategory.ADVANCED_TECHNIQUES, "Temptation Bundling", "Only allow yourself to do something you love while doing a habit you need to build.", "Katy Milkman", listOf("bundling", "temptation")),
        Insight("at021", InsightCategory.ADVANCED_TECHNIQUES, "Habit Scorecards", "Rate each habit daily on a scale. Track trends over weeks and months.", "Scorecards", listOf("rating", "trends")),
        Insight("at022", InsightCategory.ADVANCED_TECHNIQUES, "Inversion Technique", "Ask: How could I guarantee failure? Then do the opposite.", "Inversion", listOf("inversion", "opposite")),
        Insight("at023", InsightCategory.ADVANCED_TECHNIQUES, "The 5-Second Rule", "Count 5-4-3-2-1 and act before your brain can talk you out of it.", "Mel Robbins", listOf("countdown", "action")),
        Insight("at024", InsightCategory.ADVANCED_TECHNIQUES, "Habit Bundling", "Combine a habit you need with one you want. Listen to podcasts only while exercising.", "Bundling", listOf("bundling", "combination")),
        Insight("at025", InsightCategory.ADVANCED_TECHNIQUES, "Progressive Overload", "Gradually increase difficulty as habits become easier. This maintains engagement.", "Progressive", listOf("progression", "challenge")),
        Insight("at026", InsightCategory.ADVANCED_TECHNIQUES, "The Seinfeld Calendar", "Mark an X for each day you complete a habit. Don't break the chain.", "Jerry Seinfeld", listOf("calendar", "chain")),
        Insight("at027", InsightCategory.ADVANCED_TECHNIQUES, "Habit Substitution", "Don't just eliminate bad habits—replace them with better alternatives.", "Substitution", listOf("substitution", "replacement")),
        Insight("at028", InsightCategory.ADVANCED_TECHNIQUES, "Context Changes", "Move to new environments to escape old habits. Your desk might trigger procrastination.", "Context", listOf("environment", "change")),
        Insight("at029", InsightCategory.ADVANCED_TECHNIQUES, "The 2-Minute Rule", "If a task takes less than 2 minutes, do it now. This prevents task accumulation.", "David Allen", listOf("quick", "immediate")),
        Insight("at030", InsightCategory.ADVANCED_TECHNIQUES, "Habit Auditing", "Regularly review all habits. Are they still serving you? Prune what doesn't work.", "Auditing", listOf("review", "pruning")),
        Insight("at031", InsightCategory.ADVANCED_TECHNIQUES, "Dopamine Menu", "Create a list of healthy dopamine sources to choose from when craving stimulation.", "Dopamine Menu", listOf("dopamine", "alternatives")),
        Insight("at032", InsightCategory.ADVANCED_TECHNIQUES, "The Never-Miss-Twice Rule", "Missing once is acceptable; missing twice starts a new pattern. Return immediately.", "Recovery Rule", listOf("missing", "return")),
        Insight("at033", InsightCategory.ADVANCED_TECHNIQUES, "Energy Auditing", "Track your energy throughout the day. Schedule demanding habits for peak hours.", "Energy Tracking", listOf("energy", "scheduling")),
        Insight("at034", InsightCategory.ADVANCED_TECHNIQUES, "Mindfulness Integration", "Bring full attention to habit execution. Quality attention improves habit quality.", "Mindfulness", listOf("attention", "quality")),
        Insight("at035", InsightCategory.ADVANCED_TECHNIQUES, "The Reverse Goal Pyramid", "Start with your lifetime goal, break it into yearly, monthly, weekly, and daily habits.", "Goal Pyramid", listOf("breakdown", "goals")),
        Insight("at036", InsightCategory.ADVANCED_TECHNIQUES, "Habit Sprints", "Intensely focus on one habit for 2-4 weeks before adding another. Depth before breadth.", "Sprints", listOf("focus", "depth")),
        Insight("at037", InsightCategory.ADVANCED_TECHNIQUES, "The Clarity Principle", "Vague goals fail; clear goals succeed. Define exactly what, when, where, and how.", "Clarity", listOf("specificity", "clarity")),
        Insight("at038", InsightCategory.ADVANCED_TECHNIQUES, "Reward Delay Training", "Deliberately delay gratification to strengthen willpower. It's a trainable skill.", "Delay Training", listOf("delay", "training")),
        Insight("at039", InsightCategory.ADVANCED_TECHNIQUES, "Habit Triggers Audit", "Identify every trigger for bad habits. Then systematically remove or avoid them.", "Trigger Audit", listOf("triggers", "removal")),
        Insight("at040", InsightCategory.ADVANCED_TECHNIQUES, "Mood Follows Action", "Don't wait to feel motivated. Act first; motivation follows behavior.", "Action First", listOf("action", "motivation")),
        Insight("at041", InsightCategory.ADVANCED_TECHNIQUES, "The Aggregation Principle", "Improve many small things by 1%. The sum of marginal gains is transformative.", "Aggregation", listOf("marginal", "many")),
        Insight("at042", InsightCategory.ADVANCED_TECHNIQUES, "Strategic Laziness", "Make the lazy choice the good choice. Laziness will work in your favor.", "Strategic Laziness", listOf("lazy", "design")),
        Insight("at043", InsightCategory.ADVANCED_TECHNIQUES, "Habit Cue Cards", "Create physical or digital cards that remind you of habit cues throughout the day.", "Cue Cards", listOf("cues", "reminders")),
        Insight("at044", InsightCategory.ADVANCED_TECHNIQUES, "The Forcing Function", "Create situations where you have no choice but to follow through on your habit.", "Forcing", listOf("forced", "situations")),
        Insight("at045", InsightCategory.ADVANCED_TECHNIQUES, "Dual-Track Habits", "Maintain a 'normal' and 'minimum viable' version of each habit for different days.", "Dual-Track", listOf("flexible", "versions")),
        Insight("at046", InsightCategory.ADVANCED_TECHNIQUES, "Habit Streaking", "Chase streaks as a game. The longer the streak, the more motivation to continue.", "Streaking", listOf("streaks", "gamification")),
        Insight("at047", InsightCategory.ADVANCED_TECHNIQUES, "Weekly Reviews", "Every week, review what worked and what didn't. Adjust your approach accordingly.", "Reviews", listOf("weekly", "adjustment")),
        Insight("at048", InsightCategory.ADVANCED_TECHNIQUES, "Habit Gradients", "Create versions of your habit at different difficulty levels. Scale up or down as needed.", "Gradients", listOf("scaling", "levels")),
        Insight("at049", InsightCategory.ADVANCED_TECHNIQUES, "The Power of Routine", "A consistent routine reduces decision fatigue. Your habits become automatic.", "Routine", listOf("routine", "automatic")),
        Insight("at050", InsightCategory.ADVANCED_TECHNIQUES, "Negative Habit Fasting", "Periodically abstain from bad habits completely. This breaks patterns and resets.", "Fasting", listOf("abstaining", "reset")),
        Insight("at051", InsightCategory.ADVANCED_TECHNIQUES, "Reflection Journaling", "Daily reflection on habits accelerates learning. What worked? What will you change?", "Journaling", listOf("reflection", "journaling")),
        Insight("at052", InsightCategory.ADVANCED_TECHNIQUES, "The Identity Statement", "Write an identity statement: 'I am a person who...' Read it daily.", "Identity", listOf("identity", "statement")),
        Insight("at053", InsightCategory.ADVANCED_TECHNIQUES, "Habit Iteration", "Treat habits as experiments. Version 1.0 is never final. Keep iterating and improving.", "Iteration", listOf("experiment", "improve"))
    )

    // ==================== PUBLIC API ====================

    val allInsights: List<Insight> by lazy {
        habitPsychology + neuroscienceFacts + behavioralTriggers +
                progressMindset + socialPsychology + recoveryCompassion + advancedTechniques
    }

    /**
     * Get insight for a specific day of year (1-365)
     * Returns a deterministic insight based on day number
     */
    fun getInsightForDay(dayOfYear: Int): Insight {
        val adjustedDay = ((dayOfYear - 1) % 365).coerceIn(0, 364)
        return allInsights[adjustedDay]
    }

    /**
     * Get a contextual insight based on user state
     */
    fun getContextualInsight(
        streak: Int,
        recentBreak: Boolean,
        achievement: String? = null
    ): Insight {
        val relevantInsights = when {
            recentBreak -> recoveryCompassion
            streak < 7 -> habitPsychology.filter { "beginner" in it.contextTags }
            streak in 7..30 -> progressMindset
            streak > 30 -> advancedTechniques
            achievement != null -> socialPsychology
            else -> allInsights
        }
        return relevantInsights.random()
    }

    /**
     * Get insights by category
     */
    fun getInsightsByCategory(category: InsightCategory): List<Insight> {
        return when (category) {
            InsightCategory.HABIT_PSYCHOLOGY -> habitPsychology
            InsightCategory.NEUROSCIENCE -> neuroscienceFacts
            InsightCategory.BEHAVIORAL_TRIGGERS -> behavioralTriggers
            InsightCategory.PROGRESS_MINDSET -> progressMindset
            InsightCategory.SOCIAL_PSYCHOLOGY -> socialPsychology
            InsightCategory.RECOVERY_COMPASSION -> recoveryCompassion
            InsightCategory.ADVANCED_TECHNIQUES -> advancedTechniques
        }
    }

    /**
     * Get total insight count (should be 365)
     */
    fun getTotalCount(): Int = allInsights.size

    /**
     * Get insights with a specific tag
     */
    fun getInsightsWithTag(tag: String): List<Insight> {
        return allInsights.filter { tag in it.contextTags }
    }

    /**
     * Search insights by keyword in title or content
     */
    fun searchInsights(query: String): List<Insight> {
        val lowerQuery = query.lowercase()
        return allInsights.filter {
            it.title.lowercase().contains(lowerQuery) ||
            it.content.lowercase().contains(lowerQuery)
        }
    }
}
