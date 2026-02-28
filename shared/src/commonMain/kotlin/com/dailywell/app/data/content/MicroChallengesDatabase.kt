package com.dailywell.app.data.content

/**
 * MicroChallengesDatabase - 365 Unique Daily Micro-Challenges
 *
 * PRODUCTION-READY: One unique challenge per day for an entire year.
 * Quick, actionable challenges across 4 categories.
 *
 * Categories (4 × ~91 challenges = 365):
 * 1. Physical Challenges (92)
 * 2. Mental Challenges (91)
 * 3. Social Challenges (91)
 * 4. Creative Challenges (91)
 */
object MicroChallengesDatabase {

    // ==================== DATA MODELS ====================

    data class MicroChallenge(
        val id: String,
        val category: ChallengeCategory,
        val title: String,
        val description: String,
        val duration: Int, // minutes
        val difficulty: ChallengeDifficulty,
        val indoorOnly: Boolean = false,
        val requiresEquipment: Boolean = false,
        val completionCriteria: String
    )

    enum class ChallengeCategory {
        PHYSICAL,
        MENTAL,
        SOCIAL,
        CREATIVE
    }

    enum class ChallengeDifficulty {
        EASY,
        MEDIUM,
        HARD
    }

    // ==================== PHYSICAL CHALLENGES (92) ====================

    private val physicalChallenges = listOf(
        MicroChallenge("ph001", ChallengeCategory.PHYSICAL, "Morning Stretch", "Do a 5-minute full body stretch immediately after waking up.", 5, ChallengeDifficulty.EASY, true, false, "Complete 5 minutes of stretching"),
        MicroChallenge("ph002", ChallengeCategory.PHYSICAL, "Stair Master", "Take the stairs instead of the elevator for all your trips today.", 1, ChallengeDifficulty.EASY, true, false, "Use stairs exclusively today"),
        MicroChallenge("ph003", ChallengeCategory.PHYSICAL, "Walk and Talk", "Take a phone call while walking instead of sitting.", 10, ChallengeDifficulty.EASY, false, false, "Complete at least one walking phone call"),
        MicroChallenge("ph004", ChallengeCategory.PHYSICAL, "Desk Warrior", "Do 20 desk push-ups or wall push-ups at work.", 2, ChallengeDifficulty.EASY, true, false, "Complete 20 push-ups"),
        MicroChallenge("ph005", ChallengeCategory.PHYSICAL, "Balance Challenge", "Stand on one foot for 30 seconds each leg, three times today.", 3, ChallengeDifficulty.EASY, true, false, "Complete 3 sets of single-leg balance"),
        MicroChallenge("ph006", ChallengeCategory.PHYSICAL, "Hydration Hero", "Drink 8 glasses of water today, setting a timer every hour.", 1, ChallengeDifficulty.EASY, true, false, "Drink 8 glasses of water"),
        MicroChallenge("ph007", ChallengeCategory.PHYSICAL, "Posture Check", "Set 5 alarms to check and correct your posture throughout the day.", 1, ChallengeDifficulty.EASY, true, false, "Complete 5 posture checks"),
        MicroChallenge("ph008", ChallengeCategory.PHYSICAL, "10K Steps", "Reach 10,000 steps before dinner time.", 60, ChallengeDifficulty.MEDIUM, false, false, "Log 10,000 steps"),
        MicroChallenge("ph009", ChallengeCategory.PHYSICAL, "Plank Progress", "Hold a plank for as long as you can, trying to beat 1 minute.", 2, ChallengeDifficulty.MEDIUM, true, false, "Hold plank for your personal best"),
        MicroChallenge("ph010", ChallengeCategory.PHYSICAL, "Jump Rope Joy", "Do 100 jump rope jumps (or imaginary jumps).", 5, ChallengeDifficulty.MEDIUM, true, false, "Complete 100 jumps"),
        MicroChallenge("ph011", ChallengeCategory.PHYSICAL, "Lunch Walk", "Take a 15-minute walk during your lunch break.", 15, ChallengeDifficulty.EASY, false, false, "Complete a 15-minute walk"),
        MicroChallenge("ph012", ChallengeCategory.PHYSICAL, "Evening Yoga", "Do a 10-minute beginner yoga flow before bed.", 10, ChallengeDifficulty.EASY, true, false, "Complete 10 minutes of yoga"),
        MicroChallenge("ph013", ChallengeCategory.PHYSICAL, "Speed Walk", "Walk at a brisk pace for 20 minutes, faster than your normal pace.", 20, ChallengeDifficulty.MEDIUM, false, false, "Complete 20-minute brisk walk"),
        MicroChallenge("ph014", ChallengeCategory.PHYSICAL, "Core Crusher", "Do 30 crunches and 30 bicycle kicks.", 5, ChallengeDifficulty.MEDIUM, true, false, "Complete all 60 reps"),
        MicroChallenge("ph015", ChallengeCategory.PHYSICAL, "Squat Challenge", "Do 50 bodyweight squats throughout the day.", 10, ChallengeDifficulty.MEDIUM, true, false, "Complete 50 squats"),
        MicroChallenge("ph016", ChallengeCategory.PHYSICAL, "Deep Breathing", "Practice 4-7-8 breathing for 5 minutes.", 5, ChallengeDifficulty.EASY, true, false, "Complete 5 minutes of breathing exercises"),
        MicroChallenge("ph017", ChallengeCategory.PHYSICAL, "Cold Shower Finish", "End your shower with 30 seconds of cold water.", 1, ChallengeDifficulty.HARD, true, false, "Complete 30 seconds of cold water"),
        MicroChallenge("ph018", ChallengeCategory.PHYSICAL, "Burpee Blast", "Do 20 burpees in the morning.", 5, ChallengeDifficulty.HARD, true, false, "Complete 20 burpees"),
        MicroChallenge("ph019", ChallengeCategory.PHYSICAL, "Dance Break", "Put on your favorite song and dance for its entire duration.", 4, ChallengeDifficulty.EASY, true, false, "Dance through one full song"),
        MicroChallenge("ph020", ChallengeCategory.PHYSICAL, "Park Far", "Park at the far end of every parking lot today.", 5, ChallengeDifficulty.EASY, false, false, "Park far from entrances all day"),
        MicroChallenge("ph021", ChallengeCategory.PHYSICAL, "Lunge Walk", "Do walking lunges across a room, 20 steps.", 3, ChallengeDifficulty.MEDIUM, true, false, "Complete 20 walking lunges"),
        MicroChallenge("ph022", ChallengeCategory.PHYSICAL, "Wrist Stretches", "Do wrist stretches every hour to prevent strain.", 1, ChallengeDifficulty.EASY, true, false, "Complete 8 wrist stretch sessions"),
        MicroChallenge("ph023", ChallengeCategory.PHYSICAL, "Stand More", "Stand for at least 50% of your work hours today.", 240, ChallengeDifficulty.MEDIUM, true, false, "Stand for half of work time"),
        MicroChallenge("ph024", ChallengeCategory.PHYSICAL, "Sunset Walk", "Take a walk during sunset today.", 20, ChallengeDifficulty.EASY, false, false, "Complete a sunset walk"),
        MicroChallenge("ph025", ChallengeCategory.PHYSICAL, "Morning Run", "Go for a 15-minute jog before breakfast.", 15, ChallengeDifficulty.MEDIUM, false, false, "Complete a morning jog"),
        MicroChallenge("ph026", ChallengeCategory.PHYSICAL, "Bike Commute", "Bike or walk instead of driving for one errand today.", 30, ChallengeDifficulty.MEDIUM, false, false, "Replace one car trip with walking/biking"),
        MicroChallenge("ph027", ChallengeCategory.PHYSICAL, "Calf Raises", "Do 50 calf raises while waiting (in line, for food, etc.).", 5, ChallengeDifficulty.EASY, true, false, "Complete 50 calf raises"),
        MicroChallenge("ph028", ChallengeCategory.PHYSICAL, "Bear Crawl", "Bear crawl across a room, 3 times.", 3, ChallengeDifficulty.MEDIUM, true, false, "Complete 3 bear crawl lengths"),
        MicroChallenge("ph029", ChallengeCategory.PHYSICAL, "Hip Openers", "Do 10 minutes of hip-opening stretches.", 10, ChallengeDifficulty.EASY, true, false, "Complete 10 minutes of hip stretches"),
        MicroChallenge("ph030", ChallengeCategory.PHYSICAL, "Shadow Boxing", "Shadow box for 5 minutes.", 5, ChallengeDifficulty.MEDIUM, true, false, "Complete 5 minutes of shadow boxing"),
        MicroChallenge("ph031", ChallengeCategory.PHYSICAL, "Wall Sit", "Hold a wall sit for as long as possible, trying to beat 60 seconds.", 2, ChallengeDifficulty.MEDIUM, true, false, "Hold wall sit for personal best"),
        MicroChallenge("ph032", ChallengeCategory.PHYSICAL, "Neck Rolls", "Do neck rolls and stretches every 2 hours.", 1, ChallengeDifficulty.EASY, true, false, "Complete 4+ neck stretch sessions"),
        MicroChallenge("ph033", ChallengeCategory.PHYSICAL, "Speed Clean", "Speed clean your room for 10 minutes as a workout.", 10, ChallengeDifficulty.EASY, true, false, "Complete 10-minute speed clean"),
        MicroChallenge("ph034", ChallengeCategory.PHYSICAL, "Jumping Jacks", "Do 100 jumping jacks spread throughout the day.", 10, ChallengeDifficulty.MEDIUM, true, false, "Complete 100 jumping jacks"),
        MicroChallenge("ph035", ChallengeCategory.PHYSICAL, "Mountain Climbers", "Do 60 mountain climbers.", 5, ChallengeDifficulty.HARD, true, false, "Complete 60 mountain climbers"),
        MicroChallenge("ph036", ChallengeCategory.PHYSICAL, "Leg Lifts", "Do 30 leg lifts before getting out of bed.", 3, ChallengeDifficulty.EASY, true, false, "Complete 30 leg lifts"),
        MicroChallenge("ph037", ChallengeCategory.PHYSICAL, "Tricep Dips", "Do 25 tricep dips using a chair or bench.", 3, ChallengeDifficulty.MEDIUM, true, false, "Complete 25 tricep dips"),
        MicroChallenge("ph038", ChallengeCategory.PHYSICAL, "Nature Walk", "Take a 30-minute walk in nature.", 30, ChallengeDifficulty.EASY, false, false, "Complete 30-minute nature walk"),
        MicroChallenge("ph039", ChallengeCategory.PHYSICAL, "High Knees", "Do high knees for 2 minutes.", 2, ChallengeDifficulty.MEDIUM, true, false, "Complete 2 minutes of high knees"),
        MicroChallenge("ph040", ChallengeCategory.PHYSICAL, "Shoulder Rolls", "Do shoulder rolls and stretches every hour.", 1, ChallengeDifficulty.EASY, true, false, "Complete 8 shoulder sessions"),
        MicroChallenge("ph041", ChallengeCategory.PHYSICAL, "Farmer's Walk", "Walk around your home carrying something heavy for 5 minutes.", 5, ChallengeDifficulty.MEDIUM, true, false, "Complete 5-minute farmer's walk"),
        MicroChallenge("ph042", ChallengeCategory.PHYSICAL, "Push-Up Challenge", "Do 40 push-ups throughout the day.", 10, ChallengeDifficulty.MEDIUM, true, false, "Complete 40 push-ups"),
        MicroChallenge("ph043", ChallengeCategory.PHYSICAL, "Toe Touches", "Do 30 standing toe touches.", 3, ChallengeDifficulty.EASY, true, false, "Complete 30 toe touches"),
        MicroChallenge("ph044", ChallengeCategory.PHYSICAL, "Side Plank", "Hold side plank for 30 seconds each side, 3 times.", 5, ChallengeDifficulty.MEDIUM, true, false, "Complete 3 sets of side planks"),
        MicroChallenge("ph045", ChallengeCategory.PHYSICAL, "Ankle Circles", "Do ankle circles for 2 minutes total.", 2, ChallengeDifficulty.EASY, true, false, "Complete ankle mobility work"),
        MicroChallenge("ph046", ChallengeCategory.PHYSICAL, "Glute Bridge", "Do 30 glute bridges.", 5, ChallengeDifficulty.EASY, true, false, "Complete 30 glute bridges"),
        MicroChallenge("ph047", ChallengeCategory.PHYSICAL, "Sprint Intervals", "Do 5 x 30-second sprints with 1-minute rest.", 10, ChallengeDifficulty.HARD, false, false, "Complete 5 sprint intervals"),
        MicroChallenge("ph048", ChallengeCategory.PHYSICAL, "Arm Circles", "Do arm circles for 3 minutes total.", 3, ChallengeDifficulty.EASY, true, false, "Complete arm circle routine"),
        MicroChallenge("ph049", ChallengeCategory.PHYSICAL, "Dead Hang", "Hang from a bar for as long as possible.", 2, ChallengeDifficulty.MEDIUM, true, true, "Hold dead hang for personal best"),
        MicroChallenge("ph050", ChallengeCategory.PHYSICAL, "Chair Yoga", "Do 10 minutes of chair yoga at your desk.", 10, ChallengeDifficulty.EASY, true, false, "Complete 10 minutes of chair yoga"),
        MicroChallenge("ph051", ChallengeCategory.PHYSICAL, "Walking Meditation", "Take a 15-minute mindful walk, focusing on each step.", 15, ChallengeDifficulty.EASY, false, false, "Complete 15-minute mindful walk"),
        MicroChallenge("ph052", ChallengeCategory.PHYSICAL, "Spin Around", "Do 10 spins without getting dizzy (improves balance).", 2, ChallengeDifficulty.EASY, true, false, "Complete 10 spins"),
        MicroChallenge("ph053", ChallengeCategory.PHYSICAL, "Backward Walk", "Walk backward for 50 steps (safely).", 3, ChallengeDifficulty.EASY, true, false, "Complete 50 backward steps"),
        MicroChallenge("ph054", ChallengeCategory.PHYSICAL, "Sumo Squats", "Do 25 sumo squats.", 3, ChallengeDifficulty.EASY, true, false, "Complete 25 sumo squats"),
        MicroChallenge("ph055", ChallengeCategory.PHYSICAL, "Foot Massage", "Give yourself a 5-minute foot massage with a ball.", 5, ChallengeDifficulty.EASY, true, false, "Complete foot massage"),
        MicroChallenge("ph056", ChallengeCategory.PHYSICAL, "Hand Strengthening", "Squeeze a stress ball or make fists 100 times.", 5, ChallengeDifficulty.EASY, true, false, "Complete 100 squeezes"),
        MicroChallenge("ph057", ChallengeCategory.PHYSICAL, "Tai Chi Basics", "Follow a 10-minute beginner Tai Chi video.", 10, ChallengeDifficulty.EASY, true, false, "Complete Tai Chi session"),
        MicroChallenge("ph058", ChallengeCategory.PHYSICAL, "Foam Rolling", "Spend 10 minutes foam rolling tight muscles.", 10, ChallengeDifficulty.EASY, true, true, "Complete foam rolling session"),
        MicroChallenge("ph059", ChallengeCategory.PHYSICAL, "Swimming Simulation", "Do swimming arm motions for 5 minutes.", 5, ChallengeDifficulty.EASY, true, false, "Complete swimming motion exercise"),
        MicroChallenge("ph060", ChallengeCategory.PHYSICAL, "Morning Sun", "Get 10 minutes of morning sunlight on your face.", 10, ChallengeDifficulty.EASY, false, false, "Get 10 minutes of morning sun"),
        MicroChallenge("ph061", ChallengeCategory.PHYSICAL, "Donkey Kicks", "Do 30 donkey kicks per leg.", 5, ChallengeDifficulty.EASY, true, false, "Complete 60 donkey kicks total"),
        MicroChallenge("ph062", ChallengeCategory.PHYSICAL, "Superman Hold", "Do 5 x 20-second superman holds.", 3, ChallengeDifficulty.MEDIUM, true, false, "Complete 5 superman holds"),
        MicroChallenge("ph063", ChallengeCategory.PHYSICAL, "Fire Hydrants", "Do 20 fire hydrants per leg.", 5, ChallengeDifficulty.EASY, true, false, "Complete 40 fire hydrants total"),
        MicroChallenge("ph064", ChallengeCategory.PHYSICAL, "Reverse Lunges", "Do 20 reverse lunges per leg.", 5, ChallengeDifficulty.MEDIUM, true, false, "Complete 40 reverse lunges"),
        MicroChallenge("ph065", ChallengeCategory.PHYSICAL, "Box Jumps", "Do 15 box jumps or step-ups on stairs.", 5, ChallengeDifficulty.MEDIUM, true, false, "Complete 15 box jumps"),
        MicroChallenge("ph066", ChallengeCategory.PHYSICAL, "Eye Exercises", "Do eye exercises for 5 minutes (focus near/far).", 5, ChallengeDifficulty.EASY, true, false, "Complete eye exercise routine"),
        MicroChallenge("ph067", ChallengeCategory.PHYSICAL, "Kick Practice", "Practice 50 front kicks (martial arts style).", 5, ChallengeDifficulty.MEDIUM, true, false, "Complete 50 front kicks"),
        MicroChallenge("ph068", ChallengeCategory.PHYSICAL, "Child's Pose", "Hold child's pose for 5 minutes total throughout the day.", 5, ChallengeDifficulty.EASY, true, false, "Complete 5 minutes of child's pose"),
        MicroChallenge("ph069", ChallengeCategory.PHYSICAL, "Commando Plank", "Do 20 commando planks (plank to push-up position).", 5, ChallengeDifficulty.HARD, true, false, "Complete 20 commando planks"),
        MicroChallenge("ph070", ChallengeCategory.PHYSICAL, "Thoracic Rotation", "Do thoracic spine rotations, 10 per side.", 3, ChallengeDifficulty.EASY, true, false, "Complete 20 thoracic rotations"),
        MicroChallenge("ph071", ChallengeCategory.PHYSICAL, "Handstand Practice", "Practice handstand against a wall for 3 minutes.", 3, ChallengeDifficulty.HARD, true, false, "Practice handstand for 3 minutes"),
        MicroChallenge("ph072", ChallengeCategory.PHYSICAL, "Clean Eating", "Eat only whole, unprocessed foods today.", 1, ChallengeDifficulty.MEDIUM, true, false, "Eat only whole foods today"),
        MicroChallenge("ph073", ChallengeCategory.PHYSICAL, "No Caffeine", "Go the entire day without caffeine.", 1, ChallengeDifficulty.HARD, true, false, "Complete a caffeine-free day"),
        MicroChallenge("ph074", ChallengeCategory.PHYSICAL, "7-Minute Workout", "Complete a full 7-minute workout circuit.", 7, ChallengeDifficulty.MEDIUM, true, false, "Complete 7-minute workout"),
        MicroChallenge("ph075", ChallengeCategory.PHYSICAL, "Sleep by 10 PM", "Be in bed with lights out by 10 PM tonight.", 1, ChallengeDifficulty.MEDIUM, true, false, "Be asleep by 10 PM"),
        MicroChallenge("ph076", ChallengeCategory.PHYSICAL, "No Sitting Hour", "Don't sit down for one continuous hour.", 60, ChallengeDifficulty.MEDIUM, true, false, "Stay standing for one hour"),
        MicroChallenge("ph077", ChallengeCategory.PHYSICAL, "Resistance Band", "Do a 15-minute resistance band workout.", 15, ChallengeDifficulty.MEDIUM, true, true, "Complete resistance band workout"),
        MicroChallenge("ph078", ChallengeCategory.PHYSICAL, "Balance Board", "Stand on a balance board or pillow for 5 minutes.", 5, ChallengeDifficulty.MEDIUM, true, true, "Complete balance challenge"),
        MicroChallenge("ph079", ChallengeCategory.PHYSICAL, "Ab Wheel", "Do 15 ab wheel rollouts.", 5, ChallengeDifficulty.HARD, true, true, "Complete 15 ab wheel rollouts"),
        MicroChallenge("ph080", ChallengeCategory.PHYSICAL, "Heel Drops", "Do 30 heel drops for ankle strength.", 3, ChallengeDifficulty.EASY, true, false, "Complete 30 heel drops"),
        MicroChallenge("ph081", ChallengeCategory.PHYSICAL, "Band Pull-Aparts", "Do 50 band pull-aparts for posture.", 5, ChallengeDifficulty.EASY, true, true, "Complete 50 band pull-aparts"),
        MicroChallenge("ph082", ChallengeCategory.PHYSICAL, "Kettlebell Swings", "Do 50 kettlebell swings.", 10, ChallengeDifficulty.MEDIUM, true, true, "Complete 50 kettlebell swings"),
        MicroChallenge("ph083", ChallengeCategory.PHYSICAL, "Face Pulls", "Do 30 face pulls with a band.", 5, ChallengeDifficulty.EASY, true, true, "Complete 30 face pulls"),
        MicroChallenge("ph084", ChallengeCategory.PHYSICAL, "Dragon Flag Attempt", "Attempt dragon flags, even partial ones.", 5, ChallengeDifficulty.HARD, true, false, "Attempt dragon flags"),
        MicroChallenge("ph085", ChallengeCategory.PHYSICAL, "Turkish Get-Up", "Do 5 Turkish get-ups per side.", 15, ChallengeDifficulty.HARD, true, true, "Complete 10 Turkish get-ups"),
        MicroChallenge("ph086", ChallengeCategory.PHYSICAL, "Pigeon Pose", "Hold pigeon pose for 2 minutes per side.", 5, ChallengeDifficulty.MEDIUM, true, false, "Complete pigeon pose both sides"),
        MicroChallenge("ph087", ChallengeCategory.PHYSICAL, "Cat-Cow Stretch", "Do 20 cat-cow stretches.", 3, ChallengeDifficulty.EASY, true, false, "Complete 20 cat-cow stretches"),
        MicroChallenge("ph088", ChallengeCategory.PHYSICAL, "Dead Bug", "Do 20 dead bugs.", 3, ChallengeDifficulty.EASY, true, false, "Complete 20 dead bugs"),
        MicroChallenge("ph089", ChallengeCategory.PHYSICAL, "Bird Dog", "Do 15 bird dogs per side.", 5, ChallengeDifficulty.EASY, true, false, "Complete 30 bird dogs total"),
        MicroChallenge("ph090", ChallengeCategory.PHYSICAL, "Inchworm Walk", "Do 10 inchworm walks.", 5, ChallengeDifficulty.MEDIUM, true, false, "Complete 10 inchworm walks"),
        MicroChallenge("ph091", ChallengeCategory.PHYSICAL, "V-Ups", "Do 20 V-ups.", 5, ChallengeDifficulty.HARD, true, false, "Complete 20 V-ups"),
        MicroChallenge("ph092", ChallengeCategory.PHYSICAL, "Hollow Body Hold", "Hold hollow body position for 60 seconds total.", 3, ChallengeDifficulty.MEDIUM, true, false, "Complete 60 seconds of hollow body")
    )

    // ==================== MENTAL CHALLENGES (91) ====================

    private val mentalChallenges = listOf(
        MicroChallenge("mn001", ChallengeCategory.MENTAL, "Morning Meditation", "Meditate for 10 minutes first thing in the morning.", 10, ChallengeDifficulty.EASY, true, false, "Complete 10-minute meditation"),
        MicroChallenge("mn002", ChallengeCategory.MENTAL, "Gratitude List", "Write down 10 things you're grateful for.", 5, ChallengeDifficulty.EASY, true, false, "Write 10 gratitude items"),
        MicroChallenge("mn003", ChallengeCategory.MENTAL, "No Complaints", "Go the entire day without complaining.", 1, ChallengeDifficulty.HARD, true, false, "Complete complaint-free day"),
        MicroChallenge("mn004", ChallengeCategory.MENTAL, "Learn Something New", "Learn one new fact or skill today.", 15, ChallengeDifficulty.EASY, true, false, "Learn something new"),
        MicroChallenge("mn005", ChallengeCategory.MENTAL, "Phone-Free Hour", "Spend one hour completely phone-free.", 60, ChallengeDifficulty.MEDIUM, true, false, "Complete phone-free hour"),
        MicroChallenge("mn006", ChallengeCategory.MENTAL, "Read 20 Pages", "Read 20 pages of a book.", 30, ChallengeDifficulty.EASY, true, false, "Read 20 pages"),
        MicroChallenge("mn007", ChallengeCategory.MENTAL, "Mindful Eating", "Eat one meal in complete silence, focusing only on the food.", 20, ChallengeDifficulty.MEDIUM, true, false, "Complete mindful meal"),
        MicroChallenge("mn008", ChallengeCategory.MENTAL, "Positive Affirmations", "Say 10 positive affirmations aloud to yourself.", 5, ChallengeDifficulty.EASY, true, false, "Complete 10 affirmations"),
        MicroChallenge("mn009", ChallengeCategory.MENTAL, "Journal Entry", "Write a one-page journal entry about your day.", 15, ChallengeDifficulty.EASY, true, false, "Write one journal page"),
        MicroChallenge("mn010", ChallengeCategory.MENTAL, "News Fast", "Don't consume any news today.", 1, ChallengeDifficulty.MEDIUM, true, false, "Complete news-free day"),
        MicroChallenge("mn011", ChallengeCategory.MENTAL, "Visualization", "Spend 5 minutes visualizing your ideal future.", 5, ChallengeDifficulty.EASY, true, false, "Complete visualization session"),
        MicroChallenge("mn012", ChallengeCategory.MENTAL, "Cold Exposure Mindset", "Take a cold shower and focus on breathing through discomfort.", 5, ChallengeDifficulty.HARD, true, false, "Complete cold shower mindfully"),
        MicroChallenge("mn013", ChallengeCategory.MENTAL, "Single-Tasking", "Do one task at a time for the entire day—no multitasking.", 1, ChallengeDifficulty.HARD, true, false, "Complete single-tasking day"),
        MicroChallenge("mn014", ChallengeCategory.MENTAL, "Memory Exercise", "Memorize a poem or quote.", 15, ChallengeDifficulty.MEDIUM, true, false, "Memorize something by heart"),
        MicroChallenge("mn015", ChallengeCategory.MENTAL, "Breathing Exercise", "Do box breathing for 10 minutes.", 10, ChallengeDifficulty.EASY, true, false, "Complete box breathing session"),
        MicroChallenge("mn016", ChallengeCategory.MENTAL, "Declutter Mind", "Write a brain dump of everything on your mind.", 10, ChallengeDifficulty.EASY, true, false, "Complete brain dump"),
        MicroChallenge("mn017", ChallengeCategory.MENTAL, "Podcast Learning", "Listen to an educational podcast episode.", 30, ChallengeDifficulty.EASY, true, false, "Listen to one podcast episode"),
        MicroChallenge("mn018", ChallengeCategory.MENTAL, "No Social Media", "Stay off all social media for the entire day.", 1, ChallengeDifficulty.HARD, true, false, "Complete social media-free day"),
        MicroChallenge("mn019", ChallengeCategory.MENTAL, "Problem Solving", "Write out a problem and brainstorm 10 solutions.", 15, ChallengeDifficulty.MEDIUM, true, false, "Generate 10 solutions"),
        MicroChallenge("mn020", ChallengeCategory.MENTAL, "Future Letter", "Write a letter to yourself 5 years from now.", 20, ChallengeDifficulty.MEDIUM, true, false, "Write future letter"),
        MicroChallenge("mn021", ChallengeCategory.MENTAL, "Learn a Word", "Learn a new vocabulary word and use it 3 times.", 10, ChallengeDifficulty.EASY, true, false, "Learn and use new word"),
        MicroChallenge("mn022", ChallengeCategory.MENTAL, "Perspective Shift", "Think about a problem from 3 different perspectives.", 10, ChallengeDifficulty.MEDIUM, true, false, "Consider 3 perspectives"),
        MicroChallenge("mn023", ChallengeCategory.MENTAL, "Slow Down", "Do everything 25% slower than usual.", 1, ChallengeDifficulty.MEDIUM, true, false, "Complete slow-paced day"),
        MicroChallenge("mn024", ChallengeCategory.MENTAL, "Name Your Emotions", "Notice and name your emotions 10 times today.", 1, ChallengeDifficulty.EASY, true, false, "Name emotions 10 times"),
        MicroChallenge("mn025", ChallengeCategory.MENTAL, "Beginner's Mind", "Approach something familiar as if it's brand new.", 15, ChallengeDifficulty.MEDIUM, true, false, "Practice beginner's mind"),
        MicroChallenge("mn026", ChallengeCategory.MENTAL, "Silence Practice", "Spend 30 minutes in complete silence.", 30, ChallengeDifficulty.MEDIUM, true, false, "Complete 30 minutes of silence"),
        MicroChallenge("mn027", ChallengeCategory.MENTAL, "Limit Decisions", "Make only essential decisions today—defer the rest.", 1, ChallengeDifficulty.MEDIUM, true, false, "Minimize decision-making"),
        MicroChallenge("mn028", ChallengeCategory.MENTAL, "Body Scan", "Do a 10-minute body scan meditation.", 10, ChallengeDifficulty.EASY, true, false, "Complete body scan"),
        MicroChallenge("mn029", ChallengeCategory.MENTAL, "Curiosity Questions", "Ask 'why?' or 'how?' about 5 things you normally ignore.", 15, ChallengeDifficulty.EASY, true, false, "Ask 5 curious questions"),
        MicroChallenge("mn030", ChallengeCategory.MENTAL, "First Principles", "Break down a belief to its fundamental truths.", 20, ChallengeDifficulty.HARD, true, false, "Complete first principles analysis"),
        MicroChallenge("mn031", ChallengeCategory.MENTAL, "Sensory Focus", "Spend 5 minutes focusing on each sense, one at a time.", 25, ChallengeDifficulty.EASY, true, false, "Complete sensory focus"),
        MicroChallenge("mn032", ChallengeCategory.MENTAL, "Opposite Action", "When you want to avoid something, do it immediately instead.", 1, ChallengeDifficulty.HARD, true, false, "Practice opposite action"),
        MicroChallenge("mn033", ChallengeCategory.MENTAL, "Evening Review", "Review your day for 10 minutes before bed.", 10, ChallengeDifficulty.EASY, true, false, "Complete evening review"),
        MicroChallenge("mn034", ChallengeCategory.MENTAL, "Mental Math", "Do 20 mental math problems throughout the day.", 10, ChallengeDifficulty.EASY, true, false, "Complete 20 mental math problems"),
        MicroChallenge("mn035", ChallengeCategory.MENTAL, "Worry Time", "Schedule 15 minutes to worry, then stop when time is up.", 15, ChallengeDifficulty.MEDIUM, true, false, "Complete scheduled worry time"),
        MicroChallenge("mn036", ChallengeCategory.MENTAL, "Present Moment", "Set 5 alarms to pause and be present for 1 minute.", 5, ChallengeDifficulty.EASY, true, false, "Complete 5 presence moments"),
        MicroChallenge("mn037", ChallengeCategory.MENTAL, "TED Talk", "Watch a TED Talk and take notes.", 20, ChallengeDifficulty.EASY, true, false, "Watch and note TED Talk"),
        MicroChallenge("mn038", ChallengeCategory.MENTAL, "Challenge Assumption", "Identify and question one assumption you hold.", 15, ChallengeDifficulty.MEDIUM, true, false, "Question one assumption"),
        MicroChallenge("mn039", ChallengeCategory.MENTAL, "Forgiveness Practice", "Mentally forgive someone who wronged you.", 10, ChallengeDifficulty.HARD, true, false, "Practice forgiveness"),
        MicroChallenge("mn040", ChallengeCategory.MENTAL, "Success Log", "Write down 5 successes from the past week.", 10, ChallengeDifficulty.EASY, true, false, "Log 5 successes"),
        MicroChallenge("mn041", ChallengeCategory.MENTAL, "Fear Facing", "Identify one fear and take one small step toward it.", 15, ChallengeDifficulty.HARD, true, false, "Face one fear"),
        MicroChallenge("mn042", ChallengeCategory.MENTAL, "Values Clarification", "Write out your top 5 life values.", 15, ChallengeDifficulty.MEDIUM, true, false, "Clarify top 5 values"),
        MicroChallenge("mn043", ChallengeCategory.MENTAL, "Attention Training", "Focus on one object for 5 minutes without distraction.", 5, ChallengeDifficulty.MEDIUM, true, false, "Complete 5-minute focus"),
        MicroChallenge("mn044", ChallengeCategory.MENTAL, "Language Learning", "Learn 10 words in a new language.", 20, ChallengeDifficulty.MEDIUM, true, false, "Learn 10 foreign words"),
        MicroChallenge("mn045", ChallengeCategory.MENTAL, "Chess Problem", "Solve 3 chess puzzles.", 15, ChallengeDifficulty.MEDIUM, true, false, "Solve 3 chess puzzles"),
        MicroChallenge("mn046", ChallengeCategory.MENTAL, "Crossword Challenge", "Complete a crossword puzzle.", 20, ChallengeDifficulty.MEDIUM, true, false, "Complete crossword"),
        MicroChallenge("mn047", ChallengeCategory.MENTAL, "Sudoku Session", "Complete a sudoku puzzle.", 15, ChallengeDifficulty.EASY, true, false, "Complete sudoku"),
        MicroChallenge("mn048", ChallengeCategory.MENTAL, "Goal Review", "Review and refine your current goals.", 15, ChallengeDifficulty.EASY, true, false, "Review goals"),
        MicroChallenge("mn049", ChallengeCategory.MENTAL, "Regret Processing", "Write about a regret and what you learned from it.", 15, ChallengeDifficulty.HARD, true, false, "Process one regret"),
        MicroChallenge("mn050", ChallengeCategory.MENTAL, "Strengths Focus", "Identify and use one strength 3 times today.", 1, ChallengeDifficulty.EASY, true, false, "Use strength 3 times"),
        MicroChallenge("mn051", ChallengeCategory.MENTAL, "Procrastination Analysis", "Write why you procrastinate on one specific task.", 10, ChallengeDifficulty.MEDIUM, true, false, "Analyze procrastination"),
        MicroChallenge("mn052", ChallengeCategory.MENTAL, "Evening Planning", "Plan tomorrow in detail before going to sleep.", 10, ChallengeDifficulty.EASY, true, false, "Plan tomorrow tonight"),
        MicroChallenge("mn053", ChallengeCategory.MENTAL, "Reframe Challenge", "Reframe a negative thought into a positive one 5 times.", 1, ChallengeDifficulty.MEDIUM, true, false, "Reframe 5 thoughts"),
        MicroChallenge("mn054", ChallengeCategory.MENTAL, "Documentary Watch", "Watch an educational documentary.", 60, ChallengeDifficulty.EASY, true, false, "Watch documentary"),
        MicroChallenge("mn055", ChallengeCategory.MENTAL, "Role Model Study", "Research someone you admire for 15 minutes.", 15, ChallengeDifficulty.EASY, true, false, "Study a role model"),
        MicroChallenge("mn056", ChallengeCategory.MENTAL, "Decision Journal", "Write about a decision you made and why.", 10, ChallengeDifficulty.EASY, true, false, "Journal about a decision"),
        MicroChallenge("mn057", ChallengeCategory.MENTAL, "Skill Practice", "Practice a mental skill for 20 minutes.", 20, ChallengeDifficulty.MEDIUM, true, false, "Practice mental skill"),
        MicroChallenge("mn058", ChallengeCategory.MENTAL, "Logic Puzzle", "Solve a logic puzzle.", 15, ChallengeDifficulty.MEDIUM, true, false, "Solve logic puzzle"),
        MicroChallenge("mn059", ChallengeCategory.MENTAL, "Reading Challenge", "Read for one uninterrupted hour.", 60, ChallengeDifficulty.MEDIUM, true, false, "Read for one hour"),
        MicroChallenge("mn060", ChallengeCategory.MENTAL, "Observation Walk", "Take a walk and notice 20 new details.", 20, ChallengeDifficulty.EASY, false, false, "Notice 20 new things"),
        MicroChallenge("mn061", ChallengeCategory.MENTAL, "Childhood Memory", "Write about a childhood memory in detail.", 15, ChallengeDifficulty.EASY, true, false, "Describe childhood memory"),
        MicroChallenge("mn062", ChallengeCategory.MENTAL, "Mindful Tea", "Make and drink tea with complete attention.", 15, ChallengeDifficulty.EASY, true, false, "Complete mindful tea practice"),
        MicroChallenge("mn063", ChallengeCategory.MENTAL, "Self-Compassion Letter", "Write yourself a compassionate letter.", 15, ChallengeDifficulty.MEDIUM, true, false, "Write self-compassion letter"),
        MicroChallenge("mn064", ChallengeCategory.MENTAL, "Limiting Belief", "Identify and challenge one limiting belief.", 15, ChallengeDifficulty.HARD, true, false, "Challenge limiting belief"),
        MicroChallenge("mn065", ChallengeCategory.MENTAL, "Pattern Recognition", "Notice a repeating pattern in your behavior.", 1, ChallengeDifficulty.MEDIUM, true, false, "Identify one pattern"),
        MicroChallenge("mn066", ChallengeCategory.MENTAL, "Death Meditation", "Contemplate mortality for 10 minutes (Stoic practice).", 10, ChallengeDifficulty.HARD, true, false, "Complete memento mori meditation"),
        MicroChallenge("mn067", ChallengeCategory.MENTAL, "Negative Visualization", "Imagine losing something you value, then appreciate it.", 10, ChallengeDifficulty.MEDIUM, true, false, "Practice negative visualization"),
        MicroChallenge("mn068", ChallengeCategory.MENTAL, "Teaching Moment", "Teach something you know to someone else.", 15, ChallengeDifficulty.MEDIUM, false, false, "Teach someone something"),
        MicroChallenge("mn069", ChallengeCategory.MENTAL, "Art Appreciation", "Study a piece of art for 10 minutes.", 10, ChallengeDifficulty.EASY, true, false, "Study artwork deeply"),
        MicroChallenge("mn070", ChallengeCategory.MENTAL, "Podcast Notes", "Take notes while listening to a podcast.", 30, ChallengeDifficulty.EASY, true, false, "Take podcast notes"),
        MicroChallenge("mn071", ChallengeCategory.MENTAL, "Trivia Learning", "Learn 10 random trivia facts.", 15, ChallengeDifficulty.EASY, true, false, "Learn 10 trivia facts"),
        MicroChallenge("mn072", ChallengeCategory.MENTAL, "Mind Map", "Create a mind map on a topic of interest.", 20, ChallengeDifficulty.MEDIUM, true, false, "Create mind map"),
        MicroChallenge("mn073", ChallengeCategory.MENTAL, "Bias Identification", "Identify a cognitive bias affecting your thinking.", 15, ChallengeDifficulty.HARD, true, false, "Identify cognitive bias"),
        MicroChallenge("mn074", ChallengeCategory.MENTAL, "Creative Writing", "Write a short story in 15 minutes.", 15, ChallengeDifficulty.MEDIUM, true, false, "Write short story"),
        MicroChallenge("mn075", ChallengeCategory.MENTAL, "Empathy Exercise", "Imagine the full day from someone else's perspective.", 15, ChallengeDifficulty.MEDIUM, true, false, "Practice empathy exercise"),
        MicroChallenge("mn076", ChallengeCategory.MENTAL, "Music Focus", "Listen to a piece of music with full attention.", 10, ChallengeDifficulty.EASY, true, false, "Listen mindfully to music"),
        MicroChallenge("mn077", ChallengeCategory.MENTAL, "Idea Generation", "Generate 20 ideas on any topic.", 15, ChallengeDifficulty.MEDIUM, true, false, "Generate 20 ideas"),
        MicroChallenge("mn078", ChallengeCategory.MENTAL, "Paradox Pondering", "Think deeply about a paradox for 10 minutes.", 10, ChallengeDifficulty.HARD, true, false, "Contemplate a paradox"),
        MicroChallenge("mn079", ChallengeCategory.MENTAL, "Speed Reading", "Practice speed reading for 15 minutes.", 15, ChallengeDifficulty.MEDIUM, true, false, "Practice speed reading"),
        MicroChallenge("mn080", ChallengeCategory.MENTAL, "Prioritization", "Rank your to-dos using the Eisenhower matrix.", 15, ChallengeDifficulty.EASY, true, false, "Complete prioritization matrix"),
        MicroChallenge("mn081", ChallengeCategory.MENTAL, "Definition Study", "Learn the etymology of 5 words you use.", 15, ChallengeDifficulty.EASY, true, false, "Study 5 word origins"),
        MicroChallenge("mn082", ChallengeCategory.MENTAL, "Quote Reflection", "Find a quote and write about what it means to you.", 15, ChallengeDifficulty.EASY, true, false, "Reflect on a quote"),
        MicroChallenge("mn083", ChallengeCategory.MENTAL, "Mental Rehearsal", "Mentally rehearse a challenging upcoming situation.", 10, ChallengeDifficulty.MEDIUM, true, false, "Complete mental rehearsal"),
        MicroChallenge("mn084", ChallengeCategory.MENTAL, "Habit Analysis", "Analyze why one habit is easier than another.", 15, ChallengeDifficulty.MEDIUM, true, false, "Analyze habit difficulty"),
        MicroChallenge("mn085", ChallengeCategory.MENTAL, "Deep Work", "Do 90 minutes of deep, focused work.", 90, ChallengeDifficulty.HARD, true, false, "Complete 90 minutes deep work"),
        MicroChallenge("mn086", ChallengeCategory.MENTAL, "Simplification", "Simplify one area of your life.", 20, ChallengeDifficulty.MEDIUM, true, false, "Simplify one area"),
        MicroChallenge("mn087", ChallengeCategory.MENTAL, "Belief Inventory", "List 10 beliefs you hold and examine each.", 20, ChallengeDifficulty.HARD, true, false, "Examine 10 beliefs"),
        MicroChallenge("mn088", ChallengeCategory.MENTAL, "Morning Pages", "Write 3 pages stream-of-consciousness in the morning.", 20, ChallengeDifficulty.MEDIUM, true, false, "Write 3 morning pages"),
        MicroChallenge("mn089", ChallengeCategory.MENTAL, "Philosophical Question", "Ponder a philosophical question for 15 minutes.", 15, ChallengeDifficulty.HARD, true, false, "Ponder philosophical question"),
        MicroChallenge("mn090", ChallengeCategory.MENTAL, "Lesson Review", "Review something you learned recently to cement it.", 15, ChallengeDifficulty.EASY, true, false, "Review recent learning"),
        MicroChallenge("mn091", ChallengeCategory.MENTAL, "Intention Setting", "Set 3 clear intentions for tomorrow.", 10, ChallengeDifficulty.EASY, true, false, "Set 3 intentions")
    )

    // ==================== SOCIAL CHALLENGES (91) ====================

    private val socialChallenges = listOf(
        MicroChallenge("sc001", ChallengeCategory.SOCIAL, "Give a Compliment", "Give a genuine compliment to 3 different people today.", 5, ChallengeDifficulty.EASY, false, false, "Give 3 compliments"),
        MicroChallenge("sc002", ChallengeCategory.SOCIAL, "Call a Friend", "Call a friend you haven't spoken to in over a month.", 15, ChallengeDifficulty.EASY, true, false, "Call old friend"),
        MicroChallenge("sc003", ChallengeCategory.SOCIAL, "Thank You Note", "Write and send a thank you note to someone.", 15, ChallengeDifficulty.EASY, true, false, "Send thank you note"),
        MicroChallenge("sc004", ChallengeCategory.SOCIAL, "Eye Contact", "Make eye contact and smile at 5 strangers today.", 1, ChallengeDifficulty.MEDIUM, false, false, "Eye contact with 5 people"),
        MicroChallenge("sc005", ChallengeCategory.SOCIAL, "Help Someone", "Offer to help someone with a task today.", 15, ChallengeDifficulty.EASY, false, false, "Help one person"),
        MicroChallenge("sc006", ChallengeCategory.SOCIAL, "Deep Conversation", "Have a conversation longer than 10 minutes without checking your phone.", 15, ChallengeDifficulty.MEDIUM, false, false, "Have phone-free conversation"),
        MicroChallenge("sc007", ChallengeCategory.SOCIAL, "Ask Questions", "In conversations today, ask more questions than you give opinions.", 1, ChallengeDifficulty.MEDIUM, false, false, "Be more curious than opinionated"),
        MicroChallenge("sc008", ChallengeCategory.SOCIAL, "Reconnect", "Send a thinking-of-you message to someone you've lost touch with.", 5, ChallengeDifficulty.EASY, true, false, "Send reconnection message"),
        MicroChallenge("sc009", ChallengeCategory.SOCIAL, "Active Listening", "Practice active listening: repeat back what you hear in 3 conversations.", 1, ChallengeDifficulty.MEDIUM, false, false, "Practice active listening"),
        MicroChallenge("sc010", ChallengeCategory.SOCIAL, "Stranger Conversation", "Start a conversation with a stranger.", 5, ChallengeDifficulty.HARD, false, false, "Talk to a stranger"),
        MicroChallenge("sc011", ChallengeCategory.SOCIAL, "Family Call", "Call a family member you don't talk to often.", 15, ChallengeDifficulty.EASY, true, false, "Call family member"),
        MicroChallenge("sc012", ChallengeCategory.SOCIAL, "Appreciation Text", "Send 5 appreciation texts to different people.", 10, ChallengeDifficulty.EASY, true, false, "Send 5 appreciation texts"),
        MicroChallenge("sc013", ChallengeCategory.SOCIAL, "Invite Someone", "Invite someone to do an activity with you.", 5, ChallengeDifficulty.MEDIUM, false, false, "Invite someone to activity"),
        MicroChallenge("sc014", ChallengeCategory.SOCIAL, "Apologize", "Apologize for something you've been putting off.", 10, ChallengeDifficulty.HARD, false, false, "Make overdue apology"),
        MicroChallenge("sc015", ChallengeCategory.SOCIAL, "Public Praise", "Publicly praise someone for their work.", 5, ChallengeDifficulty.MEDIUM, false, false, "Give public praise"),
        MicroChallenge("sc016", ChallengeCategory.SOCIAL, "Share Vulnerability", "Share something vulnerable with someone you trust.", 15, ChallengeDifficulty.HARD, false, false, "Share something vulnerable"),
        MicroChallenge("sc017", ChallengeCategory.SOCIAL, "Random Act of Kindness", "Do a random act of kindness for a stranger.", 10, ChallengeDifficulty.EASY, false, false, "Perform random kindness"),
        MicroChallenge("sc018", ChallengeCategory.SOCIAL, "Mentor Someone", "Offer advice or guidance to someone who needs it.", 20, ChallengeDifficulty.MEDIUM, false, false, "Give mentorship"),
        MicroChallenge("sc019", ChallengeCategory.SOCIAL, "Ask for Help", "Ask someone for help with something.", 10, ChallengeDifficulty.MEDIUM, false, false, "Ask for help"),
        MicroChallenge("sc020", ChallengeCategory.SOCIAL, "Give Feedback", "Give constructive feedback to someone.", 10, ChallengeDifficulty.HARD, false, false, "Provide constructive feedback"),
        MicroChallenge("sc021", ChallengeCategory.SOCIAL, "Listen Without Advice", "Listen to someone without offering any advice.", 15, ChallengeDifficulty.HARD, false, false, "Just listen"),
        MicroChallenge("sc022", ChallengeCategory.SOCIAL, "Introduce Two People", "Introduce two people who might benefit from knowing each other.", 10, ChallengeDifficulty.MEDIUM, false, false, "Make an introduction"),
        MicroChallenge("sc023", ChallengeCategory.SOCIAL, "Video Call", "Have a video call instead of a text conversation.", 20, ChallengeDifficulty.EASY, true, false, "Replace text with video call"),
        MicroChallenge("sc024", ChallengeCategory.SOCIAL, "Join a Group", "Join an online or in-person group with shared interests.", 30, ChallengeDifficulty.MEDIUM, false, false, "Join a new group"),
        MicroChallenge("sc025", ChallengeCategory.SOCIAL, "Express Gratitude", "Tell someone exactly why you appreciate them.", 10, ChallengeDifficulty.EASY, false, false, "Express specific gratitude"),
        MicroChallenge("sc026", ChallengeCategory.SOCIAL, "Be Fully Present", "Put away all devices during a social interaction.", 30, ChallengeDifficulty.MEDIUM, false, false, "Be device-free in conversation"),
        MicroChallenge("sc027", ChallengeCategory.SOCIAL, "Smile More", "Smile at everyone you interact with today.", 1, ChallengeDifficulty.EASY, false, false, "Smile at everyone"),
        MicroChallenge("sc028", ChallengeCategory.SOCIAL, "Say No", "Say no to something you don't want to do.", 5, ChallengeDifficulty.HARD, false, false, "Practice saying no"),
        MicroChallenge("sc029", ChallengeCategory.SOCIAL, "Forgive Someone", "Choose to forgive someone, even if only internally.", 10, ChallengeDifficulty.HARD, true, false, "Practice forgiveness"),
        MicroChallenge("sc030", ChallengeCategory.SOCIAL, "Gift Giving", "Give a small gift to someone unexpectedly.", 15, ChallengeDifficulty.EASY, false, false, "Give unexpected gift"),
        MicroChallenge("sc031", ChallengeCategory.SOCIAL, "Check on Someone", "Check in on someone who might be struggling.", 10, ChallengeDifficulty.MEDIUM, false, false, "Check on someone"),
        MicroChallenge("sc032", ChallengeCategory.SOCIAL, "Handwritten Letter", "Write a handwritten letter to someone.", 20, ChallengeDifficulty.MEDIUM, true, false, "Write handwritten letter"),
        MicroChallenge("sc033", ChallengeCategory.SOCIAL, "Learn About Someone", "Ask someone about their life story or background.", 20, ChallengeDifficulty.MEDIUM, false, false, "Learn someone's story"),
        MicroChallenge("sc034", ChallengeCategory.SOCIAL, "Support Local", "Support a local business and have a conversation with the owner.", 20, ChallengeDifficulty.EASY, false, false, "Support and chat with local business"),
        MicroChallenge("sc035", ChallengeCategory.SOCIAL, "Volunteer Time", "Volunteer your time for a cause.", 60, ChallengeDifficulty.MEDIUM, false, false, "Volunteer for one hour"),
        MicroChallenge("sc036", ChallengeCategory.SOCIAL, "No Gossip", "Go the entire day without gossiping.", 1, ChallengeDifficulty.HARD, false, false, "Complete gossip-free day"),
        MicroChallenge("sc037", ChallengeCategory.SOCIAL, "Parent/Guardian Call", "Have a meaningful conversation with a parent or guardian.", 20, ChallengeDifficulty.MEDIUM, false, false, "Have meaningful parental conversation"),
        MicroChallenge("sc038", ChallengeCategory.SOCIAL, "Team Appreciation", "Thank each member of your team individually.", 15, ChallengeDifficulty.EASY, false, false, "Thank team members"),
        MicroChallenge("sc039", ChallengeCategory.SOCIAL, "New Neighbor", "Introduce yourself to a neighbor you don't know.", 10, ChallengeDifficulty.MEDIUM, false, false, "Meet a neighbor"),
        MicroChallenge("sc040", ChallengeCategory.SOCIAL, "Memory Sharing", "Share a favorite memory with someone who was there.", 15, ChallengeDifficulty.EASY, false, false, "Share a memory"),
        MicroChallenge("sc041", ChallengeCategory.SOCIAL, "Host a Gathering", "Organize a small gathering of friends.", 120, ChallengeDifficulty.HARD, false, false, "Host a gathering"),
        MicroChallenge("sc042", ChallengeCategory.SOCIAL, "Give Directions", "Offer to help a stranger who looks lost.", 5, ChallengeDifficulty.EASY, false, false, "Help someone lost"),
        MicroChallenge("sc043", ChallengeCategory.SOCIAL, "Sincere Interest", "Ask someone about their hobby and listen with genuine interest.", 15, ChallengeDifficulty.EASY, false, false, "Show interest in hobby"),
        MicroChallenge("sc044", ChallengeCategory.SOCIAL, "Name Remembering", "Learn and remember 3 new people's names today.", 1, ChallengeDifficulty.MEDIUM, false, false, "Remember 3 new names"),
        MicroChallenge("sc045", ChallengeCategory.SOCIAL, "Celebrate Someone", "Celebrate someone's achievement publicly.", 10, ChallengeDifficulty.EASY, false, false, "Celebrate someone publicly"),
        MicroChallenge("sc046", ChallengeCategory.SOCIAL, "Boundary Setting", "Set a clear boundary with someone.", 10, ChallengeDifficulty.HARD, false, false, "Set a boundary"),
        MicroChallenge("sc047", ChallengeCategory.SOCIAL, "Pay It Forward", "Pay for someone else's coffee or meal.", 5, ChallengeDifficulty.EASY, false, false, "Pay for someone's order"),
        MicroChallenge("sc048", ChallengeCategory.SOCIAL, "Old Photo Sharing", "Share an old photo with someone and reminisce.", 15, ChallengeDifficulty.EASY, true, false, "Share old photo"),
        MicroChallenge("sc049", ChallengeCategory.SOCIAL, "Attend an Event", "Attend a community or networking event.", 120, ChallengeDifficulty.MEDIUM, false, false, "Attend community event"),
        MicroChallenge("sc050", ChallengeCategory.SOCIAL, "Leave a Review", "Leave a positive review for a business you like.", 10, ChallengeDifficulty.EASY, true, false, "Leave positive review"),
        MicroChallenge("sc051", ChallengeCategory.SOCIAL, "Conflict Resolution", "Address a conflict you've been avoiding.", 30, ChallengeDifficulty.HARD, false, false, "Resolve a conflict"),
        MicroChallenge("sc052", ChallengeCategory.SOCIAL, "Learn a Name", "Ask someone's name that you should already know.", 5, ChallengeDifficulty.HARD, false, false, "Ask forgotten name"),
        MicroChallenge("sc053", ChallengeCategory.SOCIAL, "Positive Response", "Respond positively to everything for one hour.", 60, ChallengeDifficulty.MEDIUM, false, false, "One hour of positivity"),
        MicroChallenge("sc054", ChallengeCategory.SOCIAL, "Undivided Attention", "Give someone your complete, undivided attention.", 20, ChallengeDifficulty.MEDIUM, false, false, "Give full attention"),
        MicroChallenge("sc055", ChallengeCategory.SOCIAL, "Encouraging Message", "Send an encouraging message to someone facing a challenge.", 5, ChallengeDifficulty.EASY, true, false, "Send encouragement"),
        MicroChallenge("sc056", ChallengeCategory.SOCIAL, "Tip Generously", "Tip more generously than usual.", 1, ChallengeDifficulty.EASY, false, false, "Tip extra"),
        MicroChallenge("sc057", ChallengeCategory.SOCIAL, "Share Your Goals", "Tell someone about your goals and ask about theirs.", 15, ChallengeDifficulty.MEDIUM, false, false, "Share goals"),
        MicroChallenge("sc058", ChallengeCategory.SOCIAL, "Thank Service Workers", "Thank 5 service workers by name today.", 1, ChallengeDifficulty.EASY, false, false, "Thank 5 service workers"),
        MicroChallenge("sc059", ChallengeCategory.SOCIAL, "Offer Encouragement", "Offer words of encouragement to someone.", 5, ChallengeDifficulty.EASY, false, false, "Encourage someone"),
        MicroChallenge("sc060", ChallengeCategory.SOCIAL, "Positive Assumption", "Assume positive intent in all interactions today.", 1, ChallengeDifficulty.MEDIUM, false, false, "Assume positive intent"),
        MicroChallenge("sc061", ChallengeCategory.SOCIAL, "Ask for Opinion", "Ask someone for their genuine opinion on something.", 10, ChallengeDifficulty.EASY, false, false, "Request opinion"),
        MicroChallenge("sc062", ChallengeCategory.SOCIAL, "Reconnect Old Friend", "Reach out to someone from your past.", 10, ChallengeDifficulty.EASY, true, false, "Contact old friend"),
        MicroChallenge("sc063", ChallengeCategory.SOCIAL, "Group Participation", "Actively participate in a group discussion.", 30, ChallengeDifficulty.MEDIUM, false, false, "Participate in group"),
        MicroChallenge("sc064", ChallengeCategory.SOCIAL, "Acknowledge Efforts", "Acknowledge someone's effort, not just their results.", 5, ChallengeDifficulty.EASY, false, false, "Acknowledge effort"),
        MicroChallenge("sc065", ChallengeCategory.SOCIAL, "Open Up", "Share something personal with someone.", 15, ChallengeDifficulty.MEDIUM, false, false, "Share personally"),
        MicroChallenge("sc066", ChallengeCategory.SOCIAL, "Quality Time", "Spend quality time with someone without any agenda.", 60, ChallengeDifficulty.MEDIUM, false, false, "Spend quality time"),
        MicroChallenge("sc067", ChallengeCategory.SOCIAL, "Express Love", "Tell someone you love them.", 1, ChallengeDifficulty.EASY, false, false, "Say 'I love you'"),
        MicroChallenge("sc068", ChallengeCategory.SOCIAL, "Support a Dream", "Ask someone about their dreams and offer support.", 15, ChallengeDifficulty.MEDIUM, false, false, "Support someone's dream"),
        MicroChallenge("sc069", ChallengeCategory.SOCIAL, "Admit Mistake", "Admit a mistake to someone.", 10, ChallengeDifficulty.HARD, false, false, "Admit a mistake"),
        MicroChallenge("sc070", ChallengeCategory.SOCIAL, "Share Knowledge", "Share something you've learned with someone.", 15, ChallengeDifficulty.EASY, false, false, "Share knowledge"),
        MicroChallenge("sc071", ChallengeCategory.SOCIAL, "Remember Details", "Remember and mention a detail from a past conversation.", 5, ChallengeDifficulty.MEDIUM, false, false, "Recall past detail"),
        MicroChallenge("sc072", ChallengeCategory.SOCIAL, "Validate Feelings", "Validate someone's feelings without trying to fix them.", 10, ChallengeDifficulty.MEDIUM, false, false, "Validate emotions"),
        MicroChallenge("sc073", ChallengeCategory.SOCIAL, "Collaborative Activity", "Do a collaborative activity with someone.", 60, ChallengeDifficulty.EASY, false, false, "Collaborate on activity"),
        MicroChallenge("sc074", ChallengeCategory.SOCIAL, "Express Pride", "Tell someone you're proud of them.", 5, ChallengeDifficulty.EASY, false, false, "Express pride"),
        MicroChallenge("sc075", ChallengeCategory.SOCIAL, "Silent Support", "Be present for someone without speaking.", 30, ChallengeDifficulty.MEDIUM, false, false, "Offer silent presence"),
        MicroChallenge("sc076", ChallengeCategory.SOCIAL, "Plan Something", "Make concrete plans with someone instead of saying 'let's hang out soon.'", 10, ChallengeDifficulty.EASY, false, false, "Make concrete plans"),
        MicroChallenge("sc077", ChallengeCategory.SOCIAL, "Teach Something", "Teach someone a skill you have.", 30, ChallengeDifficulty.MEDIUM, false, false, "Teach a skill"),
        MicroChallenge("sc078", ChallengeCategory.SOCIAL, "Accept Compliment", "Accept a compliment graciously without deflecting.", 1, ChallengeDifficulty.MEDIUM, false, false, "Accept compliment gracefully"),
        MicroChallenge("sc079", ChallengeCategory.SOCIAL, "Offer First", "Be the first to offer help in a situation.", 10, ChallengeDifficulty.EASY, false, false, "Offer help first"),
        MicroChallenge("sc080", ChallengeCategory.SOCIAL, "Deeper Question", "Ask a question deeper than 'how are you?'", 10, ChallengeDifficulty.MEDIUM, false, false, "Ask deeper question"),
        MicroChallenge("sc081", ChallengeCategory.SOCIAL, "Patience Practice", "Be extra patient in all interactions today.", 1, ChallengeDifficulty.MEDIUM, false, false, "Practice patience"),
        MicroChallenge("sc082", ChallengeCategory.SOCIAL, "Celebrate Quietly", "Quietly celebrate someone else's success.", 5, ChallengeDifficulty.EASY, false, false, "Celebrate others quietly"),
        MicroChallenge("sc083", ChallengeCategory.SOCIAL, "Phone-Free Meal", "Have a meal with someone with no phones at the table.", 45, ChallengeDifficulty.MEDIUM, false, false, "Phone-free meal together"),
        MicroChallenge("sc084", ChallengeCategory.SOCIAL, "Authentic Response", "Give only authentic responses today—no polite lies.", 1, ChallengeDifficulty.HARD, false, false, "Respond authentically"),
        MicroChallenge("sc085", ChallengeCategory.SOCIAL, "Remember Birthday", "Reach out to someone whose birthday is coming up.", 5, ChallengeDifficulty.EASY, true, false, "Acknowledge upcoming birthday"),
        MicroChallenge("sc086", ChallengeCategory.SOCIAL, "Include Someone", "Include someone who seems left out.", 15, ChallengeDifficulty.MEDIUM, false, false, "Include the excluded"),
        MicroChallenge("sc087", ChallengeCategory.SOCIAL, "Morning Greeting", "Greet everyone you see in the morning.", 1, ChallengeDifficulty.EASY, false, false, "Greet everyone"),
        MicroChallenge("sc088", ChallengeCategory.SOCIAL, "Listen First", "In every conversation, let the other person speak first.", 1, ChallengeDifficulty.MEDIUM, false, false, "Let others speak first"),
        MicroChallenge("sc089", ChallengeCategory.SOCIAL, "Follow Up", "Follow up on something someone told you previously.", 10, ChallengeDifficulty.MEDIUM, false, false, "Follow up on past topic"),
        MicroChallenge("sc090", ChallengeCategory.SOCIAL, "Express Agreement", "Vocally agree with something good someone says.", 1, ChallengeDifficulty.EASY, false, false, "Express agreement"),
        MicroChallenge("sc091", ChallengeCategory.SOCIAL, "Warmth and Kindness", "Approach every interaction today with warmth.", 1, ChallengeDifficulty.MEDIUM, false, false, "Radiate warmth")
    )

    // ==================== CREATIVE CHALLENGES (91) ====================

    private val creativeChallenges = listOf(
        MicroChallenge("cr001", ChallengeCategory.CREATIVE, "Morning Pages", "Write 3 pages of stream-of-consciousness writing.", 20, ChallengeDifficulty.MEDIUM, true, false, "Write 3 pages"),
        MicroChallenge("cr002", ChallengeCategory.CREATIVE, "Doodle Break", "Spend 10 minutes doodling whatever comes to mind.", 10, ChallengeDifficulty.EASY, true, false, "Complete 10-minute doodle"),
        MicroChallenge("cr003", ChallengeCategory.CREATIVE, "Photo Challenge", "Take 10 interesting photos on your phone.", 20, ChallengeDifficulty.EASY, false, false, "Take 10 photos"),
        MicroChallenge("cr004", ChallengeCategory.CREATIVE, "Poem Writing", "Write a short poem.", 15, ChallengeDifficulty.MEDIUM, true, false, "Write a poem"),
        MicroChallenge("cr005", ChallengeCategory.CREATIVE, "Recipe Creation", "Create or modify a recipe and cook it.", 60, ChallengeDifficulty.MEDIUM, true, false, "Create and cook recipe"),
        MicroChallenge("cr006", ChallengeCategory.CREATIVE, "Song Creation", "Make up a silly song about your day.", 10, ChallengeDifficulty.EASY, true, false, "Create a silly song"),
        MicroChallenge("cr007", ChallengeCategory.CREATIVE, "Rearrange Space", "Rearrange furniture or decor in a room.", 30, ChallengeDifficulty.MEDIUM, true, false, "Rearrange a space"),
        MicroChallenge("cr008", ChallengeCategory.CREATIVE, "Learn an Instrument", "Practice an instrument for 15 minutes.", 15, ChallengeDifficulty.MEDIUM, true, true, "Practice instrument"),
        MicroChallenge("cr009", ChallengeCategory.CREATIVE, "Origami", "Make an origami figure.", 15, ChallengeDifficulty.MEDIUM, true, false, "Complete origami"),
        MicroChallenge("cr010", ChallengeCategory.CREATIVE, "Story Writing", "Write a 500-word short story.", 30, ChallengeDifficulty.MEDIUM, true, false, "Write 500-word story"),
        MicroChallenge("cr011", ChallengeCategory.CREATIVE, "Drawing Practice", "Draw an object in front of you.", 15, ChallengeDifficulty.EASY, true, false, "Draw from observation"),
        MicroChallenge("cr012", ChallengeCategory.CREATIVE, "Color Outside Lines", "Do an activity in a completely unconventional way.", 20, ChallengeDifficulty.MEDIUM, true, false, "Do something unconventionally"),
        MicroChallenge("cr013", ChallengeCategory.CREATIVE, "Playlist Creation", "Create a themed playlist.", 20, ChallengeDifficulty.EASY, true, false, "Create themed playlist"),
        MicroChallenge("cr014", ChallengeCategory.CREATIVE, "Collage Making", "Make a collage from magazines or digital images.", 30, ChallengeDifficulty.EASY, true, false, "Create a collage"),
        MicroChallenge("cr015", ChallengeCategory.CREATIVE, "Invent Something", "Design a solution to a small everyday problem.", 20, ChallengeDifficulty.HARD, true, false, "Design an invention"),
        MicroChallenge("cr016", ChallengeCategory.CREATIVE, "Dance Choreography", "Create a simple dance routine.", 20, ChallengeDifficulty.MEDIUM, true, false, "Choreograph a dance"),
        MicroChallenge("cr017", ChallengeCategory.CREATIVE, "Letter Design", "Design a creative version of your initials.", 15, ChallengeDifficulty.EASY, true, false, "Design initials"),
        MicroChallenge("cr018", ChallengeCategory.CREATIVE, "Vision Board", "Create a mini vision board.", 30, ChallengeDifficulty.EASY, true, false, "Create vision board"),
        MicroChallenge("cr019", ChallengeCategory.CREATIVE, "Improv Game", "Play an improv game with yourself or others.", 15, ChallengeDifficulty.MEDIUM, true, false, "Play improv game"),
        MicroChallenge("cr020", ChallengeCategory.CREATIVE, "Nature Art", "Create art using only natural materials.", 30, ChallengeDifficulty.MEDIUM, false, false, "Create nature art"),
        MicroChallenge("cr021", ChallengeCategory.CREATIVE, "Haiku Writing", "Write 5 haikus.", 15, ChallengeDifficulty.EASY, true, false, "Write 5 haikus"),
        MicroChallenge("cr022", ChallengeCategory.CREATIVE, "Sketch People", "Sketch 3 people you see (quickly).", 15, ChallengeDifficulty.MEDIUM, false, false, "Sketch 3 people"),
        MicroChallenge("cr023", ChallengeCategory.CREATIVE, "Word Association", "Do 10 minutes of free word association.", 10, ChallengeDifficulty.EASY, true, false, "10 minutes word association"),
        MicroChallenge("cr024", ChallengeCategory.CREATIVE, "Create a Meme", "Create an original meme.", 15, ChallengeDifficulty.EASY, true, false, "Create original meme"),
        MicroChallenge("cr025", ChallengeCategory.CREATIVE, "Photography Walk", "Take a photography walk with a theme.", 30, ChallengeDifficulty.EASY, false, false, "Themed photo walk"),
        MicroChallenge("cr026", ChallengeCategory.CREATIVE, "DIY Craft", "Make something from materials around your home.", 45, ChallengeDifficulty.MEDIUM, true, false, "Complete DIY craft"),
        MicroChallenge("cr027", ChallengeCategory.CREATIVE, "Sing Karaoke", "Sing a song with full commitment.", 5, ChallengeDifficulty.EASY, true, false, "Sing a full song"),
        MicroChallenge("cr028", ChallengeCategory.CREATIVE, "Creative Problem", "Solve a problem using the SCAMPER technique.", 20, ChallengeDifficulty.HARD, true, false, "Use SCAMPER technique"),
        MicroChallenge("cr029", ChallengeCategory.CREATIVE, "Body Percussion", "Create a rhythm using only body sounds.", 10, ChallengeDifficulty.EASY, true, false, "Create body percussion"),
        MicroChallenge("cr030", ChallengeCategory.CREATIVE, "Backwards Day", "Do 3 routine things in reverse order.", 15, ChallengeDifficulty.EASY, true, false, "Do 3 things backwards"),
        MicroChallenge("cr031", ChallengeCategory.CREATIVE, "Color Palette", "Create a color palette from a photo you take.", 15, ChallengeDifficulty.EASY, true, false, "Create color palette"),
        MicroChallenge("cr032", ChallengeCategory.CREATIVE, "6-Word Story", "Write a complete story in exactly 6 words.", 10, ChallengeDifficulty.HARD, true, false, "Write 6-word story"),
        MicroChallenge("cr033", ChallengeCategory.CREATIVE, "Texture Collection", "Find and photograph 10 interesting textures.", 20, ChallengeDifficulty.EASY, false, false, "Photograph 10 textures"),
        MicroChallenge("cr034", ChallengeCategory.CREATIVE, "Voice Recording", "Record yourself telling a story.", 15, ChallengeDifficulty.MEDIUM, true, false, "Record story telling"),
        MicroChallenge("cr035", ChallengeCategory.CREATIVE, "Pattern Design", "Design a repeating pattern.", 20, ChallengeDifficulty.MEDIUM, true, false, "Design repeating pattern"),
        MicroChallenge("cr036", ChallengeCategory.CREATIVE, "Soundtrack Create", "Create a 'soundtrack' for a moment in your day.", 15, ChallengeDifficulty.MEDIUM, true, false, "Create moment soundtrack"),
        MicroChallenge("cr037", ChallengeCategory.CREATIVE, "Emoji Story", "Tell a story using only emojis.", 10, ChallengeDifficulty.EASY, true, false, "Tell emoji story"),
        MicroChallenge("cr038", ChallengeCategory.CREATIVE, "Non-Dominant Hand", "Create art using your non-dominant hand.", 15, ChallengeDifficulty.MEDIUM, true, false, "Draw with other hand"),
        MicroChallenge("cr039", ChallengeCategory.CREATIVE, "Food Art", "Create art with your food.", 15, ChallengeDifficulty.EASY, true, false, "Make food art"),
        MicroChallenge("cr040", ChallengeCategory.CREATIVE, "Cloud Shapes", "Look at clouds and draw what you see.", 15, ChallengeDifficulty.EASY, false, false, "Draw cloud shapes"),
        MicroChallenge("cr041", ChallengeCategory.CREATIVE, "Blackout Poetry", "Create blackout poetry from a newspaper or book page.", 20, ChallengeDifficulty.MEDIUM, true, false, "Create blackout poetry"),
        MicroChallenge("cr042", ChallengeCategory.CREATIVE, "Letter to Future", "Write a letter to yourself in 1 year.", 20, ChallengeDifficulty.EASY, true, false, "Write future letter"),
        MicroChallenge("cr043", ChallengeCategory.CREATIVE, "Rhyme Time", "Write 10 rhyming couplets.", 15, ChallengeDifficulty.MEDIUM, true, false, "Write 10 couplets"),
        MicroChallenge("cr044", ChallengeCategory.CREATIVE, "Costume Creation", "Create a costume or outfit from unusual items.", 30, ChallengeDifficulty.MEDIUM, true, false, "Create unusual outfit"),
        MicroChallenge("cr045", ChallengeCategory.CREATIVE, "Instrument Make", "Make a simple musical instrument.", 30, ChallengeDifficulty.MEDIUM, true, false, "Make simple instrument"),
        MicroChallenge("cr046", ChallengeCategory.CREATIVE, "Shadow Art", "Create art using shadows.", 20, ChallengeDifficulty.MEDIUM, true, false, "Create shadow art"),
        MicroChallenge("cr047", ChallengeCategory.CREATIVE, "Journal Spread", "Create a decorative journal spread.", 30, ChallengeDifficulty.EASY, true, false, "Create journal spread"),
        MicroChallenge("cr048", ChallengeCategory.CREATIVE, "Character Create", "Create a fictional character with a backstory.", 20, ChallengeDifficulty.MEDIUM, true, false, "Create character"),
        MicroChallenge("cr049", ChallengeCategory.CREATIVE, "Logo Design", "Design a logo for yourself.", 30, ChallengeDifficulty.MEDIUM, true, false, "Design personal logo"),
        MicroChallenge("cr050", ChallengeCategory.CREATIVE, "Mood Board", "Create a mood board for your ideal day.", 20, ChallengeDifficulty.EASY, true, false, "Create mood board"),
        MicroChallenge("cr051", ChallengeCategory.CREATIVE, "Story Cubes", "Roll dice and create a story from the numbers.", 15, ChallengeDifficulty.MEDIUM, true, false, "Create dice story"),
        MicroChallenge("cr052", ChallengeCategory.CREATIVE, "Paper Airplane", "Design the best paper airplane you can.", 15, ChallengeDifficulty.EASY, true, false, "Design paper airplane"),
        MicroChallenge("cr053", ChallengeCategory.CREATIVE, "Portrait Draw", "Draw a self-portrait.", 20, ChallengeDifficulty.MEDIUM, true, false, "Draw self-portrait"),
        MicroChallenge("cr054", ChallengeCategory.CREATIVE, "New Route", "Take a completely new route somewhere.", 20, ChallengeDifficulty.EASY, false, false, "Take new route"),
        MicroChallenge("cr055", ChallengeCategory.CREATIVE, "Word Scramble", "Create an anagram of your name that means something.", 10, ChallengeDifficulty.MEDIUM, true, false, "Create name anagram"),
        MicroChallenge("cr056", ChallengeCategory.CREATIVE, "Sound Collage", "Record 10 sounds and make a collage.", 30, ChallengeDifficulty.MEDIUM, true, false, "Create sound collage"),
        MicroChallenge("cr057", ChallengeCategory.CREATIVE, "Time Capsule", "Create a small time capsule.", 30, ChallengeDifficulty.EASY, true, false, "Create time capsule"),
        MicroChallenge("cr058", ChallengeCategory.CREATIVE, "Comic Strip", "Draw a 4-panel comic strip.", 20, ChallengeDifficulty.MEDIUM, true, false, "Draw comic strip"),
        MicroChallenge("cr059", ChallengeCategory.CREATIVE, "Dance Alone", "Have a 10-minute solo dance party.", 10, ChallengeDifficulty.EASY, true, false, "Dance for 10 minutes"),
        MicroChallenge("cr060", ChallengeCategory.CREATIVE, "Gratitude Art", "Create art expressing gratitude.", 20, ChallengeDifficulty.EASY, true, false, "Create gratitude art"),
        MicroChallenge("cr061", ChallengeCategory.CREATIVE, "Random Combination", "Combine two unrelated objects into a new invention.", 15, ChallengeDifficulty.HARD, true, false, "Create invention combo"),
        MicroChallenge("cr062", ChallengeCategory.CREATIVE, "Photo Story", "Tell a story through 5 photos.", 30, ChallengeDifficulty.MEDIUM, false, false, "Create 5-photo story"),
        MicroChallenge("cr063", ChallengeCategory.CREATIVE, "Zentangle", "Create a Zentangle design.", 20, ChallengeDifficulty.EASY, true, false, "Create Zentangle"),
        MicroChallenge("cr064", ChallengeCategory.CREATIVE, "Sculpture Simple", "Create a simple sculpture from household items.", 30, ChallengeDifficulty.MEDIUM, true, false, "Create simple sculpture"),
        MicroChallenge("cr065", ChallengeCategory.CREATIVE, "Memory Drawing", "Draw something from memory.", 15, ChallengeDifficulty.MEDIUM, true, false, "Draw from memory"),
        MicroChallenge("cr066", ChallengeCategory.CREATIVE, "Limerick Writing", "Write 3 limericks.", 15, ChallengeDifficulty.MEDIUM, true, false, "Write 3 limericks"),
        MicroChallenge("cr067", ChallengeCategory.CREATIVE, "Create Alias", "Create a pen name and persona.", 15, ChallengeDifficulty.EASY, true, false, "Create pen name"),
        MicroChallenge("cr068", ChallengeCategory.CREATIVE, "One Color Day", "Wear or use only one color today.", 1, ChallengeDifficulty.EASY, true, false, "Monochrome day"),
        MicroChallenge("cr069", ChallengeCategory.CREATIVE, "Redesign Logo", "Redesign a famous logo.", 20, ChallengeDifficulty.MEDIUM, true, false, "Redesign famous logo"),
        MicroChallenge("cr070", ChallengeCategory.CREATIVE, "Flavor Combination", "Try a new flavor combination.", 15, ChallengeDifficulty.EASY, true, false, "Try new flavor combo"),
        MicroChallenge("cr071", ChallengeCategory.CREATIVE, "Micro Fiction", "Write a story in exactly 100 words.", 15, ChallengeDifficulty.MEDIUM, true, false, "Write 100-word story"),
        MicroChallenge("cr072", ChallengeCategory.CREATIVE, "Paint Emotions", "Paint what you're feeling (abstract is fine).", 30, ChallengeDifficulty.MEDIUM, true, false, "Paint emotions"),
        MicroChallenge("cr073", ChallengeCategory.CREATIVE, "Typeface Design", "Design a simple alphabet.", 45, ChallengeDifficulty.HARD, true, false, "Design alphabet"),
        MicroChallenge("cr074", ChallengeCategory.CREATIVE, "Dream Journal", "Write down last night's dream in detail.", 15, ChallengeDifficulty.EASY, true, false, "Journal dream"),
        MicroChallenge("cr075", ChallengeCategory.CREATIVE, "Remix Something", "Take something and remake it your way.", 30, ChallengeDifficulty.MEDIUM, true, false, "Create a remix"),
        MicroChallenge("cr076", ChallengeCategory.CREATIVE, "Color Mixing", "Mix new colors from paint or digital tools.", 20, ChallengeDifficulty.EASY, true, false, "Mix new colors"),
        MicroChallenge("cr077", ChallengeCategory.CREATIVE, "Object Story", "Write a story from an object's perspective.", 20, ChallengeDifficulty.MEDIUM, true, false, "Write object perspective"),
        MicroChallenge("cr078", ChallengeCategory.CREATIVE, "Build Something", "Build something with blocks, Legos, or similar.", 30, ChallengeDifficulty.EASY, true, true, "Build with blocks"),
        MicroChallenge("cr079", ChallengeCategory.CREATIVE, "Negative Space", "Create art focusing on negative space.", 20, ChallengeDifficulty.MEDIUM, true, false, "Create negative space art"),
        MicroChallenge("cr080", ChallengeCategory.CREATIVE, "Interview Yourself", "Write an interview with yourself.", 20, ChallengeDifficulty.MEDIUM, true, false, "Write self-interview"),
        MicroChallenge("cr081", ChallengeCategory.CREATIVE, "Gesture Drawings", "Do 10 quick gesture drawings.", 15, ChallengeDifficulty.MEDIUM, true, false, "Complete gesture drawings"),
        MicroChallenge("cr082", ChallengeCategory.CREATIVE, "Constraint Art", "Create with an artificial constraint.", 30, ChallengeDifficulty.HARD, true, false, "Create with constraint"),
        MicroChallenge("cr083", ChallengeCategory.CREATIVE, "Acrostic Poem", "Write an acrostic poem with your name.", 15, ChallengeDifficulty.EASY, true, false, "Write acrostic poem"),
        MicroChallenge("cr084", ChallengeCategory.CREATIVE, "Soundtrack Life", "Choose 5 songs that represent your life.", 20, ChallengeDifficulty.EASY, true, false, "Create life soundtrack"),
        MicroChallenge("cr085", ChallengeCategory.CREATIVE, "Reverse Drawing", "Draw starting from details instead of outline.", 20, ChallengeDifficulty.MEDIUM, true, false, "Draw in reverse"),
        MicroChallenge("cr086", ChallengeCategory.CREATIVE, "Create a Game", "Invent a simple game.", 30, ChallengeDifficulty.HARD, true, false, "Invent a game"),
        MicroChallenge("cr087", ChallengeCategory.CREATIVE, "Found Poetry", "Create a poem from found text.", 20, ChallengeDifficulty.MEDIUM, true, false, "Create found poetry"),
        MicroChallenge("cr088", ChallengeCategory.CREATIVE, "Mashup Create", "Combine two songs, ideas, or images.", 20, ChallengeDifficulty.MEDIUM, true, false, "Create mashup"),
        MicroChallenge("cr089", ChallengeCategory.CREATIVE, "Photo Edit", "Creatively edit a photo.", 20, ChallengeDifficulty.EASY, true, false, "Edit photo creatively"),
        MicroChallenge("cr090", ChallengeCategory.CREATIVE, "Monologue Write", "Write a dramatic monologue.", 20, ChallengeDifficulty.MEDIUM, true, false, "Write monologue"),
        MicroChallenge("cr091", ChallengeCategory.CREATIVE, "One Line Art", "Create art without lifting your pen.", 15, ChallengeDifficulty.MEDIUM, true, false, "Draw continuous line art")
    )

    // ==================== PUBLIC API ====================

    val allChallenges: List<MicroChallenge> by lazy {
        physicalChallenges + mentalChallenges + socialChallenges + creativeChallenges
    }

    /**
     * Get challenge for a specific day of year (1-365)
     */
    fun getChallengeForDay(dayOfYear: Int): MicroChallenge {
        val adjustedDay = ((dayOfYear - 1) % 365).coerceIn(0, 364)
        return allChallenges[adjustedDay]
    }

    /**
     * Get contextual challenge based on conditions
     */
    fun getContextualChallenge(
        isWeekend: Boolean,
        preferIndoor: Boolean,
        preferredDifficulty: ChallengeDifficulty = ChallengeDifficulty.MEDIUM
    ): MicroChallenge {
        val filtered = allChallenges.filter { challenge ->
            val indoorMatch = !preferIndoor || challenge.indoorOnly || !challenge.requiresEquipment
            val difficultyMatch = challenge.difficulty == preferredDifficulty
            indoorMatch && difficultyMatch
        }

        return if (filtered.isNotEmpty()) {
            filtered.random()
        } else {
            allChallenges.random()
        }
    }

    /**
     * Get challenges by category
     */
    fun getChallengesByCategory(category: ChallengeCategory): List<MicroChallenge> {
        return when (category) {
            ChallengeCategory.PHYSICAL -> physicalChallenges
            ChallengeCategory.MENTAL -> mentalChallenges
            ChallengeCategory.SOCIAL -> socialChallenges
            ChallengeCategory.CREATIVE -> creativeChallenges
        }
    }

    /**
     * Get challenges by difficulty
     */
    fun getChallengesByDifficulty(difficulty: ChallengeDifficulty): List<MicroChallenge> {
        return allChallenges.filter { it.difficulty == difficulty }
    }

    /**
     * Get indoor-only challenges
     */
    fun getIndoorChallenges(): List<MicroChallenge> {
        return allChallenges.filter { it.indoorOnly }
    }

    /**
     * Get challenges that require no equipment
     */
    fun getNoEquipmentChallenges(): List<MicroChallenge> {
        return allChallenges.filter { !it.requiresEquipment }
    }

    /**
     * Get quick challenges (under 15 minutes)
     */
    fun getQuickChallenges(): List<MicroChallenge> {
        return allChallenges.filter { it.duration <= 15 }
    }

    /**
     * Get total challenge count (should be 365)
     */
    fun getTotalCount(): Int = allChallenges.size

    /**
     * Search challenges by keyword
     */
    fun searchChallenges(query: String): List<MicroChallenge> {
        val lowerQuery = query.lowercase()
        return allChallenges.filter {
            it.title.lowercase().contains(lowerQuery) ||
            it.description.lowercase().contains(lowerQuery)
        }
    }
}
