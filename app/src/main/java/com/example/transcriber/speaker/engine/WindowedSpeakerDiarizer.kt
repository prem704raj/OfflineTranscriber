package com.example.transcriber.speaker.engine

import com.example.transcriber.speaker.audio.DiarizationWindowConfig
import com.example.transcriber.speaker.audio.Pcm16WindowReader
import com.example.transcriber.speaker.model.GlobalSpeakerTurn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

class WindowedSpeakerDiarizer(
    private val segmentationModelFile: File,
    private val embeddingModelFile: File,
    private val requestedSpeakerCount: Int? = null,
    private val windowConfig: DiarizationWindowConfig
) {

    suspend fun diarize(
        reader: Pcm16WindowReader,
        onProgress: ((Float) -> Unit)? = null
    ): List<GlobalSpeakerTurn> = withContext(Dispatchers.Default) {
        val totalSamples = reader.totalSamples
        if (totalSamples <= 0) return@withContext emptyList()

        var diarizationEngine: SherpaDiarizationEngine? = null
        var embeddingEngine: SherpaSpeakerEmbeddingEngine? = null

        try {
            diarizationEngine = SherpaDiarizationEngine(
                segmentationModelFile = segmentationModelFile,
                embeddingModelFile = embeddingModelFile,
                numClusters = requestedSpeakerCount,
                threshold = 0.5f,
                numThreads = 2
            )

            embeddingEngine = SherpaSpeakerEmbeddingEngine(
                embeddingModelFile = embeddingModelFile,
                numThreads = 2
            )

            val prototypeBuilder = SpeakerPrototypeBuilder()
            val stitcher = SpeakerWindowStitcher(prototypeBuilder, embeddingEngine)

            val allGlobalTurns = mutableListOf<GlobalSpeakerTurn>()

            val windowSamples = windowConfig.windowSamples
            val overlapSamples = windowConfig.overlapSamples
            val stepSamples = windowConfig.stepSamples

            var currentStartSample = 0L
            var windowIndex = 0

            while (currentStartSample < totalSamples) {
                coroutineContext.ensureActive()

                val currentWindowSamples = reader.readWindow(currentStartSample, windowSamples)
                if (currentWindowSamples.isEmpty()) break

                val windowOffsetMs = (currentStartSample * 1000L) / reader.sampleRate
                val rawTurns = diarizationEngine.diarizeWindow(currentWindowSamples, windowOffsetMs)

                if (windowIndex == 0) {
                    val stitched = stitcher.stitchFirstWindow(rawTurns, reader)
                    allGlobalTurns.addAll(stitched)
                } else {
                    val overlapStartMs = windowOffsetMs
                    val overlapEndMs = windowOffsetMs + (overlapSamples * 1000L) / reader.sampleRate
                    val stitched = stitcher.stitchSubsequentWindow(
                        rawTurns = rawTurns,
                        overlapStartMs = overlapStartMs,
                        overlapEndMs = overlapEndMs,
                        existingGlobalTurns = allGlobalTurns,
                        windowReader = reader
                    )

                    // Keep non-overlap turns and newly stitched overlap turns
                    val newTurns = stitched.filter { it.startMs >= overlapStartMs }
                    allGlobalTurns.addAll(newTurns)
                }

                currentStartSample += stepSamples
                windowIndex++

                val progress = (currentStartSample.toFloat() / totalSamples.toFloat()).coerceIn(0f, 1f)
                onProgress?.invoke(progress)
            }

            // Normalize and resolve inter-speaker overlap
            val normalized = SpeakerTurnNormalizer.normalize(allGlobalTurns)
            onProgress?.invoke(1.0f)
            normalized
        } finally {
            diarizationEngine?.close()
            embeddingEngine?.close()
        }
    }
}
