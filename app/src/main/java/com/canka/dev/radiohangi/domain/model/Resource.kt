package com.canka.dev.radiohangi.domain.model

/**
 * Generic load-state wrapper for async UI data (album art, lyrics, station lists, …).
 * Lets every screen render Loading / Success / Error uniformly without crashing on bad data.
 */
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>
}
