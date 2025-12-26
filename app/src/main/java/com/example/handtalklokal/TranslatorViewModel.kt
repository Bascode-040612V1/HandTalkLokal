package com.example.handtalklokal

import android.app.Application
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.handtalklokal.data.SentenceRepository
import com.example.handtalklokal.ImageUtils
import com.example.handtalklokal.RecognitionResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

// Data class to hold landmark information for drawing
data class LandmarkPoint(val x: Float, val y: Float, val type: String = "hand", val handIndex: Int = -1)

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val WORD_PAUSE_DELAY = 250L // 250ms pause between words
        private const val SENTENCE_PAUSE_DELAY = 750L // 750ms pause between sentences
    }
    
    private val repository = SentenceRepository(application)
    private val gestureRecognitionHelper = GestureRecognitionHelper(application)
    private val mediaPipeHelper = MediaPipeHelper(application)
    
    // Current recognized text
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()
    
    // Confidence level for the current recognition
    private val _confidenceLevel = MutableStateFlow(0.0f)
    val confidenceLevel: StateFlow<Float> = _confidenceLevel.asStateFlow()
    
    // Current phrase being built - now used to accumulate words in a line
    private val _currentPhrase = MutableStateFlow("")
    val currentPhrase: StateFlow<String> = _currentPhrase.asStateFlow()
    
    // Sentence history
    val sentenceHistory = repository.sentences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Landmark data for visualization
    private val _landmarks = MutableStateFlow<List<LandmarkPoint>>(emptyList())
    val landmarks: StateFlow<List<LandmarkPoint>> = _landmarks.asStateFlow()
    
    // Recording state - automatically start when camera is available
    private val _isRecording = MutableStateFlow(true) // Start recording by default
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    // Selected dialect
    private val _selectedDialect = MutableStateFlow("tl")
    val selectedDialect: StateFlow<String> = _selectedDialect.asStateFlow()
    
    // Camera direction
    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()
    
    // Text to speech
    private var textToSpeech: TextToSpeech? = null
    private val speechQueue = ConcurrentLinkedQueue<String>()
    private var isSpeaking = false
    private var lastSpeechType: SpeechType = SpeechType.WORD // Track if last speech was word or sentence
    
    private enum class SpeechType {
        WORD, SENTENCE
    }
    
    // Gesture tracking for duplicate suppression and debouncing
    private var lastAcceptedGesture: String? = null
    private var lastGestureTime: Long = 0
    private var pendingGesture: String? = null
    private var pendingConfidence: Float = 0.0f
    private var gestureDebounceJob: Job? = null
    
    // Timer for phrase finalization
    private var phraseTimer: Job? = null
    
    // Track the current line being built
    private var currentLine = ""
    
    // Time threshold for considering a phrase complete (in milliseconds)
    private val PHRASE_COMPLETE_THRESHOLD = 3000L // 3 seconds
    
    init {
        initializeTextToSpeech()
    }
    
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Set up utterance progress listener to track speech completion
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                        Log.d("TTS", "Utterance started: $utteranceId at ${System.currentTimeMillis()}")
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                        Log.d("TTS", "Utterance completed: $utteranceId at ${System.currentTimeMillis()}")
                        processNextInQueue()
                    }

                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                        Log.e("TTS", "Utterance error: $utteranceId at ${System.currentTimeMillis()}")
                        processNextInQueue()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        isSpeaking = false
                        Log.e("TTS", "Utterance error with code $errorCode: $utteranceId at ${System.currentTimeMillis()}")
                        processNextInQueue()
                    }
                })
            }
        }
    }
    
    fun updateRecognizedText(text: String) {
        _recognizedText.value = text
    }
    
    fun addToSentenceHistory(sentence: String) {
        if (sentence.isNotBlank()) {
            Log.d("TranslationDebug", "addToSentenceHistory called with: $sentence")
            viewModelScope.launch {
                repository.addSentence(sentence.trim())
                Log.d("TranslationDebug", "Sentence added to repository: ${sentence.trim()}")
            }
        } else {
            Log.d("TranslationDebug", "addToSentenceHistory called with blank sentence, skipping")
        }
    }
    
    fun clearSentenceHistory() {
        viewModelScope.launch {
            repository.clearSentences()
        }
    }
    
    fun setDialect(dialect: String) {
        Log.d("TranslationDebug", "Setting dialect to: $dialect")
        _selectedDialect.value = dialect
        Log.d("TranslationDebug", "Dialect updated to: ${_selectedDialect.value}")
    }
    
    fun speakText(text: String) {
        // Sanitize the text before adding to queue to ensure clean input to TTS
        val sanitizedText = text.replace(Regex("[^\\p{Print}]"), "").trim()
        
        if (sanitizedText.isNotEmpty() && sanitizedText.isNotBlank()) {
            // Add a special pause marker before sentences if the last speech was a word
            if (sanitizedText.contains(" ") && lastSpeechType == SpeechType.WORD) { // Likely a sentence
                speechQueue.offer("##SENTENCE_PAUSE##")
            } else if (!sanitizedText.contains(" ") && lastSpeechType == SpeechType.SENTENCE) { // Likely a word after a sentence
                speechQueue.offer("##WORD_PAUSE##")
            }
            
            speechQueue.offer(sanitizedText)
            if (!isSpeaking) {
                processNextInQueue()
            }
        } else {
            Log.d("TTS", "Skipping blank text in speakText: '$text'")
        }
    }

    private fun processNextInQueue() {
        val nextItem = speechQueue.poll()
        if (nextItem != null) {
            when {
                nextItem == "##WORD_PAUSE##" -> {
                    // Handle word pause
                    Log.d("TTS", "Word pause requested: ${WORD_PAUSE_DELAY}ms")
                    viewModelScope.launch {
                        delay(WORD_PAUSE_DELAY)
                        processNextInQueue() // Process next item after pause
                    }
                }
                nextItem == "##SENTENCE_PAUSE##" -> {
                    // Handle sentence pause
                    Log.d("TTS", "Sentence pause requested: ${SENTENCE_PAUSE_DELAY}ms")
                    viewModelScope.launch {
                        delay(SENTENCE_PAUSE_DELAY)
                        processNextInQueue() // Process next item after pause
                    }
                }
                else -> {
                    // Handle actual text to be spoken
                    val isSentence = nextItem.contains(" ") // Simple check if it's a sentence
                    
                    // Additional sanitization as a final check before TTS
                    val sanitizedText = nextItem.replace(Regex("[^\\p{Print}]"), "").trim()
                    
                    // Only speak if the text is not blank
                    if (sanitizedText.isNotBlank()) {
                        textToSpeech?.let { tts ->
                            // Always use Filipino TTS engine regardless of selected dialect
                            val filipinoLocale = Locale("fil", "PH")
                            
                            // Check if the Filipino locale is available and supported
                            if (tts.isLanguageAvailable(filipinoLocale) >= TextToSpeech.LANG_AVAILABLE) {
                                tts.language = filipinoLocale
                            } else {
                                // Fallback to English if Filipino is not available
                                tts.language = Locale.ENGLISH
                            }
                            
                            val utteranceId = "utterance_${System.currentTimeMillis()}"
                            
                            Log.d("TTS", "Speaking text: '$nextItem' (sanitized to: '$sanitizedText'), Utterance ID: $utteranceId, Type: ${if (isSentence) "SENTENCE" else "WORD"}, Locale: ${filipinoLocale}")
                            
                            // Update speech type before speaking
                            lastSpeechType = if (isSentence) SpeechType.SENTENCE else SpeechType.WORD
                            
                            tts.speak(sanitizedText, TextToSpeech.QUEUE_ADD, null, utteranceId)
                        }
                    } else {
                        Log.d("TTS", "Skipping blank text in speech queue: '$nextItem'")
                        processNextInQueue() // Continue with next item if current is blank
                    }
                }
            }
        }
    }
    
    /**
     * Switch between front and back camera
     */
    fun switchCamera() {
        _isFrontCamera.value = !_isFrontCamera.value
    }
    
    /**
     * Speak the full accumulated sentence (used for the Speak button)
     */
    fun speakFullSentence() {
        viewModelScope.launch {
            val sentences = sentenceHistory.value
            if (sentences.isNotEmpty()) {
                // Join all sentences with spaces for speaking
                val fullSentence = sentences.joinToString(" ").trim()
                if (fullSentence.isNotBlank()) {
                    speakText(fullSentence)
                } else {
                    Log.d("TTS", "Full sentence is blank, not speaking")
                }
            }
        }
    }
    
    /**
     * Process the captured image for sign language recognition
     * This now uses the actual TensorFlow Lite model with MediaPipe feature extraction
     */
    fun processImage(image: ImageProxy) {
        Log.d("TimingDebug", "Image processing started at ${System.currentTimeMillis()}, current dialect: ${_selectedDialect.value}")
        // Always process images when camera is active (recording is always true)
        viewModelScope.launch {
            try {
                // Convert ImageProxy to Bitmap
                val bitmap = ImageUtils.imageProxyToBitmap(image)
                
                // Extract hand and pose landmarks using MediaPipe
                val handLandmarks = mediaPipeHelper.extractHandLandmarks(bitmap)
                val poseLandmarks = mediaPipeHelper.extractPoseLandmarks(bitmap)
                
                // Update landmark data for visualization (all detected hands)
                val landmarkPoints = mutableListOf<LandmarkPoint>()
                
                // Convert MediaPipe hand landmarks to UI landmark points for visualization
                for ((handIndex, hand) in handLandmarks.withIndex()) {
                    // Convert actual landmarks to UI points
                    for (landmark in hand) {
                        // Add landmark points for visualization (normalized coordinates)
                        // Note: These are normalized coordinates (0.0 to 1.0), so we need to scale them
                        // to the actual screen size for visualization
                        landmarkPoints.add(LandmarkPoint(landmark.x(), landmark.y(), "hand", handIndex))
                    }
                }
                
                // Also add pose landmarks for visualization (shoulders, elbows, wrists)
                // Pose landmarks don't belong to a specific hand, so use -1 as handIndex
                for (poseLandmark in poseLandmarks) {
                    landmarkPoints.add(LandmarkPoint(poseLandmark.x(), poseLandmark.y(), "pose", -1))
                }
                
                _landmarks.value = landmarkPoints
                
                // Convert landmarks to features
                val features = mediaPipeHelper.landmarksToFeatures(handLandmarks, poseLandmarks)
                
                // Recognize gesture using TensorFlow Lite model
                val recognitionResult = if (features != null) {
                    gestureRecognitionHelper.recognizeGesture(features)
                } else {
                    RecognitionResult("", 0.0f, "None") // No hands detected, don't show anything
                }
                
                // Update confidence level for UI
                _confidenceLevel.value = recognitionResult.confidence
                
                // Only update text if we recognized a high confidence gesture
                if (recognitionResult.gesture.isNotEmpty() && recognitionResult.gesture != "Unknown" && recognitionResult.gesture != "Error" && recognitionResult.gesture != "Unrecognized (Medium Confidence)") {
                    // Check for duplicate gesture suppression
                    val currentTime = System.currentTimeMillis()
                    val isDuplicateGesture = recognitionResult.gesture == lastAcceptedGesture && 
                        (currentTime - lastGestureTime) < 1000 // 1 second window for duplicate suppression
                    
                    if (!isDuplicateGesture) {
                        Log.d("TimingDebug", "Processing gesture: ${recognitionResult.gesture}, dialect at processing: ${_selectedDialect.value}, time: $currentTime")
                        // Process gesture immediately without debounce
                        handleGestureImmediately(recognitionResult.gesture, recognitionResult.confidence)
                    }
                }
                // If no hands are detected or gesture is unrecognized, we don't update the text
                // This prevents flickering between recognized gestures and empty text
            } catch (e: Exception) {
                // Handle any errors during processing
                Log.e("TranslatorViewModel", "Error processing image", e)
                // Don't update the UI with error messages to avoid flickering
            } finally {
                // Close the image to prevent memory leaks
                image.close()
            }
        }
    }

    /**
     * Add gesture to sentence history and automatically speak it
     */
    private fun addToSentenceHistoryAndSpeak(sentence: String) {
        if (sentence.isNotBlank()) {
            viewModelScope.launch {
                repository.addSentence(sentence.trim())
                // Automatically speak the newly added sentence
                speakText(sentence.trim())
            }
        } else {
            Log.d("TranslationDebug", "addToSentenceHistoryAndSpeak called with blank sentence, skipping")
        }
    }
    

    
    private fun handleGestureImmediately(gesture: String, confidence: Float) {
        Log.d("TranslationDebug", "Gesture received: $gesture, current dialect: ${_selectedDialect.value}")
        
        // Sanitize the gesture to match exactly with translation map keys
        val sanitizedGesture = gesture.replace(Regex("[^a-zA-Z\\s]"), "").trim()
        Log.d("TranslationDebug", "Sanitized gesture: $sanitizedGesture")
        
        // Translate the gesture to the selected dialect before speaking
        val translatedGesture = translateGesture(sanitizedGesture)
        Log.d("TranslationDebug", "Translated result: $translatedGesture")
        
        // Only add to sentence history if translation is successful (not empty)
        if (translatedGesture.isNotBlank()) {
            // Implement the new behavior: concatenate to current line or start a new line
            // We'll concatenate if the current line is not empty and the time threshold hasn't passed
            val currentTime = System.currentTimeMillis()
            val isNewLine = currentLine.isEmpty() || (currentTime - lastGestureTime) > PHRASE_COMPLETE_THRESHOLD
            
            if (isNewLine) {
                // Start a new line with the translated gesture
                currentLine = translatedGesture
                addToSentenceHistory(currentLine)
            } else {
                // Concatenate to the current line
                currentLine = if (currentLine.isEmpty()) translatedGesture else "$currentLine $translatedGesture"
                
                // Update the last line in the repository to reflect the concatenated line
                viewModelScope.launch {
                    repository.updateLastSentence(currentLine)
                }
            }
            
            // Speak the translated text (not the original English)
            speakText(translatedGesture)
        } else {
            Log.d("TranslationDebug", "Translation failed, not adding to history: $sanitizedGesture")
        }
        
        // Reset the current phrase since we're now sending directly to completed sentences
        _currentPhrase.value = ""
        updateRecognizedText("")
        
        // Track the accepted gesture for duplicate suppression
        lastAcceptedGesture = sanitizedGesture
        lastGestureTime = System.currentTimeMillis()
    }
    
    private fun translateGesture(gesture: String): String {
        Log.d("TranslationDebug", "Translating gesture: '$gesture' with selected dialect: '${_selectedDialect.value}'")
        
        // This function implements the translation logic
        val translationMap = mapOf(
            "Hello" to mapOf(
                "en" to "Hello",
                "tl" to "Kumusta",
                "fil" to "Kumusta",
                "hil" to "Kamusta",
                "ceb" to "Kumusta",
                "mrn" to "Assalamu Alaikum"
            ),
            "Hi" to mapOf(
                "en" to "Hi",
                "tl" to "Kumusta",
                "fil" to "Kumusta",
                "hil" to "Kamusta",
                "ceb" to "Kumusta",
                "mrn" to "Assalamu"
            ),
            "Morning" to mapOf(
                "en" to "Morning",
                "tl" to "Agahan",
                "fil" to "Agahan",
                "hil" to "Agahon",
                "ceb" to "Mading",
                "mrn" to "Mading"
            ),
            "Noon" to mapOf(
                "en" to "Noon",
                "tl" to "Tanghali",
                "fil" to "Tanghali",
                "hil" to "Tanghali",
                "ceb" to "Tanghali",
                "mrn" to "Tanghali"
            ),
            "Afternoon" to mapOf(
                "en" to "Afternoon",
                "tl" to "Hapon",
                "fil" to "Hapon",
                "hil" to "Hapon",
                "ceb" to "Hapon",
                "mrn" to "Hapon"
            ),
            "Bye" to mapOf(
                "en" to "Bye",
                "tl" to "Paalam",
                "fil" to "Paalam",
                "hil" to "Palay",
                "ceb" to "Abay",
                "mrn" to "Alay"
            ),
            "Thank you" to mapOf(
                "en" to "Thank you",
                "tl" to "Salamat",
                "fil" to "Salamat",
                "hil" to "Salamat",
                "ceb" to "Salamat",
                "mrn" to "Salamat"
            ),
            "Good" to mapOf(
                "en" to "Good",
                "tl" to "Maganda",
                "fil" to "Maganda",
                "hil" to "Maayong",
                "ceb" to "Maayo",
                "mrn" to "Tambay"
            )
        )
        
        // Check if the gesture exists in the translation map
        val phraseTranslations = translationMap[gesture]
        
        // Guard: Return empty string if no translation exists for the selected dialect
        val result = if (phraseTranslations != null) {
            // Only return translation for the selected dialect, no fallback to English
            val selectedDialectValue = _selectedDialect.value
            phraseTranslations[selectedDialectValue] ?: ""
        } else {
            // If no translation exists for this gesture at all, return empty string
            ""
        }
        
        Log.d("TranslationDebug", "Translation result for '$gesture': '$result' using dialect '${_selectedDialect.value}'")
        return result
    }
    
    private fun getLocaleForDialect(code: String): Locale {
        return when (code) {
            "en" -> Locale.ENGLISH
            "tl", "fil", "hil", "ceb", "mrn" -> Locale("fil", "PH")  // Use Filipino locale for all Philippine languages
            else -> Locale("fil", "PH")  // Default to Filipino/Tagalog
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        textToSpeech?.apply {
            setOnUtteranceProgressListener(null)
            stop()
            shutdown()
        }
        gestureRecognitionHelper.close()
        mediaPipeHelper.close()
        phraseTimer?.cancel()
        gestureDebounceJob?.cancel()
    }
}