package app.offlinetranscriber.mobile.storage

data class StorageUsage(
    val modelsBytes: Long,
    val recordingsBytes: Long,
    val extractedAudioBytes: Long,
    val temporaryBytes: Long
) {
    val totalKnownBytes: Long
        get() =
            modelsBytes +
                recordingsBytes +
                extractedAudioBytes +
                temporaryBytes
}
