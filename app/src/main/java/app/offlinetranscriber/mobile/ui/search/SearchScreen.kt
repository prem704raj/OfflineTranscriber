package app.offlinetranscriber.mobile.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.offlinetranscriber.mobile.data.model.SearchResultRow
import app.offlinetranscriber.mobile.search.SearchFilter
import app.offlinetranscriber.mobile.search.SearchViewModel
import app.offlinetranscriber.mobile.ui.design.AppDimens
import app.offlinetranscriber.mobile.ui.design.AppShapes
import app.offlinetranscriber.mobile.ui.icons.OtIcons
import app.offlinetranscriber.mobile.ui.layout.ReadingWidthContainer
import app.offlinetranscriber.mobile.ui.system.OtEmptyState
import app.offlinetranscriber.mobile.ui.system.OtStatusKind
import app.offlinetranscriber.mobile.ui.system.OtStatusLabel

@Composable
fun SearchScreen(
    onOpenResult: (SearchResultRow) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    ReadingWidthContainer(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = AppDimens.ScreenHorizontal,
                end = AppDimens.ScreenHorizontal,
                top = AppDimens.Space6,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space3)
        ) {
            item {
                Text(
                    text = "Search your knowledge",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(AppDimens.Space1))

                Text(
                    text = "Find the exact moment across every local transcript.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(AppDimens.Space4))

                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = OtIcons.SearchTimeline,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(
                                onClick = viewModel::clearQuery
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    placeholder = {
                        Text("Search lectures, meetings, videos…")
                    },
                    shape = AppShapes.Control
                )

                Spacer(Modifier.height(AppDimens.Space3))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Space2)
                ) {
                    items(
                        count = SearchFilter.entries.size
                    ) { index ->
                        val filter = SearchFilter.entries[index]

                        FilterChip(
                            selected = state.filter == filter,
                            onClick = {
                                viewModel.setFilter(filter)
                            },
                            label = {
                                Text(filter.label)
                            }
                        )
                    }
                }

                if (state.loading) {
                    Spacer(Modifier.height(AppDimens.Space3))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            when {
                state.query.isBlank() -> {
                    item {
                        SearchWelcomeState()
                    }
                }

                state.searched &&
                    !state.loading &&
                    state.results.isEmpty() -> {
                    item {
                        OtEmptyState(
                            title = "No matching moments",
                            body = "None of your local transcripts contain this search.",
                            modifier = Modifier.padding(top = 40.dp),
                            action = if (state.filter != SearchFilter.ALL) {
                                {
                                    TextButton(
                                        onClick = { viewModel.setFilter(SearchFilter.ALL) }
                                    ) {
                                        Text("Clear filters")
                                    }
                                }
                            } else null
                        )
                    }
                }

                else -> {
                    items(
                        items = state.results,
                        key = { "${it.transcriptId}_${it.segmentId}" }
                    ) { result ->
                        SearchResultCard(
                            result = result,
                            query = state.query,
                            onOpen = {
                                onOpenResult(result)
                            },
                            onToggleBookmark = {
                                viewModel.toggleBookmark(
                                    transcriptId = result.transcriptId,
                                    segmentId = result.segmentId
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchWelcomeState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.Space5)
    ) {
        OtStatusLabel(
            text = "Private local search",
            kind = OtStatusKind.LOCAL
        )

        Spacer(Modifier.height(AppDimens.Space2))

        Text(
            text = "Search happens entirely on this device. Try a topic such as “normalization”, “budget”, or “deadline”.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
