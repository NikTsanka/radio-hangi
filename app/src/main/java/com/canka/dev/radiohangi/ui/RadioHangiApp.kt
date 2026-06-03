package com.canka.dev.radiohangi.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.canka.dev.radiohangi.RadioHangiApplication
import com.canka.dev.radiohangi.player.rememberManagedMediaController
import com.canka.dev.radiohangi.ui.navigation.Destination
import com.canka.dev.radiohangi.ui.navigation.EQUALIZER_ROUTE
import com.canka.dev.radiohangi.ui.navigation.LYRICS_ROUTE
import com.canka.dev.radiohangi.ui.navigation.RadioHangiNavHost
import com.canka.dev.radiohangi.ui.radio.ConnectionIndicator
import com.canka.dev.radiohangi.ui.radio.RadioViewModel
import com.canka.dev.radiohangi.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Root app shell: top app bar (with the theme toggle), bottom navigation between the two
 * screens, and the navigation host. Theme state is hoisted up to [com.canka.dev.radiohangi.MainActivity].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioHangiApp(
    themeMode: ThemeMode,
    onCycleTheme: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rawRoute = backStackEntry?.destination?.route
    val isLyrics = rawRoute == LYRICS_ROUTE
    val isEqualizer = rawRoute == EQUALIZER_ROUTE
    val isSubPage = isLyrics || isEqualizer // full-screen pages with a back arrow (not tabs)
    val current = Destination.fromRoute(rawRoute)

    // Shared across the Radio screen and the Lyrics page so both reflect the same now-playing
    // state (and don't each spin up their own MediaController). Scoped to the Activity since
    // this composable sits above the NavHost.
    val radioViewModel: RadioViewModel = viewModel(factory = RadioViewModel.Factory)

    // Back behavior: from the lyrics page, back returns to Radio; from a non-home tab, back
    // returns to Radio; on Radio, the first back arms an "exit" prompt and a second back within
    // the window finishes the app (kills the task).
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exitArmed by remember { mutableStateOf(false) }

    // Zeno connection status drives the "Live" indicator hoisted into the app bar.
    val appContainer = (context.applicationContext as RadioHangiApplication).container
    val connection by appContainer.radioRepository.connection.collectAsStateWithLifecycle()

    // A controller so the explicit double-back "exit" can stop playback (swipe-from-recents
    // still keeps playing — that path is handled by PlaybackService.onTaskRemoved).
    val controller by rememberManagedMediaController()
    BackHandler {
        when {
            isSubPage -> navController.popBackStack()
            current != Destination.Radio -> navController.navigateToTab(Destination.Radio)
            exitArmed -> {
                controller?.run {
                    stop()
                    clearMediaItems()
                }
                (context as? Activity)?.finishAndRemoveTask()
            }
            else -> {
                exitArmed = true
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                scope.launch {
                    delay(2_000)
                    exitArmed = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    if (isSubPage) {
                        // Sub-page (Lyrics / Equalizer): a back arrow returns to the Radio screen.
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        // Brand pinned to the left corner, with the live status beside it on Radio.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Radio Hangi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp),
                            )
                            if (current == Destination.Radio) {
                                ConnectionIndicator(
                                    status = connection,
                                    modifier = Modifier.padding(start = 10.dp),
                                )
                            }
                        }
                    }
                },
                // Center: the sub-page name, the screen name on World, empty on Radio.
                title = {
                    when {
                        isLyrics -> Text("Lyrics")
                        isEqualizer -> Text("Equalizer")
                        current != Destination.Radio -> Text("World Radio")
                    }
                },
                actions = {
                    // Home / Lyrics / Equalizer shortcuts, only on the Radio tab.
                    if (current == Destination.Radio && !isSubPage) {
                        IconButton(onClick = radioViewModel::playHomeRadio) {
                            Icon(Icons.Filled.Home, contentDescription = "Home radio")
                        }
                        IconButton(onClick = { navController.navigate(LYRICS_ROUTE) }) {
                            Icon(Icons.Filled.Lyrics, contentDescription = "Lyrics")
                        }
                        IconButton(onClick = { navController.navigate(EQUALIZER_ROUTE) }) {
                            Icon(Icons.Filled.GraphicEq, contentDescription = "Equalizer")
                        }
                    }
                    if (!isSubPage) {
                        IconButton(onClick = onCycleTheme) {
                            Icon(
                                imageVector = themeIcon(themeMode),
                                contentDescription = "Toggle theme (current: ${themeMode.name})",
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Column {
                // Persistent mini-player above the nav bar. Hidden on the Radio screen, which
                // already shows the full now-playing; tapping it there would be redundant.
                if (current != Destination.Radio) {
                    MiniPlayer(onOpen = { navController.navigateToTab(Destination.Radio) })
                }
                NavigationBar {
                    Destination.entries.forEach { dest ->
                        val selected = backStackEntry?.destination
                            ?.hierarchy
                            ?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(dest) },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        RadioHangiNavHost(
            navController = navController,
            radioViewModel = radioViewModel,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/** Navigate to a top-level [dest] with standard single-top / save-and-restore bottom-nav behavior. */
private fun NavHostController.navigateToTab(dest: Destination) {
    navigate(dest.route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun themeIcon(mode: ThemeMode): ImageVector = when (mode) {
    ThemeMode.System -> Icons.Filled.BrightnessAuto
    ThemeMode.Light -> Icons.Filled.LightMode
    ThemeMode.Dark -> Icons.Filled.DarkMode
}
