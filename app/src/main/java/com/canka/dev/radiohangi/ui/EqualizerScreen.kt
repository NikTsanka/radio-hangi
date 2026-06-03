package com.canka.dev.radiohangi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canka.dev.radiohangi.RadioHangiApplication
import kotlin.math.roundToInt

/**
 * Audio equalizer: an enable switch, a preset picker, and a band-gain slider per frequency.
 * Reads/controls the app-scoped [com.canka.dev.radiohangi.player.EqualizerController], which is
 * bound to the playback session. Shows a graceful message where no equalizer effect exists.
 */
@Composable
fun EqualizerScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val controller = (context.applicationContext as RadioHangiApplication).container.equalizerController
    val state by controller.state.collectAsStateWithLifecycle()

    if (!state.available) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "The equalizer isn't available on this device.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Equalizer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Switch(checked = state.enabled, onCheckedChange = controller::setEnabled)
        }

        Spacer(Modifier.height(16.dp))

        PresetPicker(
            presets = state.presets,
            selected = state.selectedPreset,
            enabled = state.enabled,
            onSelect = controller::usePreset,
        )

        Spacer(Modifier.height(20.dp))

        state.bandFrequencies.forEachIndexed { index, freqHz ->
            val level = state.bandLevels.getOrElse(index) { 0 }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatFrequency(freqHz),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(0.22f),
                )
                Slider(
                    value = level.toFloat(),
                    onValueChange = { controller.setBandLevel(index, it.roundToInt().toShort()) },
                    valueRange = state.minLevel.toFloat()..state.maxLevel.toFloat(),
                    enabled = state.enabled,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatGain(level),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(0.18f).padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun PresetPicker(
    presets: List<String>,
    selected: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    if (presets.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val label = presets.getOrNull(selected) ?: "Custom"
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Preset: $label", modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            presets.forEachIndexed { index, name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun formatFrequency(hz: Int): String =
    if (hz >= 1000) "${hz / 1000} kHz" else "$hz Hz"

/** Millibels → a signed dB label, e.g. "+3 dB". */
private fun formatGain(millibels: Short): String {
    val db = (millibels / 100f).roundToInt()
    return if (db > 0) "+$db dB" else "$db dB"
}
