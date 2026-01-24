package com.example.handtalklokal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandSignTutorialsScreenWithFeatures(navController: NavHostController) {
    val viewModel: TranslatorViewModel = viewModel()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var currentLessonIndex by remember { mutableStateOf(0) }
    
    // Tutorial state
    val showTutorial by viewModel.showTutorial.collectAsState()
    val currentTutorialStep by viewModel.currentTutorialStep.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                // Add top app bar with back button when in lesson view
                if (selectedCategory != null) {
                    TopAppBar(
                        title = { 
                            Text(
                                text = selectedCategory!!,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { 
                                selectedCategory = null
                                currentLessonIndex = 0
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                    contentDescription = "Back to categories"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                            navigationIconContentColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Only show main title when not in a specific category
                if (selectedCategory == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hand Sign Tutorials",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (selectedCategory == null) {
                    // Category Selection Screen
                    CategorySelectionScreen(
                        modifier = Modifier,
                        onCategorySelected = { category -> 
                            selectedCategory = category
                            currentLessonIndex = 0
                        }
                    )
                } else {
                    // Lesson Screen
                    LessonScreen(
                        modifier = Modifier,
                        category = selectedCategory!!, 
                        lessonIndex = currentLessonIndex,
                        onNavigateToLesson = { index -> currentLessonIndex = index },
                        totalLessons = getLessonsForCategory(selectedCategory!!).size,
                        onBackToCategories = {
                            selectedCategory = null
                            currentLessonIndex = 0
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CategorySelectionScreen(
    modifier: Modifier = Modifier,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf(
        "Alphabet",
        "Common Words",
        "Basic Phrases",
        "Emotions",
        "Numbers"
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select a Tutorial Category",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categories) { category ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategorySelected(category) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(category),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(end = 16.dp)
                        )
                        
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LessonScreen(
    modifier: Modifier = Modifier,
    category: String,
    lessonIndex: Int,
    onNavigateToLesson: (Int) -> Unit,
    totalLessons: Int,
    onBackToCategories: () -> Unit
) {
    val lessons = getLessonsForCategory(category)
    val currentLesson = lessons.getOrNull(lessonIndex)
    
    if (currentLesson == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Lesson not found",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBackToCategories) {
                Text("Back to Categories")
            }
        }
        return
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Lesson ${lessonIndex + 1}",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Placeholder for hand sign demonstration
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Hand Sign Demonstration\n(Placeholder)",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentLesson.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Navigation controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back to categories button (replaces previous button when at first lesson)
            if (lessonIndex == 0) {
                Button(
                    onClick = onBackToCategories,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack, 
                            contentDescription = "Back to categories"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Categories",
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            } else {
                // Previous lesson button
                Button(
                    onClick = { 
                        if (lessonIndex > 0) {
                            onNavigateToLesson(lessonIndex - 1)
                        }
                    },
                    enabled = lessonIndex > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack, 
                            contentDescription = "Previous"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Previous",
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            
            Text(
                text = "${lessonIndex + 1} / $totalLessons",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Button(
                onClick = { 
                    if (lessonIndex < totalLessons - 1) {
                        onNavigateToLesson(lessonIndex + 1)
                    }
                },
                enabled = lessonIndex < totalLessons - 1,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                ) {
                    Text(
                        text = "Next",
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowForward, 
                        contentDescription = "Next"
                    )
                }
            }

        }
    }
}

data class Lesson(
    val title: String
)

fun getLessonsForCategory(category: String): List<Lesson> {
    return when (category) {
        "Alphabet" -> listOf(
            Lesson(title = "Letter A"),
            Lesson(title = "Letter B"),
            Lesson(title = "Letter C")
        )
        "Common Words" -> listOf(
            Lesson(title = "Hello"),
            Lesson(title = "Thank You"),
            Lesson(title = "Please")
        )
        "Basic Phrases" -> listOf(
            Lesson(title = "How Are You?"),
            Lesson(title = "My Name Is")
        )
        "Emotions" -> listOf(
            Lesson(title = "Happy"),
            Lesson(title = "Sad")
        )
        "Numbers" -> listOf(
            Lesson(title = "Number 1"),
            Lesson(title = "Number 2"),
            Lesson(title = "Number 3")
        )
        else -> listOf(
            Lesson(title = "Default Lesson")
        )
    }
}

fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        "Alphabet" -> Icons.Filled.Menu
        "Common Words" -> Icons.AutoMirrored.Default.Chat
        "Basic Phrases" -> Icons.Filled.QuestionMark
        "Emotions" -> Icons.Filled.Mood
        "Numbers" -> Icons.Filled.FormatListNumbered
        else -> Icons.AutoMirrored.Default.List
    }
}