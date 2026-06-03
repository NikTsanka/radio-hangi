package com.canka.dev.radiohangi.ui.world

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.canka.dev.radiohangi.domain.model.Country
import com.canka.dev.radiohangi.domain.model.SortOrder
import com.canka.dev.radiohangi.domain.model.Station

/**
 * Screen B — World Radio. Debounced search, country + sort dropdowns, multi-select genre
 * chips, paginated station list, and a favorites view with a count badge.
 */
@Composable
fun WorldScreen(viewModel: WorldViewModel = viewModel(factory = WorldViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val favoriteUuids = state.favoriteUuids

    // The station whose info sheet is open (long-press), or null when none.
    var infoStation by remember { mutableStateOf<Station?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            label = { Text("Search stations") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CountryDropdown(
                countries = state.countries,
                selected = state.selectedCountry,
                onSelect = viewModel::onCountrySelected,
                modifier = Modifier.weight(1f),
            )
            SortDropdown(
                selected = state.sort,
                onSelect = viewModel::onSortSelected,
                modifier = Modifier.weight(1f),
            )
        }

        GenreChipsRow(
            tags = state.tags,
            selected = state.selectedTags,
            onToggle = viewModel::onToggleTag,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        // Browse / Favorites toggle (favorites carries a count badge).
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = !state.showFavoritesOnly,
                onClick = { if (state.showFavoritesOnly) viewModel.onToggleFavoritesOnly() },
                label = { Text("Browse") },
            )
            FilterChip(
                selected = state.showFavoritesOnly,
                onClick = { if (!state.showFavoritesOnly) viewModel.onToggleFavoritesOnly() },
                label = { Text("Favorites (${state.favorites.size})") },
            )
        }

        val list: List<Station> = if (state.showFavoritesOnly) state.favorites else state.stations

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && list.isEmpty() ->
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center),
                    )

                state.error != null && list.isEmpty() ->
                    ErrorState(state.error!!, Modifier.align(Alignment.Center))

                list.isEmpty() ->
                    Text(
                        text = if (state.showFavoritesOnly) "No favorites yet" else "No stations found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )

                else -> StationList(
                    stations = list,
                    favoriteUuids = favoriteUuids,
                    showLoadMore = !state.showFavoritesOnly && state.canLoadMore,
                    isLoadingMore = state.isLoadingMore,
                    onPlay = viewModel::playStation,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onLoadMore = viewModel::loadMore,
                    onShowInfo = { infoStation = it },
                )
            }
        }
    }

    // Long-pressing a station opens its details.
    infoStation?.let { station ->
        StationInfoSheet(
            station = station,
            isFavorite = station.uuid in favoriteUuids,
            onDismiss = { infoStation = null },
            onPlay = { viewModel.playStation(station) },
            onToggleFavorite = { viewModel.toggleFavorite(station) },
        )
    }
}

@Composable
private fun StationList(
    stations: List<Station>,
    favoriteUuids: Set<String>,
    showLoadMore: Boolean,
    isLoadingMore: Boolean,
    onPlay: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    onLoadMore: () -> Unit,
    onShowInfo: (Station) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(stations, key = { it.uuid }) { station ->
            StationRow(
                station = station,
                isFavorite = station.uuid in favoriteUuids,
                onPlay = { onPlay(station) },
                onToggleFavorite = { onToggleFavorite(station) },
                onLongPress = { onShowInfo(station) },
            )
        }
        if (showLoadMore) {
            item(key = "load_more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else {
                        Button(onClick = onLoadMore) { Text("Load more") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryDropdown(
    countries: List<Country>,
    selected: Country?,
    onSelect: (Country?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = selected?.let { "${it.flagPrefix}${it.name}" } ?: "All countries",
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("All countries") },
                onClick = { onSelect(null); expanded = false },
            )
            countries.forEach { country ->
                DropdownMenuItem(
                    text = { Text("${country.flagPrefix}${country.name} (${country.stationCount})") },
                    onClick = { onSelect(country); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun SortDropdown(
    selected: SortOrder,
    onSelect: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(text = selected.label, maxLines = 1, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.label) },
                    onClick = { onSelect(order); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(24.dp),
    )
}
