package com.example.transcriber.library

import com.example.transcriber.billing.ProFeature
import com.example.transcriber.data.model.BookmarkMomentRow
import com.example.transcriber.data.model.CollectionSummaryRow
import com.example.transcriber.data.model.LibraryTranscriptRow

data class CollectionPickerState(
    val transcriptId: Long,
    val selectedCollectionIds: Set<Long>
)

data class LibraryUiState(
    val collections: List<CollectionSummaryRow> = emptyList(),
    val recentBookmarks: List<BookmarkMomentRow> = emptyList(),
    val transcripts: List<LibraryTranscriptRow> = emptyList(),
    val collectionPicker: CollectionPickerState? = null,
    val openPaywallFeature: ProFeature? = null,
    val errorMessage: String? = null
)
