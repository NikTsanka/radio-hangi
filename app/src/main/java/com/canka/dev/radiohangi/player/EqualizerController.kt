package com.canka.dev.radiohangi.player

import android.content.Context
import android.media.audiofx.Equalizer
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Equalizer settings survive app restarts (enabled flag + selected preset).
private val Context.equalizerDataStore by preferencesDataStore(name = "equalizer")
private val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
private val EQ_PRESET = intPreferencesKey("eq_preset")

/**
 * UI-facing snapshot of the equalizer. [available] is false on devices (and most emulators)
 * that don't provide an Equalizer audio effect — the UI degrades to an "unavailable" message.
 */
data class EqualizerState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val presets: List<String> = emptyList(),
    /** Active preset index, or -1 ("Custom") after manual band edits. */
    val selectedPreset: Int = -1,
    val bandFrequencies: List<Int> = emptyList(), // center frequency per band, Hz
    val bandLevels: List<Short> = emptyList(),     // current level per band, millibels
    val minLevel: Short = 0,
    val maxLevel: Short = 0,
)

/**
 * Wraps a system [Equalizer] effect bound to the playback ExoPlayer's audio session. The
 * session id is supplied by [PlaybackService] via [bind]; the UI reads [state] and calls the
 * mutators. App-scoped singleton (held in [com.canka.dev.radiohangi.data.AppContainer]).
 *
 * Desired enabled/preset are retained so they survive a session rebind. All effect calls are
 * guarded — a flaky AudioEffect implementation can throw, and that must never crash playback.
 */
class EqualizerController(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var equalizer: Equalizer? = null
    private var sessionId: Int = 0

    private var desiredEnabled = false
    private var desiredPreset: Short = 0

    init {
        // Restore the persisted enabled flag + preset; if the effect is already bound
        // (unlikely — playback starts after user interaction), re-apply on the spot.
        scope.launch {
            val prefs = context.equalizerDataStore.data.first()
            desiredEnabled = prefs[EQ_ENABLED] ?: false
            desiredPreset = (prefs[EQ_PRESET] ?: 0).toShort()
            equalizer?.let { eq ->
                runCatching {
                    eq.enabled = desiredEnabled
                    if (desiredEnabled && desiredPreset < eq.numberOfPresets) eq.usePreset(desiredPreset)
                }
                publish()
            }
        }
    }

    private fun persist() {
        scope.launch {
            context.equalizerDataStore.edit { prefs ->
                prefs[EQ_ENABLED] = desiredEnabled
                prefs[EQ_PRESET] = desiredPreset.toInt()
            }
        }
    }

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    /** Called by the service once the ExoPlayer audio session id is known. */
    fun bind(sessionId: Int) {
        if (sessionId == this.sessionId && equalizer != null) return
        this.sessionId = sessionId
        rebuild()
    }

    private fun rebuild() {
        equalizer?.let { runCatching { it.release() } }
        equalizer = null
        if (sessionId == 0) {
            _state.value = EqualizerState()
            return
        }
        val eq = runCatching { Equalizer(0, sessionId) }.getOrNull()
        if (eq == null) {
            _state.value = EqualizerState(available = false)
            return
        }
        equalizer = eq
        runCatching {
            eq.enabled = desiredEnabled
            if (desiredEnabled && desiredPreset < eq.numberOfPresets) eq.usePreset(desiredPreset)
        }
        publish()
    }

    private fun publish() {
        val eq = equalizer ?: run {
            _state.value = EqualizerState()
            return
        }
        _state.value = runCatching {
            val bands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange // [min, max], millibels
            EqualizerState(
                available = true,
                enabled = eq.enabled,
                presets = (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) },
                selectedPreset = eq.currentPreset.toInt(),
                bandFrequencies = (0 until bands).map { eq.getCenterFreq(it.toShort()) / 1000 },
                bandLevels = (0 until bands).map { eq.getBandLevel(it.toShort()) },
                minLevel = range[0],
                maxLevel = range[1],
            )
        }.getOrElse { EqualizerState(available = false) }
    }

    fun setEnabled(enabled: Boolean) {
        desiredEnabled = enabled
        equalizer?.let { runCatching { it.enabled = enabled } }
        persist()
        publish()
    }

    fun usePreset(preset: Int) {
        desiredPreset = preset.toShort()
        equalizer?.let { runCatching { it.usePreset(preset.toShort()) } }
        persist()
        publish()
    }

    fun setBandLevel(band: Int, level: Short) {
        equalizer?.let { runCatching { it.setBandLevel(band.toShort(), level) } }
        publish()
    }

    fun release() {
        equalizer?.let { runCatching { it.release() } }
        equalizer = null
    }
}
