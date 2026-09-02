package com.example.transcriber.backup.restore

import com.example.transcriber.backup.format.BackupManifest
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipFile

data class RestoredMediaMapping(
    val transcriptOldId: Long,
    val mediaId: String,
    val finalFile: File,
    val mimeType: String?
)

class BackupMediaRestorer(
    private val restoredRoot: File
) {

    fun restoreRequired(
        zip: ZipFile,
        manifest: BackupManifest,
        mediaLinks: Map<Long, String>,
        requiredTranscriptOldIds: Set<Long>,
        onBytes: (Long) -> Unit = {}
    ): List<RestoredMediaMapping> {
        restoredRoot.mkdirs()

        val mediaById = manifest.media.associateBy { it.mediaId }
        val created = mutableListOf<File>()

        try {
            return requiredTranscriptOldIds.mapNotNull { transcriptOldId ->
                val mediaId = mediaLinks[transcriptOldId] ?: return@mapNotNull null
                val meta = mediaById[mediaId] ?: error("Backup media link refers to unknown mediaId: $mediaId")
                val entry = zip.getEntry(meta.entryName) ?: error("Backup media entry is missing: ${meta.entryName}")

                val extension = meta.entryName.substringAfterLast('.', "bin")
                    .lowercase()
                    .takeIf { it.matches(Regex("""[a-z0-9]{1,10}""")) } ?: "bin"

                val target = File(restoredRoot, "${UUID.randomUUID()}.$extension")
                val digest = MessageDigest.getInstance("SHA-256")

                zip.getInputStream(entry).buffered(64 * 1024).use { input ->
                    FileOutputStream(target).buffered(64 * 1024).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var copied = 0L

                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            if (read == 0) continue

                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            copied += read
                            onBytes(copied)
                        }
                        output.flush()
                    }
                }

                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                require(actual.equals(meta.sha256, ignoreCase = true)) {
                    "Restored media checksum verification failed for ${meta.entryName}"
                }

                created.add(target)

                RestoredMediaMapping(
                    transcriptOldId = transcriptOldId,
                    mediaId = mediaId,
                    finalFile = target,
                    mimeType = meta.mimeType
                )
            }
        } catch (error: Throwable) {
            created.forEach { it.delete() }
            throw error
        }
    }
}
