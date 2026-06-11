package com.canka.dev.radiohangi.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.canka.dev.radiohangi.data.AppConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Player preferences that survive app restarts (currently just the volume slider level).
private val Context.playerDataStore by preferencesDataStore(name = "player")

/** Persists the last chosen player volume so a fresh launch restores it. */
class PlayerPrefsRepository(private val context: Context) {

    private val volumeKey = floatPreferencesKey("volume")

    val volume: Flow<Float> = context.playerDataStore.data.map { prefs ->
        (prefs[volumeKey] ?: AppConfig.DEFAULT_VOLUME).coerceIn(0f, 1f)
    }

    suspend fun saveVolume(value: Float) {
        context.playerDataStore.edit { prefs -> prefs[volumeKey] = value.coerceIn(0f, 1f) }
    }
}
