package com.example.transcriber.data.model

data class SearchResultRow(
    val transcriptId: Long,
    val segmentId: Long,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val mediaType: String,
    val isBookmarked: Boolean
)
