package com.canka.dev.radiohangi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.canka.dev.radiohangi.ui.radio.LyricsScreen
import com.canka.dev.radiohangi.ui.radio.RadioScreen
import com.canka.dev.radiohangi.ui.radio.RadioViewModel
import com.canka.dev.radiohangi.ui.world.WorldScreen

/** Non-tab route for the full-screen lyrics page (reached from the app-bar button). */
const val LYRICS_ROUTE = "lyrics"

/** Single-activity navigation graph: the two top-level screens plus the lyrics page. */
@Composable
fun RadioHangiNavHost(
    navController: NavHostController,
    radioViewModel: RadioViewModel,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.START.route,
        modifier = modifier,
    ) {
        composable(Destination.Radio.route) { RadioScreen(radioViewModel) }
        composable(Destination.World.route) { WorldScreen() }
        composable(LYRICS_ROUTE) { LyricsScreen(radioViewModel) }
    }
}
