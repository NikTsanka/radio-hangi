package com.canka.dev.radiohangi.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radio
import androidx.compose.ui.graphics.vector.ImageVector

/** Top-level navigation destinations, shown as bottom-bar items. */
enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Radio("radio", "Radio", Icons.Filled.Radio),   // Screen A — Main Radio
    World("world", "World", Icons.Filled.Public);   // Screen B — World Radio

    companion object {
        val START: Destination = Radio
        fun fromRoute(route: String?): Destination =
            entries.firstOrNull { it.route == route } ?: START
    }
}
