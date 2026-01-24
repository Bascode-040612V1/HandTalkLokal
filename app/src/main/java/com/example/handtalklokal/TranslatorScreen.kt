package com.example.handtalklokal

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.zIndex
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.material3.ExperimentalMaterial3Api
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.PermissionStatus
import kotlinx.coroutines.launch
import java.util.*
import com.example.handtalklokal.components.TutorialOverlay
import androidx.compose.runtime.LaunchedEffect

// Updated dialect mapping excluding English and Filipino
fun getDialectName(code: String): String {
    return when (code) {
        "tl" -> "Tagalog"
        "hil" -> "Hiligaynon"
        "ceb" -> "Cebuano"
        "mrn" -> "Maranao"
        else -> "Tagalog"  // Default to Tagalog
    }
}

// Dialect data class for the selection dialog
data class DialectOption(
    val code: String,
    val name: String,
    val displayName: String
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SignLanguageTranslatorScreenWithFeatures(
    navController: NavHostController,
    viewModel: TranslatorViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val recognizedText by viewModel.recognizedText.collectAsState()
    val currentPhrase by viewModel.currentPhrase.collectAsState()
    val sentenceHistory by viewModel.sentenceHistory.collectAsState(initial = emptyList())
    val isRecording by viewModel.isRecording.collectAsState()
    val selectedDialect by viewModel.selectedDialect.collectAsState()
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()
    val landmarks by viewModel.landmarks.collectAsState() // Collect landmark data
    val listState = rememberLazyListState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    // Monitor permission state changes to advance tutorial when granted
    LaunchedEffect(cameraPermissionState.status) {
        // For single permission state, check the status directly
        if (cameraPermissionState.status == PermissionStatus.Granted) {
            viewModel.onCameraPermissionGranted()
        }
    }
    
    // Tutorial state
    val showTutorial by viewModel.showTutorial.collectAsState()
    val currentTutorialStep by viewModel.currentTutorialStep.collectAsState()
    
    // Declare showDialog state at the composable level so it's accessible to both content and buttons
    var showDialog by remember { mutableStateOf(false) }
    
    // Define available dialects for the selection dialog (excluding English and Filipino)
    val dialectOptions = listOf(
        DialectOption("tl", "Tagalog", "Tagalog"),
        DialectOption("hil", "Hiligaynon", "Hiligaynon"),
        DialectOption("ceb", "Cebuano", "Cebuano"),
        DialectOption("mrn", "Maranao", "Maranao")
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            // Wrap entire content in a Box to allow absolute positioning of buttons
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Title and main content column
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Modern title at the top of the screen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
                    ) {
                        Text(
                            text = "Sign Language Translator",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        // Information icon positioned at top-right corner below the title
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            IconButton(
                                onClick = {
                                    // Start tutorial from step 1 (camera permission) and skip welcome step
                                    viewModel.startTutorialFromStep1()
                                },
                                modifier = Modifier
                                    .size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Show Tutorial",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                
                    // Main content column that can scroll if needed
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        
                        // Camera Preview
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp)
                                .padding(bottom = 16.dp)
                                .onGloballyPositioned { coordinates ->
                                    // Capture camera preview bounds for tutorial
                                    if (showTutorial) {
                                        viewModel.updateTutorialTargetBounds(
                                            TutorialTarget.CAMERA_PREVIEW,
                                            coordinates.boundsInRoot()
                                        )
                                    }
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            // Camera Preview Container with 1:1 aspect ratio
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f) // Square 1:1 aspect ratio
                                    .padding(16.dp)
                            ) {
                                // Declare camera provider state outside AndroidView
                                var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
                                val executor = remember { ContextCompat.getMainExecutor(context) }
                                
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    // Direct Camera Preview - no wrapper boxes
                                    AndroidView(
                                        factory = { ctx ->
                                            val previewView = PreviewView(ctx)
                                            previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
                                            previewView
                                        },
                                        update = { previewView ->
                                            // Function to bind camera with current settings
                                            fun bindCamera() {
                                                cameraProvider?.let { provider ->
                                                    // Select camera based on state
                                                    val cameraSelector = if (isFrontCamera) {
                                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                                    } else {
                                                        CameraSelector.DEFAULT_BACK_CAMERA
                                                    }
                                                    
                                                    // Configure preview with square resolution
                                                    val preview = androidx.camera.core.Preview.Builder()
                                                        .setTargetResolution(android.util.Size(480, 480)) // Square resolution for 1:1 aspect ratio
                                                        .build()
                                                    
                                                    
                                                    // Set proper target rotation to match device orientation
                                                    preview.setTargetRotation(previewView.display.rotation)
                                                    preview.setSurfaceProvider(previewView.surfaceProvider)
                                                    
                                                    // Configure image analysis use case for processing frames
                                                    val imageAnalyzer = ImageAnalysis.Builder()
                                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                                        .build()
                                                    
                                                    imageAnalyzer.setAnalyzer(executor) { image ->
                                                        viewModel.processImage(image)
                                                    }
                                                    
                                                    try {
                                                        // Unbind use cases before rebinding
                                                        provider.unbindAll()
                                                        
                                                        // Bind use cases to camera
                                                        provider.bindToLifecycle(
                                                            lifecycleOwner, cameraSelector, preview, imageAnalyzer
                                                        )
                                                    } catch (exc: Exception) {
                                                        Log.e("CameraPreview", "Camera binding error", exc)
                                                    }
                                                }
                                            }
                                            
                                            // Initialize camera provider if not already done
                                            if (cameraProvider == null) {
                                                val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
                                                cameraProviderFuture.addListener({
                                                    cameraProvider = cameraProviderFuture.get()
                                                    bindCamera() // Bind camera with initial settings
                                                }, executor)
                                            } else {
                                                // Re-bind camera with new settings when isFrontCamera changes
                                                bindCamera()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().aspectRatio(1f) // Constrain to 1:1 visible area
                                    )
                                    
                                    // Camera switch button - positioned at the bottom center
                                    Button(
                                        onClick = { viewModel.switchCamera() },
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(16.dp)
                                            .size(56.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Black.copy(alpha = 0.7f),
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = CircleShape
                                    ) {
                                        Icon(
                                            imageVector = if (isFrontCamera) Icons.Default.CameraFront else Icons.Default.CameraRear,
                                            contentDescription = if (isFrontCamera) "Switch to back camera" else "Switch to front camera",
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    
                                    // Recording indicator
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .align(Alignment.TopEnd)
                                            .background(Color.Red, CircleShape)
                                    )
                                } else {
                                    // Permission denied UI
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 16.dp, end = 16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VideocamOff,
                                                contentDescription = "Camera off",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            
                                            Text(
                                                text = "Camera permission required",
                                                style = MaterialTheme.typography.headlineSmall,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                                            )
                                            
                                            Text(
                                                text = "Please grant camera permission to use the sign language translator",
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(bottom = 16.dp)
                                            )
                                            
                                            Button(
                                                onClick = {
                                                    cameraPermissionState.launchPermissionRequest()
                                                },
                                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                                    // Capture permission button bounds for tutorial
                                                    if (showTutorial) {
                                                        viewModel.updateTutorialTargetBounds(
                                                            TutorialTarget.CAMERA_PERMISSION_BUTTON,
                                                            coordinates.boundsInRoot()
                                                        )
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            ) {
                                                Text("Grant Camera Permission")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Current Phrase and Sentence History
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp)
                                .padding(bottom = 8.dp)
                                .onGloballyPositioned { coordinates ->
                                    // Capture sentence output bounds for tutorial
                                    if (showTutorial) {
                                        viewModel.updateTutorialTargetBounds(
                                            TutorialTarget.SENTENCE_OUTPUT,
                                            coordinates.boundsInRoot()
                                        )
                                    }
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Removed Current Phrase box as requested
                                // Only showing Completed Sentences box
                                
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(75.dp)
                                ) {
                                    items(sentenceHistory.reversed()) { sentence ->  // Show in reverse order (newest first)
                                        // The sentence is already translated in the ViewModel, so display as-is
                                        Text(
                                            text = sentence,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Fixed button row at the bottom of the screen, above the navigation bar
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clear Sentence button - wrapped in Box to prevent compression
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 100.dp)
                            .onGloballyPositioned { coordinates ->
                                // Capture clear button bounds for tutorial
                                if (showTutorial) {
                                    viewModel.updateTutorialTargetBounds(
                                        TutorialTarget.CLEAR_BUTTON,
                                        coordinates.boundsInRoot()
                                    )
                                }
                            }
                    ) {
                        Button(
                            onClick = {
                                viewModel.clearSentenceHistory()
                                if (showTutorial && currentTutorialStep == 5) {
                                    // In tutorial mode on step 5, clicking the clear button advances to next step
                                    viewModel.nextTutorialStep()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Clear Sentence",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    
                    // Select Dialect button - takes remaining space
                    Button(
                        onClick = {
                            if (showTutorial && currentTutorialStep == 3) {
                                // In tutorial mode, clicking the dialect button advances to step 4 (dialect selection)
                                viewModel.nextTutorialStep() // This moves to step 4
                                // Also show the dialog for step 4
                                showDialog = true
                            } else {
                                // Outside tutorial, just show the dialog
                                showDialog = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                // Capture dialect button bounds for tutorial
                                if (showTutorial) {
                                    viewModel.updateTutorialTargetBounds(
                                        TutorialTarget.DIALECT_BUTTON,
                                        coordinates.boundsInRoot()
                                    )
                                }
                            },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = getDialectName(selectedDialect),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand"
                            )
                        }
                    }
                }
                
                // Custom Dialect Selection Modal
                if (showDialog) {
                    DialectSelectionModal(
                        onDismiss = { showDialog = false },
                        onDialectSelected = { dialectCode ->
                            viewModel.setDialect(dialectCode)
                            if (showTutorial && currentTutorialStep == 4) {
                                // In tutorial mode on step 4, selecting a dialect advances to next step
                                viewModel.nextTutorialStep()
                                // Dismiss the dialog after advancing
                                showDialog = false
                            } else {
                                showDialog = false
                            }
                        },
                        dialectOptions = dialectOptions,
                        selectedDialect = selectedDialect,
                        isInTutorial = showTutorial && currentTutorialStep == 4,
                        viewModel = viewModel
                    )
                }
            }
        }
        
        // Tutorial Overlay - rendered on top of everything
        TutorialOverlay(
            viewModel = viewModel,
            currentStep = currentTutorialStep,
            showTutorial = showTutorial,
            onStepComplete = { step ->
                when (step) {
                    0 -> {
                        // Request permission and move to next step if granted
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.nextTutorialStep()
                        }
                    }
                    4 -> {
                        // Move to next step after dialect selection
                        // Dismiss the dialog before advancing
                        showDialog = false
                        viewModel.nextTutorialStep()
                    }
                }
            },
            onTutorialFinish = {
                viewModel.finishTutorial()
            },
            onTutorialNext = {
                viewModel.nextTutorialStep()
            },
            onDialectButtonClicked = {
                viewModel.setCurrentTutorialStep(4) // Move to dialect selection step
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun DialectSelectionModal(
    onDismiss: () -> Unit,
    onDialectSelected: (String) -> Unit,
    dialectOptions: List<DialectOption>,
    selectedDialect: String,
    isInTutorial: Boolean,
    viewModel: TranslatorViewModel? = null
) {
    // Capture the modal's bounds for tutorial overlay
    var modalBounds by remember { mutableStateOf<Rect?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)) // Semi-transparent background
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (!isInTutorial) { // Don't dismiss during tutorial
                    onDismiss()
                }
            }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.8f) // 80% of screen width
                .onGloballyPositioned { coordinates ->
                    modalBounds = coordinates.boundsInRoot()
                    // Update tutorial bounds if in tutorial mode and viewModel is provided
                    if (isInTutorial && viewModel != null && modalBounds != null) {
                        viewModel.updateTutorialTargetBounds(TutorialTarget.MODAL_DIALOG, modalBounds!!)
                    }
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Dialect",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dialectOptions) { dialect ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDialectSelected(dialect.code)
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedDialect == dialect.code) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedDialect == dialect.code,
                                    onClick = {
                                        onDialectSelected(dialect.code)
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = dialect.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (selectedDialect == dialect.code) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
                
                if (!isInTutorial) { // Only show cancel button outside tutorial mode
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}