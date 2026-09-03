package app.offlinetranscriber.mobile.modelmanager

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
        minimumValidBytes = 20L * 1024L * 1024L,
        sha256 = "818710568da3ca15689e31a743197b520007872ff9576237bda97bd1b469c3d7"
    )

    val balanced = WhisperModelSpec(
        id = "base-q5",
        label = "Balanced",
        description = "Recommended balance of speed and accuracy for most devices.",
        fileName = "ggml-base-q5_1.bin",
        downloadUrl = BASE + "ggml-base-q5_1.bin",
        approximateBytes = 57L * 1024L * 1024L,
        minimumValidBytes = 40L * 1024L * 1024L,
        sha256 = "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898"
    )

    val accurate = WhisperModelSpec(
        id = "small-q5",
        label = "Accurate",
        description = "Higher accuracy for complex lectures and strong hardware.",
        fileName = "ggml-small-q5_1.bin",
        downloadUrl = BASE + "ggml-small-q5_1.bin",
        approximateBytes = 181L * 1024L * 1024L,
        minimumValidBytes = 130L * 1024L * 1024L,
        sha256 = "ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb"
    )

    val all = listOf(fast, balanced, accurate)

    fun byId(id: String): WhisperModelSpec? =
        all.firstOrNull { it.id == id }

    fun byFileName(fileName: String): WhisperModelSpec? =
        all.firstOrNull { it.fileName.equals(fileName, ignoreCase = true) }
}
