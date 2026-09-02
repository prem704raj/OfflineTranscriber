package com.example.transcriber.speaker.engine

import com.example.transcriber.speaker.model.RawSpeakerTurn
import com.k2fsa.sherpa.onnx.FastClusteringConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarization
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarizationConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationPyannoteModelConfig
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import java.io.Closeable
import java.io.File

class SherpaDiarizationEngine(
    private val segmentationModelFile: File,
    private val embeddingModelFile: File,
    private val numClusters: Int? = null,
    private val threshold: Float = 0.5f,
    private val numThreads: Int = 2
) : Closeable {

    private var diarizer: OfflineSpeakerDiarization? = null

    init {
        require(segmentationModelFile.exists()) { "Segmentation model not found: ${segmentationModelFile.absolutePath}" }
        require(embeddingModelFile.exists()) { "Embedding model not found: ${embeddingModelFile.absolutePath}" }

        val pyannoteConfig = OfflineSpeakerSegmentationPyannoteModelConfig(
            model = segmentationModelFile.absolutePath,
            windowShiftRatio = 0.5f
        )
        val segmentationConfig = OfflineSpeakerSegmentationModelConfig(
            pyannote = pyannoteConfig,
            numThreads = numThreads,
            debug = false,
            provider = "cpu"
        )
        val embeddingConfig = SpeakerEmbeddingExtractorConfig(
            model = embeddingModelFile.absolutePath,
            numThreads = numThreads,
            debug = false,
            provider = "cpu"
        )
        val clusteringConfig = FastClusteringConfig(
            numClusters = numClusters ?: -1,
            threshold = threshold
        )
        val diarizationConfig = OfflineSpeakerDiarizationConfig(
            segmentation = segmentationConfig,
            embedding = embeddingConfig,
            clustering = clusteringConfig,
            minDurationOn = 0.3f,
            minDurationOff = 0.5f
        )

        diarizer = OfflineSpeakerDiarization(
            config = diarizationConfig
        )
    }

    fun diarizeWindow(
        samples: FloatArray,
        windowOffsetMs: Long
    ): List<RawSpeakerTurn> {
        val currentDiarizer = diarizer ?: throw IllegalStateException("Diarizer has been closed or not initialized")
        if (samples.isEmpty()) return emptyList()

        val segments = currentDiarizer.process(samples)
        return segments.map { seg ->
            val startMs = windowOffsetMs + (seg.start * 1000f).toLong()
            val endMs = windowOffsetMs + (seg.end * 1000f).toLong()
            RawSpeakerTurn(
                localSpeaker = seg.speaker,
                startMs = maxOf(windowOffsetMs, startMs),
                endMs = maxOf(startMs + 50L, endMs)
            )
        }
    }

    override fun close() {
        try {
            diarizer?.release()
        } catch (_: Exception) {}
        diarizer = null
    }
}
