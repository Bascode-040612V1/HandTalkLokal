@file:OptIn(ExperimentalAnimationApi::class)

package com.example.handtalklokal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "translator",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("translator") { 
                val viewModel: TranslatorViewModel = viewModel()
                SignLanguageTranslatorScreenWithFeatures(navController, viewModel) 
            }
            composable("tutorials") { HandSignTutorialsScreenWithFeatures(navController) }
        }
    }
}

// Bottom Navigation Bar

data class BottomNavItem(val route: String, val icon: ImageVector, val label: String)

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("translator", Icons.Default.Translate, "Translator"),
        BottomNavItem("tutorials", Icons.Default.MenuBook, "Tutorial")
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Custom Navigation Bar implementation
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
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
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
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
                                ) { height -> height * 3 } + fadeIn(
                                    animationSpec = tween(durationMillis = 400, delayMillis = 200)
                                )) with
                                (slideOutVertically(
                                    animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)
                                ) { height -> -height * 3 } + fadeOut(
                                    animationSpec = tween(durationMillis = 400)
                                ))
                            } else {
                                (slideInVertically(
                                    animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)
                                ) { height -> -height * 3 } + fadeIn(
                                    animationSpec = tween(durationMillis = 400, delayMillis = 200)
                                )) with
                                (slideOutVertically(
                                    animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)
                                ) { height -> height * 3 } + fadeOut(
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandSignTutorialsScreen(navController: NavHostController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Hand Sign Tutorials") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                text = "Basic Hand Sign Tutorials",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Learn basic hand signs through interactive tutorials.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}