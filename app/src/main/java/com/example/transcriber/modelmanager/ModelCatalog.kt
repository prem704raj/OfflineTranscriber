package com.example.transcriber.modelmanager

object ModelCatalog {
    private const val BASE =
        "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/"

    val fast = WhisperModelSpec(
        id = "tiny-q5",
        label = "Fast",
        description = "Lowest storage and fastest processing. Great for quick notes.",
        fileName = "ggml-tiny-q5_1.bin",
        downloadUrl = BASE + "ggml-tiny-q5_1.bin",
        approximateBytes = 31L * 1024L * 1024L,
        minimumValidBytes = 20L * 1024L * 1024L
    )

    val balanced = WhisperModelSpec(
        id = "base-q5",
        label = "Balanced",
        description = "Recommended balance of speed and accuracy for most devices.",
        fileName = "ggml-base-q5_1.bin",
        downloadUrl = BASE + "ggml-base-q5_1.bin",
        approximateBytes = 57L * 1024L * 1024L,
        minimumValidBytes = 40L * 1024L * 1024L
    )

    val accurate = WhisperModelSpec(
        id = "small-q5",
        label = "Accurate",
        description = "Higher accuracy for complex lectures and strong hardware.",
        fileName = "ggml-small-q5_1.bin",
        downloadUrl = BASE + "ggml-small-q5_1.bin",
        approximateBytes = 181L * 1024L * 1024L,
        minimumValidBytes = 130L * 1024L * 1024L
    )

    val all = listOf(
        fast,
        balanced,
        accurate
    )

    fun byId(id: String): WhisperModelSpec? =
        all.firstOrNull { it.id == id }

    fun byFileName(fileName: String): WhisperModelSpec? =
        all.firstOrNull { it.fileName.equals(fileName, ignoreCase = true) }
}
