package app.offlinetranscriber.mobile.speaker.persistence

import app.offlinetranscriber.mobile.data.database.SpeakerTurnRow
import app.offlinetranscriber.mobile.data.model.SpeakerClusterEntity
import app.offlinetranscriber.mobile.speaker.model.GlobalSpeakerTurn

data class PreservedSpeakerName(
    val customName: String,
    val userNamed: Boolean
)

object SpeakerNamePreserver {

    fun matchPreviousNames(
        oldTurns: List<SpeakerTurnRow>,
        newTurns: List<GlobalSpeakerTurn>
    ): Map<Int, PreservedSpeakerName> {
        if (oldTurns.isEmpty() || newTurns.isEmpty()) return emptyMap()

        // Filter old clusters that have user names
        val oldNamedClusters = oldTurns
            .filter { it.userNamed && it.customName.isNotBlank() }
            .groupBy { it.speakerClusterId }

        if (oldNamedClusters.isEmpty()) return emptyMap()

        val newTurnsBySpeaker = newTurns.groupBy { it.globalSpeakerIndex }
        val result = mutableMapOf<Int, PreservedSpeakerName>()
        val matchedOldClusters = mutableSetOf<Long>()

        for ((newSpeakerIndex, turns) in newTurnsBySpeaker) {
            val totalNewDuration = turns.sumOf { maxOf(0L, it.endMs - it.startMs) }
            if (totalNewDuration <= 0L) continue

            var bestOldClusterId: Long? = null
            var bestOverlapMs = 0L
            var bestName: String? = null

            for ((oldClusterId, oldClusterTurns) in oldNamedClusters) {
                if (matchedOldClusters.contains(oldClusterId)) continue

                var overlapMs = 0L
                for (nTurn in turns) {
                    for (oTurn in oldClusterTurns) {
                        val intStart = maxOf(nTurn.startMs, oTurn.startMs)
                        val intEnd = minOf(nTurn.endMs, oTurn.endMs)
                        val dur = maxOf(0L, intEnd - intStart)
                        overlapMs += dur
                    }
                }

                if (overlapMs > bestOverlapMs) {
                    bestOverlapMs = overlapMs
                    bestOldClusterId = oldClusterId
                    bestName = oldClusterTurns.first().customName
                }
            }

            if (bestOldClusterId != null && bestName != null) {
                val overlapRatio = bestOverlapMs.toFloat() / totalNewDuration.toFloat()
                if (overlapRatio >= 0.35f) {
                    result[newSpeakerIndex] = PreservedSpeakerName(
                        customName = bestName,
                        userNamed = true
                    )
                    matchedOldClusters.add(bestOldClusterId)
                }
            }
        }

        return result
    }
}
