package app.offlinetranscriber.mobile.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.offlinetranscriber.mobile.TranscriberApplication
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

@OptIn(
    FlowPreview::class,
    ExperimentalCoroutinesApi::class
)
class SearchViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        (application as TranscriberApplication).knowledgeRepository

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(SearchFilter.ALL)
    private val refreshCounter = MutableStateFlow(0)

    private val _state =
        MutableStateFlow(SearchUiState())

    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                query.debounce(250L),
                filter,
                refreshCounter
            ) { q, f, _ ->
                q to f
            }.flatMapLatest { (q, f) ->
                flow {
                    if (!SearchQueryPolicy.shouldSearch(q)) {
                        emit(
                            SearchUiState(
                                query = q,
                                filter = f,
                                loading = false,
                                results = emptyList(),
                                searched = false
                            )
                        )
                        return@flow
                    }

                    val safeQuery = FtsQueryBuilder.build(q)

                    if (safeQuery == null) {
                        emit(
                            SearchUiState(
                                query = q,
                                filter = f,
                                loading = false,
                                results = emptyList(),
                                searched = false
                            )
                        )
                        return@flow
                    }

                    emit(
                        SearchUiState(
                            query = q,
                            filter = f,
                            loading = true,
                            results = _state.value.results,
                            searched = true
                        )
                    )

                    try {
                        val results = repository.search(
                            ftsQuery = safeQuery,
                            mediaType = f.mediaType,
                            bookmarksOnly = f.bookmarksOnly,
                            limit = 150
                        )

                        emit(
                            SearchUiState(
                                query = q,
                                filter = f,
                                loading = false,
                                results = results,
                                searched = true
                            )
                        )
                    } catch (error: Throwable) {
                        emit(
                            SearchUiState(
                                query = q,
                                filter = f,
                                loading = false,
                                results = emptyList(),
                                searched = true,
                                errorMessage = error.message
                                    ?: "Search failed."
                            )
                        )
                    }
                }
            }.collect { next ->
                _state.value = next
            }
        }
    }

    fun setQuery(value: String) {
        query.value = value
        _state.value = _state.value.copy(
            query = value
        )
    }

    fun clearQuery() {
        setQuery("")
    }

    fun setFilter(value: SearchFilter) {
        filter.value = value
        _state.value = _state.value.copy(
            filter = value
        )
    }

    fun toggleBookmark(
        transcriptId: Long,
        segmentId: Long
    ) {
        viewModelScope.launch {
            repository.toggleBookmark(
                transcriptId = transcriptId,
                segmentId = segmentId
            )
            refreshCounter.value += 1
        }
    }
}
