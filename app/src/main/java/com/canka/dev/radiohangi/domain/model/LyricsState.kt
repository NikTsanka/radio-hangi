package com.canka.dev.radiohangi.domain.model

/**
 * Lyrics panel state for Screen A. Distinguishes "still loading" from "definitively not
 * found" so the UI shows a graceful message instead of a blank or perpetual spinner.
 */
sealed interface LyricsState {
    data object Idle : LyricsState
    data object Loading : LyricsState
    data class Found(val text: String) : LyricsState
    data object NotFound : LyricsState
}
