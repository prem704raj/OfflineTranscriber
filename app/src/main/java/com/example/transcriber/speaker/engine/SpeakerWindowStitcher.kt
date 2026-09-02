package com.example.transcriber.speaker.engine

import com.example.transcriber.speaker.audio.Pcm16WindowReader
import com.example.transcriber.speaker.model.GlobalSpeakerTurn
import com.example.transcriber.speaker.model.RawSpeakerTurn

class SpeakerWindowStitcher(
    private val prototypeBuilder: SpeakerPrototypeBuilder,
    private val embeddingEngine: SherpaSpeakerEmbeddingEngine
) {

    fun stitchFirstWindow(
        rawTurns: List<RawSpeakerTurn>,
        windowReader: Pcm16WindowReader
    ): List<GlobalSpeakerTurn> {
        // Group raw turns by local speaker
        val grouped = rawTurns.groupBy { it.localSpeaker }
        val localToGlobal = mutableMapOf<Int, Int>()

        for ((localSpeaker, turns) in grouped) {
            val globalIndex = localToGlobal.getOrPut(localSpeaker) {
                prototypeBuilder.speakerCount()
            }

            // Extract embedding from longest turn to register prototype
            val longestTurn = turns.maxByOrNull { it.endMs - it.startMs }
            if (longestTurn != null) {
                val samples = windowReader.readTimeRange(longestTurn.startMs, longestTurn.endMs)
                val emb = embeddingEngine.computeEmbedding(samples)
                if (emb != null) {
                    prototypeBuilder.addOrUpdateSpeaker(globalIndex, emb)
                }
            }
        }

        return rawTurns.map { turn ->
            GlobalSpeakerTurn(
                globalSpeakerIndex = localToGlobal[turn.localSpeaker] ?: turn.localSpeaker,
                startMs = turn.startMs,
                endMs = turn.endMs
            )
        }
    }

    fun stitchSubsequentWindow(
        rawTurns: List<RawSpeakerTurn>,
        overlapStartMs: Long,
        overlapEndMs: Long,
        existingGlobalTurns: List<GlobalSpeakerTurn>,
        windowReader: Pcm16WindowReader
    ): List<GlobalSpeakerTurn> {
        val overlapGlobalTurns = existingGlobalTurns.filter { turn ->
            turn.endMs > overlapStartMs && turn.startMs < overlapEndMs
        }

        val localTurnsBySpeaker = rawTurns.groupBy { it.localSpeaker }
        val localToGlobalMap = mutableMapOf<Int, Int>()

        for ((localSpeaker, turns) in localTurnsBySpeaker) {
            // 1. Calculate temporal overlap with existing global speakers in overlap region
            val localOverlapTurns = turns.filter { it.endMs > overlapStartMs && it.startMs < overlapEndMs }
            val localOverlapDuration = localOverlapTurns.sumOf { turn ->
                val overlapStart = maxOf(turn.startMs, overlapStartMs)
                val overlapEnd = minOf(turn.endMs, overlapEndMs)
                maxOf(0L, overlapEnd - overlapStart)
            }

            val overlapScores = mutableMapOf<Int, Float>()
            if (localOverlapDuration > 0) {
                for (globalTurn in overlapGlobalTurns) {
                    for (localTurn in localOverlapTurns) {
                        val intStart = maxOf(localTurn.startMs, globalTurn.startMs)
                        val intEnd = minOf(localTurn.endMs, globalTurn.endMs)
                        val intDur = maxOf(0L, intEnd - intStart)
                        if (intDur > 0) {
                            val currentScore = overlapScores.getOrDefault(globalTurn.globalSpeakerIndex, 0.0f)
                            overlapScores[globalTurn.globalSpeakerIndex] = currentScore + (intDur.toFloat() / localOverlapDuration.toFloat())
                        }
                    }
                }
            }

            // 2. Calculate audio embedding similarity
            val longestTurn = turns.maxByOrNull { it.endMs - it.startMs }
            var turnEmbedding: FloatArray? = null
            if (longestTurn != null) {
                val samples = windowReader.readTimeRange(longestTurn.startMs, longestTurn.endMs)
                turnEmbedding = embeddingEngine.computeEmbedding(samples)
            }

            // 3. Match candidate
            var bestGlobalSpeaker = -1
            var bestScore = -1.0f

            for (proto in prototypeBuilder.getPrototypes()) {
                val gIndex = proto.globalSpeakerIndex
                val overlapScore = overlapScores[gIndex] ?: 0.0f
                val embSim = if (turnEmbedding != null) {
                    prototypeBuilder.cosineSimilarity(turnEmbedding, proto.embedding)
                } else 0.0f

                val combined = if (localOverlapDuration > 0) {
                    0.60f * overlapScore + 0.40f * embSim.coerceAtLeast(0f)
                } else {
                    embSim
                }

                if (combined > bestScore) {
                    bestScore = combined
                    bestGlobalSpeaker = gIndex
                }
            }

            // Minimum acceptance threshold
            val assignedGlobal = if (bestScore >= 0.45f && bestGlobalSpeaker >= 0) {
                bestGlobalSpeaker
            } else {
                // New speaker discovered
                prototypeBuilder.speakerCount()
            }

            localToGlobalMap[localSpeaker] = assignedGlobal

            // Update prototype with new turn embedding if available
            if (turnEmbedding != null) {
                prototypeBuilder.addOrUpdateSpeaker(assignedGlobal, turnEmbedding)
            }
        }

        // Map turns to global indices
        return rawTurns.map { turn ->
            GlobalSpeakerTurn(
                globalSpeakerIndex = localToGlobalMap[turn.localSpeaker] ?: turn.localSpeaker,
                startMs = turn.startMs,
                endMs = turn.endMs
            )
        }
    }
}
