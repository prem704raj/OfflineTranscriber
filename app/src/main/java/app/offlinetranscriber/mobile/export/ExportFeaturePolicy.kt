package app.offlinetranscriber.mobile.export

import app.offlinetranscriber.mobile.billing.Entitlement
import app.offlinetranscriber.mobile.export.model.ExportContentType
import app.offlinetranscriber.mobile.export.model.ExportOptions
import app.offlinetranscriber.mobile.export.model.ExportTarget

data class ExportAccessResult(
    val allowed: Boolean,
    val reason: String? = null
)

object ExportFeaturePolicy {

    fun check(
        target: ExportTarget,
        options: ExportOptions,
        entitlement: Entitlement
    ): ExportAccessResult {
        val pro = entitlement == Entitlement.PRO

        if (options.format.requiresPro && !pro) {
            return ExportAccessResult(
                allowed = false,
                reason = "PDF and DOCX exports require Pro."
            )
        }

        if (options.includeSpeakerLabels && !pro) {
            return ExportAccessResult(
                allowed = false,
                reason = "Speaker-labeled exports require Pro."
            )
        }

        if (target.contentType != ExportContentType.TRANSCRIPT && !pro) {
            return ExportAccessResult(
                allowed = false,
                reason = "Advanced insight exports require Pro."
            )
        }

        return ExportAccessResult(allowed = true)
    }
}
