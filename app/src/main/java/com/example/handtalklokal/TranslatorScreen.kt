package com.example.handtalklokal

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
        "en" -> "English"
        "fil" -> "Filipino"
        "hil" -> "Hiligaynon"
        "ceb" -> "Cebuano"
        "mrn" -> "Maranao"
        else -> "English"
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
    val sentenceHistory by viewModel.sentenceHistory.collectAsState(initial = emptyList())
    val isRecording by viewModel.isRecording.collectAsState()
    val selectedDialect by viewModel.selectedDialect.collectAsState()
    val listState = rememberLazyListState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Sign Language Translator",
                        style = MaterialTheme.typography.headlineSmall
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Dialect Selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                    Text(
                        text = "Dialect:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    var expanded by remember { mutableStateOf(false) }
                    val dialects = listOf(
                        "en" to "English",
                        "fil" to "Filipino",
                        "hil" to "Hiligaynon",
                        "ceb" to "Cebuano",
                        "mrn" to "Maranao"
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = dialects.find { it.first == selectedDialect }?.second ?: "English",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            dialects.forEach { dialect ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = dialect.second,
                                            style = MaterialTheme.typography.bodyMedium
                                        ) 
                                    },
                                    onClick = {
                                        viewModel.setDialect(dialect.first)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f) // Ensure 1:1 aspect ratio for the container
                        .padding(16.dp)
                ) {
                    // Debug: Show current permission state
                    /*
                    Text(
                        text = "Permission state: ${cameraPermissionState.status}",
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    */
                    
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        // Actual Camera Preview
                        CameraPreview(
                            isRecording = true, // Always recording when permission is granted
                            onImageCaptured = { image ->
                                // Process the captured image
                                viewModel.processImage(image)
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
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
            
            // Recognized Text Display
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
                    Text(
                        text = "Recognized Gesture:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = recognizedText.ifEmpty { "Waiting for gesture..." },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Sentence History
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
                    Text(
                        text = "Sentence History:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        items(sentenceHistory) { sentence ->
                            Text(
                                text = sentence,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            // Control Buttons - Replaced with Dialect Selector
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dialect:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = false,
                        onExpandedChange = { /* Handle menu expansion */ }
                    ) {
                        OutlinedButton(
                            onClick = { /* Handle click */ },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = getDialectName(selectedDialect),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        ExposedDropdownMenu(
                            expanded = false,
                            onDismissRequest = { /* Handle dismiss */ }
                        ) {
                            listOf(
                                "en" to "English",
                                "fil" to "Filipino",
                                "hil" to "Hiligaynon",
                                "ceb" to "Cebuano",
                                "mrn" to "Maranao"
                            ).forEach { dialect ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = dialect.second,
                                            style = MaterialTheme.typography.bodyMedium
                                        ) 
                                    },
                                    onClick = {
                                        viewModel.setDialect(dialect.first)
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Button(
                        onClick = {
                            // Clear history
                            viewModel.clearSentenceHistory()
                            viewModel.updateRecognizedText("")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Clear",
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    isRecording: Boolean,
    onImageCaptured: (ImageProxy) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // 1:1 aspect ratio
            .padding(16.dp)
            .border(2.dp, Color.White, CircleShape) // Visual indicator for 1:1 area
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)
                
                coroutineScope.launch {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        // Preview use case
                        val preview = androidx.camera.core.Preview.Builder().build()
                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        
                        // Select back camera as default
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        
                        try {
                            // Unbind use cases before rebinding
                            cameraProvider.unbindAll()
                            
                            // Bind use cases to camera
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner, cameraSelector, preview
                            )
                        } catch (exc: Exception) {
                            Log.e("CameraPreview", "Camera binding error", exc)
                        }
                    }, executor)
                }
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Recording indicator
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .background(Color.Red, CircleShape)
            )
        }
    }
}
