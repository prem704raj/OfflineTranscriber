package com.example.transcriber.caption.source

import com.example.transcriber.caption.model.CaptionCue
import com.example.transcriber.caption.model.CaptionProjectSnapshot
import com.example.transcriber.data.database.SpeakerDiarizationDao
import com.example.transcriber.data.database.TranscriptDao
import com.example.transcriber.data.model.MediaType
import java.io.FileNotFoundException

class RoomCaptionProjectProvider(
    private val transcriptDao: TranscriptDao,
    private val speakerDao: SpeakerDiarizationDao
) : CaptionProjectProvider {

    override suspend fun load(
        transcriptId: Long,
        includeSpeakers: Boolean
    ): CaptionProjectSnapshot {
        val transcript = transcriptDao.getTranscriptById(transcriptId)
            ?: throw FileNotFoundException("Transcript $transcriptId not found.")

        if (transcript.mediaType != MediaType.VIDEO) {
            throw IllegalArgumentException("Transcript $transcriptId is not a video recording.")
        }

        val sourceUri = transcript.sourceUri.ifBlank { transcript.audioUriString ?: "" }
        if (sourceUri.isBlank()) {
            throw FileNotFoundException("Original video is no longer available.")
        }

        val rawSegments = transcriptDao.getSegmentsOnce(transcriptId).ifEmpty {
            transcript.getSegments().map {
                com.example.transcriber.data.model.TranscriptSegmentEntity(
                    id = it.id,
                    transcriptId = transcriptId,
                    startMs = it.startMs,
                    endMs = it.endMs,
                    text = it.text
                )
            }
        }

        val speakerAssignments = if (includeSpeakers) {
            speakerDao.getAssignmentsOnce(transcriptId).associateBy { it.segmentId }
        } else {
            emptyMap()
        }

        val cues = rawSegments.map { seg ->
            val speakerInfo = speakerAssignments[seg.id]
            val speakerLabel = speakerInfo?.customName?.takeIf { it.isNotBlank() }
            val speakerOrdinal = speakerInfo?.speakerIndex

            CaptionCue(
                segmentId = seg.id,
                startUs = seg.startMs * 1000L,
                endUs = seg.endMs * 1000L,
                text = seg.text,
                speakerClusterId = speakerInfo?.speakerClusterId,
                speakerLabel = speakerLabel,
                speakerOrdinal = speakerOrdinal
            )
        }

        val validCues = CaptionCueValidator.validate(cues, transcript.audioDurationMs)

        return CaptionProjectSnapshot(
            transcriptId = transcriptId,
            sourceVideoUri = sourceUri,
            durationMs = transcript.audioDurationMs,
            cues = validCues
        )
    }
}
