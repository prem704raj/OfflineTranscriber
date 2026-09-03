package app.offlinetranscriber.mobile.export.source

interface ExportSnapshotProvider {

    suspend fun transcript(
        transcriptId: Long,
        includeSpeakerLabels: Boolean
    ): TranscriptExportSnapshot

    suspend fun meeting(
        transcriptId: Long
    ): MeetingExportSnapshot

    suspend fun study(
        transcriptId: Long
    ): StudyExportSnapshot

    suspend fun ask(
        conversationId: Long
    ): AskExportSnapshot
}
