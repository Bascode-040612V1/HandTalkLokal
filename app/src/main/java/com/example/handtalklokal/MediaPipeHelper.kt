package com.example.handtalklokal

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.framework.image.BitmapImageBuilder
import java.io.Closeable
import kotlin.math.sqrt
import kotlin.math.pow

/**
 * MediaPipe Helper for hand and pose landmark detection
 * 
 * This implementation uses MediaPipe Tasks Vision for hand and pose detection.
 */
class MediaPipeHelper(context: Context) : Closeable {
    private var handLandmarker: HandLandmarker? = null
    private var poseLandmarker: PoseLandmarker? = null
    
    init {
        try {
            // Initialize hand landmarker
            val handBaseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build()
            
            val handOptions = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(handBaseOptions)
                .setNumHands(2)
                .build()
                
            handLandmarker = HandLandmarker.createFromOptions(context, handOptions)
            Log.d("MediaPipeHelper", "MediaPipe hand landmarker initialized successfully")
            
            // Initialize pose landmarker
            val poseBaseOptions = BaseOptions.builder()
                .setModelAssetPath("pose_landmarker.task")
                .build()
            
            val poseOptions = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(poseBaseOptions)
                .setNumPoses(1)
                .build()
                
            poseLandmarker = PoseLandmarker.createFromOptions(context, poseOptions)
            Log.d("MediaPipeHelper", "MediaPipe pose landmarker initialized successfully")
        } catch (e: Exception) {
            Log.e("MediaPipeHelper", "Failed to initialize MediaPipe components", e)
        }
    }
    
    /**
     * Extract hand landmarks from bitmap using MediaPipe
     */
    fun extractHandLandmarks(bitmap: Bitmap): List<List<NormalizedLandmark>> {
        Log.d("MediaPipeDebug", "Starting hand landmark extraction. Bitmap size: ${bitmap.width}x${bitmap.height}")
        return try {
            handLandmarker?.let { landmarker ->
                val mpImage = BitmapImageBuilder(bitmap).build()
                val result = landmarker.detect(mpImage)
                Log.d("MediaPipeDebug", "Hand detection completed. Result type: ${result.javaClass.name}")
                
                // Extract landmarks from all detected hands using direct API access
                val allLandmarks = mutableListOf<List<NormalizedLandmark>>()
                
                try {
                    // Use reflection to find the correct method for accessing landmarks
                    Log.d("MediaPipeHelper", "Result class: ${result.javaClass.name}")
                    
                    // Try common method names for accessing landmarks
                    val methods = result.javaClass.methods
                    var landmarksMethod: java.lang.reflect.Method? = null
                    
                    for (method in methods) {
                        if ((method.name.equals("landmarks", ignoreCase = true) || 
                             method.name.equals("hand_landmarks", ignoreCase = true) ||
                             method.name.equals("getLandmarks", ignoreCase = true) ||
                             method.name.equals("getHandLandmarks", ignoreCase = true)) && 
                            method.parameterCount == 0) {
                            landmarksMethod = method
                            Log.d("MediaPipeHelper", "Found landmarks method: ${method.name}")
                            break
                        }
                    }
                    
                    if (landmarksMethod != null) {
                        val handLandmarksList = landmarksMethod.invoke(result)
                        Log.d("MediaPipeDebug", "Successfully invoked landmarks method. Result type: ${handLandmarksList?.javaClass?.name}")
                        
                        // Check if it's a collection and process accordingly
                        if (handLandmarksList is Collection<*>) {
                            Log.d("MediaPipeDebug", "Hand landmarks list is collection with ${handLandmarksList.size} items")
                            for (singleHandLandmarks in handLandmarksList) {
                                val landmarksForHand = mutableListOf<NormalizedLandmark>()
                                
                                // Check if singleHandLandmarks is iterable
                                if (singleHandLandmarks is Iterable<*>) {
                                    var landmarkCount = 0
                                    for (landmark in singleHandLandmarks) {
                                        if (landmark is NormalizedLandmark) {
                                            landmarksForHand.add(landmark)
                                            landmarkCount++
                                        }
                                    }
                                    Log.d("MediaPipeDebug", "Processed hand with ${landmarkCount} landmarks")
                                } else {
                                    Log.d("MediaPipeDebug", "singleHandLandmarks is not iterable, type: ${singleHandLandmarks?.javaClass?.name}")
                                }
                                
                                if (landmarksForHand.isNotEmpty()) {
                                    allLandmarks.add(landmarksForHand)
                                    Log.d("MediaPipeDebug", "Added hand with ${landmarksForHand.size} landmarks. Total hands: ${allLandmarks.size}")
                                } else {
                                    Log.d("MediaPipeDebug", "Empty landmarks found for hand")
                                }
                            }
                        } else {
                            Log.d("MediaPipeDebug", "handLandmarksList is not a collection, type: ${handLandmarksList?.javaClass?.name}")
                        }
                    }
                    
                    if (allLandmarks.isNotEmpty()) {
                        Log.d("MediaPipeDebug", "Extracted ${allLandmarks.size} hands with landmarks successfully")
                    } else {
                        Log.d("MediaPipeDebug", "No hands detected in the frame")
                    }
                } catch (e: Exception) {
                    Log.e("MediaPipeHelper", "Error accessing landmarks with reflection", e)
                }
                
                allLandmarks
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("MediaPipeHelper", "Error extracting hand landmarks", e)
            emptyList()
        }
    }
    
    /**
     * Extract pose landmarks from bitmap using MediaPipe
     */
    fun extractPoseLandmarks(bitmap: Bitmap): List<NormalizedLandmark> {
        Log.d("MediaPipeDebug", "Starting pose landmark extraction. Bitmap size: ${bitmap.width}x${bitmap.height}")
        return try {
            poseLandmarker?.let { landmarker ->
                val mpImage = BitmapImageBuilder(bitmap).build()
                val result = landmarker.detect(mpImage)
                Log.d("MediaPipeDebug", "Pose detection completed. Result type: ${result.javaClass.name}")
                
                try {
                    // Use reflection to find the correct method for accessing pose landmarks
                    Log.d("MediaPipeHelper", "Pose result class: ${result.javaClass.name}")
                    
                    // Try common method names for accessing pose landmarks
                    val methods = result.javaClass.methods
                    var poseLandmarksMethod: java.lang.reflect.Method? = null
                    
                    for (method in methods) {
                        if ((method.name.contains("poseLandmarks", ignoreCase = true) || 
                             method.name.contains("pose_landmarks", ignoreCase = true) ||
                             method.name.contains("landmarks", ignoreCase = true) ||
                             method.name.contains("getPoseLandmarks", ignoreCase = true) ||
                             method.name.contains("getLandmarks", ignoreCase = true)) && 
                            method.parameterCount == 0) {
                            poseLandmarksMethod = method
                            Log.d("MediaPipeHelper", "Found pose landmarks method: ${method.name}")
                            break
                        }
                    }
                    
                    if (poseLandmarksMethod != null) {
                        val poseLandmarksResult = poseLandmarksMethod.invoke(result)
                        Log.d("MediaPipeDebug", "Successfully invoked pose landmarks method. Result type: ${poseLandmarksResult?.javaClass?.name}")
                        
                        // Check if it's a collection and process accordingly
                        if (poseLandmarksResult is Collection<*>) {
                            for (poseLandmarks in poseLandmarksResult) {
                                if (poseLandmarks is Iterable<*>) {
                                    val landmarks = mutableListOf<NormalizedLandmark>()
                                    for (landmark in poseLandmarks) {
                                        if (landmark is NormalizedLandmark) {
                                            landmarks.add(landmark)
                                        }
                                    }
                                    Log.d("MediaPipeDebug", "Extracted ${landmarks.size} pose landmarks successfully")
                                    return landmarks
                                }
                            }
                        }
                    }
                    
                    Log.d("MediaPipeDebug", "No pose landmarks extracted from the frame")
                } catch (e: Exception) {
                    Log.e("MediaPipeHelper", "Error accessing pose landmarks with reflection", e)
                }
                
                emptyList()
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("MediaPipeHelper", "Error extracting pose landmarks", e)
            emptyList()
        }
    }
    
    /**
     * Convert landmarks to feature array for gesture recognition
     * This matches the Python training model format:
     * 138 features = 126 hand features (2 hands × 21 landmarks × 3 coordinates) + 12 pose features
     * 
     * Hand order is standardized as follows to ensure deterministic behavior:
     * - Index 0: Right hand side of frame (leftmost hand)
     * - Index 1: Left hand side of frame (rightmost hand)
     */
    fun landmarksToFeatures(
        handLandmarks: List<List<NormalizedLandmark>>,
        poseLandmarks: List<NormalizedLandmark>
    ): FloatArray? {
        Log.d("MediaPipeDebug", "Converting landmarks to features. Hands detected: ${handLandmarks.size}, Pose landmarks: ${poseLandmarks.size}")
        
        // Check if any hands were detected
        if (handLandmarks.isEmpty()) {
            Log.d("MediaPipeDebug", "No hands detected, returning null for gesture recognition")
            // No hands detected, return null to indicate no gesture should be recognized
            return null
        }
        
        // Create feature array with 138 elements (matching Python model)
        val features = FloatArray(138) { 0.0f }
        
        // Sort hands by X position (leftmost first) to ensure deterministic ordering
        // This ensures consistent feature extraction regardless of detection order
        val sortedHands = handLandmarks.mapIndexed { index, landmarks ->
            // Calculate average X position of landmarks to determine hand position
            val avgX = if (landmarks.isNotEmpty()) {
                landmarks.sumOf { it.x().toDouble() } / landmarks.size
            } else {
                Double.MAX_VALUE // Put invalid hands at the end
            }
            Pair(avgX, landmarks)
        }.sortedBy { it.first }.map { it.second }
        
        // Extract hand landmarks features (up to 2 hands) in deterministic order
        for (handIndex in 0 until minOf(2, sortedHands.size)) {
            val landmarks = sortedHands[handIndex]
            
            // Get wrist landmark (index 0) to normalize all other landmarks relative to it
            if (landmarks.isNotEmpty() && landmarks.size >= 10) { // Need at least wrist and middle finger base
                val wrist = landmarks[0] // Landmark 0 is the wrist
                val middleBase = landmarks[9] // Middle finger MCP (base)
                
                // Calculate hand scale (distance from wrist to middle finger base)
                // This prevents the "sticky" issue when moving closer/further from camera
                val dx = (middleBase.x() - wrist.x()).toDouble()
                val dy = (middleBase.y() - wrist.y()).toDouble()
                val dist = sqrt(dx * dx + dy * dy).toFloat().coerceAtLeast(0.001f) // Prevent divide by zero
                
                // Each hand has 21 landmarks with x, y, z coordinates
                for (landmarkIndex in landmarks.indices) {
                    val featureIndex = handIndex * 63 + landmarkIndex * 3 // 63 features per hand (21 landmarks × 3 coordinates)
                    if (featureIndex + 2 < features.size) {
                        // Normalize coordinates relative to wrist AND scale by hand size
                        features[featureIndex] = (landmarks[landmarkIndex].x() - wrist.x()) / dist
                        features[featureIndex + 1] = (landmarks[landmarkIndex].y() - wrist.y()) / dist
                        features[featureIndex + 2] = (landmarks[landmarkIndex].z() - wrist.z()) / dist
                    }
                }
            } else if (landmarks.isNotEmpty()) {
                // Fallback to simple wrist normalization if we don't have enough landmarks
                val wrist = landmarks[0] // Landmark 0 is the wrist
                val wristX = wrist.x()
                val wristY = wrist.y()
                val wristZ = wrist.z()
                
                // Each hand has 21 landmarks with x, y, z coordinates
                for (landmarkIndex in landmarks.indices) {
                    val featureIndex = handIndex * 63 + landmarkIndex * 3 // 63 features per hand (21 landmarks × 3 coordinates)
                    if (featureIndex + 2 < features.size) {
                        // Normalize coordinates relative to wrist (as done in training)
                        features[featureIndex] = landmarks[landmarkIndex].x() - wristX
                        features[featureIndex + 1] = landmarks[landmarkIndex].y() - wristY
                        features[featureIndex + 2] = landmarks[landmarkIndex].z() - wristZ
                    }
                }
            }
        }
        
        // If only one hand is detected, pad with zeros for the second hand
        if (sortedHands.size == 1) {
            // Zero padding for second hand (63 features: 21 landmarks × 3 coordinates)
            // Already initialized to 0.0f
        }
        
        // Extract pose landmarks features (focus on upper body: elbows and wrists)
        // Using landmarks for elbows and wrists only (indices 13, 14, 15, 16), matching Python implementation
        // Elbows: 13 (left), 14 (right)
        // Wrists: 15 (left), 16 (right)
        val poseLandmarkIndices = listOf(13, 14, 15, 16) // elbows and wrists only
        
        var poseFeatureIndex = 126 // Start after hand features
        for (landmarkIndex in poseLandmarkIndices) {
            if (poseLandmarks.isNotEmpty() && landmarkIndex < poseLandmarks.size) {
                val landmark = poseLandmarks[landmarkIndex]
                if (poseFeatureIndex + 2 < features.size) {
                    // Use raw coordinates for pose landmarks (not normalized relative to anything)
                    features[poseFeatureIndex] = landmark.x()
                    features[poseFeatureIndex + 1] = landmark.y()
                    features[poseFeatureIndex + 2] = landmark.z()
                    poseFeatureIndex += 3
                }
            } else {
                // Pad with zeros if landmark not available
                if (poseFeatureIndex + 2 < features.size) {
                    features[poseFeatureIndex] = 0.0f
                    features[poseFeatureIndex + 1] = 0.0f
                    features[poseFeatureIndex + 2] = 0.0f
                    poseFeatureIndex += 3
                }
            }
        }
        
        // Ensure we always return exactly 138 features
        if (features.size != 138) {
            Log.w("MediaPipeHelper", "Feature array size mismatch: expected 138, got ${features.size}")
            // Return null to indicate error if size is not as expected
            return if (features.size < 138) {
                // Pad with zeros if we have fewer features
                val paddedFeatures = FloatArray(138) { i ->
                    if (i < features.size) features[i] else 0.0f
                }
                paddedFeatures
            } else {
                // Truncate if we have more features than expected
                features.copyOf(138)
            }
        }
        
        return features
    }
    
    /**
     * Release resources
     */
    override fun close() {
        try {
            handLandmarker?.close()
        } catch (e: Exception) {
            Log.e("MediaPipeHelper", "Error closing MediaPipe hand landmarker", e)
        }
        try {
            poseLandmarker?.close()
        } catch (e: Exception) {
            Log.e("MediaPipeHelper", "Error closing MediaPipe pose landmarker", e)
        }
    }
}