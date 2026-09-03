package app.offlinetranscriber.mobile.modelmanager

data class WhisperModelSpec(
    val id: String,
    val label: String,
    val description: String,
    val fileName: String,
    val downloadUrl: String,
    val approximateBytes: Long,
    val minimumValidBytes: Long,
    val sha256: String
) {
    val formattedApproximateSize: String
        get() = String.format(
            java.util.Locale.US,
            "%.1f MB",
            approximateBytes.toDouble() / (1024.0 * 1024.0)
        )
}
