@file:OptIn(ExperimentalAnimationApi::class)

package com.example.handtalklokal.components

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.handtalklokal.TutorialTarget
import com.example.handtalklokal.TranslatorViewModel
import com.example.handtalklokal.ui.theme.Blue80
import androidx.compose.animation.ExperimentalAnimationApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Tutorial overlay component that displays step-by-step instructions for the app
 * NOTE: Do not add buttons to this tutorial overlay screen since there are already buttons in the translator screen
 */
@Composable
fun TutorialOverlay(
    viewModel: TranslatorViewModel,
    currentStep: Int,
    showTutorial: Boolean,
    onStepComplete: (Int) -> Unit,
    onTutorialFinish: () -> Unit,
    onTutorialNext: () -> Unit,
    onDialectButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tutorialTargetBounds by viewModel.tutorialTargetBounds.collectAsState()
    
    // Debug logging for bounds availability
    LaunchedEffect(currentStep, tutorialTargetBounds) {
        val targetRect = when (currentStep) {
            0 -> tutorialTargetBounds[TutorialTarget.CAMERA_PERMISSION_BUTTON]
            1 -> tutorialTargetBounds[TutorialTarget.CAMERA_PREVIEW]
            2 -> tutorialTargetBounds[TutorialTarget.SENTENCE_OUTPUT]
            3 -> tutorialTargetBounds[TutorialTarget.DIALECT_BUTTON]
            4 -> tutorialTargetBounds[TutorialTarget.DIALECT_BUTTON]
            5 -> tutorialTargetBounds[TutorialTarget.CLEAR_BUTTON]
            6 -> tutorialTargetBounds[TutorialTarget.BOTTOM_NAV]
            7 -> tutorialTargetBounds[TutorialTarget.BOTTOM_NAV]
            else -> null
        }
        Log.d("TutorialOverlay", "Step $currentStep - Target bounds available: ${targetRect != null}, Bounds: $targetRect")
    }
    
    // Increase volume and start gesture monitoring when reaching camera preview step
    LaunchedEffect(currentStep) {
        if (currentStep == 1) { // Camera preview step
            viewModel.increaseVolume()
            viewModel.startGestureMonitoringForTutorial()
        }
    }
    
    // Monitor sentence output changes during step 2 to advance tutorial
    val sentenceHistory by viewModel.sentenceHistory.collectAsState()
    LaunchedEffect(currentStep, sentenceHistory) {
        if (currentStep == 2 && sentenceHistory.isNotEmpty()) { // Sentence output step
            // Wait a brief moment to ensure the UI has updated
            delay(500)
            onTutorialNext() // Advance to next step
        }
    }
    
    // Add delay handler for step transitions
    var isTransitioning by remember { mutableStateOf(false) }
    val transitionDelay = 300L // 300ms delay between steps
    val scope = rememberCoroutineScope()
    
    if (showTutorial) {
        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            // Semi-transparent blue overlay with cut-out
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        alpha = 0.99f // Ensure proper rendering without affecting touch
                    }
            ) {
                // Draw blue overlay
                drawRect(
                    color = Blue80.copy(alpha = 0.7f),
                    size = size
                )
                
                // Handle cutout drawing differently for step 4 (dialect dialog)
                when (currentStep) {
                    4 -> {
                        var dialogRect = tutorialTargetBounds[TutorialTarget.MODAL_DIALOG] ?: run {
                            // Fallback to centered position if modal bounds not available
                            val dialogWidth = 280.dp.toPx()  // Approximate width of the dialog
                            val dialogHeight = 320.dp.toPx() // Approximate height of the dialog (adjust for actual content)
                            val centerX = size.width / 2
                            val centerY = size.height / 2 - 40.dp.toPx() // Move dialog cutout up by 40dp from center
                            
                            // Create a rectangle that is shifted upward on the bottom side only
                            // This shifts the entire cutout upward but more dramatically affects the lower portion
                            androidx.compose.ui.geometry.Rect(
                                centerX - dialogWidth / 2,
                                centerY - dialogHeight / 2 - 20.dp.toPx(),  // Move top edge up
                                centerX + dialogWidth / 2,
                                centerY + dialogHeight / 2 - 60.dp.toPx()  // Move bottom edge up more (60dp instead of 20dp)
                            )
                        }
                        
                        // Apply offset to the actual bounds as well to move the lower side up
                        if (tutorialTargetBounds[TutorialTarget.MODAL_DIALOG] != null) {
                            val originalRect = tutorialTargetBounds[TutorialTarget.MODAL_DIALOG]!!
                            val topOffset = 20.dp.toPx()  // Move top edge up by 20dp
                            val bottomOffset = 60.dp.toPx()  // Move bottom edge up by 60dp more
                            
                            dialogRect = androidx.compose.ui.geometry.Rect(
                                originalRect.left,
                                originalRect.top - topOffset,  // Move top up
                                originalRect.right,
                                originalRect.bottom - bottomOffset  // Move bottom up more
                            )
                        }
                        
                        Log.d("TutorialOverlay", "Drawing cutout for step $currentStep, dialogRect: $dialogRect")
                        
                        // Clear the rounded rectangular area for the highlight (create the "hole")
                        drawRoundRect(
                            color = Color.Transparent,
                            topLeft = dialogRect.topLeft,
                            size = dialogRect.size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx()), // Rounded corners with 12dp radius
                            blendMode = BlendMode.Clear
                        )
                        
                        // Draw dashed border around the highlight area with rounded corners
                        val path = Path().apply {
                            addRoundRect(
                                roundRect = androidx.compose.ui.geometry.RoundRect(
                                    rect = androidx.compose.ui.geometry.Rect(
                                        dialogRect.left - 4.dp.toPx(),
                                        dialogRect.top - 4.dp.toPx(),
                                        dialogRect.right + 4.dp.toPx(),
                                        dialogRect.bottom + 4.dp.toPx()
                                    ),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx())
                                )
                            )
                        }
                        
                        drawPath(
                            path = path,
                            color = Color.White,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        )
                    }
                    else -> {
                        // For all other steps, use the target bounds approach
                        val targetRect = when (currentStep) {
                            0 -> tutorialTargetBounds[TutorialTarget.CAMERA_PERMISSION_BUTTON]
                            1 -> tutorialTargetBounds[TutorialTarget.CAMERA_PREVIEW]
                            2 -> tutorialTargetBounds[TutorialTarget.SENTENCE_OUTPUT]
                            3 -> tutorialTargetBounds[TutorialTarget.DIALECT_BUTTON]
                            5 -> tutorialTargetBounds[TutorialTarget.CLEAR_BUTTON]
                            6 -> tutorialTargetBounds[TutorialTarget.BOTTOM_NAV]
                            7 -> tutorialTargetBounds[TutorialTarget.BOTTOM_NAV] // Finish step
                            else -> null
                        }
                        
                        Log.d("TutorialOverlay", "Drawing cutout for step $currentStep, targetRect: $targetRect")
                        
                        targetRect?.let { rect ->
                            // Calculate offset to move the hole based on current step
                            val offsetUp = when (currentStep) {
                                0 -> 40.dp.toPx() // Camera permission button needs more upward adjustment
                                1 -> 41.dp.toPx() // Camera preview step
                                2 -> 38.dp.toPx() // Sentence output step - more upward adjustment
                                3 -> 40.dp.toPx() // Dialect selection button - upward adjustment
                                5 -> 40.dp.toPx() // Clear sentence button - upward adjustment
                                else -> 0.dp.toPx() // Other steps don't need upward adjustment
                            }
                            
                            // Calculate horizontal offset if needed
                            val offsetLeft = when (currentStep) {
                                1 -> 0.dp.toPx() // Camera preview - center the cutout
                                else -> 0.dp.toPx()
                            }
                            
                            // Clear the rounded rectangular area for the highlight (create the "hole")
                            val adjustedRect = androidx.compose.ui.geometry.Rect(
                                rect.left + offsetLeft,
                                rect.top - offsetUp,
                                rect.right + offsetLeft,
                                rect.bottom - offsetUp
                            )
                            
                            Log.d("TutorialOverlay", "Adjusted rect for cutout: $adjustedRect")
                            
                            drawRoundRect(
                                color = Color.Transparent,
                                topLeft = adjustedRect.topLeft,
                                size = adjustedRect.size,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx()), // Rounded corners with 12dp radius
                                blendMode = BlendMode.Clear
                            )
                            
                            // Draw dashed border around the highlight area with rounded corners
                            val path = Path().apply {
                                addRoundRect(
                                    roundRect = androidx.compose.ui.geometry.RoundRect(
                                        rect = androidx.compose.ui.geometry.Rect(
                                            rect.left - 4.dp.toPx() + offsetLeft,
                                            rect.top - 4.dp.toPx() - offsetUp,
                                            rect.right + 4.dp.toPx() + offsetLeft,
                                            rect.bottom + 4.dp.toPx() - offsetUp
                                        ),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx())
                                    )
                                )
                            }
                            
                            drawPath(
                                path = path,
                                color = Color.White,
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            )
                        } ?: run {
                            Log.w("TutorialOverlay", "No target rect found for step $currentStep")
                        }
                    }
                }
            }

            // Always show content for responsiveness, regardless of bounds availability
            if (true) {
                // Get the target rect for positioning the tutorial content
                val targetRect = when (currentStep) {
                    0 -> tutorialTargetBounds[TutorialTarget.CAMERA_PERMISSION_BUTTON]
                    1 -> tutorialTargetBounds[TutorialTarget.CAMERA_PREVIEW]
                    2 -> tutorialTargetBounds[TutorialTarget.SENTENCE_OUTPUT]
                    3 -> tutorialTargetBounds[TutorialTarget.DIALECT_BUTTON]
                    4 -> {
                        // For step 4, try to use modal bounds if available, otherwise fall back to dialect button
                        tutorialTargetBounds[TutorialTarget.MODAL_DIALOG] ?: tutorialTargetBounds[TutorialTarget.DIALECT_BUTTON]
                    }
                    5 -> tutorialTargetBounds[TutorialTarget.CLEAR_BUTTON]
                    6 -> tutorialTargetBounds[TutorialTarget.BOTTOM_NAV]
                    7 -> tutorialTargetBounds[TutorialTarget.BOTTOM_NAV]
                    else -> null
                }
                
                // Calculate the position for the tutorial content based on the target rect
                val contentAlignment = when (currentStep) {
                    0 -> Alignment.TopCenter // Camera permission button - show content above it
                    1 -> Alignment.BottomCenter // Camera preview - show content below it
                    2 -> Alignment.TopCenter // Sentence output box - show content above it
                    3 -> Alignment.TopCenter // Dialect button - show content above it (so cutout remains visible)
                    4 -> Alignment.TopCenter // Dialect dialog - show above the dialog cutout
                    5 -> Alignment.TopCenter // Clear button - show content above it
                    6 -> Alignment.BottomCenter // Bottom nav - show content below it
                    7 -> Alignment.Center // Finish step - show in center
                    else -> Alignment.Center
                }
                
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = contentAlignment
                ) {
                    when (currentStep) {
                        0 -> Step0PermissionTutorial(
                            onGrantPermission = { 
                                if (!isTransitioning) {
                                    isTransitioning = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(transitionDelay)
                                        onStepComplete(0)
                                        isTransitioning = false
                                    }
                                }
                            },
                            targetRect = targetRect
                        )
                        1 -> Step1CameraTutorial(
                            onNext = { 
                                if (!isTransitioning) {
                                    isTransitioning = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(transitionDelay)
                                        onTutorialNext()
                                        isTransitioning = false
                                    }
                                }
                            },
                            targetRect = targetRect
                        )
                        2 -> Step2OutputTutorial(
                            onNext = { 
                                if (!isTransitioning) {
                                    isTransitioning = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(transitionDelay)
                                        onTutorialNext()
                                        isTransitioning = false
                                    }
                                }
                            },
                            targetRect = targetRect
                        )
                        3 -> Step3DialectTutorial(
                            onDialectButtonClicked = { 
                                if (!isTransitioning) {
                                    isTransitioning = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(transitionDelay)
                                        onTutorialNext() // Go to next step (dialect popup)
                                        isTransitioning = false
                                    }
                                }
                            },
                            targetRect = targetRect
                        )
                        4 -> Step4DialectDialogTutorial(
                            onDialectSelected = { 
                                if (!isTransitioning) {
                                    isTransitioning = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(transitionDelay)
                                        onStepComplete(4) // Move to the next step (clear button)
                                        isTransitioning = false
                                    }
                                }
                            },
                            targetRect = targetRect
                        )
                        5 -> Step5ClearButtonTutorial(
                            onNext = { 
                                if (!isTransitioning) {
                                    isTransitioning = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(transitionDelay)
                                        onTutorialNext()
                                        isTransitioning = false
                                    }
                                }
                            },
                            targetRect = targetRect
                        )
                        6 -> Step6NavigationTutorial(
                            onNext = { 
                                if (!isTransitioning) {
                                    isTransitioning = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(transitionDelay)
                                        onTutorialNext()
                                        isTransitioning = false
                                    }
                                }
                            },
                            targetRect = targetRect
                        )
                        7 -> Step7FinishTutorial(
                            onFinish = { 
                                if (!isTransitioning) {
                                    isTransitioning = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(transitionDelay)
                                        onTutorialFinish()
                                        isTransitioning = false
                                    }
                                }
                            },
                            targetRect = targetRect
                        )
                        else -> {
                            // Default center alignment for other cases
                            StepPlaceholderTutorial(
                                title = "Step $currentStep",
                                description = "Tutorial step $currentStep content",
                                onNext = { 
                                    if (!isTransitioning) {
                                        isTransitioning = true
                                        scope.launch {
                                            kotlinx.coroutines.delay(transitionDelay)
                                            onTutorialNext()
                                            isTransitioning = false
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Individual step tutorials would be defined here - placeholders for now
@Composable
fun Step0PermissionTutorial(onGrantPermission: () -> Unit, targetRect: Rect?) {
    Box(modifier = Modifier
        .background(Color.White, RoundedCornerShape(8.dp))
        .padding(16.dp)) {
        Column {
            Text("Welcome to Hand Talk Lokal!", fontSize = 20.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text("First, we need camera permission to detect hand gestures.", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            // Note: Do not add buttons to this tutorial overlay screen since there are already buttons in the translator screen
        }
    }
}

@Composable
fun Step1CameraTutorial(onNext: () -> Unit, targetRect: Rect?) {
    Box(modifier = Modifier
        .background(Color.White, RoundedCornerShape(8.dp))
        .padding(16.dp)) {
        Column {
            Text("Camera Preview", fontSize = 20.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Position your hands in the camera view to start detecting gestures.", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            // Note: Do not add buttons to this tutorial overlay screen since there are already buttons in the translator screen
            Button(onClick = onNext) {
                Text("Next")
            }
        }
    }
}

@Composable
fun Step2OutputTutorial(onNext: () -> Unit, targetRect: Rect?) {
    Box(modifier = Modifier
        .background(Color.White, RoundedCornerShape(8.dp))
        .padding(16.dp)) {
        Column {
            Text("Sentence Output", fontSize = 20.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Detected signs will appear here as text that can be converted to speech.", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            // Note: Do not add buttons to this tutorial overlay screen since there are already buttons in the translator screen
            Button(onClick = onNext) {
                Text("Next")
            }
        }
    }
}

@Composable
fun Step3DialectTutorial(onDialectButtonClicked: () -> Unit, targetRect: Rect?) {
    Box(modifier = Modifier
        .background(Color.White, RoundedCornerShape(8.dp))
        .padding(16.dp)) {
        Column {
            Text("Select Dialect", fontSize = 20.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tap the dialect button to choose your preferred dialect for translation.", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            // Note: Do not add buttons to this tutorial overlay screen since there are already buttons in the translator screen
            Button(onClick = onDialectButtonClicked) {
                Text("Next")
            }
        }
    }
}

@Composable
fun Step4DialectDialogTutorial(onDialectSelected: () -> Unit, targetRect: Rect?) {
    Box(modifier = Modifier
        .background(Color.White, RoundedCornerShape(8.dp))
        .padding(16.dp)) {
        Column {
            Text("Choose Dialect", fontSize = 20.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Select a dialect you want from the options displayed.", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            // Note: Do not add buttons to this tutorial overlay screen since there are already buttons in the translator screen
            Button(onClick = onDialectSelected) {
                Text("Next")
            }
        }
    }
}

@Composable
fun Step5ClearButtonTutorial(onNext: () -> Unit, targetRect: Rect?) {
    Box(modifier = Modifier
        .background(Color.White, RoundedCornerShape(8.dp))
        .padding(16.dp)) {
        Column {
            Text("Clear Button", fontSize = 20.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Use this button to clear the current translation.", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            // Note: Do not add buttons to this tutorial overlay screen since there are already buttons in the translator screen
            Button(onClick = onNext) {
                Text("Next")
            }
        }
    }
}

@Composable
fun Step6NavigationTutorial(onNext: () -> Unit, targetRect: Rect?) {
    Box(modifier = Modifier
        .background(Color.White, RoundedCornerShape(8.dp))
        .padding(16.dp)) {
        Column {
            Text("Navigation", fontSize = 20.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Use the bottom navigation to switch between different app features.", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            // Note: Do not add buttons to this tutorial overlay screen since there are already buttons in the translator screen
            Button(onClick = onNext) {
                Text("Next")
            }
        }
    }
}

@Composable
fun Step7FinishTutorial(onFinish: () -> Unit, targetRect: Rect?) {
    Box(modifier = Modifier
        .background(Color.White, RoundedCornerShape(8.dp))
        .padding(16.dp)) {
        Column {
            Text("Tutorial Complete!", fontSize = 20.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text("You're now ready to use Hand Talk Lokal. Enjoy!", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            // Note: Do not add buttons to this tutorial overlay screen since there are already buttons in the translator screen
            Button(onClick = onFinish) {
                Text("Finish")
            }
        }
    }
}

@Composable
fun StepPlaceholderTutorial(title: String, description: String, onNext: () -> Unit) {
    Box(modifier = Modifier
        .background(Color.White, RoundedCornerShape(8.dp))
        .padding(16.dp)) {
        Column {
            Text(title, fontSize = 20.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            // Note: Do not add buttons to this tutorial overlay screen since there are already buttons in the translator screen
            Button(onClick = onNext) {
                Text("Next")
            }
        }
    }
}