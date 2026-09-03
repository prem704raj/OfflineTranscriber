package app.offlinetranscriber.mobile.export.model

enum class ExportContentType {
    TRANSCRIPT,
    MEETING_PACK,
    STUDY_PACK,
    ASK_CONVERSATION
}

enum class ExportFormat(
    val extension: String,
    val mimeType: String,
    val requiresPro: Boolean
) {
    TXT(
        "txt",
        "text/plain",
        false
    ),
    MARKDOWN(
        "md",
        "text/markdown",
        false
    ),
    PDF(
        "pdf",
        "application/pdf",
        true
    ),
    DOCX(
        "docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        true
    )
}

enum class PdfPageSize {
    A4,
    LETTER
}

enum class ExportTextScale {
    COMPACT,
    STANDARD,
    LARGE
}

data class ExportOptions(
    val format: ExportFormat,
    val includeMetadata: Boolean = true,
    val includeTimestamps: Boolean = true,
    val includeSpeakerLabels: Boolean = false,
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val textScale: ExportTextScale = ExportTextScale.STANDARD,
    val includeQuizAnswers: Boolean = true,
    val includeAskCitations: Boolean = true
)

data class ExportTarget(
    val contentType: ExportContentType,
    val sourceId: Long
)

data class ExportArtifact(
    val fileName: String,
    val mimeType: String,
    val filePath: String,
    val byteCount: Long
)
