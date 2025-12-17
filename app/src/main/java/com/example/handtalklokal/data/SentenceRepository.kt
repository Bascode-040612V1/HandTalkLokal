package com.example.handtalklokal.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore extension for Context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sentences")

class SentenceRepository(private val context: Context) {
    
    private val SENTENCES_KEY = stringPreferencesKey("saved_sentences")
    
    val sentences: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            val sentencesString = preferences[SENTENCES_KEY] ?: ""
            if (sentencesString.isEmpty()) {
                emptyList()
            } else {
                sentencesString.split("|SEPARATOR|")
            }
        }
    
    suspend fun addSentence(sentence: String) {
        context.dataStore.edit { preferences ->
            val currentSentences = preferences[SENTENCES_KEY] ?: ""
            val newSentences = if (currentSentences.isEmpty()) {
                sentence
            } else {
                "$currentSentences|SEPARATOR|$sentence"
            }
            preferences[SENTENCES_KEY] = newSentences
        }
    }
    
    suspend fun clearSentences() {
        context.dataStore.edit { preferences ->
            preferences[SENTENCES_KEY] = ""
        }
    }
    
    suspend fun deleteSentence(sentence: String) {
        context.dataStore.edit { preferences ->
            val currentSentences = preferences[SENTENCES_KEY] ?: ""
            if (currentSentences.isNotEmpty()) {
                val sentencesList = currentSentences.split("|SEPARATOR|").toMutableList()
                sentencesList.remove(sentence)
                preferences[SENTENCES_KEY] = sentencesList.joinToString("|SEPARATOR|")
            }
        }
    }
}