package com.canka.dev.radiohangi.ui.radio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canka.dev.radiohangi.domain.model.LyricsState

/**
 * Full-screen lyrics for the currently playing song. Reads the same [RadioViewModel] as the
 * Radio screen (shared instance), so it always reflects what's playing — the Zeno home stream
 * or a World station that broadcasts an "Artist - Title" stream title.
 */
@Composable
fun LyricsScreen(viewModel: RadioViewModel) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val current = ui.current

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground(coverUrl = current?.coverUrl)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = current?.song ?: "Radio Hangi",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = current?.artist?.takeIf { it.isNotBlank() } ?: "Live stream",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            if (ui.isStation && ui.lyrics == LyricsState.Idle) {
                // A World station that isn't broadcasting an "Artist - Title" song right now.
                Text(
                    text = "Lyrics appear when the station shares the current song.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LyricsPanel(
                    state = ui.lyrics,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
