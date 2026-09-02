package com.example.transcriber.export.render

import com.example.transcriber.export.document.ExportDocument
import com.example.transcriber.export.model.ExportOptions
import com.example.transcriber.export.render.docx.DocxPackageParts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocxExportRenderer : ExportRenderer {

    override suspend fun render(
        document: ExportDocument,
        options: ExportOptions,
        targetFile: File
    ): Long = withContext(Dispatchers.IO) {
        FileOutputStream(targetFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // [Content_Types].xml
                addZipEntry(zos, "[Content_Types].xml", DocxPackageParts.contentTypesXml)

                // _rels/.rels
                addZipEntry(zos, "_rels/.rels", DocxPackageParts.packageRelsXml)

                // word/_rels/document.xml.rels
                addZipEntry(zos, "word/_rels/document.xml.rels", DocxPackageParts.documentRelsXml)

                // word/styles.xml
                addZipEntry(zos, "word/styles.xml", DocxPackageParts.stylesXml)

                // word/document.xml
                val documentXml = DocxPackageParts.buildDocumentXml(document, options)
                addZipEntry(zos, "word/document.xml", documentXml)

                zos.finish()
            }
        }
        targetFile.length()
    }

    private fun addZipEntry(zos: ZipOutputStream, path: String, content: String) {
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        val entry = ZipEntry(path)
        zos.putNextEntry(entry)
        zos.write(bytes)
        zos.closeEntry()
    }
}
