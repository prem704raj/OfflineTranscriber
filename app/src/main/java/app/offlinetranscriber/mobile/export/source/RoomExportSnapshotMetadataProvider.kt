package app.offlinetranscriber.mobile.export.source

import app.offlinetranscriber.mobile.data.database.AppDatabase
import app.offlinetranscriber.mobile.export.model.ExportContentType
import app.offlinetranscriber.mobile.export.model.ExportTarget
import java.io.FileNotFoundException

class RoomExportSnapshotMetadataProvider(
    private val database: AppDatabase
) : ExportSnapshotMetadataProvider {

    private val transcriptDao = database.transcriptDao()
    private val speakerDao = database.speakerDiarizationDao()
    private val askDao = database.askDao()

    override suspend fun load(
        target: ExportTarget
    ): ExportTargetMetadata {
        return when (target.contentType) {
            ExportContentType.TRANSCRIPT,
            ExportContentType.MEETING_PACK,
            ExportContentType.STUDY_PACK -> {
                val transcript = transcriptDao.getTranscriptById(target.sourceId)
                    ?: throw FileNotFoundException("Transcript ${target.sourceId} not found")

                val run = speakerDao.getRun(target.sourceId)
                val hasSpeakerData = (run?.detectedSpeakerCount ?: 0) > 0

                ExportTargetMetadata(
                    title = transcript.title,
                    hasSpeakerData = hasSpeakerData
                )
            }
            ExportContentType.ASK_CONVERSATION -> {
                val conversation = askDao.getConversation(target.sourceId)
                    ?: throw FileNotFoundException("Conversation ${target.sourceId} not found")

                ExportTargetMetadata(
                    title = conversation.title,
                    hasSpeakerData = false
                )
            }
        }
    }
}
