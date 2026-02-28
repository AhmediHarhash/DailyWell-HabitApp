package com.dailywell.app.ui.screens.psychology

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.LocalDailyWellColors
import com.dailywell.app.data.model.*
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.DailyWellSprings
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import com.dailywell.app.ui.components.StaggeredItem
import com.dailywell.app.ui.components.fadeInOnAppear
import com.dailywell.app.ui.components.pressScale

/**
 * Daily Psychology Lesson Screen (Noom-inspired)
 * Engaging UI for 5-10 minute daily lessons on behavior change
 */

/** Sealed state for the three possible views within the lesson screen */
private enum class LessonViewState {
    Content, Quiz, Reflection
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    lesson: DailyLesson,
    progress: UserLessonProgress,
    onComplete: (CompletedLesson) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var showQuiz by remember { mutableStateOf(false) }
    var quizAnswers by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showReflection by remember { mutableStateOf(false) }
    var reflectionText by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf<Int?>(null) }

    // Derive current view state for animated transitions
    val currentViewState by remember {
        derivedStateOf {
            when {
                showQuiz && lesson.quiz != null -> LessonViewState.Quiz
                showReflection && lesson.reflection != null -> LessonViewState.Reflection
                else -> LessonViewState.Content
            }
        }
    }

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Day ${lesson.day} of 180",
                    subtitle = lesson.category.name.replace("_", " "),
                    onNavigationClick = onBack
                )
            }
        ) { padding ->
            // Animated transitions between Content, Quiz, and Reflection views
            Crossfade(
                targetState = currentViewState,
                animationSpec = tween(durationMillis = 400, easing = EaseInOutCubic),
                label = "lessonViewTransition"
            ) { viewState ->
                when (viewState) {
                    LessonViewState.Quiz -> {
                        QuizView(
                            quiz = lesson.quiz!!,
                            answers = quizAnswers,
                            onAnswerSelected = { questionIndex, answerIndex ->
                                quizAnswers = quizAnswers.toMutableList().apply {
                                    if (size <= questionIndex) {
                                        while (size <= questionIndex) add(-1)
                                    }
                                    this[questionIndex] = answerIndex
                                }
                            },
                            onSubmit = {
                                showQuiz = false
                                if (lesson.reflection != null) {
                                    showReflection = true
                                }
                            },
                            modifier = Modifier.padding(padding)
                        )
                    }
                    LessonViewState.Reflection -> {
                        ReflectionView(
                            prompt = lesson.reflection!!,
                            text = reflectionText,
                            onTextChange = { reflectionText = it },
                            onComplete = {
                                showReflection = false
                            },
                            modifier = Modifier.padding(padding)
                        )
                    }
                    LessonViewState.Content -> {
                        LessonContent(
                            lesson = lesson,
                            progress = progress,
                            rating = rating,
                            onRatingChange = { rating = it },
                            onContinue = {
                                if (lesson.quiz != null) {
                                    showQuiz = true
                                } else if (lesson.reflection != null) {
                                    showReflection = true
                                } else {
                                    // Complete the lesson
                                    val completed = CompletedLesson(
                                        lessonId = lesson.id,
                                        completedOn = kotlinx.datetime.Clock.System.now().toString(),
                                        readTime = lesson.readTime,
                                        rating = rating
                                    )
                                    onComplete(completed)
                                }
                            },
                            modifier = Modifier.padding(padding)
                                )
                    }
                }
            }
        }
    }
}

@Composable
fun LessonContent(
    lesson: DailyLesson,
    progress: UserLessonProgress,
    rating: Int?,
    onRatingChange: (Int) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animated progress value
    val animatedProgress by animateFloatAsState(
        targetValue = progress.currentDay / 180f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "progressAnimation"
    )

    // Running item index for staggered entrance
    var staggerIndex = 0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StaggeredItem(index = staggerIndex++) {
                PremiumSectionChip(
                    text = "Daily lesson",
                    icon = DailyWellIcons.Coaching.Lesson
                )
            }
        }

        // Progress indicator
        item {
            StaggeredItem(index = staggerIndex++) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF4CAF50)
                )
            }
        }

        // Title & read time
        item {
            StaggeredItem(index = staggerIndex++) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        lesson.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = DailyWellIcons.Misc.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${lesson.readTime} min read",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Category badge
        item {
            StaggeredItem(index = staggerIndex++) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = getCategoryColor(lesson.category).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(lesson.category),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = getCategoryColor(lesson.category)
                        )
                        Text(
                            lesson.category.name.replace("_", " "),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = getCategoryColor(lesson.category)
                        )
                    }
                }
            }
        }

        // Main content
        item {
            StaggeredItem(index = staggerIndex++) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        lesson.content,
                        modifier = Modifier.padding(20.dp),
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Key takeaways
        if (lesson.keyTakeaways.isNotEmpty()) {
            item {
                StaggeredItem(index = staggerIndex++) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = DailyWellIcons.Onboarding.Philosophy,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = Color(0xFFFFC107)
                        )
                        Text(
                            "Key Takeaways",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Use the stagger index base and increment per takeaway
            val takeawayBaseIndex = staggerIndex
            itemsIndexed(lesson.keyTakeaways) { takeawayIndex, takeaway ->
                StaggeredItem(index = takeawayBaseIndex + takeawayIndex) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF9C4)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("\u2022", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                takeaway,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            staggerIndex += lesson.keyTakeaways.size
        }

        // Action item
        item {
            StaggeredItem(index = staggerIndex++) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = DailyWellIcons.Habits.Intentions,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = Color(0xFF2E7D32)
                            )
                            Text(
                                "Today's Action",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Text(
                            lesson.actionItem,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            }
        }

        // Rating
        item {
            StaggeredItem(index = staggerIndex++) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "How helpful was this lesson?",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { star ->
                            val isSelected = rating != null && rating >= star

                            // Animate scale when star is tapped
                            val starScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.0f else 0.85f,
                                animationSpec = DailyWellSprings.Bouncy,
                                label = "starScale$star"
                            )

                            Icon(
                                imageVector = if (isSelected) DailyWellIcons.Status.Star else DailyWellIcons.Status.StarOutline,
                                contentDescription = "Rate $star",
                                modifier = Modifier
                                    .size(28.dp)
                                    .graphicsLayer {
                                        scaleX = starScale
                                        scaleY = starScale
                                    }
                                    .clickable { onRatingChange(star) },
                                tint = if (isSelected) Color(0xFFFFC107) else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Continue button
        item {
            StaggeredItem(index = staggerIndex++) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .pressScale(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            if (lesson.quiz != null) "Take Quiz" else if (lesson.reflection != null) "Reflect" else "Complete Lesson",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(Icons.Default.ArrowForward, "Continue")
                    }
                }
            }
        }

        // Spacer
        item {
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun QuizView(
    quiz: List<QuizQuestion>,
    answers: List<Int>,
    onAnswerSelected: (Int, Int) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResults by remember { mutableStateOf(false) }
    val score = answers.filterIndexed { index, answer ->
        index < quiz.size && answer == quiz[index].correctAnswer
    }.size

    // Animate score counter when results are shown
    val animatedScore by animateIntAsState(
        targetValue = if (showResults) score else 0,
        animationSpec = tween(durationMillis = 600, easing = EaseOutCubic),
        label = "scoreAnimation"
    )

    // Scale bounce for the score card when revealed
    val scoreCardScale by animateFloatAsState(
        targetValue = if (showResults) 1f else 0.8f,
        animationSpec = DailyWellSprings.Bouncy,
        label = "scoreCardScale"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            StaggeredItem(index = 0) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = DailyWellIcons.Coaching.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Quick Quiz",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Test your understanding",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(quiz.size) { questionIndex ->
            val question = quiz[questionIndex]
            val selectedAnswer = if (answers.size > questionIndex) answers[questionIndex] else -1
            val isCorrect = selectedAnswer == question.correctAnswer

            StaggeredItem(index = questionIndex + 1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Question ${questionIndex + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            question.question,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 22.sp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            question.options.forEachIndexed { optionIndex, option ->
                                val isSelected = selectedAnswer == optionIndex
                                val showCorrect = showResults && optionIndex == question.correctAnswer
                                val showWrong = showResults && isSelected && !isCorrect

                                // Animate answer color transitions smoothly
                                val animatedContainerColor by animateColorAsState(
                                    targetValue = when {
                                        showCorrect -> Color(0xFFE8F5E9)
                                        showWrong -> Color(0xFFFFEBEE)
                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    animationSpec = tween(durationMillis = 350, easing = EaseInOutCubic),
                                    label = "answerColor_${questionIndex}_$optionIndex"
                                )

                                val animatedIndicatorColor by animateColorAsState(
                                    targetValue = when {
                                        showCorrect -> Color(0xFF4CAF50)
                                        showWrong -> Color(0xFFF44336)
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        else -> Color.Transparent
                                    },
                                    animationSpec = tween(durationMillis = 350, easing = EaseInOutCubic),
                                    label = "indicatorColor_${questionIndex}_$optionIndex"
                                )

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pressScale()
                                        .clickable(enabled = !showResults) {
                                            onAnswerSelected(questionIndex, optionIndex)
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = animatedContainerColor,
                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(animatedIndicatorColor)
                                                .border(
                                                    2.dp,
                                                    if (isSelected || showCorrect || showWrong) Color.Transparent
                                                    else Color.Gray,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (showCorrect) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    "Correct",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else if (showWrong) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    "Wrong",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            option,
                                            fontSize = 15.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Animate explanation appearance
                        AnimatedVisibility(
                            visible = showResults,
                            enter = fadeIn(tween(300)) + expandVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ),
                            exit = fadeOut(tween(200)) + shrinkVertically()
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFF9C4)
                                )
                            ) {
                                Text(
                                    question.explanation,
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            if (!showResults) {
                StaggeredItem(index = quiz.size + 1) {
                    Button(
                        onClick = { showResults = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .pressScale(),
                        enabled = answers.size == quiz.size && answers.all { it >= 0 },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Submit Answers", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Score card with bounce animation
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scoreCardScale
                                scaleY = scoreCardScale
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (score == quiz.size) Color(0xFFE8F5E9) else Color(0xFFFFF9C4)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Your Score",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "$animatedScore / ${quiz.size}",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (score == quiz.size) {
                                    Icon(
                                        imageVector = DailyWellIcons.Social.Cheer,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = Color(0xFF4CAF50)
                                    )
                                } else if (score == quiz.size - 1) {
                                    Icon(
                                        imageVector = DailyWellIcons.Social.HighFive,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = Color(0xFFFFC107)
                                    )
                                }
                                Text(
                                    when (score) {
                                        quiz.size -> "Perfect! You nailed it!"
                                        quiz.size - 1 -> "Almost perfect! Great job!"
                                        else -> "Good effort! Review the explanations above."
                                    },
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .pressScale(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReflectionView(
    prompt: String,
    text: String,
    onTextChange: (String) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Fade-in the entire reflection view content
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .fadeInOnAppear(durationMs = 450),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StaggeredItem(index = 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = DailyWellIcons.Coaching.Reflection,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Personal Reflection",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        StaggeredItem(index = 1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    prompt,
                    modifier = Modifier.padding(20.dp),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        StaggeredItem(index = 2) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("Type your thoughts here...") },
                shape = RoundedCornerShape(16.dp)
            )
        }

        StaggeredItem(index = 3) {
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .pressScale(),
                enabled = text.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Save Reflection", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// Helper functions
fun getCategoryColor(category: LessonCategory): Color = when (category) {
    LessonCategory.PSYCHOLOGY_BASICS -> Color(0xFF2196F3)
    LessonCategory.EMOTIONAL_EATING -> Color(0xFFE91E63)
    LessonCategory.MINDSET -> Color(0xFF9C27B0)
    LessonCategory.NUTRITION_SCIENCE -> Color(0xFF4CAF50)
    LessonCategory.BEHAVIOR_CHANGE -> Color(0xFFFF9800)
    LessonCategory.SOCIAL_SITUATIONS -> Color(0xFFFF5722)
    LessonCategory.STRESS_MANAGEMENT -> Color(0xFF00BCD4)
    LessonCategory.GOAL_SETTING -> Color(0xFFFFEB3B)
    LessonCategory.MAINTENANCE -> Color(0xFF8BC34A)
    LessonCategory.SELF_LOVE -> Color(0xFFF06292)
}

@Deprecated("Use getCategoryIcon() instead", replaceWith = ReplaceWith("getCategoryIcon(category)"))
fun getCategoryEmoji(category: LessonCategory): String = when (category) {
    LessonCategory.PSYCHOLOGY_BASICS -> "\uD83E\uDDE0"
    LessonCategory.EMOTIONAL_EATING -> "\uD83D\uDC94"
    LessonCategory.MINDSET -> "\uD83D\uDCAA"
    LessonCategory.NUTRITION_SCIENCE -> "\uD83E\uDD57"
    LessonCategory.BEHAVIOR_CHANGE -> "\uD83D\uDD04"
    LessonCategory.SOCIAL_SITUATIONS -> "\uD83D\uDC65"
    LessonCategory.STRESS_MANAGEMENT -> "\uD83E\uDDD8"
    LessonCategory.GOAL_SETTING -> "\uD83C\uDFAF"
    LessonCategory.MAINTENANCE -> "\u2696\uFE0F"
    LessonCategory.SELF_LOVE -> "\u2764\uFE0F"
}

fun getCategoryIcon(category: LessonCategory): ImageVector = when (category) {
    LessonCategory.PSYCHOLOGY_BASICS -> DailyWellIcons.Coaching.AICoach
    LessonCategory.EMOTIONAL_EATING -> DailyWellIcons.Health.Heart
    LessonCategory.MINDSET -> DailyWellIcons.Health.Workout
    LessonCategory.NUTRITION_SCIENCE -> DailyWellIcons.Health.Nutrition
    LessonCategory.BEHAVIOR_CHANGE -> DailyWellIcons.Habits.Recovery
    LessonCategory.SOCIAL_SITUATIONS -> DailyWellIcons.Social.People
    LessonCategory.STRESS_MANAGEMENT -> DailyWellIcons.Habits.Calm
    LessonCategory.GOAL_SETTING -> DailyWellIcons.Habits.Intentions
    LessonCategory.MAINTENANCE -> DailyWellIcons.Health.Weight
    LessonCategory.SELF_LOVE -> DailyWellIcons.Health.Heart
}
