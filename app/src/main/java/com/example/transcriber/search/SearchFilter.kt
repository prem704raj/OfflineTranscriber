package com.example.transcriber.search

import com.example.transcriber.data.model.MediaType

enum class SearchFilter(
    val label: String,
    val mediaType: String?,
    val bookmarksOnly: Boolean
) {
    ALL(
        label = "All",
        mediaType = null,
        bookmarksOnly = false
    ),
    AUDIO(
        label = "Audio",
        mediaType = MediaType.AUDIO,
        bookmarksOnly = false
    ),
    VIDEO(
        label = "Video",
        mediaType = MediaType.VIDEO,
        bookmarksOnly = false
    ),
    BOOKMARKED(
        label = "Bookmarked",
        mediaType = null,
        bookmarksOnly = true
    )
}
