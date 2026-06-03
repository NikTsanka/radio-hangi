package com.canka.dev.radiohangi.domain.model

/** High-level playback state surfaced to the UI (derived from Media3 Player state). */
enum class PlaybackState { Idle, Buffering, Playing, Paused, Error }

/** Connection-status indicator state for the stream/metadata pipeline. */
enum class ConnectionStatus { Idle, Connecting, Connected, Error }

/** Sleep-timer choices shown on Screen A (Off / 15 / 30 / 60 min). */
enum class SleepTimerOption(val minutes: Int, val label: String) {
    Off(0, "Off"),
    Min15(15, "15 min"),
    Min30(30, "30 min"),
    Min60(60, "60 min"),
}
