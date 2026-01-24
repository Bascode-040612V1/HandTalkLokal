package com.example.handtalklokal.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore extension for Context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tutorial")

class TutorialRepository(private val context: Context) {
    
    private val TUTORIAL_COMPLETED_KEY = booleanPreferencesKey("tutorial_completed")
    
    val isTutorialCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[TUTORIAL_COMPLETED_KEY] ?: false
        }
    
    suspend fun setTutorialCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TUTORIAL_COMPLETED_KEY] = completed
        }
    }
}