package com.canka.dev.radiohangi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canka.dev.radiohangi.ui.radio.SpinningDisc
import kotlinx.coroutines.delay

/** Backdrop matching the system splash ([R.color.splash_bg]) so the hand-off is seamless. */
private val SplashBackground = Color(0xFF0E0E10)
private val BrandGold = Color(0xFFC9A96E)

/**
 * In-app splash: the spinning vinyl record (bundled cover art as its label) on the same dark
 * backdrop as the system splash, shown briefly while the app warms up. Calls [onFinished] after
 * [holdMillis] so the host can cross-fade to the main UI.
 */
@Composable
fun SplashScreenContent(onFinished: () -> Unit, holdMillis: Long = 2_500L) {
    LaunchedEffect(Unit) {
        delay(holdMillis)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Reuse the real now-playing disc; null cover → bundled cover.png as the label.
            SpinningDisc(
                coverUrl = null,
                isPlaying = true,
                modifier = Modifier.size(220.dp),
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = "Radio Hangi",
                color = BrandGold,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )
        }
    }
}
