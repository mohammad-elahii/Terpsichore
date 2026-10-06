package com.Mohammad.Elahi.terpsichore.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.Mohammad.Elahi.terpsichore.R
import com.Mohammad.Elahi.terpsichore.core.sources.SearchAggregator
import com.Mohammad.Elahi.terpsichore.core.sources.SearchResult
import com.Mohammad.Elahi.terpsichore.core.sources.toChip
import com.Mohammad.Elahi.terpsichore.di.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
)

class SearchViewModel(private val aggregator: SearchAggregator) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty() || _uiState.value.loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, failed = false) }
            try {
                val results = aggregator.search(query)
                _uiState.update { it.copy(results = results, loading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, failed = true) }
            }
        }
    }
}

@Composable
fun SearchScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val viewModel: SearchViewModel = viewModel { SearchViewModel(container.searchAggregator) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            label = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (state.failed) {
            Text(
                text = stringResource(R.string.search_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(
                        if (state.query.isBlank()) R.string.search_empty_prompt
                        else R.string.search_no_results
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.results, key = { "${it.source.name}-${it.track.id}" }) { result ->
                    TrackRow(result)
                }
            }
        }
    }
}

@Composable
private fun TrackRow(result: SearchResult, modifier: Modifier = Modifier) {
    val track = result.track
    val chip = result.quality.toChip()
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = track.title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = listOfNotNull(track.artist, track.album).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = listOfNotNull(result.source.name, chip.label.takeIf { chip.visible })
                .joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
