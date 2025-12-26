package com.example.handtalklokal

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
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
            
            // Prepare output buffer based on model output shape
            val outputShape = interpreter?.getOutputTensor(0)?.shape() ?: intArrayOf(1, 8)
            val outputBuffer = Array(1) { FloatArray(outputShape[1]) }
            
            // Run inference
            interpreter?.run(inputBuffer, outputBuffer)
            
            // Get the result with highest probability
            val probabilities = outputBuffer[0]
            var maxProb = -1.0f
            var maxIndex = -1
            
            for (i in probabilities.indices) {
                if (probabilities[i] > maxProb) {
                    maxProb = probabilities[i]
                    maxIndex = i
                }
            }
            
            Log.d("GestureRecognition", "Max probability: $maxProb, Index: $maxIndex, Labels size: ${labels.size}")
            
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
                    val result = "Unrecognized (Medium Confidence)"
                    Log.d("GestureRecognition", "Medium confidence recognized gesture. Returning: $result")
                    RecognitionResult(result, maxProb, "Medium")
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