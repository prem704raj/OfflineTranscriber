package com.example.transcriber.backup.data

import android.util.JsonWriter
import java.io.OutputStream
import java.io.OutputStreamWriter

class MediaLinkBackupExporter(
    private val links: List<MediaLinkBackupRow>
) : BackupSectionExporter {

    override val entryName = "data/media_links.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var count = 0L
        for (link in links) {
            writer.beginObject()
            writer.name("transcriptOldId").value(link.transcriptOldId)
            writer.name("mediaId").value(link.mediaId)
            writer.endObject()
            count++
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}
