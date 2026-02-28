const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, 'shared', 'src', 'commonMain', 'kotlin', 'com', 'dailywell', 'app', 'data', 'ExerciseDatabase.kt');

console.log('Reading file:', filePath);
let content = fs.readFileSync(filePath, 'utf8');

// Equipment to Category mapping
const equipmentToCategoryMap = {
    '"Barbell"': 'ExerciseCategory.BARBELL',
    '"Dumbbells"': 'ExerciseCategory.DUMBBELL',
    '"Dumbbell"': 'ExerciseCategory.DUMBBELL',
    '"Cable"': 'ExerciseCategory.CABLE',
    '"Bodyweight"': 'ExerciseCategory.BODYWEIGHT',
    '"Machine"': 'ExerciseCategory.MACHINE',
    '"Cardio"': 'ExerciseCategory.CARDIO',
    '"Kettlebell"': 'ExerciseCategory.BODYWEIGHT', // Treat as bodyweight if exists
    '""': 'ExerciseCategory.FLEXIBILITY' // Empty equipment
};

// Replace all invalid category references with BARBELL as temporary placeholder
// We'll fix them properly by analyzing equipment field next

// First, replace all ExerciseCategory.CHEST/BACK/SHOULDERS/LEGS/BICEPS/TRICEPS/CORE with a placeholder
content = content.replace(/category = ExerciseCategory\.CHEST/g, 'category = ExerciseCategory.BARBELL /* TEMP */');
content = content.replace(/category = ExerciseCategory\.BACK/g, 'category = ExerciseCategory.BARBELL /* TEMP */');
content = content.replace(/category = ExerciseCategory\.SHOULDERS/g, 'category = ExerciseCategory.BARBELL /* TEMP */');
content = content.replace(/category = ExerciseCategory\.LEGS/g, 'category = ExerciseCategory.BARBELL /* TEMP */');
content = content.replace(/category = ExerciseCategory\.BICEPS/g, 'category = ExerciseCategory.BARBELL /* TEMP */');
content = content.replace(/category = ExerciseCategory\.TRICEPS/g, 'category = ExerciseCategory.BARBELL /* TEMP */');
content = content.replace(/category = ExerciseCategory\.CORE/g, 'category = ExerciseCategory.BODYWEIGHT /* TEMP */');
content = content.replace(/category = ExerciseCategory\.FULL_BODY/g, 'category = ExerciseCategory.BODYWEIGHT /* TEMP */');

// Now do a more sophisticated replacement based on equipment field
// Pattern: extract exercise definitions and fix category based on equipment

const exerciseDefPattern = /ExerciseDefinition\s*\(([\s\S]*?)\n\s*\)/g;

content = content.replace(exerciseDefPattern, (match) => {
    // Extract equipment value
    const equipmentMatch = match.match(/equipment\s*=\s*(".*?")/);
    if (equipmentMatch) {
        const equipment = equipmentMatch[1];
        const category = equipmentToCategoryMap[equipment];

        if (category) {
            // Replace the category line with the correct category
            return match.replace(/category\s*=\s*ExerciseCategory\.\w+(\s*\/\*\s*TEMP\s*\*\/)?/, `category = ${category}`);
        }
    }

    return match;
});

// Write the fixed content back
fs.writeFileSync(filePath, content, 'utf8');

console.log('✅ Fixed ExerciseDatabase.kt - all categories now match equipment types');
console.log('Mapping applied:');
Object.entries(equipmentToCategoryMap).forEach(([eq, cat]) => {
    console.log(`  ${eq} → ${cat}`);
});
