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

// Single DataStore file for World Radio favorites (mirrors the web localStorage approach).
private val Context.favoritesDataStore by preferencesDataStore(name = AppConfig.FAVORITES_DATASTORE)

/**
 * Persists World Radio favorite stations as a JSON-encoded list in Preferences DataStore.
 * Mirrors the web version's `wr-favs` / `wr-favs-data` localStorage keys.
 */
class FavoritesRepository(
    private val context: Context,
    private val json: Json,
) {
    private val key = stringPreferencesKey(AppConfig.FAVORITES_KEY)

    val favorites: Flow<List<Station>> = context.favoritesDataStore.data.map { prefs ->
        prefs[key]?.let { decode(it) } ?: emptyList()
    }

    /** Adds the station if absent, removes it if present (by uuid). */
    suspend fun toggle(station: Station) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[key]?.let { decode(it) } ?: emptyList()
            val updated = if (current.any { it.uuid == station.uuid }) {
                current.filterNot { it.uuid == station.uuid }
            } else {
                current + station
            }
            prefs[key] = json.encodeToString(updated)
        }
    }

    private fun decode(raw: String): List<Station> =
        runCatching { json.decodeFromString<List<Station>>(raw) }.getOrDefault(emptyList())
}
