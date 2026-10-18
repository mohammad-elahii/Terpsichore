package com.Mohammad.Elahi.terpsichore.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.Mohammad.Elahi.terpsichore.R
import com.Mohammad.Elahi.terpsichore.core.sources.QualityChip
import com.Mohammad.Elahi.terpsichore.core.sources.SearchAggregator
import com.Mohammad.Elahi.terpsichore.core.sources.SearchResult
import com.Mohammad.Elahi.terpsichore.core.sources.Source
import com.Mohammad.Elahi.terpsichore.core.sources.toChip
import com.Mohammad.Elahi.terpsichore.di.AppContainer
import com.Mohammad.Elahi.terpsichore.ui.preview.PreviewPlayer
import com.Mohammad.Elahi.terpsichore.ui.preview.PreviewState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEBOUNCE_MS = 400L
private const val MIN_QUERY_LENGTH = 2

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
    val partialFailures: Boolean = false,
)

class SearchViewModel(
    private val aggregator: SearchAggregator,
    private val sources: List<com.Mohammad.Elahi.terpsichore.core.sources.MusicSource>,
) : ViewModel() {

    val previewPlayer = PreviewPlayer(viewModelScope)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var debounceJob: Job? = null
    private var searchGeneration: Int = 0

    fun onQueryChange(query: String) {
        debounceJob?.cancel()
        debounceJob = null
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) {
            searchGeneration++
            _uiState.update {
                it.copy(
                    query = query,
                    results = emptyList(),
                    loading = false,
                    failed = false,
                    partialFailures = false,
                )
            }
            return
        }
        _uiState.update { it.copy(query = query) }
        debounceJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            performSearch(trimmed)
        }
    }

    fun search() {
        debounceJob?.cancel()
        debounceJob = null
        val query = _uiState.value.query.trim()
        performSearch(query)
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) return
        val generation = ++searchGeneration
        _uiState.update { it.copy(loading = true, failed = false, partialFailures = false) }
        viewModelScope.launch {
            try {
                val outcome = aggregator.searchWithFailures(query)
                if (generation != searchGeneration) return@launch
                _uiState.update {
                    it.copy(
                        results = outcome.results,
                        loading = false,
                        failed = outcome.allSourcesFailed,
                        partialFailures = outcome.failures.isNotEmpty() && outcome.results.isNotEmpty(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (generation != searchGeneration) return@launch
                _uiState.update { it.copy(loading = false, failed = true) }
            }
        }
    }

    fun previewUrlFor(result: SearchResult): suspend () -> String? = {
        result.track.previewUrl ?: sources
            .firstOrNull { it.id == result.source }
            ?.previewUrl(result.track.id)
    }

    override fun onCleared() {
        previewPlayer.release()
        super.onCleared()
    }
}

@Composable
fun SearchScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val viewModel: SearchViewModel = viewModel {
        SearchViewModel(container.searchAggregator, container.sources)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val previewState by viewModel.previewPlayer.state.collectAsStateWithLifecycle()

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
        } else if (state.partialFailures && state.results.isNotEmpty()) {
            Text(
                text = stringResource(R.string.search_partial),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            if (state.results.isEmpty()) {
                if (!state.loading && !state.failed) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(
                                if (state.query.isBlank()) R.string.search_empty_prompt
                                else R.string.search_no_results
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp),
                ) {
                    items(state.results, key = { "${it.source.name}-${it.track.id}" }) { result ->
                        TrackRow(
                            result = result,
                            previewState = previewState,
                            player = viewModel.previewPlayer,
                            urlProvider = viewModel.previewUrlFor(result),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    result: SearchResult,
    previewState: PreviewState,
    player: PreviewPlayer,
    urlProvider: suspend () -> String?,
    modifier: Modifier = Modifier,
) {
    val track = result.track
    val rowKey = "${result.source.name}-${track.id}"
    val previewActive = previewState.key == rowKey
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(56.dp),
                ) {
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(
                            track.artist,
                            track.durationSeconds?.let { formatDuration(it) },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (result.source == Source.DEEZER) {
                    IconButton(
                        onClick = { player.toggle(rowKey, urlProvider) },
                        enabled = true,
                    ) {
                        Icon(
                            imageVector = if (previewActive && (previewState.playing || previewState.loading)) {
                                Icons.Filled.Stop
                            } else {
                                Icons.Filled.PlayArrow
                            },
                            contentDescription = stringResource(R.string.btn_preview),
                        )
                    }
                }
                IconButton(onClick = {}, enabled = false) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = stringResource(R.string.btn_download),
                    )
                }
            }
            if (previewActive) {
                LinearProgressIndicator(
                    progress = { previewState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SourceBadge(result.source)
                QualityChipView(result.quality.toChip())
            }
        }
    }
}

@Composable
private fun SourceBadge(source: Source, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier,
    ) {
        Text(
            text = source.label(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun QualityChipView(chip: QualityChip, modifier: Modifier = Modifier) {
    if (!chip.visible) return
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier,
    ) {
        Text(
            text = chip.label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

private fun Source.label(): String = when (this) {
    Source.DEEZER -> "Deezer"
    Source.JAMENDO -> "Jamendo"
    Source.ARCHIVE -> "Archive"
    Source.AUDIUS -> "Audius"
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainder = seconds % 60
    return "$minutes:${remainder.toString().padStart(2, '0')}"
}
