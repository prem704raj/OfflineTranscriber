package com.example.transcriber.export

import com.example.transcriber.billing.Entitlement
import com.example.transcriber.export.model.ExportArtifact
import com.example.transcriber.export.model.ExportContentType
import com.example.transcriber.export.model.ExportFormat
import com.example.transcriber.export.model.ExportOptions
import com.example.transcriber.export.model.ExportTarget

data class ExportUiState(
    val target: ExportTarget = ExportTarget(ExportContentType.TRANSCRIPT, 0L),
    val title: String = "",
    val hasSpeakerData: Boolean = false,
    val options: ExportOptions = ExportOptions(format = ExportFormat.TXT),
    val entitlement: Entitlement = Entitlement.FREE,
    val progressState: ExportProgressState = ExportProgressState.IDLE,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val pendingArtifactForSave: ExportArtifact? = null,
    val previewSnippet: String = ""
)
