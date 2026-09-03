package app.offlinetranscriber.mobile.backup.data

import java.io.OutputStream

data class BackupSectionResult(
    val rowCount: Long
)

interface BackupSectionExporter {

    val entryName: String

    suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit = {}
    ): BackupSectionResult
}
