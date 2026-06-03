package com.canka.dev.radiohangi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.canka.dev.radiohangi.ui.RadioHangiApp
import com.canka.dev.radiohangi.ui.SplashScreenContent
import com.canka.dev.radiohangi.ui.theme.RadioHangiTheme
import com.canka.dev.radiohangi.ui.theme.ThemeMode

/**
 * The single Activity. Hosts the Compose UI tree and owns the manual theme-mode state
 * (System → Light → Dark) that the in-app toggle cycles through.
 */
class MainActivity : ComponentActivity() {

    // Result is ignored: if the user declines, playback still works — only the
    // notification is suppressed by the system.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 12+ splash (vinyl icon on a dark backdrop). Must run before super.onCreate.
        installSplashScreen()
    super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.System) }
            RadioHangiTheme(themeMode = themeMode) {
                // Brief in-app splash (the spinning record) cross-fading into the app.
                var showSplash by remember { mutableStateOf(true) }
                Crossfade(targetState = showSplash, animationSpec = tween(500), label = "splash") { splash ->
                    if (splash) {
                        SplashScreenContent(onFinished = { showSplash = false })
                    } else {
                        RadioHangiApp(
                            themeMode = themeMode,
                            onCycleTheme = {
                                themeMode = when (themeMode) {
                                    ThemeMode.System -> ThemeMode.Light
                                    ThemeMode.Light -> ThemeMode.Dark
                                    ThemeMode.Dark -> ThemeMode.System
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    /** POST_NOTIFICATIONS is a runtime permission on Android 13 (API 33) and above. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
