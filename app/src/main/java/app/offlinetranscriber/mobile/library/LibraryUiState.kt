package app.offlinetranscriber.mobile.library

import app.offlinetranscriber.mobile.billing.ProFeature
import app.offlinetranscriber.mobile.data.model.BookmarkMomentRow
import app.offlinetranscriber.mobile.data.model.CollectionSummaryRow
import app.offlinetranscriber.mobile.data.model.LibraryTranscriptRow

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
