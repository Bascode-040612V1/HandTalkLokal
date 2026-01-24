@file:OptIn(ExperimentalAnimationApi::class)

package com.example.handtalklokal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.handtalklokal.ui.theme.HandTalkLokalTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.ExperimentalAnimationApi
import com.example.handtalklokal.components.TutorialOverlay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HandTalkLokalTheme {
                HandTalkApp()
            }
        }
    }
}

@Composable
fun HandTalkApp() {
    val navController = rememberNavController()
    val viewModel: TranslatorViewModel = viewModel()
    
    // Removed automatic tutorial start - tutorial will be shown based on completion state
    // LaunchedEffect(Unit) {
    //     viewModel.startTutorial()
    // }
    
    Scaffold(
        bottomBar = { 
            BottomNavigationBar(
                navController = navController,
                viewModel = viewModel
            ) 
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "translator",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("translator") { 
                SignLanguageTranslatorScreenWithFeatures(navController, viewModel) 
            }
            composable("tutorials") { 
                // Navigate to the actual tutorial screen implementation
                // The actual implementation is in TutorialScreen.kt
                HandSignTutorialsScreenWithFeatures(navController)
            }
        }
    }
}

// Bottom Navigation Bar

data class BottomNavItem(val route: String, val icon: ImageVector, val label: String)

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    viewModel: TranslatorViewModel
) {
    val items = listOf(
        BottomNavItem("translator", Icons.Default.Translate, "Translator"),
        BottomNavItem("tutorials", Icons.AutoMirrored.Default.MenuBook, "Tutorial")
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Tutorial state to access within the composable
    val showTutorial by viewModel.showTutorial.collectAsState()
    val currentTutorialStep by viewModel.currentTutorialStep.collectAsState()
    
    // Custom Navigation Bar implementation
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                // Capture bottom navigation bounds for tutorial
                if (showTutorial) {
                    viewModel.updateTutorialTargetBounds(
                        TutorialTarget.BOTTOM_NAV,
                        coordinates.boundsInRoot()
                    )
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item.route == currentRoute
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (showTutorial && currentTutorialStep == 6) {
                                // In tutorial mode on step 6, clicking navigation finishes the tutorial
                                viewModel.finishTutorial()
                            } else {
                                // Normal navigation behavior outside tutorial
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animate the transition between icon and text
                    AnimatedContent(
                        targetState = isSelected,
                        transitionSpec = {
                            if (targetState) {
                                (slideInVertically(
                                    animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)
                                ) + fadeIn(
                                    animationSpec = tween(durationMillis = 400, delayMillis = 200)
                                )) togetherWith
                                (slideOutVertically(
                                    animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)
                                ) + fadeOut(
                                    animationSpec = tween(durationMillis = 400)
                                ))
                            } else {
                                (slideInVertically(
                                    animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)
                                ) + fadeIn(
                                    animationSpec = tween(durationMillis = 400, delayMillis = 200)
                                )) togetherWith
                                (slideOutVertically(
                                    animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)
                                ) + fadeOut(
                                    animationSpec = tween(durationMillis = 400)
                                ))
                            }
                        },
                        label = "Icon/Text Transition"
                    ) { isActive ->
                        if (isActive) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Blue underline indicator
                    if (isSelected) {
                        Spacer(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .width(40.dp)
                                .height(3.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(11.dp))
                    }
                }
            }
        }
    }
}

// Placeholder screens - will implement fully in next steps
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignLanguageTranslatorScreen(navController: NavHostController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Sign Language Translator") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sign Language to Speech Translator",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "This feature will use your camera to recognize hand signs and convert them to speech.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}