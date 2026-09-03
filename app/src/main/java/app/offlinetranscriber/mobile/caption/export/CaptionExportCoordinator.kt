package app.offlinetranscriber.mobile.caption.export

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import app.offlinetranscriber.mobile.billing.Entitlement
import app.offlinetranscriber.mobile.billing.EntitlementRepository
import app.offlinetranscriber.mobile.caption.export.background.CaptionExportRequestCodec
import app.offlinetranscriber.mobile.caption.export.background.CaptionExportWorkspace
import app.offlinetranscriber.mobile.caption.model.CaptionExportResolution
import app.offlinetranscriber.mobile.caption.source.CaptionCueValidator
import app.offlinetranscriber.mobile.data.database.TranscriptDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

@OptIn(UnstableApi::class)
class CaptionExportCoordinator(
    private val context: Context,
    private val transcriptDao: TranscriptDao,
    private val entitlementRepository: EntitlementRepository,
    private val workspace: CaptionExportWorkspace,
    private val videoExporter: CaptionVideoExporter
) {

    suspend fun execute(
        jobId: String,
        onProgress: suspend (Int) -> Unit,
        cancelled: () -> Boolean
    ): File = withContext(Dispatchers.IO) {
        val entitlement = entitlementRepository.entitlement.first()
        check(entitlement == Entitlement.PRO) {
            "Burned-in video export requires Pro."
        }

        val workDir = workspace.workDir(jobId)
        val requestFile = File(workDir, "request.json")
        if (!requestFile.exists()) {
            throw FileNotFoundException("Export request for job $jobId was not found.")
        }

        val request = CaptionExportRequestCodec.read(requestFile)

        val transcript = transcriptDao.getTranscriptById(request.transcriptId)
            ?: throw FileNotFoundException("Transcript ${request.transcriptId} no longer exists.")

        val sourceUriString = transcript.sourceUri.ifBlank { transcript.audioUriString ?: "" }
        if (sourceUriString.isBlank()) {
            throw FileNotFoundException("Original video is no longer available.")
        }

        val sourceUri = Uri.parse(sourceUriString)

        val cues = CaptionCueValidator.validate(
            request.cues,
            transcript.audioDurationMs
        )
        require(cues.isNotEmpty()) {
            "No valid caption cues found to render."
        }

        val info = SourceVideoInfoReader.read(context, sourceUri)

        val dimensions = when (request.resolution) {
            CaptionExportResolution.ORIGINAL -> null
            else -> CaptionOutputDimensions.calculate(
                info.orientedWidth,
                info.orientedHeight,
                request.resolution
            )
        }

        val partialOutput = File(workDir, "output.partial.mp4")
        if (partialOutput.exists()) partialOutput.delete()

        val exportResult = videoExporter.export(
            sourceUri = sourceUri,
            cues = cues,
            style = request.style,
            resolution = request.resolution,
            outputFile = partialOutput,
            outputDimensions = dimensions,
            onProgress = onProgress,
            cancelled = cancelled
        )

        CaptionVideoOutputValidator.validate(context, exportResult.file)

        val readyDir = workspace.readyDir(jobId)
        val finalOutput = File(readyDir, request.outputFileName)
        if (finalOutput.exists()) finalOutput.delete()

        if (!partialOutput.renameTo(finalOutput)) {
            partialOutput.copyTo(finalOutput, overwrite = true)
            partialOutput.delete()
        }

        workspace.cleanupWork(jobId)

        finalOutput
    }

    fun cancelAndCleanup(jobId: String) {
        try {
            workspace.cleanupWork(jobId)
        } catch (_: Exception) {}
    }
}
