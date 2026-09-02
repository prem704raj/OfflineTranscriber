package com.example.transcriber.speaker.modelmanager

data class SpeakerModelSpec(
    val id: String,
    val label: String,
    val downloadUrl: String,
    val expectedSha256: String,
    val sizeBytes: Long,
    val fileName: String,
    val isArchive: Boolean,
    val extractedRelativePath: String? = null
)

object SpeakerModelCatalog {

    val SEGMENTATION_MODEL = SpeakerModelSpec(
        id = "pyannote_segmentation_3_0_int8",
        label = "PyAnnote Segmentation 3.0 (int8)",
        downloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/speaker-segmentation-models/sherpa-onnx-pyannote-segmentation-3-0.tar.bz2",
        expectedSha256 = "24615ee884c897d9d2ba09bb4d30da6bb1b15e685065962db5b02e76e4996488",
        sizeBytes = 2_150_000L,
        fileName = "sherpa-onnx-pyannote-segmentation-3-0.tar.bz2",
        isArchive = true,
        extractedRelativePath = "sherpa-onnx-pyannote-segmentation-3-0/model.int8.onnx"
    )

    val EMBEDDING_MODEL = SpeakerModelSpec(
        id = "3dspeaker_eres2net_16k",
        label = "3D-Speaker ERes2Net (16k)",
        downloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/speaker-recongition-models/3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx",
        expectedSha256 = "1a331345f04805badbb495c775a6ddffcdd1a732567d5ec8b3d5749e3c7a5e4b",
        sizeBytes = 41_500_000L,
        fileName = "3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx",
        isArchive = false,
        extractedRelativePath = null
    )

    val REQUIRED_MODELS = listOf(SEGMENTATION_MODEL, EMBEDDING_MODEL)
    val TOTAL_DOWNLOAD_SIZE_BYTES = REQUIRED_MODELS.sumOf { it.sizeBytes }
}
