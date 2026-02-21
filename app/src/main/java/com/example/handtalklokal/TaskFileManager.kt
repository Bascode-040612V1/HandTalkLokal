package com.example.handtalklokal

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * TaskFileManager handles loading and parsing of the gesture task file from assets
 * This class loads the task file and parses gesture definitions into memory structures
 */
class TaskFileManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TaskFileManager"
        private const val TASK_FILE_PATH = "gesture_tasks.json"  // Updated task file name
    }
    
    private var taskFile: TaskFile? = null
    
    /**
     * Loads the task file from assets and parses it into memory structures
     * Returns true if successful, false otherwise
     */
    fun loadTaskFile(filePath: String = TASK_FILE_PATH): Boolean {
        return try {
            Log.d(TAG, "Loading task file from: $filePath")
            
            val jsonString = context.assets.open(filePath).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
            
            taskFile = parseTaskFile(jsonString)
            Log.d(TAG, "Successfully loaded task file with ${taskFile?.gestures?.size} gestures")
            Log.d(TAG, "Task file metadata: ${taskFile?.metadata}")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading task file: ${e.message}", e)
            false
        }
    }
    
    /**
     * Parses the JSON string representation of the task file into structured data
     */
    private fun parseTaskFile(jsonString: String): TaskFile {
        val jsonObject = JSONObject(jsonString)
        
        // Parse metadata
        val metadataObject = jsonObject.getJSONObject("metadata")
        val metadata = TaskFileMetadata(
            featureCount = metadataObject.getInt("feature_count"),
            featureOrder = "hand0_hand1_pose", // Default assumption for our data
            normalizationApplied = true, // Default assumption
            normalizationMethod = "min_max", // Default assumption
            totalGestures = metadataObject.optInt("unique_gestures", 0),
            samplesPerGesture = 0, // Will calculate dynamically
            timestamp = "unknown" // Not available in this format
        )
        
        // Parse gestures
        val gesturesObject = jsonObject.getJSONObject("gestures")
        val gesturesMap = mutableMapOf<String, GestureDefinition>()
        
        val gestureNames = gesturesObject.keys()
        var totalSamples = 0
        while (gestureNames.hasNext()) {
            val gestureName = gestureNames.next()
            val samplesArray = gesturesObject.getJSONArray(gestureName)
            
            val samplesList = mutableListOf<GestureSample>()
            for (i in 0 until samplesArray.length()) {
                val sampleObject = samplesArray.getJSONObject(i)
                
                // In the current format, the features are directly in the sample object
                val featuresArray = sampleObject.getJSONArray("features")
                val features = FloatArray(featuresArray.length())
                for (j in 0 until featuresArray.length()) {
                    features[j] = featuresArray.getDouble(j).toFloat()
                }
                
                val sample = GestureSample(
                    features = features,
                    sampleId = "${gestureName}_sample_${i}", // Generate a sample ID
                    confidenceThreshold = 0.8f // Default threshold
                )
                
                samplesList.add(sample)
            }
            
            val gestureDefinition = GestureDefinition(
                label = gestureName,
                samples = samplesList
            )
            
            gesturesMap[gestureName] = gestureDefinition
            totalSamples += samplesList.size
        }
        
        // Update metadata with calculated values
        val updatedMetadata = metadata.copy(
            totalGestures = gesturesMap.size,
            samplesPerGesture = if (gesturesMap.isNotEmpty()) totalSamples / gesturesMap.size else 0
        )
        
        return TaskFile(
            schemaVersion = "1.0", // Default schema version for this format
            metadata = updatedMetadata,
            gestures = gesturesMap
        )
    }
    
    /**
     * Gets the loaded task file, returns null if not loaded
     */
    fun getTaskFile(): TaskFile? {
        return taskFile
    }
    
    /**
     * Checks if the task file has been successfully loaded
     */
    fun isLoaded(): Boolean {
        return taskFile != null
    }
    
    /**
     * Gets the count of gestures loaded
     */
    fun getGestureCount(): Int {
        return taskFile?.gestures?.size ?: 0
    }
    
    /**
     * Gets a specific gesture definition by label
     */
    fun getGestureByLabel(label: String): GestureDefinition? {
        return taskFile?.gestures?.get(label)
    }
    
    /**
     * Gets all gesture labels
     */
    fun getAllGestureLabels(): Set<String> {
        return taskFile?.gestures?.keys ?: emptySet()
    }
}