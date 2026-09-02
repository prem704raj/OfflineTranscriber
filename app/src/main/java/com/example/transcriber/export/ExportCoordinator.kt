package com.example.transcriber.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import com.example.transcriber.billing.Entitlement
import com.example.transcriber.export.document.ExportDocumentAssembler
import com.example.transcriber.export.files.ExportArtifactValidator
import com.example.transcriber.export.files.ExportCacheManager
import com.example.transcriber.export.files.ExportFileName
import com.example.transcriber.export.files.ExportSaver
import com.example.transcriber.export.files.ExportShareManager
import com.example.transcriber.export.model.ExportArtifact
import com.example.transcriber.export.model.ExportContentType
import com.example.transcriber.export.model.ExportFormat
import com.example.transcriber.export.model.ExportOptions
import com.example.transcriber.export.model.ExportTarget
import com.example.transcriber.export.model.PdfPageSize
import com.example.transcriber.export.print.PdfFilePrintAdapter
import com.example.transcriber.export.render.ExportRendererFactory
import com.example.transcriber.export.render.PdfExportRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class ExportCoordinator(
    private val assembler: ExportDocumentAssembler,
    private val cacheManager: ExportCacheManager,
    private val saver: ExportSaver,
    private val shareManager: ExportShareManager
) {

    suspend fun generateArtifact(
        target: ExportTarget,
        options: ExportOptions,
        entitlement: Entitlement,
        title: String
    ): ExportArtifact = withContext(Dispatchers.IO) {
        val check = ExportFeaturePolicy.check(target, options, entitlement)
        if (!check.allowed) {
            throw IllegalStateException(check.reason ?: "Feature requires Pro subscription.")
        }

        val suffix = when (target.contentType) {
            ExportContentType.TRANSCRIPT -> null
            ExportContentType.MEETING_PACK -> "Meeting"
            ExportContentType.STUDY_PACK -> "Study"
            ExportContentType.ASK_CONVERSATION -> "Ask"
        }

        val fileName = ExportFileName.create(title, options.format, suffix)
        val workFile = cacheManager.createWorkFile(fileName)

        val document = assembler.assemble(target, options)
        val renderer = ExportRendererFactory.create(options.format)

        val bytesWritten = renderer.render(document, options, workFile)
        val artifact = ExportArtifact(
            fileName = fileName,
            mimeType = options.format.mimeType,
            filePath = workFile.absolutePath,
            byteCount = bytesWritten
        )

        ExportArtifactValidator.validate(artifact, options.format)
        artifact
    }

    suspend fun saveArtifact(
        artifact: ExportArtifact,
        targetUri: Uri
    ): Long {
        return saver.saveToUri(artifact, targetUri)
    }

    fun createShareIntent(
        artifact: ExportArtifact
    ): Intent {
        return shareManager.createShareIntent(artifact)
    }

    suspend fun printPdf(
        context: Context,
        target: ExportTarget,
        options: ExportOptions,
        entitlement: Entitlement,
        title: String
    ) = withContext(Dispatchers.IO) {
        val check = ExportFeaturePolicy.check(target, options.copy(format = ExportFormat.PDF), entitlement)
        if (!check.allowed) {
            throw IllegalStateException(check.reason ?: "Print requires Pro subscription.")
        }

        val pdfOptions = options.copy(format = ExportFormat.PDF)
        val pdfArtifact = generateArtifact(target, pdfOptions, entitlement, title)
        val pdfFile = File(pdfArtifact.filePath)

        withContext(Dispatchers.Main) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                ?: throw IOException("Print service unavailable on this device")

            val adapter = PdfFilePrintAdapter(pdfFile, title)
            val attributes = PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMediaSize(
                    if (options.pageSize == PdfPageSize.LETTER) {
                        PrintAttributes.MediaSize.NA_LETTER
                    } else {
                        PrintAttributes.MediaSize.ISO_A4
                    }
                )
                .build()

            printManager.print(title, adapter, attributes)
        }
    }
}
