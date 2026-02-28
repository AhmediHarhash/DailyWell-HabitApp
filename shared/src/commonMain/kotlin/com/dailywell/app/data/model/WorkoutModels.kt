package com.dailywell.app.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Feature #11: Workout Tracking
 * Complete workout logging system for strength and cardio
 */

@Serializable
data class WorkoutSession(
    val id: String,
    val userId: String,
    val date: String,
    val workoutType: WorkoutType,
    val name: String,              // e.g., "Push Day", "Leg Day", "HIIT Cardio"
    val exercises: List<Exercise>,
    val duration: Int,             // minutes
    val totalVolume: Int = 0,      // kg × reps (for strength)
    val caloriesBurned: Int = 0,
    val notes: String? = null,
    val startTime: Instant,
    val endTime: Instant,
    val feeling: WorkoutFeeling? = null
)

@Serializable
enum class WorkoutType {
    STRENGTH,
    CARDIO,
    HIIT,
    YOGA,
    PILATES,
    SPORTS,
    STRETCHING,
    OTHER
}

@Serializable
enum class WorkoutFeeling {
    EXHAUSTED,
    TIRED,
    NORMAL,
    GOOD,
    EXCELLENT
}

/**
 * Exercise definition - catalog entry for an exercise
 */
@Serializable
data class ExerciseDefinition(
    val id: String,
    val name: String,
    val muscleGroups: List<String>,    // String for flexibility
    val category: ExerciseCategory,
    val equipment: String,
    val description: String
)

/**
 * Exercise in a workout session - includes sets and performance data
 */
@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val category: ExerciseCategory,
    val muscleGroups: List<MuscleGroup>,
    val sets: List<ExerciseSet>,
    val restTime: Int = 60,        // seconds between sets
    val notes: String? = null,
    val personalRecord: Boolean = false
)

@Serializable
enum class ExerciseCategory {
    BARBELL,
    DUMBBELL,
    MACHINE,
    BODYWEIGHT,
    CABLE,
    CARDIO,
    FLEXIBILITY
}

@Serializable
enum class MuscleGroup {
    CHEST,
    BACK,
    SHOULDERS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    ABS,
    OBLIQUES,
    QUADS,
    HAMSTRINGS,
    GLUTES,
    CALVES,
    FULL_BODY,
    CARDIO
}

@Serializable
data class ExerciseSet(
    val setNumber: Int,
    val reps: Int,
    val weight: Float = 0f,        // kg or lbs
    val isWarmup: Boolean = false,
    val isDropset: Boolean = false,
    val completed: Boolean = false,
    val distance: Float? = null,   // km (for cardio)
    val time: Int? = null,         // seconds (for cardio/timed exercises)
    val rpe: Int? = null           // Rate of Perceived Exertion (1-10)
) {
    val volume: Int get() = (reps * weight).toInt()
}

@Serializable
data class WorkoutTemplate(
    val id: String,
    val name: String,
    val workoutType: WorkoutType,
    val exercises: List<ExerciseTemplate>,
    val estimatedDuration: Int,    // minutes
    val difficulty: Difficulty,
    val targetMuscles: List<MuscleGroup>,
    val isCustom: Boolean = false
)

@Serializable
enum class Difficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

@Serializable
data class ExerciseTemplate(
    val exerciseName: String,
    val category: ExerciseCategory,
    val targetSets: Int,
    val minReps: Int,              // e.g., 8
    val maxReps: Int,              // e.g., 12
    val restTime: Int = 60,
    val notes: String? = null
) {
    val targetReps: IntRange get() = minReps..maxReps
}

@Serializable
data class ExerciseProgress(
    val exerciseName: String,
    val history: List<ExerciseRecord>,
    val personalRecord: PersonalRecord? = null,
    val improvement: ProgressTrend
)

@Serializable
data class ExerciseRecord(
    val date: String,
    val maxWeight: Float,
    val totalVolume: Int,
    val bestSet: ExerciseSet
)

@Serializable
data class PersonalRecord(
    val weight: Float,
    val reps: Int,
    val achievedOn: String,
    val oneRepMax: Int             // Calculated 1RM
) {
    companion object {
        // Epley Formula: 1RM = weight × (1 + reps/30)
        fun calculate1RM(weight: Float, reps: Int): Int {
            return (weight * (1 + reps / 30f)).toInt()
        }
    }
}

@Serializable
enum class ProgressTrend {
    IMPROVING,
    STABLE,
    DECLINING,
    NEW_EXERCISE
}

// Popular workout templates
object WorkoutTemplates {
    val PUSH_DAY = WorkoutTemplate(
        id = "push_day",
        name = "Push Day (Chest, Shoulders, Triceps)",
        workoutType = WorkoutType.STRENGTH,
        exercises = listOf(
            ExerciseTemplate("Bench Press", ExerciseCategory.BARBELL, 4, 6, 8, 180),
            ExerciseTemplate("Incline Dumbbell Press", ExerciseCategory.DUMBBELL, 3, 8, 12, 120),
            ExerciseTemplate("Shoulder Press", ExerciseCategory.DUMBBELL, 3, 8, 12, 120),
            ExerciseTemplate("Lateral Raises", ExerciseCategory.DUMBBELL, 3, 12, 15, 60),
            ExerciseTemplate("Tricep Dips", ExerciseCategory.BODYWEIGHT, 3, 10, 12, 90),
            ExerciseTemplate("Cable Tricep Pushdown", ExerciseCategory.CABLE, 3, 12, 15, 60)
        ),
        estimatedDuration = 60,
        difficulty = Difficulty.INTERMEDIATE,
        targetMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
    )

    val PULL_DAY = WorkoutTemplate(
        id = "pull_day",
        name = "Pull Day (Back, Biceps)",
        workoutType = WorkoutType.STRENGTH,
        exercises = listOf(
            ExerciseTemplate("Deadlift", ExerciseCategory.BARBELL, 4, 5, 6, 240),
            ExerciseTemplate("Pull-ups", ExerciseCategory.BODYWEIGHT, 4, 6, 10, 120),
            ExerciseTemplate("Barbell Row", ExerciseCategory.BARBELL, 3, 8, 10, 120),
            ExerciseTemplate("Lat Pulldown", ExerciseCategory.CABLE, 3, 10, 12, 90),
            ExerciseTemplate("Barbell Curl", ExerciseCategory.BARBELL, 3, 8, 12, 90),
            ExerciseTemplate("Hammer Curls", ExerciseCategory.DUMBBELL, 3, 10, 12, 60)
        ),
        estimatedDuration = 60,
        difficulty = Difficulty.INTERMEDIATE,
        targetMuscles = listOf(MuscleGroup.BACK, MuscleGroup.BICEPS)
    )

    val LEG_DAY = WorkoutTemplate(
        id = "leg_day",
        name = "Leg Day",
        workoutType = WorkoutType.STRENGTH,
        exercises = listOf(
            ExerciseTemplate("Squat", ExerciseCategory.BARBELL, 4, 6, 8, 180),
            ExerciseTemplate("Romanian Deadlift", ExerciseCategory.BARBELL, 3, 8, 10, 120),
            ExerciseTemplate("Leg Press", ExerciseCategory.MACHINE, 3, 10, 12, 120),
            ExerciseTemplate("Leg Curl", ExerciseCategory.MACHINE, 3, 10, 12, 90),
            ExerciseTemplate("Leg Extension", ExerciseCategory.MACHINE, 3, 12, 15, 90),
            ExerciseTemplate("Calf Raises", ExerciseCategory.MACHINE, 4, 15, 20, 60)
        ),
        estimatedDuration = 60,
        difficulty = Difficulty.INTERMEDIATE,
        targetMuscles = listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES)
    )

    val ALL_TEMPLATES = listOf(PUSH_DAY, PULL_DAY, LEG_DAY)
}

// Popular exercises database
object ExerciseDatabase {
    val POPULAR_EXERCISES = listOf(
        // Chest
        "Bench Press", "Incline Bench Press", "Decline Bench Press",
        "Dumbbell Press", "Incline Dumbbell Press", "Chest Fly",
        "Cable Crossover", "Push-ups",

        // Back
        "Deadlift", "Pull-ups", "Chin-ups", "Barbell Row",
        "Dumbbell Row", "T-Bar Row", "Lat Pulldown",
        "Seated Cable Row", "Face Pulls",

        // Shoulders
        "Overhead Press", "Dumbbell Shoulder Press", "Arnold Press",
        "Lateral Raises", "Front Raises", "Rear Delt Fly",
        "Upright Row", "Shrugs",

        // Arms
        "Barbell Curl", "Dumbbell Curl", "Hammer Curl",
        "Preacher Curl", "Cable Curl", "Tricep Dips",
        "Tricep Pushdown", "Overhead Tricep Extension",
        "Skull Crushers", "Close-Grip Bench Press",

        // Legs
        "Squat", "Front Squat", "Leg Press", "Romanian Deadlift",
        "Leg Curl", "Leg Extension", "Lunges", "Bulgarian Split Squat",
        "Calf Raises", "Seated Calf Raise",

        // Core
        "Plank", "Ab Wheel", "Hanging Leg Raise", "Cable Crunch",
        "Russian Twist", "Bicycle Crunch"
    )
}
