package com.dailywell.app.data

import com.dailywell.app.data.model.ExerciseCategory
import com.dailywell.app.data.model.ExerciseDefinition

/**
 * Exercise Database - 200+ Exercises
 * Categorized by muscle group and equipment
 *
 * PERFECTION MODE: Comprehensive exercise library
 * - All major muscle groups covered
 * - Bodyweight, dumbbell, barbell, machine, cable exercises
 * - Searchable and filterable
 */
object ExerciseDatabase {

    /**
     * All exercises in database
     */
    val ALL_EXERCISES: List<ExerciseDefinition> by lazy {
        buildList {
            addAll(CHEST_EXERCISES)
            addAll(BACK_EXERCISES)
            addAll(SHOULDER_EXERCISES)
            addAll(BICEPS_EXERCISES)
            addAll(TRICEPS_EXERCISES)
            addAll(LEG_EXERCISES)
            addAll(CORE_EXERCISES)
            addAll(CARDIO_EXERCISES)
            addAll(FULL_BODY_EXERCISES)
        }
    }

    /**
     * Search exercises by name
     */
    fun search(query: String): List<ExerciseDefinition> {
        val lowerQuery = query.lowercase()
        return ALL_EXERCISES.filter {
            it.name.lowercase().contains(lowerQuery) ||
            it.muscleGroups.any { it.lowercase().contains(lowerQuery) }
        }
    }

    /**
     * Filter by category
     */
    fun filterByCategory(category: ExerciseCategory): List<ExerciseDefinition> {
        return ALL_EXERCISES.filter { it.category == category }
    }

    /**
     * Filter by muscle group
     */
    fun filterByMuscleGroup(muscleGroup: String): List<ExerciseDefinition> {
        return ALL_EXERCISES.filter { muscleGroup in it.muscleGroups }
    }

    /**
     * Filter by equipment
     */
    fun filterByEquipment(equipment: String): List<ExerciseDefinition> {
        return ALL_EXERCISES.filter { it.equipment == equipment }
    }

    /**
     * Get exercise by ID
     */
    fun getById(id: String): ExerciseDefinition? {
        return ALL_EXERCISES.firstOrNull { it.id == id }
    }

    // ========== CHEST EXERCISES ==========

    val CHEST_EXERCISES = listOf(
        // Barbell Exercises
        ExerciseDefinition(
            id = "bench_press",
            name = "Barbell Bench Press",
            muscleGroups = listOf("Chest", "Triceps", "Shoulders"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Lie on bench, lower bar to chest, press up. King of chest exercises."
        ),
        ExerciseDefinition(
            id = "incline_bench_press",
            name = "Incline Barbell Bench Press",
            muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Bench at 30-45 degrees. Targets upper chest."
        ),
        ExerciseDefinition(
            id = "decline_bench_press",
            name = "Decline Barbell Bench Press",
            muscleGroups = listOf("Chest", "Triceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Bench declined 15-30 degrees. Targets lower chest."
        ),
        ExerciseDefinition(
            id = "close_grip_bench_press",
            name = "Close-Grip Bench Press",
            muscleGroups = listOf("Triceps", "Chest"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Narrow grip. More tricep emphasis."
        ),
        ExerciseDefinition(
            id = "wide_grip_bench_press",
            name = "Wide-Grip Bench Press",
            muscleGroups = listOf("Chest"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Wider grip. More chest stretch and activation."
        ),
        ExerciseDefinition(
            id = "floor_press",
            name = "Barbell Floor Press",
            muscleGroups = listOf("Chest", "Triceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Press from floor. Great for lockout strength."
        ),

        // Dumbbell Exercises
        ExerciseDefinition(
            id = "dumbbell_bench_press",
            name = "Dumbbell Bench Press",
            muscleGroups = listOf("Chest", "Triceps", "Shoulders"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Greater range of motion than barbell. Better chest stretch."
        ),
        ExerciseDefinition(
            id = "incline_dumbbell_press",
            name = "Incline Dumbbell Press",
            muscleGroups = listOf("Chest", "Shoulders"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Targets upper chest with fuller range than barbell."
        ),
        ExerciseDefinition(
            id = "decline_dumbbell_press",
            name = "Decline Dumbbell Press",
            muscleGroups = listOf("Chest"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Targets lower chest. Great stretch."
        ),
        ExerciseDefinition(
            id = "dumbbell_fly",
            name = "Dumbbell Fly",
            muscleGroups = listOf("Chest"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Isolation exercise. Great chest stretch. Control the eccentric."
        ),
        ExerciseDefinition(
            id = "incline_dumbbell_fly",
            name = "Incline Dumbbell Fly",
            muscleGroups = listOf("Chest"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Targets upper chest. Fantastic stretch."
        ),
        ExerciseDefinition(
            id = "decline_dumbbell_fly",
            name = "Decline Dumbbell Fly",
            muscleGroups = listOf("Chest"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Lower chest isolation. Deep stretch."
        ),
        ExerciseDefinition(
            id = "dumbbell_pullover",
            name = "Dumbbell Pullover",
            muscleGroups = listOf("Chest", "Back", "Core"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbell",
            description = "Lower weight behind head. Expands ribcage."
        ),
        ExerciseDefinition(
            id = "crush_press",
            name = "Dumbbell Crush Press",
            muscleGroups = listOf("Chest"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Press dumbbells together throughout. Constant tension."
        ),

        // Cable Exercises
        ExerciseDefinition(
            id = "cable_fly",
            name = "Cable Fly",
            muscleGroups = listOf("Chest"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Constant tension throughout movement. Great for chest definition."
        ),
        ExerciseDefinition(
            id = "low_cable_fly",
            name = "Low Cable Fly",
            muscleGroups = listOf("Chest"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Pull from low position. Targets upper chest."
        ),
        ExerciseDefinition(
            id = "high_cable_fly",
            name = "High Cable Fly",
            muscleGroups = listOf("Chest"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Pull from high position. Targets lower chest."
        ),
        ExerciseDefinition(
            id = "cable_crossover",
            name = "Cable Crossover",
            muscleGroups = listOf("Chest"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Cross cables in front. Peak chest contraction."
        ),
        ExerciseDefinition(
            id = "standing_cable_press",
            name = "Standing Cable Press",
            muscleGroups = listOf("Chest", "Core"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Press cables forward. Functional pressing pattern."
        ),

        // Bodyweight Exercises
        ExerciseDefinition(
            id = "push_up",
            name = "Push-Up",
            muscleGroups = listOf("Chest", "Triceps", "Shoulders", "Core"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Classic bodyweight exercise. Can be done anywhere."
        ),
        ExerciseDefinition(
            id = "dip",
            name = "Chest Dips",
            muscleGroups = listOf("Chest", "Triceps", "Shoulders"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Lean forward for chest emphasis. Great mass builder."
        ),
        ExerciseDefinition(
            id = "incline_push_up",
            name = "Incline Push-Up",
            muscleGroups = listOf("Chest", "Triceps"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Hands elevated. Easier variation, targets lower chest."
        ),
        ExerciseDefinition(
            id = "decline_push_up",
            name = "Decline Push-Up",
            muscleGroups = listOf("Chest", "Shoulders"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Feet elevated. Harder variation, targets upper chest."
        ),
        ExerciseDefinition(
            id = "wide_push_up",
            name = "Wide Push-Up",
            muscleGroups = listOf("Chest"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Wider hand position. More chest activation."
        ),
        ExerciseDefinition(
            id = "archer_push_up",
            name = "Archer Push-Up",
            muscleGroups = listOf("Chest", "Core"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Shift weight to one side. Unilateral chest work."
        ),
        ExerciseDefinition(
            id = "plyometric_push_up",
            name = "Plyometric Push-Up",
            muscleGroups = listOf("Chest", "Power"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Explosive push-up. Builds power."
        ),

        // Machine Exercises
        ExerciseDefinition(
            id = "chest_press_machine",
            name = "Chest Press Machine",
            muscleGroups = listOf("Chest", "Triceps"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Fixed movement path. Good for beginners or drop sets."
        ),
        ExerciseDefinition(
            id = "pec_deck",
            name = "Pec Deck Machine",
            muscleGroups = listOf("Chest"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Isolation exercise for chest. Great pump."
        ),
        ExerciseDefinition(
            id = "hammer_strength_press",
            name = "Hammer Strength Chest Press",
            muscleGroups = listOf("Chest", "Triceps"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Plate-loaded machine. Natural pressing path."
        ),
        ExerciseDefinition(
            id = "incline_machine_press",
            name = "Incline Machine Press",
            muscleGroups = listOf("Chest", "Shoulders"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Machine incline press. Safe heavy pressing."
        )
    )

    // ========== BACK EXERCISES ==========

    val BACK_EXERCISES = listOf(
        // Deadlift Variations
        ExerciseDefinition(
            id = "deadlift",
            name = "Barbell Deadlift",
            muscleGroups = listOf("Back", "Legs", "Core"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "King of all exercises. Full posterior chain."
        ),
        ExerciseDefinition(
            id = "sumo_deadlift",
            name = "Sumo Deadlift",
            muscleGroups = listOf("Back", "Legs"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Wide stance. More quad and glute emphasis."
        ),
        ExerciseDefinition(
            id = "trap_bar_deadlift",
            name = "Trap Bar Deadlift",
            muscleGroups = listOf("Back", "Legs"),
            category = ExerciseCategory.BARBELL,
            equipment = "Trap Bar",
            description = "Neutral grip. More quad-dominant, safer for back."
        ),
        ExerciseDefinition(
            id = "rack_pull",
            name = "Rack Pull",
            muscleGroups = listOf("Back", "Traps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Partial deadlift. Heavy weight for upper back and traps."
        ),
        ExerciseDefinition(
            id = "deficit_deadlift",
            name = "Deficit Deadlift",
            muscleGroups = listOf("Back", "Legs"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Stand on platform. Increased range of motion."
        ),

        // Barbell Rows
        ExerciseDefinition(
            id = "barbell_row",
            name = "Barbell Row",
            muscleGroups = listOf("Back", "Biceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Bent-over position. Pull to lower chest/upper abs."
        ),
        ExerciseDefinition(
            id = "pendlay_row",
            name = "Pendlay Row",
            muscleGroups = listOf("Back", "Power"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Row from floor each rep. Explosive power."
        ),
        ExerciseDefinition(
            id = "underhand_barbell_row",
            name = "Underhand Barbell Row",
            muscleGroups = listOf("Back", "Biceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Underhand grip. More bicep and lower lat activation."
        ),
        ExerciseDefinition(
            id = "t_bar_row",
            name = "T-Bar Row",
            muscleGroups = listOf("Back", "Biceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Thick back builder. Pull to chest."
        ),
        ExerciseDefinition(
            id = "yates_row",
            name = "Yates Row",
            muscleGroups = listOf("Back", "Biceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Underhand, more upright. Named after Dorian Yates."
        ),
        ExerciseDefinition(
            id = "seal_row",
            name = "Seal Row",
            muscleGroups = listOf("Back"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Lying face down on bench. Pure back isolation."
        ),

        // Pull-Up Variations
        ExerciseDefinition(
            id = "pull_up",
            name = "Pull-Up",
            muscleGroups = listOf("Back", "Biceps"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Palms facing away. Best back width builder."
        ),
        ExerciseDefinition(
            id = "chin_up",
            name = "Chin-Up",
            muscleGroups = listOf("Back", "Biceps"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Palms facing you. More bicep involvement."
        ),
        ExerciseDefinition(
            id = "wide_grip_pull_up",
            name = "Wide-Grip Pull-Up",
            muscleGroups = listOf("Back"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Wider grip. Maximum lat width."
        ),
        ExerciseDefinition(
            id = "neutral_grip_pull_up",
            name = "Neutral Grip Pull-Up",
            muscleGroups = listOf("Back", "Biceps"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Palms facing each other. Joint-friendly."
        ),
        ExerciseDefinition(
            id = "weighted_pull_up",
            name = "Weighted Pull-Up",
            muscleGroups = listOf("Back", "Biceps"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Add weight belt. Progressive overload."
        ),
        ExerciseDefinition(
            id = "muscle_up",
            name = "Muscle-Up",
            muscleGroups = listOf("Back", "Chest", "Triceps"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Pull-up to dip. Advanced gymnastics move."
        ),

        // Cable Exercises
        ExerciseDefinition(
            id = "lat_pulldown",
            name = "Lat Pulldown",
            muscleGroups = listOf("Back", "Biceps"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Pull bar to upper chest. Great pull-up progression."
        ),
        ExerciseDefinition(
            id = "wide_grip_lat_pulldown",
            name = "Wide-Grip Lat Pulldown",
            muscleGroups = listOf("Back"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Wider grip. Maximum lat activation."
        ),
        ExerciseDefinition(
            id = "close_grip_lat_pulldown",
            name = "Close-Grip Lat Pulldown",
            muscleGroups = listOf("Back", "Biceps"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Narrow grip. More range of motion."
        ),
        ExerciseDefinition(
            id = "straight_arm_pulldown",
            name = "Straight-Arm Pulldown",
            muscleGroups = listOf("Back"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Keep arms straight. Pure lat isolation."
        ),
        ExerciseDefinition(
            id = "cable_row",
            name = "Seated Cable Row",
            muscleGroups = listOf("Back", "Biceps"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Pull to stomach. Squeeze shoulder blades together."
        ),
        ExerciseDefinition(
            id = "wide_grip_cable_row",
            name = "Wide-Grip Cable Row",
            muscleGroups = listOf("Back"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Wide grip attachment. Upper back emphasis."
        ),
        ExerciseDefinition(
            id = "single_arm_cable_row",
            name = "Single-Arm Cable Row",
            muscleGroups = listOf("Back"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "One arm at a time. Address imbalances."
        ),
        ExerciseDefinition(
            id = "face_pull",
            name = "Face Pull",
            muscleGroups = listOf("Rear Delts", "Upper Back"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Pull rope to face. Essential for shoulder health."
        ),

        // Dumbbell Rows
        ExerciseDefinition(
            id = "dumbbell_row",
            name = "Single-Arm Dumbbell Row",
            muscleGroups = listOf("Back", "Biceps"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "One arm at a time. Great range of motion."
        ),
        ExerciseDefinition(
            id = "bent_over_dumbbell_row",
            name = "Bent-Over Dumbbell Row",
            muscleGroups = listOf("Back", "Biceps"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Both dumbbells at once. Bent-over position."
        ),
        ExerciseDefinition(
            id = "chest_supported_dumbbell_row",
            name = "Chest-Supported Dumbbell Row",
            muscleGroups = listOf("Back"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Lying on incline bench. Removes lower back strain."
        ),
        ExerciseDefinition(
            id = "kroc_row",
            name = "Kroc Row",
            muscleGroups = listOf("Back", "Grip"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbell",
            description = "Heavy single-arm row with body English. High reps."
        ),
        ExerciseDefinition(
            id = "renegade_row",
            name = "Renegade Row",
            muscleGroups = listOf("Back", "Core"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Row from plank position. Core stability challenge."
        ),

        // Machine Exercises
        ExerciseDefinition(
            id = "machine_row",
            name = "Machine Row",
            muscleGroups = listOf("Back"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Seated machine row. Controlled movement."
        ),
        ExerciseDefinition(
            id = "hammer_strength_row",
            name = "Hammer Strength Row",
            muscleGroups = listOf("Back"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Plate-loaded row. Natural pulling path."
        ),
        ExerciseDefinition(
            id = "assisted_pull_up",
            name = "Assisted Pull-Up",
            muscleGroups = listOf("Back", "Biceps"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Machine assistance. Great for progression."
        ),

        // Other Back Exercises
        ExerciseDefinition(
            id = "inverted_row",
            name = "Inverted Row",
            muscleGroups = listOf("Back", "Biceps"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Pull body to bar. Horizontal pulling."
        ),
        ExerciseDefinition(
            id = "superman",
            name = "Superman",
            muscleGroups = listOf("Lower Back", "Glutes"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Lying face down, raise arms and legs. Lower back strength."
        ),
        ExerciseDefinition(
            id = "back_extension",
            name = "Back Extension",
            muscleGroups = listOf("Lower Back", "Glutes"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Bend at hips, extend back. Strengthens erectors."
        ),
        ExerciseDefinition(
            id = "good_morning",
            name = "Good Morning",
            muscleGroups = listOf("Lower Back", "Hamstrings"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Hip hinge with bar on back. Posterior chain."
        )
    )

    // ========== SHOULDER EXERCISES ==========

    val SHOULDER_EXERCISES = listOf(
        // Pressing Exercises
        ExerciseDefinition(
            id = "overhead_press",
            name = "Barbell Overhead Press",
            muscleGroups = listOf("Shoulders", "Triceps", "Core"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Standing or seated. Press bar overhead. King of shoulder exercises."
        ),
        ExerciseDefinition(
            id = "seated_overhead_press",
            name = "Seated Barbell Press",
            muscleGroups = listOf("Shoulders", "Triceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Seated version. Removes leg drive, pure shoulder."
        ),
        ExerciseDefinition(
            id = "push_press",
            name = "Push Press",
            muscleGroups = listOf("Shoulders", "Legs", "Power"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Use leg drive to press overhead. More weight, power development."
        ),
        ExerciseDefinition(
            id = "bradford_press",
            name = "Bradford Press",
            muscleGroups = listOf("Shoulders"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Press front to back overhead. Constant tension."
        ),
        ExerciseDefinition(
            id = "dumbbell_shoulder_press",
            name = "Dumbbell Shoulder Press",
            muscleGroups = listOf("Shoulders", "Triceps"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Seated or standing. Greater range than barbell."
        ),
        ExerciseDefinition(
            id = "arnold_press",
            name = "Arnold Press",
            muscleGroups = listOf("Shoulders"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Rotate palms as you press. Hits all three delt heads."
        ),
        ExerciseDefinition(
            id = "single_arm_dumbbell_press",
            name = "Single-Arm Dumbbell Press",
            muscleGroups = listOf("Shoulders", "Core"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbell",
            description = "Press one arm at a time. Core stability challenge."
        ),
        ExerciseDefinition(
            id = "landmine_press",
            name = "Landmine Press",
            muscleGroups = listOf("Shoulders", "Core"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Angular press. Shoulder-friendly."
        ),
        ExerciseDefinition(
            id = "machine_shoulder_press",
            name = "Machine Shoulder Press",
            muscleGroups = listOf("Shoulders", "Triceps"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Fixed path. Good for drop sets."
        ),

        // Lateral Raises
        ExerciseDefinition(
            id = "lateral_raise",
            name = "Dumbbell Lateral Raise",
            muscleGroups = listOf("Shoulders"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Isolation for side delts. Raises arms to sides."
        ),
        ExerciseDefinition(
            id = "cable_lateral_raise",
            name = "Cable Lateral Raise",
            muscleGroups = listOf("Shoulders"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Constant tension. Better muscle activation."
        ),
        ExerciseDefinition(
            id = "leaning_lateral_raise",
            name = "Leaning Lateral Raise",
            muscleGroups = listOf("Shoulders"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbell",
            description = "Hold onto support, lean out. Greater stretch."
        ),
        ExerciseDefinition(
            id = "lu_raise",
            name = "Lu Raise",
            muscleGroups = listOf("Shoulders"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Lateral raise above head. Named after Lu Xiaojun."
        ),

        // Front Raises
        ExerciseDefinition(
            id = "front_raise",
            name = "Front Raise",
            muscleGroups = listOf("Shoulders"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Raise weight to front. Targets front delts."
        ),
        ExerciseDefinition(
            id = "barbell_front_raise",
            name = "Barbell Front Raise",
            muscleGroups = listOf("Shoulders"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Front raise with barbell. Can use more weight."
        ),
        ExerciseDefinition(
            id = "plate_front_raise",
            name = "Plate Front Raise",
            muscleGroups = listOf("Shoulders"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Plate",
            description = "Hold plate with both hands. Simple and effective."
        ),
        ExerciseDefinition(
            id = "cable_front_raise",
            name = "Cable Front Raise",
            muscleGroups = listOf("Shoulders"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Constant tension throughout."
        ),

        // Rear Delt Exercises
        ExerciseDefinition(
            id = "rear_delt_fly",
            name = "Rear Delt Fly",
            muscleGroups = listOf("Rear Delts"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Bent over, raise weights to sides. Essential for balanced shoulders."
        ),
        ExerciseDefinition(
            id = "reverse_pec_deck",
            name = "Reverse Pec Deck",
            muscleGroups = listOf("Rear Delts"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Machine rear delt fly. Great isolation."
        ),
        ExerciseDefinition(
            id = "cable_rear_delt_fly",
            name = "Cable Rear Delt Fly",
            muscleGroups = listOf("Rear Delts"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Cross cables for rear delts. Constant tension."
        ),
        ExerciseDefinition(
            id = "prone_rear_delt_raise",
            name = "Prone Rear Delt Raise",
            muscleGroups = listOf("Rear Delts"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Lying face down on incline bench. Pure rear delt."
        ),

        // Trap Exercises
        ExerciseDefinition(
            id = "shrug",
            name = "Barbell Shrug",
            muscleGroups = listOf("Traps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Shrug shoulders up. Builds traps."
        ),
        ExerciseDefinition(
            id = "dumbbell_shrug",
            name = "Dumbbell Shrug",
            muscleGroups = listOf("Traps"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Greater range than barbell. Natural movement path."
        ),
        ExerciseDefinition(
            id = "upright_row",
            name = "Upright Row",
            muscleGroups = listOf("Shoulders", "Traps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Pull bar up along body to chin. Careful with shoulder position."
        ),
        ExerciseDefinition(
            id = "cable_upright_row",
            name = "Cable Upright Row",
            muscleGroups = listOf("Shoulders", "Traps"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Smoother tension curve than barbell."
        ),
        ExerciseDefinition(
            id = "snatch_grip_high_pull",
            name = "Snatch-Grip High Pull",
            muscleGroups = listOf("Traps", "Shoulders", "Power"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Wide grip, explosive pull. Olympic weightlifting accessory."
        ),

        // Specialized Exercises
        ExerciseDefinition(
            id = "cuban_press",
            name = "Cuban Press",
            muscleGroups = listOf("Shoulders", "Rotator Cuff"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Upright row to external rotation to press. Shoulder health."
        ),
        ExerciseDefinition(
            id = "bus_driver",
            name = "Bus Driver",
            muscleGroups = listOf("Shoulders"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Plate",
            description = "Hold plate, rotate like steering wheel. Delt endurance."
        ),
        ExerciseDefinition(
            id = "handstand_push_up",
            name = "Handstand Push-Up",
            muscleGroups = listOf("Shoulders", "Triceps"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Advanced. Push-up in handstand position."
        ),
        ExerciseDefinition(
            id = "pike_push_up",
            name = "Pike Push-Up",
            muscleGroups = listOf("Shoulders"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Hips up, head towards floor. Handstand progression."
        )
    )

    // ========== LEG EXERCISES ==========

    val LEG_EXERCISES = listOf(
        // Squat Variations
        ExerciseDefinition(
            id = "squat",
            name = "Barbell Back Squat",
            muscleGroups = listOf("Quads", "Glutes", "Core"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "King of leg exercises. Squat to parallel or below."
        ),
        ExerciseDefinition(
            id = "front_squat",
            name = "Barbell Front Squat",
            muscleGroups = listOf("Quads", "Core"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Bar on front delts. More quad emphasis, less back strain."
        ),
        ExerciseDefinition(
            id = "goblet_squat",
            name = "Goblet Squat",
            muscleGroups = listOf("Quads", "Glutes", "Core"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbell",
            description = "Hold dumbbell at chest. Great squat variation for beginners."
        ),
        ExerciseDefinition(
            id = "box_squat",
            name = "Box Squat",
            muscleGroups = listOf("Quads", "Glutes"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Squat to box. Teaches depth and builds power."
        ),
        ExerciseDefinition(
            id = "pause_squat",
            name = "Pause Squat",
            muscleGroups = listOf("Quads", "Glutes"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Pause at bottom. Builds strength out of the hole."
        ),
        ExerciseDefinition(
            id = "overhead_squat",
            name = "Overhead Squat",
            muscleGroups = listOf("Quads", "Core", "Shoulders"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Hold bar overhead while squatting. Requires mobility."
        ),
        ExerciseDefinition(
            id = "zercher_squat",
            name = "Zercher Squat",
            muscleGroups = listOf("Quads", "Core"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Hold bar in elbow crease. Unique squat variation."
        ),
        ExerciseDefinition(
            id = "anderson_squat",
            name = "Anderson Squat",
            muscleGroups = listOf("Quads", "Glutes"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Start from pins at bottom. Concentric only."
        ),
        ExerciseDefinition(
            id = "sissy_squat",
            name = "Sissy Squat",
            muscleGroups = listOf("Quads"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Lean back, knees forward. Intense quad isolation."
        ),
        ExerciseDefinition(
            id = "pistol_squat",
            name = "Pistol Squat",
            muscleGroups = listOf("Quads", "Glutes", "Balance"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Single-leg squat. Advanced bodyweight exercise."
        ),

        // Lunge Variations
        ExerciseDefinition(
            id = "lunge",
            name = "Dumbbell Lunge",
            muscleGroups = listOf("Quads", "Glutes"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Step forward, lower down. Great for balance and unilateral strength."
        ),
        ExerciseDefinition(
            id = "barbell_lunge",
            name = "Barbell Lunge",
            muscleGroups = listOf("Quads", "Glutes"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Lunge with barbell on back. Can use more weight."
        ),
        ExerciseDefinition(
            id = "walking_lunge",
            name = "Walking Lunge",
            muscleGroups = listOf("Quads", "Glutes"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Continuous forward lunges. Great conditioning."
        ),
        ExerciseDefinition(
            id = "reverse_lunge",
            name = "Reverse Lunge",
            muscleGroups = listOf("Quads", "Glutes"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Step backwards. Easier on knees."
        ),
        ExerciseDefinition(
            id = "bulgarian_split_squat",
            name = "Bulgarian Split Squat",
            muscleGroups = listOf("Quads", "Glutes"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Rear foot elevated. Killer quad and glute exercise."
        ),
        ExerciseDefinition(
            id = "curtsy_lunge",
            name = "Curtsy Lunge",
            muscleGroups = listOf("Glutes", "Quads"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Cross leg behind. Targets glutes."
        ),
        ExerciseDefinition(
            id = "lateral_lunge",
            name = "Lateral Lunge",
            muscleGroups = listOf("Quads", "Glutes", "Adductors"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Step to side. Hits adductors."
        ),

        // Hamstring Exercises
        ExerciseDefinition(
            id = "romanian_deadlift",
            name = "Romanian Deadlift",
            muscleGroups = listOf("Hamstrings", "Glutes", "Lower Back"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Hinge at hips, lower bar. Fantastic hamstring builder."
        ),
        ExerciseDefinition(
            id = "stiff_leg_deadlift",
            name = "Stiff-Leg Deadlift",
            muscleGroups = listOf("Hamstrings", "Lower Back"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Legs straight. Maximum hamstring stretch."
        ),
        ExerciseDefinition(
            id = "single_leg_rdl",
            name = "Single-Leg Romanian Deadlift",
            muscleGroups = listOf("Hamstrings", "Glutes", "Balance"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbell",
            description = "One leg at a time. Balance challenge."
        ),
        ExerciseDefinition(
            id = "leg_curl",
            name = "Lying Leg Curl",
            muscleGroups = listOf("Hamstrings"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Isolation for hamstrings. Curl heels to glutes."
        ),
        ExerciseDefinition(
            id = "seated_leg_curl",
            name = "Seated Leg Curl",
            muscleGroups = listOf("Hamstrings"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Seated position. Different hamstring activation."
        ),
        ExerciseDefinition(
            id = "nordic_curl",
            name = "Nordic Hamstring Curl",
            muscleGroups = listOf("Hamstrings"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Lower body forward. Eccentric hamstring destroyer."
        ),
        ExerciseDefinition(
            id = "glute_ham_raise",
            name = "Glute-Ham Raise",
            muscleGroups = listOf("Hamstrings", "Glutes"),
            category = ExerciseCategory.MACHINE,
            equipment = "GHR Machine",
            description = "Full posterior chain. Advanced exercise."
        ),
        ExerciseDefinition(
            id = "good_morning_legs",
            name = "Good Morning",
            muscleGroups = listOf("Hamstrings", "Lower Back"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Hip hinge with bar on back. Hamstring and erector focus."
        ),

        // Glute Exercises
        ExerciseDefinition(
            id = "glute_bridge",
            name = "Barbell Glute Bridge",
            muscleGroups = listOf("Glutes", "Hamstrings"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Hip thrust motion. Best glute builder."
        ),
        ExerciseDefinition(
            id = "hip_thrust",
            name = "Barbell Hip Thrust",
            muscleGroups = listOf("Glutes", "Hamstrings"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Shoulders on bench. Maximum glute activation."
        ),
        ExerciseDefinition(
            id = "single_leg_hip_thrust",
            name = "Single-Leg Hip Thrust",
            muscleGroups = listOf("Glutes"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "One leg. Address imbalances."
        ),
        ExerciseDefinition(
            id = "cable_kickback",
            name = "Cable Kickback",
            muscleGroups = listOf("Glutes"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Kick leg back. Glute isolation."
        ),
        ExerciseDefinition(
            id = "donkey_kick",
            name = "Donkey Kick",
            muscleGroups = listOf("Glutes"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "On all fours, kick up. Glute activation."
        ),
        ExerciseDefinition(
            id = "fire_hydrant",
            name = "Fire Hydrant",
            muscleGroups = listOf("Glutes", "Hip Abductors"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Raise leg to side. Hip abductor work."
        ),
        ExerciseDefinition(
            id = "frog_pump",
            name = "Frog Pump",
            muscleGroups = listOf("Glutes"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Feet together, knees out. Glute pump."
        ),

        // Quad Exercises
        ExerciseDefinition(
            id = "leg_extension",
            name = "Leg Extension",
            muscleGroups = listOf("Quads"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Isolation for quads. Extend legs against resistance."
        ),
        ExerciseDefinition(
            id = "leg_press",
            name = "Leg Press",
            muscleGroups = listOf("Quads", "Glutes"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Push platform away. Allows heavy weight safely."
        ),
        ExerciseDefinition(
            id = "hack_squat",
            name = "Hack Squat",
            muscleGroups = listOf("Quads", "Glutes"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Angled squat machine. Quad-dominant."
        ),
        ExerciseDefinition(
            id = "belt_squat",
            name = "Belt Squat",
            muscleGroups = listOf("Quads", "Glutes"),
            category = ExerciseCategory.MACHINE,
            equipment = "Belt Squat Machine",
            description = "Weight on belt. No spinal loading."
        ),
        ExerciseDefinition(
            id = "step_up",
            name = "Dumbbell Step-Up",
            muscleGroups = listOf("Quads", "Glutes"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Step onto box. Functional leg strength."
        ),
        ExerciseDefinition(
            id = "spanish_squat",
            name = "Spanish Squat",
            muscleGroups = listOf("Quads"),
            category = ExerciseCategory.CABLE,
            equipment = "Band",
            description = "Band behind knees. Quad isolation, knee-friendly."
        ),

        // Adductor/Abductor Exercises
        ExerciseDefinition(
            id = "adductor_machine",
            name = "Hip Adduction Machine",
            muscleGroups = listOf("Adductors"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Squeeze legs together. Inner thigh."
        ),
        ExerciseDefinition(
            id = "abductor_machine",
            name = "Hip Abduction Machine",
            muscleGroups = listOf("Abductors", "Glutes"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Push legs apart. Outer hip and glutes."
        ),
        ExerciseDefinition(
            id = "copenhagen_plank",
            name = "Copenhagen Plank",
            muscleGroups = listOf("Adductors", "Core"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Side plank with top leg elevated. Advanced adductor."
        ),
        ExerciseDefinition(
            id = "cossack_squat",
            name = "Cossack Squat",
            muscleGroups = listOf("Quads", "Adductors", "Mobility"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Deep side-to-side squat. Great mobility drill."
        ),

        // Calf Exercises
        ExerciseDefinition(
            id = "calf_raise",
            name = "Standing Calf Raise",
            muscleGroups = listOf("Calves"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Rise on toes. Full range for calf growth."
        ),
        ExerciseDefinition(
            id = "seated_calf_raise",
            name = "Seated Calf Raise",
            muscleGroups = listOf("Calves"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Seated position targets soleus muscle."
        ),
        ExerciseDefinition(
            id = "single_leg_calf_raise",
            name = "Single-Leg Calf Raise",
            muscleGroups = listOf("Calves"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "One leg at a time. Address imbalances."
        ),
        ExerciseDefinition(
            id = "donkey_calf_raise",
            name = "Donkey Calf Raise",
            muscleGroups = listOf("Calves"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Bent over position. Full calf stretch."
        ),

        // Plyometric Leg Exercises
        ExerciseDefinition(
            id = "box_jump",
            name = "Box Jump",
            muscleGroups = listOf("Quads", "Power"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Box",
            description = "Explosive jump onto box. Power development."
        ),
        ExerciseDefinition(
            id = "broad_jump",
            name = "Broad Jump",
            muscleGroups = listOf("Quads", "Power"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Jump forward for distance. Explosive power."
        ),
        ExerciseDefinition(
            id = "jump_squat",
            name = "Jump Squat",
            muscleGroups = listOf("Quads", "Power"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Squat then jump. Plyometric leg power."
        ),
        ExerciseDefinition(
            id = "skater_jump",
            name = "Skater Jump",
            muscleGroups = listOf("Quads", "Balance"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Lateral jumps. Builds lateral power and balance."
        )
    )

    // ========== BICEPS EXERCISES ==========

    val BICEPS_EXERCISES = listOf(
        // Barbell Curls
        ExerciseDefinition(
            id = "barbell_curl",
            name = "Barbell Curl",
            muscleGroups = listOf("Biceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Classic bicep exercise. Curl bar to shoulders."
        ),
        ExerciseDefinition(
            id = "ez_bar_curl",
            name = "EZ-Bar Curl",
            muscleGroups = listOf("Biceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "EZ-Bar",
            description = "Angled grip. Easier on wrists."
        ),
        ExerciseDefinition(
            id = "wide_grip_curl",
            name = "Wide-Grip Barbell Curl",
            muscleGroups = listOf("Biceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Wide grip. Targets short head."
        ),
        ExerciseDefinition(
            id = "close_grip_curl",
            name = "Close-Grip Barbell Curl",
            muscleGroups = listOf("Biceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Close grip. Targets long head."
        ),
        ExerciseDefinition(
            id = "drag_curl",
            name = "Drag Curl",
            muscleGroups = listOf("Biceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Drag bar up body. Reduces front delt involvement."
        ),
        ExerciseDefinition(
            id = "21s",
            name = "Barbell Curl 21s",
            muscleGroups = listOf("Biceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "7 bottom half, 7 top half, 7 full. Bicep burner."
        ),

        // Dumbbell Curls
        ExerciseDefinition(
            id = "dumbbell_curl",
            name = "Dumbbell Curl",
            muscleGroups = listOf("Biceps"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Alternate or together. Full range of motion."
        ),
        ExerciseDefinition(
            id = "alternating_dumbbell_curl",
            name = "Alternating Dumbbell Curl",
            muscleGroups = listOf("Biceps"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "One arm at a time. More focus per arm."
        ),
        ExerciseDefinition(
            id = "hammer_curl",
            name = "Hammer Curl",
            muscleGroups = listOf("Biceps", "Forearms"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Neutral grip (palms facing each other). Hits brachialis."
        ),
        ExerciseDefinition(
            id = "incline_dumbbell_curl",
            name = "Incline Dumbbell Curl",
            muscleGroups = listOf("Biceps"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "On incline bench. Maximum stretch on biceps."
        ),
        ExerciseDefinition(
            id = "concentration_curl",
            name = "Concentration Curl",
            muscleGroups = listOf("Biceps"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbell",
            description = "Seated, elbow braced on thigh. Maximum isolation."
        ),
        ExerciseDefinition(
            id = "zottman_curl",
            name = "Zottman Curl",
            muscleGroups = listOf("Biceps", "Forearms"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Curl up underhand, lower overhand. Hits biceps and forearms."
        ),
        ExerciseDefinition(
            id = "cross_body_curl",
            name = "Cross-Body Hammer Curl",
            muscleGroups = listOf("Biceps", "Brachialis"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbell",
            description = "Curl across body. Targets brachialis."
        ),

        // Preacher Curls
        ExerciseDefinition(
            id = "preacher_curl",
            name = "Preacher Curl",
            muscleGroups = listOf("Biceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Arm supported on pad. Strict form, great peak contraction."
        ),
        ExerciseDefinition(
            id = "dumbbell_preacher_curl",
            name = "Dumbbell Preacher Curl",
            muscleGroups = listOf("Biceps"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbell",
            description = "One arm at a time. Full isolation."
        ),
        ExerciseDefinition(
            id = "ez_bar_preacher_curl",
            name = "EZ-Bar Preacher Curl",
            muscleGroups = listOf("Biceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "EZ-Bar",
            description = "Easier on wrists than straight bar."
        ),

        // Cable Curls
        ExerciseDefinition(
            id = "cable_curl",
            name = "Cable Curl",
            muscleGroups = listOf("Biceps"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Constant tension throughout movement."
        ),
        ExerciseDefinition(
            id = "high_cable_curl",
            name = "High Cable Curl",
            muscleGroups = listOf("Biceps"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Cables from above. Peak contraction pose."
        ),
        ExerciseDefinition(
            id = "cable_hammer_curl",
            name = "Cable Hammer Curl",
            muscleGroups = listOf("Biceps", "Forearms"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Rope attachment. Neutral grip with constant tension."
        ),

        // Chin-Up Variations
        ExerciseDefinition(
            id = "chin_up_biceps",
            name = "Chin-Up (Biceps Focus)",
            muscleGroups = listOf("Biceps", "Back"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Underhand grip. Great compound bicep exercise."
        ),

        // Other
        ExerciseDefinition(
            id = "spider_curl",
            name = "Spider Curl",
            muscleGroups = listOf("Biceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Lean over bench. Strict form, no momentum."
        ),
        ExerciseDefinition(
            id = "bayesian_curl",
            name = "Bayesian Curl",
            muscleGroups = listOf("Biceps"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Cable behind you. Maximum bicep stretch."
        )
    )

    // ========== TRICEPS EXERCISES ==========

    val TRICEPS_EXERCISES = listOf(
        // Pressing Exercises
        ExerciseDefinition(
            id = "close_grip_bench",
            name = "Close-Grip Bench Press",
            muscleGroups = listOf("Triceps", "Chest"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Hands shoulder-width. Best mass builder for triceps."
        ),
        ExerciseDefinition(
            id = "jm_press",
            name = "JM Press",
            muscleGroups = listOf("Triceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Hybrid skull crusher and close-grip bench. Tricep destroyer."
        ),
        ExerciseDefinition(
            id = "board_press",
            name = "Board Press",
            muscleGroups = listOf("Triceps", "Chest"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Partial bench press to boards. Lockout strength."
        ),

        // Dips
        ExerciseDefinition(
            id = "tricep_dip",
            name = "Tricep Dips",
            muscleGroups = listOf("Triceps", "Chest"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Body upright (not leaning forward). Killer tricep exercise."
        ),
        ExerciseDefinition(
            id = "weighted_dip",
            name = "Weighted Dips",
            muscleGroups = listOf("Triceps", "Chest"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Add weight belt. Progressive overload."
        ),
        ExerciseDefinition(
            id = "bench_dip",
            name = "Bench Dips",
            muscleGroups = listOf("Triceps"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Hands on bench behind you. Beginner-friendly."
        ),

        // Overhead Extensions
        ExerciseDefinition(
            id = "overhead_extension",
            name = "Dumbbell Overhead Extension",
            muscleGroups = listOf("Triceps"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbell",
            description = "Lower weight behind head. Stretches long head of triceps."
        ),
        ExerciseDefinition(
            id = "ez_bar_overhead_extension",
            name = "EZ-Bar Overhead Extension",
            muscleGroups = listOf("Triceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "EZ-Bar",
            description = "Overhead with EZ-bar. Less wrist strain."
        ),
        ExerciseDefinition(
            id = "cable_overhead_extension",
            name = "Cable Overhead Extension",
            muscleGroups = listOf("Triceps"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Rope from low pulley. Constant tension."
        ),
        ExerciseDefinition(
            id = "french_press",
            name = "French Press",
            muscleGroups = listOf("Triceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Lying overhead extension. Classic tricep exercise."
        ),

        // Pushdowns
        ExerciseDefinition(
            id = "tricep_pushdown",
            name = "Cable Tricep Pushdown",
            muscleGroups = listOf("Triceps"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Push rope/bar down. Keep elbows tucked."
        ),
        ExerciseDefinition(
            id = "rope_pushdown",
            name = "Rope Pushdown",
            muscleGroups = listOf("Triceps"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Rope attachment. Split apart at bottom."
        ),
        ExerciseDefinition(
            id = "reverse_grip_pushdown",
            name = "Reverse-Grip Pushdown",
            muscleGroups = listOf("Triceps"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Underhand grip. Targets medial head."
        ),
        ExerciseDefinition(
            id = "single_arm_pushdown",
            name = "Single-Arm Pushdown",
            muscleGroups = listOf("Triceps"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "One arm at a time. Address imbalances."
        ),

        // Skull Crushers
        ExerciseDefinition(
            id = "skull_crusher",
            name = "Skull Crushers",
            muscleGroups = listOf("Triceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Lying, lower bar to forehead. Great tricep stretch."
        ),
        ExerciseDefinition(
            id = "dumbbell_skull_crusher",
            name = "Dumbbell Skull Crusher",
            muscleGroups = listOf("Triceps"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Dumbbells instead of barbell. Independent arm work."
        ),
        ExerciseDefinition(
            id = "ez_bar_skull_crusher",
            name = "EZ-Bar Skull Crusher",
            muscleGroups = listOf("Triceps"),
            category = ExerciseCategory.BARBELL,
            equipment = "EZ-Bar",
            description = "Easier on wrists and elbows."
        ),

        // Kickbacks
        ExerciseDefinition(
            id = "tricep_kickback",
            name = "Dumbbell Kickback",
            muscleGroups = listOf("Triceps"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbell",
            description = "Bent over, extend arm back. Isolation exercise."
        ),
        ExerciseDefinition(
            id = "cable_kickback",
            name = "Cable Kickback",
            muscleGroups = listOf("Triceps"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Constant tension. Better than dumbbell version."
        ),

        // Bodyweight
        ExerciseDefinition(
            id = "diamond_pushup",
            name = "Diamond Push-Up",
            muscleGroups = listOf("Triceps", "Chest"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Hands together forming diamond. Tricep-focused push-up."
        ),
        ExerciseDefinition(
            id = "close_grip_pushup",
            name = "Close-Grip Push-Up",
            muscleGroups = listOf("Triceps", "Chest"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Narrow hand position. More tricep emphasis."
        ),

        // Other
        ExerciseDefinition(
            id = "tate_press",
            name = "Tate Press",
            muscleGroups = listOf("Triceps"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Elbows out, lower to chest. Unique tricep exercise."
        )
    )

    // ========== CORE EXERCISES ==========

    val CORE_EXERCISES = listOf(
        // Plank Variations
        ExerciseDefinition(
            id = "plank",
            name = "Plank",
            muscleGroups = listOf("Core"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Hold rigid position. Full core engagement."
        ),
        ExerciseDefinition(
            id = "side_plank",
            name = "Side Plank",
            muscleGroups = listOf("Obliques", "Core"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "On side, hold position. Oblique stability."
        ),
        ExerciseDefinition(
            id = "plank_reach",
            name = "Plank with Reach",
            muscleGroups = listOf("Core", "Shoulders"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "From plank, reach arm forward. Anti-rotation."
        ),
        ExerciseDefinition(
            id = "rocking_plank",
            name = "Rocking Plank",
            muscleGroups = listOf("Core"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Rock forward and back in plank. Dynamic stability."
        ),
        ExerciseDefinition(
            id = "plank_to_pike",
            name = "Plank to Pike",
            muscleGroups = listOf("Core", "Shoulders"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "From plank, pike hips up. Dynamic core."
        ),

        // Crunches
        ExerciseDefinition(
            id = "crunch",
            name = "Crunch",
            muscleGroups = listOf("Abs"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Curl shoulders up. Focus on abs contraction."
        ),
        ExerciseDefinition(
            id = "bicycle_crunch",
            name = "Bicycle Crunch",
            muscleGroups = listOf("Abs", "Obliques"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Alternate elbow to opposite knee. Full ab activation."
        ),
        ExerciseDefinition(
            id = "reverse_crunch",
            name = "Reverse Crunch",
            muscleGroups = listOf("Abs"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Curl knees to chest. Lower ab focus."
        ),
        ExerciseDefinition(
            id = "cable_crunch",
            name = "Cable Crunch",
            muscleGroups = listOf("Abs"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Kneel, crunch down with rope. Weighted ab work."
        ),
        ExerciseDefinition(
            id = "decline_crunch",
            name = "Decline Crunch",
            muscleGroups = listOf("Abs"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bench",
            description = "Crunches on decline bench. Harder variation."
        ),

        // Leg Raises
        ExerciseDefinition(
            id = "leg_raise",
            name = "Hanging Leg Raise",
            muscleGroups = listOf("Abs", "Hip Flexors"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Hang from bar, raise legs. Advanced ab exercise."
        ),
        ExerciseDefinition(
            id = "lying_leg_raise",
            name = "Lying Leg Raise",
            muscleGroups = listOf("Abs"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Lying down, raise legs. Lower ab focus."
        ),
        ExerciseDefinition(
            id = "captain_chair_raise",
            name = "Captain's Chair Leg Raise",
            muscleGroups = listOf("Abs"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Forearms supported, raise legs. Controlled ab work."
        ),
        ExerciseDefinition(
            id = "knee_raise",
            name = "Hanging Knee Raise",
            muscleGroups = listOf("Abs"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Bring knees to chest. Easier than full leg raise."
        ),

        // Sit-Ups
        ExerciseDefinition(
            id = "sit_up",
            name = "Sit-Up",
            muscleGroups = listOf("Abs", "Hip Flexors"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Full sit-up motion. Classic ab exercise."
        ),
        ExerciseDefinition(
            id = "ghd_sit_up",
            name = "GHD Sit-Up",
            muscleGroups = listOf("Abs"),
            category = ExerciseCategory.MACHINE,
            equipment = "GHD Machine",
            description = "Full range sit-up on GHD. Advanced."
        ),
        ExerciseDefinition(
            id = "v_up",
            name = "V-Up",
            muscleGroups = listOf("Abs"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Touch hands to feet in V position. Tough ab exercise."
        ),

        // Twisting/Rotation
        ExerciseDefinition(
            id = "russian_twist",
            name = "Russian Twist",
            muscleGroups = listOf("Obliques", "Abs"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Seated, lean back, twist side to side. Oblique burner."
        ),
        ExerciseDefinition(
            id = "cable_woodchop",
            name = "Cable Woodchop",
            muscleGroups = listOf("Obliques", "Core"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Chop motion across body. Great rotational core exercise."
        ),
        ExerciseDefinition(
            id = "landmine_rotation",
            name = "Landmine Rotation",
            muscleGroups = listOf("Obliques", "Core"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Rotate barbell side to side. Rotational power."
        ),
        ExerciseDefinition(
            id = "standing_oblique_crunch",
            name = "Standing Oblique Crunch",
            muscleGroups = listOf("Obliques"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Standing, bring elbow to knee. Oblique activation."
        ),

        // Rollouts
        ExerciseDefinition(
            id = "ab_wheel",
            name = "Ab Wheel Rollout",
            muscleGroups = listOf("Core", "Shoulders"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Ab Wheel",
            description = "Roll out and back. Extremely difficult core exercise."
        ),
        ExerciseDefinition(
            id = "barbell_rollout",
            name = "Barbell Rollout",
            muscleGroups = listOf("Core", "Shoulders"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Roll barbell out from knees. Alternative to ab wheel."
        ),

        // Anti-Rotation/Stability
        ExerciseDefinition(
            id = "pallof_press",
            name = "Pallof Press",
            muscleGroups = listOf("Core", "Obliques"),
            category = ExerciseCategory.CABLE,
            equipment = "Cable",
            description = "Press cable forward, resist rotation. Anti-rotation core."
        ),
        ExerciseDefinition(
            id = "dead_bug",
            name = "Dead Bug",
            muscleGroups = listOf("Core"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "On back, alternate arm and leg. Core stability."
        ),
        ExerciseDefinition(
            id = "bird_dog",
            name = "Bird Dog",
            muscleGroups = listOf("Core", "Lower Back"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "On hands and knees, extend opposite limbs. Balance and stability."
        ),
        ExerciseDefinition(
            id = "hollow_hold",
            name = "Hollow Body Hold",
            muscleGroups = listOf("Abs", "Core"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Hold hollow position. Gymnastics core exercise."
        ),

        // Weighted Core
        ExerciseDefinition(
            id = "weighted_crunch",
            name = "Weighted Crunch",
            muscleGroups = listOf("Abs"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Plate",
            description = "Hold plate on chest. Progressive overload."
        ),
        ExerciseDefinition(
            id = "weighted_sit_up",
            name = "Weighted Sit-Up",
            muscleGroups = listOf("Abs"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Plate",
            description = "Hold weight during sit-up. Build ab strength."
        ),

        // Machine/Other
        ExerciseDefinition(
            id = "ab_machine",
            name = "Ab Crunch Machine",
            muscleGroups = listOf("Abs"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Weighted ab machine. Progressive resistance."
        ),
        ExerciseDefinition(
            id = "torso_rotation_machine",
            name = "Torso Rotation Machine",
            muscleGroups = listOf("Obliques"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Rotate torso against resistance."
        ),
        ExerciseDefinition(
            id = "l_sit",
            name = "L-Sit",
            muscleGroups = listOf("Abs", "Hip Flexors"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Hold L position. Advanced core strength."
        ),
        ExerciseDefinition(
            id = "dragon_flag",
            name = "Dragon Flag",
            muscleGroups = listOf("Abs", "Core"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bench",
            description = "Bruce Lee's favorite. Extremely advanced."
        )
    )

    // ========== CARDIO EXERCISES ==========

    val CARDIO_EXERCISES = listOf(
        // Running/Walking
        ExerciseDefinition(
            id = "running",
            name = "Running",
            muscleGroups = listOf("Legs", "Cardio"),
            category = ExerciseCategory.CARDIO,
            equipment = "None",
            description = "Outdoor or treadmill. Best overall cardio."
        ),
        ExerciseDefinition(
            id = "sprinting",
            name = "Sprints",
            muscleGroups = listOf("Legs", "Cardio", "Power"),
            category = ExerciseCategory.CARDIO,
            equipment = "None",
            description = "High-intensity sprints. Burns fat, builds power."
        ),
        ExerciseDefinition(
            id = "walking",
            name = "Walking",
            muscleGroups = listOf("Legs", "Cardio"),
            category = ExerciseCategory.CARDIO,
            equipment = "None",
            description = "Low-intensity. Great for recovery or beginners."
        ),
        ExerciseDefinition(
            id = "incline_walking",
            name = "Incline Walking",
            muscleGroups = listOf("Legs", "Glutes", "Cardio"),
            category = ExerciseCategory.CARDIO,
            equipment = "Treadmill",
            description = "Walk on incline. Burns calories, builds glutes."
        ),
        ExerciseDefinition(
            id = "stair_climber",
            name = "Stair Climber",
            muscleGroups = listOf("Legs", "Cardio"),
            category = ExerciseCategory.MACHINE,
            equipment = "Machine",
            description = "Continuous stair climbing. Great calorie burn."
        ),

        // Cycling
        ExerciseDefinition(
            id = "cycling",
            name = "Cycling",
            muscleGroups = listOf("Legs", "Cardio"),
            category = ExerciseCategory.CARDIO,
            equipment = "Bike",
            description = "Stationary or road bike. Low-impact cardio."
        ),
        ExerciseDefinition(
            id = "assault_bike",
            name = "Assault Bike",
            muscleGroups = listOf("Full Body", "Cardio"),
            category = ExerciseCategory.CARDIO,
            equipment = "Assault Bike",
            description = "Fan bike with arms and legs. Brutal HIIT cardio."
        ),
        ExerciseDefinition(
            id = "spin_class",
            name = "Spin Class",
            muscleGroups = listOf("Legs", "Cardio"),
            category = ExerciseCategory.CARDIO,
            equipment = "Spin Bike",
            description = "High-intensity cycling. Great for endurance."
        ),

        // Rowing
        ExerciseDefinition(
            id = "rowing",
            name = "Rowing Machine",
            muscleGroups = listOf("Back", "Legs", "Cardio"),
            category = ExerciseCategory.CARDIO,
            equipment = "Rowing Machine",
            description = "Full-body cardio. Burns massive calories."
        ),

        // Jump Rope
        ExerciseDefinition(
            id = "jump_rope",
            name = "Jump Rope",
            muscleGroups = listOf("Calves", "Cardio"),
            category = ExerciseCategory.CARDIO,
            equipment = "Jump Rope",
            description = "High-intensity cardio. Great for conditioning."
        ),
        ExerciseDefinition(
            id = "double_unders",
            name = "Double Unders",
            muscleGroups = listOf("Calves", "Cardio"),
            category = ExerciseCategory.CARDIO,
            equipment = "Jump Rope",
            description = "Two rope passes per jump. Advanced jump rope."
        ),

        // Bodyweight Cardio
        ExerciseDefinition(
            id = "burpee",
            name = "Burpees",
            muscleGroups = listOf("Full Body", "Cardio"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Drop down, push-up, jump up. Killer full-body exercise."
        ),
        ExerciseDefinition(
            id = "mountain_climber",
            name = "Mountain Climbers",
            muscleGroups = listOf("Core", "Cardio"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Plank position, drive knees to chest. Great cardio finisher."
        ),
        ExerciseDefinition(
            id = "high_knees",
            name = "High Knees",
            muscleGroups = listOf("Legs", "Cardio"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Run in place, knees high. Cardio and coordination."
        ),
        ExerciseDefinition(
            id = "jumping_jacks",
            name = "Jumping Jacks",
            muscleGroups = listOf("Full Body", "Cardio"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Classic cardio exercise. Full body warm-up."
        ),
        ExerciseDefinition(
            id = "jumping_lunges",
            name = "Jumping Lunges",
            muscleGroups = listOf("Legs", "Cardio"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Bodyweight",
            description = "Jump between lunge positions. Cardio and leg strength."
        ),

        // Other Machines
        ExerciseDefinition(
            id = "elliptical",
            name = "Elliptical",
            muscleGroups = listOf("Legs", "Cardio"),
            category = ExerciseCategory.CARDIO,
            equipment = "Elliptical",
            description = "Low-impact cardio. Easier on joints."
        ),
        ExerciseDefinition(
            id = "swimming",
            name = "Swimming",
            muscleGroups = listOf("Full Body", "Cardio"),
            category = ExerciseCategory.CARDIO,
            equipment = "Pool",
            description = "Full-body cardio. Zero impact."
        ),
        ExerciseDefinition(
            id = "battle_ropes",
            name = "Battle Ropes",
            muscleGroups = listOf("Arms", "Core", "Cardio"),
            category = ExerciseCategory.CARDIO,
            equipment = "Battle Ropes",
            description = "Wave ropes up and down. High-intensity cardio."
        ),
        ExerciseDefinition(
            id = "sled_push",
            name = "Sled Push",
            muscleGroups = listOf("Legs", "Cardio", "Power"),
            category = ExerciseCategory.CARDIO,
            equipment = "Sled",
            description = "Push weighted sled. Builds power and conditioning."
        ),
        ExerciseDefinition(
            id = "sled_pull",
            name = "Sled Pull",
            muscleGroups = listOf("Back", "Legs", "Cardio"),
            category = ExerciseCategory.CARDIO,
            equipment = "Sled",
            description = "Pull sled backwards. Conditioning and strength."
        )
    )

    // ========== FULL BODY EXERCISES ==========

    val FULL_BODY_EXERCISES = listOf(
        // Olympic Lifts
        ExerciseDefinition(
            id = "clean",
            name = "Barbell Clean",
            muscleGroups = listOf("Full Body"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Explosive pull from floor to shoulders. Olympic lift."
        ),
        ExerciseDefinition(
            id = "power_clean",
            name = "Power Clean",
            muscleGroups = listOf("Full Body", "Power"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Clean without full squat. Builds explosive power."
        ),
        ExerciseDefinition(
            id = "hang_clean",
            name = "Hang Clean",
            muscleGroups = listOf("Full Body"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Clean from hang position. Emphasizes second pull."
        ),
        ExerciseDefinition(
            id = "clean_and_jerk",
            name = "Clean and Jerk",
            muscleGroups = listOf("Full Body"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Clean to shoulders, then jerk overhead. Olympic lift."
        ),
        ExerciseDefinition(
            id = "snatch",
            name = "Barbell Snatch",
            muscleGroups = listOf("Full Body"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Pull bar from floor to overhead in one motion. Advanced."
        ),
        ExerciseDefinition(
            id = "power_snatch",
            name = "Power Snatch",
            muscleGroups = listOf("Full Body", "Power"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Snatch without full squat. Power development."
        ),
        ExerciseDefinition(
            id = "hang_snatch",
            name = "Hang Snatch",
            muscleGroups = listOf("Full Body"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Snatch from hang. Focuses on hip extension."
        ),

        // Kettlebell Exercises
        ExerciseDefinition(
            id = "kettlebell_swing",
            name = "Kettlebell Swing",
            muscleGroups = listOf("Glutes", "Hamstrings", "Core"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Kettlebell",
            description = "Hip hinge motion. Explosive posterior chain exercise."
        ),
        ExerciseDefinition(
            id = "kettlebell_snatch",
            name = "Kettlebell Snatch",
            muscleGroups = listOf("Full Body"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Kettlebell",
            description = "Swing to overhead. Full-body power."
        ),
        ExerciseDefinition(
            id = "kettlebell_clean",
            name = "Kettlebell Clean",
            muscleGroups = listOf("Full Body"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Kettlebell",
            description = "Swing to rack position. Full-body coordination."
        ),
        ExerciseDefinition(
            id = "turkish_get_up",
            name = "Turkish Get-Up",
            muscleGroups = listOf("Full Body", "Core"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Kettlebell",
            description = "Stand up with weight overhead. Ultimate full-body exercise."
        ),

        // Thrusters/Complexes
        ExerciseDefinition(
            id = "thruster",
            name = "Dumbbell Thruster",
            muscleGroups = listOf("Full Body"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Squat to overhead press. CrossFit staple."
        ),
        ExerciseDefinition(
            id = "barbell_thruster",
            name = "Barbell Thruster",
            muscleGroups = listOf("Full Body"),
            category = ExerciseCategory.BARBELL,
            equipment = "Barbell",
            description = "Front squat to push press. Metabolic crusher."
        ),
        ExerciseDefinition(
            id = "man_maker",
            name = "Man Maker",
            muscleGroups = listOf("Full Body"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Burpee + row + clean + press. Ultimate full-body move."
        ),
        ExerciseDefinition(
            id = "devil_press",
            name = "Devil Press",
            muscleGroups = listOf("Full Body"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Burpee to double dumbbell snatch. Extremely difficult."
        ),

        // Carries
        ExerciseDefinition(
            id = "farmers_walk",
            name = "Farmer's Walk",
            muscleGroups = listOf("Full Body", "Grip"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbells",
            description = "Carry heavy weights. Builds everything."
        ),
        ExerciseDefinition(
            id = "overhead_carry",
            name = "Overhead Carry",
            muscleGroups = listOf("Shoulders", "Core"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbell",
            description = "Walk with weight overhead. Shoulder stability."
        ),
        ExerciseDefinition(
            id = "suitcase_carry",
            name = "Suitcase Carry",
            muscleGroups = listOf("Core", "Obliques"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Dumbbell",
            description = "Carry weight on one side. Anti-lateral flexion."
        ),
        ExerciseDefinition(
            id = "waiter_carry",
            name = "Waiter Carry",
            muscleGroups = listOf("Shoulders", "Core"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Kettlebell",
            description = "Walk with kettlebell overhead. Shoulder stability."
        ),

        // Other Full Body
        ExerciseDefinition(
            id = "wall_ball",
            name = "Wall Ball",
            muscleGroups = listOf("Full Body"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Medicine Ball",
            description = "Squat and throw ball to wall. Full-body conditioning."
        ),
        ExerciseDefinition(
            id = "slam_ball",
            name = "Slam Ball",
            muscleGroups = listOf("Full Body", "Core"),
            category = ExerciseCategory.DUMBBELL,
            equipment = "Slam Ball",
            description = "Slam ball to ground. Power and conditioning."
        ),
        ExerciseDefinition(
            id = "tire_flip",
            name = "Tire Flip",
            muscleGroups = listOf("Full Body", "Power"),
            category = ExerciseCategory.BODYWEIGHT,
            equipment = "Tire",
            description = "Flip heavy tire. Strongman exercise."
        ),
        ExerciseDefinition(
            id = "prowler_push",
            name = "Prowler Push",
            muscleGroups = listOf("Full Body", "Power"),
            category = ExerciseCategory.MACHINE,
            equipment = "Prowler",
            description = "Push weighted sled. Builds strength and conditioning."
        )
    )
}

/**
 * Exercise definition (database entry)
 */
data class ExerciseDefinition(
    val id: String,
    val name: String,
    val muscleGroups: List<String>,
    val category: ExerciseCategory,
    val equipment: String,
    val description: String
)
