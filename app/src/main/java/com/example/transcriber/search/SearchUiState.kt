package com.example.transcriber.search

import com.example.transcriber.data.model.SearchResultRow

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val loading: Boolean = false,
    val results: List<SearchResultRow> = emptyList(),
    val searched: Boolean = false,
    val errorMessage: String? = null
)
