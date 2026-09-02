package com.example.transcriber.speaker.engine

import com.example.transcriber.speaker.model.GlobalSpeakerTurn

object SpeakerTurnNormalizer {

    fun normalize(
        turns: List<GlobalSpeakerTurn>,
        minTurnDurationMs: Long = 250L,
        maxMergeGapMs: Long = 400L
    ): List<GlobalSpeakerTurn> {
        if (turns.isEmpty()) return emptyList()

        // 1. Filter out invalid turns & sort chronologically
        val validTurns = turns
            .filter { it.endMs > it.startMs && (it.endMs - it.startMs) >= minTurnDurationMs }
            .sortedWith(compareBy({ it.startMs }, { it.endMs }))

        if (validTurns.isEmpty()) return emptyList()

        // 2. Merge adjacent turns for the same speaker & resolve small gaps
        val mergedTurns = mutableListOf<GlobalSpeakerTurn>()
        for (turn in validTurns) {
            if (mergedTurns.isEmpty()) {
                mergedTurns.add(turn)
                continue
            }

            val last = mergedTurns.last()
            if (last.globalSpeakerIndex == turn.globalSpeakerIndex) {
                // Check if they overlap or have a gap smaller than maxMergeGapMs
                if (turn.startMs <= last.endMs + maxMergeGapMs) {
                    val merged = last.copy(endMs = maxOf(last.endMs, turn.endMs))
                    mergedTurns[mergedTurns.size - 1] = merged
                    continue
                }
            }
            mergedTurns.add(turn)
        }

        // 3. Resolve inter-speaker overlaps chronologically
        val resolvedTurns = mutableListOf<GlobalSpeakerTurn>()
        for (turn in mergedTurns) {
            if (resolvedTurns.isEmpty()) {
                resolvedTurns.add(turn)
                continue
            }

            val lastIdx = resolvedTurns.size - 1
            val last = resolvedTurns[lastIdx]

            if (turn.startMs < last.endMs) {
                // Different speakers overlap
                if (turn.startMs >= last.startMs + minTurnDurationMs) {
                    // Clip the earlier speaker's turn at the start of the new speaker
                    resolvedTurns[lastIdx] = last.copy(endMs = turn.startMs)
                    if (turn.endMs - turn.startMs >= minTurnDurationMs) {
                        resolvedTurns.add(turn)
                    }
                } else {
                    // Overlap is at the very beginning of the earlier turn; adjust start of current turn
                    val adjustedStart = last.endMs
                    if (turn.endMs - adjustedStart >= minTurnDurationMs) {
                        resolvedTurns.add(turn.copy(startMs = adjustedStart))
                    }
                }
            } else {
                resolvedTurns.add(turn)
            }
        }

        // 4. Compact and re-index speakers by order of appearance (0, 1, 2, ...)
        val speakerMap = mutableMapOf<Int, Int>()
        var nextIndex = 0

        val finalTurns = resolvedTurns
            .filter { it.endMs - it.startMs >= minTurnDurationMs }
            .map { turn ->
                val compactIndex = speakerMap.getOrPut(turn.globalSpeakerIndex) {
                    nextIndex++
                }
                turn.copy(globalSpeakerIndex = compactIndex)
            }

        return finalTurns
    }
}
