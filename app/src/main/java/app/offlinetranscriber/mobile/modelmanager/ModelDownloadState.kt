package app.offlinetranscriber.mobile.modelmanager

sealed interface ModelDownloadState {
    data object Idle : ModelDownloadState

    data class Downloading(
        val modelId: String,
        val bytesDownloaded: Long,
        val totalBytes: Long?
    ) : ModelDownloadState {
        val fraction: Float?
            get() = totalBytes
                ?.takeIf { it > 0L }
                ?.let {
                    (bytesDownloaded.toDouble() /
                        it.toDouble())
                        .coerceIn(0.0, 1.0)
                        .toFloat()
                }
    }

    data class Completed(
        val modelId: String
    ) : ModelDownloadState

    data class Failed(
        val modelId: String,
        val message: String
    ) : ModelDownloadState
}
