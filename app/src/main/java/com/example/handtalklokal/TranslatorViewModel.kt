package com.example.handtalklokal

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.handtalklokal.data.SentenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.*

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = SentenceRepository(application)
    
    // Current recognized text
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()
    
    // Sentence history
    val sentenceHistory = repository.sentences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Recording state - automatically start when camera is available
    private val _isRecording = MutableStateFlow(true) // Start recording by default
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    // Selected dialect
    private val _selectedDialect = MutableStateFlow("en")
    val selectedDialect: StateFlow<String> = _selectedDialect.asStateFlow()
    
    // Text to speech
    private var textToSpeech: TextToSpeech? = null
    
    init {
        initializeTextToSpeech()
    }
    
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // TTS initialized successfully
            }
        }
    }
    
    fun updateRecognizedText(text: String) {
        _recognizedText.value = text
    }
    
    fun addToSentenceHistory(sentence: String) {
        viewModelScope.launch {
            repository.addSentence(sentence)
        }
    }
    
    fun clearSentenceHistory() {
        viewModelScope.launch {
            repository.clearSentences()
        }
    }
    
    // We don't need toggleRecording anymore since recording is always on
    /*
    fun toggleRecording() {
        _isRecording.value = !_isRecording.value
        if (!_isRecording.value) {
            // When stopping recording, add the recognized text to history
            if (_recognizedText.value.isNotBlank()) {
                addToSentenceHistory(_recognizedText.value)
            }
        }
    }
    */
    
    fun setDialect(dialect: String) {
        _selectedDialect.value = dialect
    }
    
    fun speakText(text: String) {
        textToSpeech?.let { tts ->
            tts.language = getLocaleForDialect(_selectedDialect.value)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }
    
    /**
     * Speak the full accumulated sentence (used for the Speak button)
     */
    fun speakFullSentence() {
        viewModelScope.launch {
            val sentences = sentenceHistory.value
            if (sentences.isNotEmpty()) {
                val fullSentence = sentences.joinToString(" ")
                speakText(fullSentence)
            }
        }
    }
    
    /**
     * Process the captured image for sign language recognition
     * In a real implementation, this would contain the actual computer vision logic
     */
    fun processImage(image: ImageProxy) {
        // Always process images when camera is active (recording is always true)
        // Simulate processing delay
        viewModelScope.launch {
            // Reduce frequency of processing to avoid flooding
            kotlinx.coroutines.delay(1000) // 1 second delay between recognitions
            
            // Simulate recognition result
            val gestures = listOf(
                "Hello", "Thank You", "Please", "Yes", "No", 
                "Help", "Water", "Food", "More", "Finished"
            )
            
            val randomGesture = gestures.random()
            updateRecognizedText(randomGesture)
            
            // Automatically add to sentence history and speak it
            addToSentenceHistoryAndSpeak(randomGesture)
        }
    }
    
    /**
     * Add gesture to sentence history and automatically speak it
     */
    private fun addToSentenceHistoryAndSpeak(sentence: String) {
        viewModelScope.launch {
            repository.addSentence(sentence)
            // Automatically speak the newly added sentence
            speakText(sentence)
        }
    }
    
    private fun getLocaleForDialect(code: String): Locale {
        return when (code) {
            "en" -> Locale.ENGLISH
            "fil" -> Locale("fil", "PH")
            "hil" -> Locale("hil", "PH")
            "ceb" -> Locale("ceb", "PH")
            "mrn" -> Locale("mrn", "PH")
            else -> Locale.ENGLISH
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        textToSpeech?.apply {
            stop()
            shutdown()
        }
    }
}