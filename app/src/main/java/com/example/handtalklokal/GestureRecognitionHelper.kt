    /**
     * Recognize gesture from extracted features with temporal stability and quality checks
     *
     * @param features Array of 138 float values representing hand and pose landmarks
     * @return RecognitionResult with gesture label and confidence level
     */
    fun recognizeGesture(features: FloatArray): RecognitionResult {
        // Check if we're in cooldown period after last gesture
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastGestureTime < GESTURE_COOLDOWN_MS) {
            return RecognitionResult("", 0.0f, "Low") // Return empty during cooldown
        }
        
        if (interpreter == null || features.size != inputTensorSize) {
            Log.w("GestureRecognition", "Interpreter not initialized or features size mismatch. Features size: ${features.size}, Expected: $inputTensorSize")
            // Reset state if no features are available
            resetState()
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
            
            // Check for model-label mismatch and fail fast if detected
            if (modelNumClasses != labels.size) {
                Log.e("GestureRecognition", "CRITICAL ERROR: Model/label mismatch: model=$modelNumClasses labels=${labels.size}")
                Log.e("GestureRecognition", "This will cause incorrect predictions. Please retrain model or update labels.txt")
                return RecognitionResult("Unknown", 0.0f, "Low")
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
            var secondProb = -1.0f
            var secondIndex = -1
            
            Log.d("GestureRecognition", "Model output probabilities:")
            for (i in probabilities.indices) {
                val labelForIndex = if (i < labels.size) labels[i] else "UNKNOWN_INDEX_$i"
                Log.d("GestureRecognition", "  Index $i ($labelForIndex): ${probabilities[i]}")
                if (i < modelNumClasses && probabilities[i] > maxProb) {
                    // Update second highest before updating max
                    secondProb = maxProb
                    secondIndex = maxIndex
                    maxProb = probabilities[i]
                    maxIndex = i
                } else if (i < modelNumClasses && probabilities[i] > secondProb) {
                    secondProb = probabilities[i]
                    secondIndex = i
                }
            }
            
            Log.d("GestureRecognition", "Max probability: $maxProb, Index: $maxIndex")
            Log.d("GestureRecognition", "Second probability: $secondProb, Index: $secondIndex")
            
            // Hard rejection gate: check all three conditions
            val meetsConfidenceThreshold = maxProb >= CONFIDENCE_THRESHOLD
            val meetsMarginThreshold = (maxProb - secondProb) >= MARGIN_THRESHOLD
            val predictedClass = if (maxIndex >= 0 && maxIndex < labels.size) labels[maxIndex] else "Unknown"
            val isNotNoGesture = !predictedClass.equals("NO_GESTURE", ignoreCase = true) // Check if not NO_GESTURE class
            
            val passesHardGate = meetsConfidenceThreshold && meetsMarginThreshold && isNotNoGesture
            
            // Update state based on current prediction vs previous prediction
            if (passesHardGate && maxIndex >= 0 && maxIndex < labels.size) {
                val currentPrediction = labels[maxIndex]
                
                if (currentPrediction == lastPredictedClass) {
                    // Same prediction as last frame, increment stability counter
                    stableFrameCount++
                    currentStableGesture = currentPrediction
                } else {
                    // Different prediction, reset stability counter
                    stableFrameCount = 1
                    currentStableGesture = currentPrediction
                    lastPredictedClass = currentPrediction
                    lastConfidence = maxProb
                }
            } else {
                // Failed hard gate, reset stability counter
                stableFrameCount = 0
                currentStableGesture = null
                lastPredictedClass = null
            }
            
            // Check if we have enough consecutive frames for stable recognition
            val meetsTemporalStability = stableFrameCount >= CONSECUTIVE_FRAMES_THRESHOLD
            
            // Return result based on temporal stability and hard gate
            return if (passesHardGate && meetsTemporalStability) {
                // Valid gesture detected, emit it and start cooldown
                val result = currentStableGesture!!
                // Sanitize the result to ensure consistent formatting
                // Convert to title case and remove extra spaces
                val sanitizedResult = result.trim()
                    .split(" ")
                    .joinToString(" ") { word -> 
                        word.lowercase().replaceFirstChar { it.uppercase() } 
                    }
                Log.d("GestureRecognition", "Stable recognized gesture: $result (sanitized to: $sanitizedResult), stable frames: $stableFrameCount")
                lastGestureTime = currentTime
                resetStabilityState() // Reset for next gesture
                RecognitionResult(sanitizedResult, lastConfidence, "High")
            } else {
                // Either not stable enough or failed hard gate
                if (!passesHardGate) {
                    Log.d("GestureRecognition", "Does not pass hard gate: conf=$maxProb (>=${CONFIDENCE_THRESHOLD}), margin=${maxProb - secondProb} (>=${MARGIN_THRESHOLD}), isNoGesture=${!isNotNoGesture}. Returning Unknown")
                } else if (!meetsTemporalStability) {
                    Log.d("GestureRecognition", "Does not meet temporal stability: $stableFrameCount/$CONSECUTIVE_FRAMES_THRESHOLD frames")
                }
                RecognitionResult("Unknown", maxProb, "Low")
            }
        } catch (e: Exception) {
            Log.e("GestureRecognition", "Error during inference", e)
            resetState()
            return RecognitionResult("Error", 0.0f, "Error")
        }
    }