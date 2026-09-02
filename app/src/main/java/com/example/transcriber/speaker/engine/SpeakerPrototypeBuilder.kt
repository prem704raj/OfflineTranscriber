package com.example.transcriber.speaker.engine

import com.example.transcriber.speaker.model.SpeakerPrototype
import kotlin.math.sqrt

class SpeakerPrototypeBuilder {

    private val prototypes = mutableMapOf<Int, FloatArray>()
    private val prototypeCounts = mutableMapOf<Int, Int>()

    fun getPrototypes(): List<SpeakerPrototype> {
        return prototypes.map { (index, emb) ->
            SpeakerPrototype(index, emb)
        }
    }

    fun hasSpeakers(): Boolean = prototypes.isNotEmpty()

    fun speakerCount(): Int = prototypes.size

    fun addOrUpdateSpeaker(speakerIndex: Int, embedding: FloatArray) {
        val current = prototypes[speakerIndex]
        if (current == null) {
            prototypes[speakerIndex] = l2Normalize(embedding)
            prototypeCounts[speakerIndex] = 1
        } else {
            val count = prototypeCounts[speakerIndex] ?: 1
            val updated = FloatArray(embedding.size)
            for (i in embedding.indices) {
                updated[i] = (current[i] * count + embedding[i]) / (count + 1)
            }
            prototypes[speakerIndex] = l2Normalize(updated)
            prototypeCounts[speakerIndex] = count + 1
        }
    }

    fun findBestMatch(embedding: FloatArray, minSimilarityThreshold: Float = 0.55f): Pair<Int, Float>? {
        if (prototypes.isEmpty()) return null

        var bestSpeaker = -1
        var bestSimilarity = -1.0f

        val normEmb = l2Normalize(embedding)
        for ((speakerIndex, proto) in prototypes) {
            val sim = cosineSimilarity(normEmb, proto)
            if (sim > bestSimilarity) {
                bestSimilarity = sim
                bestSpeaker = speakerIndex
            }
        }

        return if (bestSimilarity >= minSimilarityThreshold) {
            Pair(bestSpeaker, bestSimilarity)
        } else {
            null
        }
    }

    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.size != v2.size || v1.isEmpty()) return 0.0f
        var dot = 0.0f
        for (i in v1.indices) {
            dot += (v1[i] * v2[i])
        }
        return dot.coerceIn(-1.0f, 1.0f)
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
}
