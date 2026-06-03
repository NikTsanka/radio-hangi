package com.canka.dev.radiohangi.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.canka.dev.radiohangi.data.AppConfig
import com.canka.dev.radiohangi.domain.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Single DataStore file for recently-played stations.
private val Context.recentsDataStore by preferencesDataStore(name = AppConfig.RECENTS_DATASTORE)

/**
 * Persists recently-played World Radio stations as a JSON list in Preferences DataStore,
 * newest first and capped at [AppConfig.RECENTS_SIZE]. Used by the Radio-screen quick-play row
 * and the home-screen widget.
 */
class RecentsRepository(
    private val context: Context,
    private val json: Json,
) {
    private val key = stringPreferencesKey(AppConfig.RECENTS_KEY)

    val recents: Flow<List<Station>> = context.recentsDataStore.data.map { prefs ->
        prefs[key]?.let { decode(it) } ?: emptyList()
    }

    /** Records a played station: moves it to the front, de-dupes by uuid, caps the list. */
    suspend fun add(station: Station) {
        context.recentsDataStore.edit { prefs ->
            val current = prefs[key]?.let { decode(it) } ?: emptyList()
            val updated = (listOf(station) + current.filterNot { it.uuid == station.uuid })
                .take(AppConfig.RECENTS_SIZE)
            prefs[key] = json.encodeToString(updated)
        }
    }

    private fun decode(raw: String): List<Station> =
        runCatching { json.decodeFromString<List<Station>>(raw) }.getOrDefault(emptyList())
}
