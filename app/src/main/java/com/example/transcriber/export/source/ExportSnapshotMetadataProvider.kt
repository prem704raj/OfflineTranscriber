package com.example.transcriber.export.source

import com.example.transcriber.export.model.ExportTarget

data class ExportTargetMetadata(
    val title: String,
    val hasSpeakerData: Boolean
)

interface ExportSnapshotMetadataProvider {
    suspend fun load(
        target: ExportTarget
    ): ExportTargetMetadata
}
