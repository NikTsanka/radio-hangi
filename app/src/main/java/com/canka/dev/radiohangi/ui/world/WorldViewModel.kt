package com.canka.dev.radiohangi.ui.world

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.canka.dev.radiohangi.RadioHangiApplication
import com.canka.dev.radiohangi.data.AppConfig
import com.canka.dev.radiohangi.data.repository.FavoritesRepository
import com.canka.dev.radiohangi.data.repository.RecentsRepository
import com.canka.dev.radiohangi.data.repository.WorldRepository
import com.canka.dev.radiohangi.domain.model.Country
import com.canka.dev.radiohangi.domain.model.SortOrder
import com.canka.dev.radiohangi.domain.model.Station
import com.canka.dev.radiohangi.domain.model.Tag
import com.canka.dev.radiohangi.player.PlaybackService
import com.canka.dev.radiohangi.player.buildStationMediaItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorldUiState(
    val query: String = "",
    val countries: List<Country> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val selectedCountry: Country? = null,
    val selectedTags: Set<String> = emptySet(),
    val sort: SortOrder = SortOrder.Votes,
    val stations: List<Station> = emptyList(),
    val favorites: List<Station> = emptyList(),
    val showFavoritesOnly: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null,
) {
    val favoriteUuids: Set<String> get() = favorites.mapTo(HashSet()) { it.uuid }
}

/** Identity of the filters that should trigger a fresh search. */
private data class FilterKey(
    val query: String,
    val countryCode: String?,
    val tags: Set<String>,
    val sort: SortOrder,
)

/**
 * ViewModel for Screen B (World Radio). Owns the search/filter/sort/pagination state, the
 * favorites stream, and a [MediaController] used to play a selected station.
 */
class WorldViewModel(
    application: Application,
    private val repository: WorldRepository,
    private val favoritesRepository: FavoritesRepository,
    private val recentsRepository: RecentsRepository,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(WorldUiState())
    val state = _state.asStateFlow()

    // One-shot user-facing messages (e.g. the result of a "like" vote), shown as a toast.
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    private var controller: MediaController? = null
    private var nextOffset = 0

    init {
        connectController()
        observeFavorites()
        loadFilters()
        observeFilterChanges()
    }

    private fun connectController() {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            { controller = runCatching { future.get() }.getOrNull() },
            ContextCompat.getMainExecutor(context),
        )
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoritesRepository.favorites.collect { favs ->
                _state.update { it.copy(favorites = favs) }
            }
        }
    }

    private fun loadFilters() {
        viewModelScope.launch {
            runCatching { repository.countries() }
                .onSuccess { countries -> _state.update { it.copy(countries = countries) } }
        }
        viewModelScope.launch {
            runCatching { repository.tags() }
                .onSuccess { tags -> _state.update { it.copy(tags = tags) } }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeFilterChanges() {
        viewModelScope.launch {
            _state
                .map { FilterKey(it.query, it.selectedCountry?.code, it.selectedTags, it.sort) }
                .distinctUntilChanged()
                .debounce(AppConfig.SEARCH_DEBOUNCE_MS) // 500ms debounced search
                // collectLatest: a newer filter cancels an in-flight search so stale results
                // never flash before the current ones.
                .collectLatest { runSearch(reset = true) }
        }
    }

    private suspend fun runSearch(reset: Boolean) {
        val current = _state.value
        if (reset) {
            nextOffset = 0
            _state.update { it.copy(isLoading = true, error = null) }
        } else {
            _state.update { it.copy(isLoadingMore = true, error = null) }
        }

        runCatching {
            repository.searchStations(
                name = current.query,
                countryCode = current.selectedCountry?.code,
                tags = current.selectedTags.toList(),
                sort = current.sort,
                offset = nextOffset,
            )
        }.onSuccess { page ->
            nextOffset += page.size
            _state.update { s ->
                s.copy(
                    stations = if (reset) page else s.stations + page,
                    isLoading = false,
                    isLoadingMore = false,
                    canLoadMore = page.size == AppConfig.STATIONS_PAGE_SIZE,
                    error = null,
                )
            }
        }.onFailure { e ->
            _state.update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.message ?: "Failed to load stations",
                )
            }
        }
    }

    // ---- UI events ----

    fun onQueryChange(query: String) = _state.update { it.copy(query = query) }

    fun onCountrySelected(country: Country?) = _state.update { it.copy(selectedCountry = country) }

    fun onToggleTag(tag: String) = _state.update { s ->
        val tags = if (tag in s.selectedTags) s.selectedTags - tag else s.selectedTags + tag
        s.copy(selectedTags = tags)
    }

    fun onSortSelected(sort: SortOrder) = _state.update { it.copy(sort = sort) }

    fun onToggleFavoritesOnly() = _state.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }

    fun loadMore() {
        val s = _state.value
        if (s.isLoading || s.isLoadingMore || !s.canLoadMore) return
        viewModelScope.launch { runSearch(reset = false) }
    }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch { favoritesRepository.toggle(station) }
    }

    /** Casts a "like" vote for the station on Radio Browser and reports the outcome via [messages]. */
    fun voteStation(station: Station) {
        viewModelScope.launch {
            val ok = repository.voteStation(station.uuid)
            _messages.tryEmit(
                if (ok) "Liked \"${station.name}\" 👍"
                else "You've already liked this station recently",
            )
        }
    }

    fun playStation(station: Station) {
        val c = controller ?: return
        if (station.streamUrl.isBlank()) return // no playable URL from Radio Browser
        c.setMediaItem(buildStationMediaItem(station))
        c.prepare()
        c.play()
        // Fire-and-forget click registration + record as recently played.
        viewModelScope.launch { repository.registerClick(station.uuid) }
        viewModelScope.launch { recentsRepository.add(station) }
    }

    override fun onCleared() {
        controller?.release()
        controller = null
        super.onCleared()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RadioHangiApplication
                WorldViewModel(
                    app,
                    app.container.worldRepository,
                    app.container.favoritesRepository,
                    app.container.recentsRepository,
                )
            }
        }
    }
}
