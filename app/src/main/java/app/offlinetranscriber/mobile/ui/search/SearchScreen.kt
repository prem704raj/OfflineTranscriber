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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.offlinetranscriber.mobile.data.model.SearchResultRow
import app.offlinetranscriber.mobile.search.SearchFilter
import app.offlinetranscriber.mobile.search.SearchViewModel

@Composable
fun SearchScreen(
    onOpenResult: (SearchResultRow) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 110.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Search your knowledge",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Find the exact moment across every local transcript.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
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
                shape = RoundedCornerShape(22.dp)
            )

            Spacer(Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
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
                    EmptySearchState(
                        filtered = state.filter != SearchFilter.ALL,
                        onClearFilter = {
                            viewModel.setFilter(SearchFilter.ALL)
                        }
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

@Composable
private fun SearchWelcomeState() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.padding(6.dp))

                Text(
                    text = "Private local search",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Search happens on this device. Try a topic such as “normalization”, “BCNF”, or “project deadline”.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptySearchState(
    filtered: Boolean,
    onClearFilter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "No matching moments",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "None of your local transcripts contain this search.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (filtered) {
            Spacer(Modifier.height(14.dp))

            TextButton(
                onClick = onClearFilter
            ) {
                Text("Clear filters")
            }
        }
    }
}
