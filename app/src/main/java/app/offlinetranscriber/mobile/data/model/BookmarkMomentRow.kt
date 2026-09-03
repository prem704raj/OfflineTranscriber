package app.offlinetranscriber.mobile.data.model

data class BookmarkMomentRow(
    val bookmarkId: Long,
    val transcriptId: Long,
    val segmentId: Long,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val mediaType: String,
    val createdAt: Long
)
