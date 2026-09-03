package app.offlinetranscriber.mobile.export

import app.offlinetranscriber.mobile.billing.Entitlement
import app.offlinetranscriber.mobile.export.model.ExportContentType
import app.offlinetranscriber.mobile.export.model.ExportFormat
import app.offlinetranscriber.mobile.export.model.ExportOptions
import app.offlinetranscriber.mobile.export.model.ExportTarget
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportFeaturePolicyTest {

    @Test
    fun `free user can export plain text and markdown transcripts`() {
        val target = ExportTarget(ExportContentType.TRANSCRIPT, 1L)

        val txtOptions = ExportOptions(format = ExportFormat.TXT)
        val txtResult = ExportFeaturePolicy.check(target, txtOptions, Entitlement.FREE)
        assertTrue(txtResult.allowed)

        val mdOptions = ExportOptions(format = ExportFormat.MARKDOWN)
        val mdResult = ExportFeaturePolicy.check(target, mdOptions, Entitlement.FREE)
        assertTrue(mdResult.allowed)
    }

    @Test
    fun `free user cannot export pdf or docx`() {
        val target = ExportTarget(ExportContentType.TRANSCRIPT, 1L)

        val pdfOptions = ExportOptions(format = ExportFormat.PDF)
        val pdfResult = ExportFeaturePolicy.check(target, pdfOptions, Entitlement.FREE)
        assertFalse(pdfResult.allowed)

        val docxOptions = ExportOptions(format = ExportFormat.DOCX)
        val docxResult = ExportFeaturePolicy.check(target, docxOptions, Entitlement.FREE)
        assertFalse(docxResult.allowed)
    }

    @Test
    fun `free user cannot export speaker labeled transcripts`() {
        val target = ExportTarget(ExportContentType.TRANSCRIPT, 1L)
        val options = ExportOptions(format = ExportFormat.TXT, includeSpeakerLabels = true)
        val result = ExportFeaturePolicy.check(target, options, Entitlement.FREE)
        assertFalse(result.allowed)
    }

    @Test
    fun `free user cannot export meeting or study packs`() {
        val meetingTarget = ExportTarget(ExportContentType.MEETING_PACK, 1L)
        val options = ExportOptions(format = ExportFormat.TXT)
        val meetingResult = ExportFeaturePolicy.check(meetingTarget, options, Entitlement.FREE)
        assertFalse(meetingResult.allowed)

        val studyTarget = ExportTarget(ExportContentType.STUDY_PACK, 1L)
        val studyResult = ExportFeaturePolicy.check(studyTarget, options, Entitlement.FREE)
        assertFalse(studyResult.allowed)
    }

    @Test
    fun `pro user has full unrestricted export access`() {
        val target = ExportTarget(ExportContentType.MEETING_PACK, 1L)
        val options = ExportOptions(format = ExportFormat.PDF, includeSpeakerLabels = true)
        val result = ExportFeaturePolicy.check(target, options, Entitlement.PRO)
        assertTrue(result.allowed)
    }
}
