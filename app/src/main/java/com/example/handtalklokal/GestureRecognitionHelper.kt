package com.example.handtalklokal

import android.content.Context
import android.util.Log


// Data class to hold recognition result with confidence level
data class RecognitionResult(
    val gesture: String,
    val confidence: Float,
    val confidenceLevel: String // "High", "Medium", or "Low"
)

class GestureRecognitionHelper(private val context: Context) {
    private var taskFileManager: TaskFileManager = TaskFileManager(context)
    private var labels: List<String> = listOf()
    private val inputTensorSize = 138 // Number of features in your dataset
    
    // Dual-threshold system based on empirical logs
    private val ACCEPT_THRESHOLD = 6.3f  // Confident match threshold
    private val REJECT_THRESHOLD = 7.6f  // Definitely not a gesture threshold
    
    // Temporal stability requirements
    private var lastStableGesture: String? = null
    private var stableFrameCounter: Int = 0
    private val STABLE_FRAMES = 5  // Require N consecutive frames for acceptance
    
    init {
        try {
            // Load the gesture task file
            val loaded = taskFileManager.loadTaskFile()
            if (loaded) {
                // Initialize labels from the loaded task file
                labels = taskFileManager.getAllGestureLabels().toList()
                
                Log.d("GestureRecognition", "Task file and labels loaded successfully")
                Log.d("GestureRecognition", "Labels order: $labels")
                Log.d("GestureRecognition", "Loaded ${taskFileManager.getGestureCount()} gestures")
            } else {
                Log.e("GestureRecognition", "Failed to load task file")
            }
        } catch (e: Exception) {
            Log.e("GestureRecognition", "Error loading task file", e)
        }
    }
    
    /**
     * Recognize gesture from extracted features using Euclidean distance
     *
     * @param features Array of 138 float values representing hand and pose landmarks
     * @return RecognitionResult with gesture label and confidence level
     */
    fun recognizeGesture(features: FloatArray): RecognitionResult {
        if (features.size != inputTensorSize) {
            Log.w("GestureRecognition", "Features size mismatch. Features size: ${features.size}, Expected: $inputTensorSize")
            return RecognitionResult("Unknown", 0.0f, "Low")
        }
        
        Log.d("GestureRecognition", "Received ${features.size} features for recognition")
        
        // Check if task file is loaded
        if (!taskFileManager.isLoaded()) {
            Log.e("GestureRecognition", "Task file not loaded")
            return RecognitionResult("Unknown", 0.0f, "Low")
        }
        
        // Find the closest matching gesture using Euclidean distance
        val result = findClosestGesture(features)
        
        return result
    }
    
    /**
     * Find the closest matching gesture by computing Euclidean distance
     * to all samples in the task file
     */
    private fun findClosestGesture(features: FloatArray): RecognitionResult {
        var minDistance = Float.MAX_VALUE
        var closestGesture = "Unknown"
        var closestConfidence = 0.0f
        
        // Iterate through all gestures and their samples
        for (gestureLabel in taskFileManager.getAllGestureLabels()) {
            val gestureDefinition = taskFileManager.getGestureByLabel(gestureLabel)
            gestureDefinition?.samples?.forEach { sample ->
                val distance = computeEuclideanDistance(features, sample.features)
                
                // Log distance for debugging (optional, can be removed later)
                Log.v("GestureRecognition", "Gesture: $gestureLabel, Distance: $distance")
                
                if (distance < minDistance) {
                    minDistance = distance
                    closestGesture = gestureLabel
                    // Convert distance to confidence (inverse relationship)
                    // Confidence decreases as distance increases
                    closestConfidence = if (distance <= ACCEPT_THRESHOLD) {
                        kotlin.math.min(kotlin.math.max((1.0f - (distance / ACCEPT_THRESHOLD)), 0.0f), 1.0f)
                    } else {
                        0.0f
                    }
                }
            }
        }
        
        Log.d("GestureRecognition", "Closest gesture: $closestGesture, Distance: $minDistance, Confidence: $closestConfidence")
        
        // Dual-threshold decision logic
        val (finalGesture, finalConfidence, finalConfidenceLevel) = when {
            minDistance <= ACCEPT_THRESHOLD -> {
                // Confident match - increment stability counter
                if (closestGesture == lastStableGesture) {
                    stableFrameCounter++
                } else {
                    stableFrameCounter = 1
                    lastStableGesture = closestGesture
                }
                
                val outputGesture = if (stableFrameCounter >= STABLE_FRAMES) {
                    closestGesture
                } else {
                    lastStableGesture ?: "NO_GESTURE"
                }
                
                // Calculate confidence based on accept threshold
                val conf = kotlin.math.min(kotlin.math.max((1.0f - (minDistance / ACCEPT_THRESHOLD)), 0.0f), 1.0f)
                val confLevel = when {
                    conf > 0.7 -> "High"
                    conf > 0.4 -> "Medium"
                    else -> "Low"
                }
                
                Log.d("GESTURE_DEBUG", "best=$closestGesture dist=$minDistance zone=ACCEPT")
                Triple(outputGesture, conf, confLevel)
            }
            
            minDistance >= REJECT_THRESHOLD -> {
                // Definitely not a gesture - reset stability
                lastStableGesture = null
                stableFrameCounter = 0
                Log.d("GESTURE_DEBUG", "best=$closestGesture dist=$minDistance zone=REJECT")
                Triple("NO_GESTURE", 0.0f, "Low")
            }
            
            else -> {
                // Gray zone - ambiguous, maintain previous stable gesture
                Log.d("GESTURE_DEBUG", "best=$closestGesture dist=$minDistance zone=GRAY")
                Triple(lastStableGesture ?: "NO_GESTURE", 0.0f, "Low")
            }
        }
        
        // Determine confidence level based on final confidence value
        val confidenceLevel = when {
            finalConfidence > 0.7 -> "High"
            finalConfidence > 0.4 -> "Medium"
            else -> "Low"
        }
        
        return RecognitionResult(finalGesture, finalConfidence, finalConfidenceLevel)
    }
    
    /**
     * Compute Euclidean distance between two feature arrays
     */
    private fun computeEuclideanDistance(features1: FloatArray, features2: FloatArray): Float {
        if (features1.size != features2.size) {
            throw IllegalArgumentException("Feature arrays must have the same size")
        }
        
        var sumSquaredDiff = 0.0f
        for (i in features1.indices) {
            val diff = features1[i] - features2[i]
            sumSquaredDiff += diff * diff
        }
        
        return kotlin.math.sqrt(sumSquaredDiff)
    }
    
    /**
     * Release resources
     */
    fun close() {
        // Reset stability counters
        lastStableGesture = null
        stableFrameCounter = 0
        Log.d("GestureRecognition", "Gesture recognition helper closed")
    }
}