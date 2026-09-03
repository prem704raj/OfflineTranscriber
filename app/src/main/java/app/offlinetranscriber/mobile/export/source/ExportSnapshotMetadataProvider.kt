package app.offlinetranscriber.mobile.export.source

import app.offlinetranscriber.mobile.export.model.ExportTarget

data class ExportTargetMetadata(
    val title: String,
    val hasSpeakerData: Boolean
)

interface ExportSnapshotMetadataProvider {
    suspend fun load(
        target: ExportTarget
    ): ExportTargetMetadata
}
