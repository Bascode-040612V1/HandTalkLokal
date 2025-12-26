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
        } catch (e: Exception) {
            Log.e("MediaPipeHelper", "Failed to initialize MediaPipe hand landmarker", e)
        }
        
        try {
            // Initialize pose landmarker
            val poseBaseOptions = BaseOptions.builder()
                .setModelAssetPath("pose_landmarker.task")
                .build()
            
            val poseOptions = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(poseBaseOptions)
                .setOutputSegmentationMasks(false)
                .setNumPoses(1)
                .build()
                
            poseLandmarker = PoseLandmarker.createFromOptions(context, poseOptions)
            Log.d("MediaPipeHelper", "MediaPipe pose landmarker initialized successfully")
        } catch (e: Exception) {
            Log.e("MediaPipeHelper", "Failed to initialize MediaPipe pose landmarker", e)
        }
    }
    
    /**
     * Extract hand landmarks from bitmap using MediaPipe
     */
    fun extractHandLandmarks(bitmap: Bitmap): List<List<NormalizedLandmark>> {
        return try {
            handLandmarker?.let { landmarker ->
                val mpImage = BitmapImageBuilder(bitmap).build()
                val result = landmarker.detect(mpImage)
                
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
                        
                        // Check if it's a collection and process accordingly
                        if (handLandmarksList is Collection<*>) {
                            for (singleHandLandmarks in handLandmarksList) {
                                val landmarksForHand = mutableListOf<NormalizedLandmark>()
                                
                                // Check if singleHandLandmarks is iterable
                                if (singleHandLandmarks is Iterable<*>) {
                                    for (landmark in singleHandLandmarks) {
                                        if (landmark is NormalizedLandmark) {
                                            landmarksForHand.add(landmark)
                                        }
                                    }
                                }
                                
                                if (landmarksForHand.isNotEmpty()) {
                                    allLandmarks.add(landmarksForHand)
                                }
                            }
                        }
                    }
                    
                    Log.d("MediaPipeHelper", "Extracted ${allLandmarks.size} hands with landmarks")
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
        return try {
            poseLandmarker?.let { landmarker ->
                val mpImage = BitmapImageBuilder(bitmap).build()
                val result = landmarker.detect(mpImage)
                
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
                                    Log.d("MediaPipeHelper", "Extracted ${landmarks.size} pose landmarks")
                                    return landmarks
                                }
                            }
                        }
                    }
                    
                    Log.d("MediaPipeHelper", "No pose landmarks extracted")
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
     */
    fun landmarksToFeatures(
        handLandmarks: List<List<NormalizedLandmark>>,
        poseLandmarks: List<NormalizedLandmark>
    ): FloatArray? {
        // Check if any hands were detected
        if (handLandmarks.isEmpty()) {
            // No hands detected, return null to indicate no gesture should be recognized
            return null
        }
        
        // Create feature array with 138 elements (matching Python model)
        val features = FloatArray(138) { 0.0f }
        
        // Extract hand landmarks features (up to 2 hands)
        for (handIndex in 0 until minOf(2, handLandmarks.size)) {
            val landmarks = handLandmarks[handIndex]
            
            // Each hand has 21 landmarks with x, y, z coordinates
            for (landmarkIndex in landmarks.indices) {
                val featureIndex = handIndex * 63 + landmarkIndex * 3 // 63 features per hand (21 landmarks × 3 coordinates)
                if (featureIndex + 2 < features.size) {
                    features[featureIndex] = landmarks[landmarkIndex].x()
                    features[featureIndex + 1] = landmarks[landmarkIndex].y()
                    features[featureIndex + 2] = landmarks[landmarkIndex].z()
                }
            }
        }
        
        // If only one hand is detected, pad with zeros for the second hand
        if (handLandmarks.size == 1) {
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