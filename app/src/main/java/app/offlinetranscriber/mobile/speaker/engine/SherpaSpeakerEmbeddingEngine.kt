package app.offlinetranscriber.mobile.speaker.engine

import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import java.io.Closeable
import java.io.File
import kotlin.math.sqrt

class SherpaSpeakerEmbeddingEngine(
    private val embeddingModelFile: File,
    private val numThreads: Int = 2
) : Closeable {

    private var extractor: SpeakerEmbeddingExtractor? = null

    init {
        require(embeddingModelFile.exists()) { "Embedding model file not found: ${embeddingModelFile.absolutePath}" }
        val config = SpeakerEmbeddingExtractorConfig(
            model = embeddingModelFile.absolutePath,
            numThreads = numThreads,
            debug = false,
            provider = "cpu"
        )
        extractor = SpeakerEmbeddingExtractor(config = config)
    }

    fun computeEmbedding(samples: FloatArray, sampleRate: Int = 16000): FloatArray? {
        val currentExtractor = extractor ?: return null
        if (samples.size < sampleRate * 0.25) { // Needs at least 250ms of audio
            return null
        }

        val stream = currentExtractor.createStream()
        try {
            stream.acceptWaveform(samples, sampleRate)
            stream.inputFinished()
            val embedding = currentExtractor.compute(stream)
            if (embedding.isEmpty()) return null
            return l2Normalize(embedding)
        } finally {
            stream.release()
        }
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSquares = 0.0
        for (v in vector) {
            sumSquares += (v * v)
        }
        val norm = sqrt(sumSquares).toFloat()
        if (norm <= 1e-8f) return vector
        val result = FloatArray(vector.size)
        for (i in vector.indices) {
            result[i] = vector[i] / norm
        }
        return result
    }

    override fun close() {
        try {
            extractor?.release()
        } catch (_: Exception) {}
        extractor = null
    }
}
