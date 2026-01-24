package com.example.handtalklokal

import android.content.Context
import android.util.Log
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer


// Data class to hold recognition result with confidence level
data class RecognitionResult(
    val gesture: String,
    val confidence: Float,
    val confidenceLevel: String // "High", "Medium", or "Low"
)

class GestureRecognitionHelper(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var labels: List<String> = listOf()
    private val inputTensorSize = 138 // Number of features in your dataset
    
    init {
        try {
            // Load the TensorFlow Lite model
            val model = loadModelFile()
            interpreter = Interpreter(model)
            
            // Load labels
            labels = loadLabels()
            
            Log.d("GestureRecognition", "Model and labels loaded successfully")
            Log.d("GestureRecognition", "Labels order: $labels")
            Log.d("GestureRecognition", "Model output shape: ${interpreter?.getOutputTensor(0)?.shape()?.joinToString(", ")}")
        } catch (e: Exception) {
            Log.e("GestureRecognition", "Error loading model or labels", e)
        }
    }
    
    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        return FileUtil.loadMappedFile(context, "gesture_model.tflite")
    }

    private fun loadLabels(): List<String> {
        try {
            val labels = mutableListOf<String>()
            val inputStream = context.assets.open("labels.txt")
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            reader.forEachLine { line ->
                if (line.isNotBlank()) {
                    // Clean the line to remove any invisible characters
                    val cleanLine = line.trim().replace("\\s+".toRegex(), " ")
                    labels.add(cleanLine)
                }
            }
            reader.close()
            Log.d("GestureRecognition", "Loaded ${labels.size} labels: $labels")
            return labels
        } catch (e: IOException) {
            Log.e("GestureRecognition", "Error loading labels", e)
            return listOf()
        }
    }
    
    /**
     * Recognize gesture from extracted features
     *
     * @param features Array of 138 float values representing hand and pose landmarks
     * @return RecognitionResult with gesture label and confidence level
     */
    fun recognizeGesture(features: FloatArray): RecognitionResult {
        if (interpreter == null || features.size != inputTensorSize) {
            Log.w("GestureRecognition", "Interpreter not initialized or features size mismatch. Features size: ${features.size}, Expected: $inputTensorSize")
            return RecognitionResult("Unknown", 0.0f, "Low")
        }
        
        try {
            // Prepare input buffer
            val inputBuffer = ByteBuffer.allocateDirect(4 * inputTensorSize)
            inputBuffer.order(ByteOrder.nativeOrder())
            
            // Put features into buffer
            for (feature in features) {
                inputBuffer.putFloat(feature)
            }
            
            // Get the model's actual output shape to determine how many classes it was trained on
            val modelOutputShape = interpreter?.getOutputTensor(0)?.shape()
            val modelNumClasses = if (modelOutputShape != null && modelOutputShape.size > 1) {
                modelOutputShape[1] // Second dimension is number of classes
            } else {
                8 // Default fallback
            }
            
            Log.d("GestureRecognition", "Model expects $modelNumClasses output classes, we have ${labels.size} labels")
            
            // Prepare output buffer based on model's expected output size, not label size
            val outputBuffer = Array(1) { FloatArray(modelNumClasses) }
            
            // Run inference
            interpreter?.run(inputBuffer, outputBuffer)
            
            // Get the result with highest probability
            val probabilities = outputBuffer[0]
            var maxProb = -1.0f
            var maxIndex = -1
            
            Log.d("GestureRecognition", "Model output probabilities:")
            for (i in probabilities.indices) {
                val labelForIndex = if (i < labels.size) labels[i] else "UNKNOWN_INDEX_$i"
                Log.d("GestureRecognition", "  Index $i ($labelForIndex): ${probabilities[i]}")
                if (i < modelNumClasses && probabilities[i] > maxProb) {
                    maxProb = probabilities[i]
                    maxIndex = i
                }
            }
            
            Log.d("GestureRecognition", "Max probability: $maxProb, Index: $maxIndex")
            
            // Define confidence thresholds matching Training_the_model configuration
            val highConfidenceThreshold = 0.7f
            val mediumConfidenceThreshold = 0.4f
            
            return when {
                maxProb >= highConfidenceThreshold && maxIndex >= 0 && maxIndex < labels.size -> {
                    val result = labels[maxIndex]
                    // Sanitize the result to remove any non-printable characters
                    val sanitizedResult = result.replace(Regex("[^\\p{Print}]"), "").trim()
                    Log.d("GestureRecognition", "High confidence recognized gesture: $result (sanitized to: $sanitizedResult)")
                    RecognitionResult(sanitizedResult, maxProb, "High")
                }
                maxProb >= mediumConfidenceThreshold && maxIndex >= 0 && maxIndex < labels.size -> {
                    val result = labels[maxIndex]
                    // Sanitize the result to remove any non-printable characters
                    val sanitizedResult = result.replace(Regex("[^\\p{Print}]"), "").trim()
                    Log.d("GestureRecognition", "Medium confidence recognized gesture: $result (sanitized to: $sanitizedResult)")
                    RecognitionResult(sanitizedResult, maxProb, "Medium")
                }
                else -> {
                    Log.d("GestureRecognition", "Low confidence or invalid index. Returning Unknown")
                    RecognitionResult("Unknown", maxProb, "Low")
                }
            }
        } catch (e: Exception) {
            Log.e("GestureRecognition", "Error during inference", e)
            return RecognitionResult("Error", 0.0f, "Error")
        }
    }
    
    /**
     * Release resources
     */
    fun close() {
        try {
            interpreter?.close()
            interpreter = null
        } catch (e: Exception) {
            Log.e("GestureRecognition", "Error closing interpreter", e)
        }
    }
}