package com.example.handtalklokal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandSignTutorialsScreenWithFeatures(navController: NavHostController) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var currentLessonIndex by remember { mutableStateOf(0) }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Hand Sign Tutorials",
                        style = MaterialTheme.typography.headlineSmall
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = { 
                            if (selectedCategory != null) {
                                selectedCategory = null
                                currentLessonIndex = 0
                            } else {
                                navController.popBackStack()
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        if (selectedCategory == null) {
            // Category Selection Screen
            CategorySelectionScreen(
                modifier = Modifier.padding(innerPadding),
                onCategorySelected = { category -> 
                    selectedCategory = category
                    currentLessonIndex = 0
                }
            )
        } else {
            // Lesson Screen
            LessonScreen(
                modifier = Modifier.padding(innerPadding),
                category = selectedCategory!!,
                lessonIndex = currentLessonIndex,
                onNavigateToLesson = { index -> currentLessonIndex = index },
                totalLessons = getLessonsForCategory(selectedCategory!!).size
            )
        }
    }
}

@Composable
fun CategorySelectionScreen(
    modifier: Modifier = Modifier,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf(
        "Alphabet" to "Learn the alphabet in sign language",
        "Common Words" to "Essential words for daily communication",
        "Basic Phrases" to "Useful phrases for conversations",
        "Emotions" to "Express feelings through signs",
        "Numbers" to "Counting and mathematical signs"
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
                        .clickable { onCategorySelected(category.first) },
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
                            imageVector = getCategoryIcon(category.first),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(end = 16.dp)
                        )
                        
                        Column {
                            Text(
                                text = category.first,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = category.second,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
    totalLessons: Int
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
            text = "$category - Lesson ${lessonIndex + 1}",
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
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = currentLesson.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = currentLesson.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
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
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
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
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Next",
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowForward, 
                        contentDescription = "Next"
                    )
                }
            }

        }
    }
}

data class Lesson(
    val title: String,
    val description: String
)

fun getLessonsForCategory(category: String): List<Lesson> {
    return when (category) {
        "Alphabet" -> listOf(
            Lesson(
                title = "Letter A",
                description = "Form the letter A by making a fist with your thumb resting against your palm. Extend your index and middle fingers straight up, keeping them close together."
            ),
            Lesson(
                title = "Letter B",
                description = "Form the letter B by extending all four fingers straight up while tucking your thumb across your palm."
            ),
            Lesson(
                title = "Letter C",
                description = "Form the letter C by curving all your fingers and thumb to create a 'C' shape."
            )
        )
        "Common Words" -> listOf(
            Lesson(
                title = "Hello",
                description = "Extend your fingers and cross your thumb over your palm. Starting with your hand in a fist, extend your fingers and twist your hand forward twice."
            ),
            Lesson(
                title = "Thank You",
                description = "Place your fingertips on your chin and move your hand forward and slightly down, as if offering something."
            ),
            Lesson(
                title = "Please",
                description = "Place your flat hand over your chest and move it in a circular motion."
            )
        )
        "Basic Phrases" -> listOf(
            Lesson(
                title = "How Are You?",
                description = "Combine the signs for 'How' and 'You' with appropriate facial expressions."
            ),
            Lesson(
                title = "My Name Is",
                description = "Point to yourself, then spell out your name using the alphabet signs."
            )
        )
        "Emotions" -> listOf(
            Lesson(
                title = "Happy",
                description = "Place both hands on your chest with palms facing you, then lift them up and away from your body while opening your hands."
            ),
            Lesson(
                title = "Sad",
                description = "Place both hands on your cheeks with fingers pointing down, then pull your hands down while frowning."
            )
        )
        "Numbers" -> listOf(
            Lesson(
                title = "Number 1",
                description = "Extend just your index finger while keeping the rest of your fingers in a fist."
            ),
            Lesson(
                title = "Number 2",
                description = "Extend your index and middle fingers while keeping the rest in a fist."
            ),
            Lesson(
                title = "Number 3",
                description = "Extend your index, middle, and ring fingers while keeping the rest in a fist."
            )
        )
        else -> listOf(
            Lesson(
                title = "Default Lesson",
                description = "This is a default lesson description."
            )
        )
    }
}

fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        "Alphabet" -> Icons.Filled.Menu
        "Common Words" -> Icons.AutoMirrored.Filled.Chat
        "Basic Phrases" -> Icons.Filled.QuestionMark
        "Emotions" -> Icons.Filled.Mood
        "Numbers" -> Icons.Filled.FormatListNumbered
        else -> Icons.Filled.List
    }
}