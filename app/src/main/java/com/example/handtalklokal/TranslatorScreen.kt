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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.zIndex
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
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
import kotlinx.coroutines.launch
import java.util.*

fun getDialectName(code: String): String {
    return when (code) {
        "tl" -> "Tagalog"
        "hil" -> "Hiligaynon"
        "ceb" -> "Cebuano"
        "mrn" -> "Maranao"
        else -> "Tagalog"  // Default to Tagalog
    }
}

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
    
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Modern title at the top of the screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Sign Language Translator",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Declare showDialog state at column level for accessibility
            var showDialog by remember { mutableStateOf(false) }
            
            // Camera Preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
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
                            modifier = Modifier.fillMaxSize() // Fill the 1:1 container
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
                        
// Dialect selector button moved below camera preview
                        
                        // Recording indicator
                        if (true) { // Always show recording indicator since camera is always active
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Red, CircleShape)
                            )
                        }
                    } else {
                        // Permission denied UI
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
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
                                    onClick = { cameraPermissionState.launchPermissionRequest() },
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
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
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
                            .height(150.dp)
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
            
            // Combined button row - Clear Sentence and Select Dialect side by side (now below sentence history)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clear Sentence button - positioned to the left, wraps content
                Button(
                    onClick = { viewModel.clearSentenceHistory() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text(
                        text = "Clear Sentence",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                
                // Select Dialect button - positioned to the right, fills remaining space
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.weight(1f), // Take remaining space
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
            
            // Dialect Selection Dialog
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = {
                        Text(
                            text = "Select Dialect",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    text = {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(listOf("tl", "hil", "ceb", "mrn")) { dialectCode ->
                                TextButton(
                                    onClick = {
                                        viewModel.setDialect(dialectCode)
                                        showDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = getDialectName(dialectCode),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            // Current Phrase and Sentence History
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
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
                            .height(150.dp)
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
}

