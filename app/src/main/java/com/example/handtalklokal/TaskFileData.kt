package com.example.handtalklokal

/**
 * Data class representing a single gesture sample with its features and metadata
 */
data class GestureSample(
    val features: FloatArray,  // Exactly 138 features in order: hand0, hand1, pose
    val sampleId: String,     // Unique identifier for this sample
    val confidenceThreshold: Float = 0.8f  // Threshold for this sample
)

/**
 * Data class representing a gesture with multiple samples
 */
data class GestureDefinition(
    val label: String,        // The gesture name/label
    val samples: List<GestureSample>  // Multiple samples for this gesture
)

/**
 * Data class representing the metadata of the task file
 */
data class TaskFileMetadata(
    val featureCount: Int,
    val featureOrder: String,
    val normalizationApplied: Boolean,
    val normalizationMethod: String,
    val totalGestures: Int,
    val samplesPerGesture: Int,
    val timestamp: String
)

/**
 * Data class representing the complete task file structure
 */
data class TaskFile(
    val schemaVersion: String,
    val metadata: TaskFileMetadata,
    val gestures: Map<String, GestureDefinition>  // Map of gesture label to definition
)