package app.offlinetranscriber.mobile.speaker.alignment

import app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity
import app.offlinetranscriber.mobile.speaker.model.GlobalSpeakerTurn
import app.offlinetranscriber.mobile.speaker.model.SegmentSpeakerMatch

object SegmentSpeakerAligner {

    fun alignSegmentsToSpeakers(
        segments: List<TranscriptSegmentEntity>,
        turns: List<GlobalSpeakerTurn>,
        speakerClusterMap: Map<Int, Long> // globalSpeakerIndex -> speakerClusterId
    ): List<SegmentSpeakerMatch> {
        if (segments.isEmpty() || turns.isEmpty()) {
            return segments.map { SegmentSpeakerMatch(it.id, null, 0.0f) }
        }

        return segments.map { segment ->
            val segStart = segment.startMs
            val segEnd = segment.endMs
            val segDuration = maxOf(1L, segEnd - segStart)

            // Calculate overlap duration for each speaker
            val speakerOverlapDurations = mutableMapOf<Int, Long>()

            for (turn in turns) {
                if (turn.endMs <= segStart || turn.startMs >= segEnd) continue

                val overlapStart = maxOf(turn.startMs, segStart)
                val overlapEnd = minOf(turn.endMs, segEnd)
                val overlapDuration = maxOf(0L, overlapEnd - overlapStart)

                if (overlapDuration > 0) {
                    val current = speakerOverlapDurations.getOrDefault(turn.globalSpeakerIndex, 0L)
                    speakerOverlapDurations[turn.globalSpeakerIndex] = current + overlapDuration
                }
            }

            if (speakerOverlapDurations.isEmpty()) {
                return@map SegmentSpeakerMatch(
                    segmentId = segment.id,
                    speakerClusterId = null,
                    overlapRatio = 0.0f
                )
            }

            val ratios = speakerOverlapDurations.mapValues { (_, duration) ->
                (duration.toFloat() / segDuration.toFloat()).coerceIn(0f, 1f)
            }.toList().sortedByDescending { it.second }

            val topSpeaker = ratios.first()
            val topRatio = topSpeaker.second
            val secondRatio = if (ratios.size > 1) ratios[1].second else 0.0f
            val margin = topRatio - secondRatio

            val reliable = when {
                secondRatio <= 0.05f && topRatio >= 0.35f -> true
                topRatio >= 0.55f && margin >= 0.10f -> true
                topRatio >= 0.40f && margin >= 0.18f -> true
                else -> false
            }

            if (reliable) {
                val clusterId = speakerClusterMap[topSpeaker.first]
                SegmentSpeakerMatch(
                    segmentId = segment.id,
                    speakerClusterId = clusterId,
                    overlapRatio = topRatio
                )
            } else {
                SegmentSpeakerMatch(
                    segmentId = segment.id,
                    speakerClusterId = null,
                    overlapRatio = topRatio
                )
            }
        }
    }
}
