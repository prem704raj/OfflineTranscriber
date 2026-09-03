package app.offlinetranscriber.mobile.export

import app.offlinetranscriber.mobile.billing.Entitlement
import app.offlinetranscriber.mobile.export.model.ExportArtifact
import app.offlinetranscriber.mobile.export.model.ExportContentType
import app.offlinetranscriber.mobile.export.model.ExportFormat
import app.offlinetranscriber.mobile.export.model.ExportOptions
import app.offlinetranscriber.mobile.export.model.ExportTarget

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
