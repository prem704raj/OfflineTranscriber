package com.example.transcriber.backup

import com.example.transcriber.backup.format.BackupFormat
import com.example.transcriber.backup.format.BackupManifest
import com.example.transcriber.backup.format.BackupManifestCodec
import com.example.transcriber.backup.format.BackupManifestEntry
import com.example.transcriber.backup.restore.BackupStructureValidator
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class BackupStructureValidatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val validator = BackupStructureValidator()
    private val codec = BackupManifestCodec()

    private fun createZip(
        file: File,
        transcriptsJson: String,
        segmentsJson: String
    ): BackupManifest {
        val manifest = BackupManifest(
            product = BackupFormat.PRODUCT,
            formatVersion = BackupFormat.FORMAT_VERSION,
            backupId = "test-123",
            createdAtEpochMs = 1700000000000L,
            appVersionName = "1.2.0",
            appVersionCode = 12L,
            sourceRoomSchemaVersion = 9,
            includesSettings = false,
            includesMedia = false,
            sectionCounts = mapOf(
                "data/transcripts.json" to 1L,
                "data/transcript_segments.json" to 1L
            ),
            entries = listOf(
                BackupManifestEntry("data/transcripts.json", transcriptsJson.toByteArray().size.toLong(), "hash1"),
                BackupManifestEntry("data/transcript_segments.json", segmentsJson.toByteArray().size.toLong(), "hash2")
            ),
            media = emptyList(),
            warnings = emptyList()
        )

        ZipOutputStream(FileOutputStream(file)).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            codec.write(manifest, zip)
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("data/transcripts.json"))
            zip.write(transcriptsJson.toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("data/transcript_segments.json"))
            zip.write(segmentsJson.toByteArray())
            zip.closeEntry()
        }

        return manifest
    }

    @Test
    fun testValidTranscriptsAndSegmentsPassValidation() {
        val zipFile = tempFolder.newFile("valid.zip")
        val transcriptsJson = """[{"oldId": 1, "title": "Test", "audioFileName": "audio.m4a", "audioDurationMs": 5000, "createdAt": 1000, "fullText": "Hello", "segmentsJson": "", "modelUsed": "TINY", "audioUriString": null, "sourceUri": "", "mediaType": "AUDIO", "contentFingerprint": "fp1"}]"""
        val segmentsJson = """[{"oldId": 10, "transcriptOldId": 1, "startMs": 0, "endMs": 2000, "text": "Hello"}]"""

        val manifest = createZip(zipFile, transcriptsJson, segmentsJson)

        ZipFile(zipFile).use { zip ->
            validator.validate(zip, manifest)
        }
    }

    @Test
    fun testOrphanedSegmentRejected() {
        val zipFile = tempFolder.newFile("orphaned.zip")
        val transcriptsJson = """[{"oldId": 1, "title": "Test", "audioFileName": "audio.m4a", "audioDurationMs": 5000, "createdAt": 1000, "fullText": "Hello", "segmentsJson": "", "modelUsed": "TINY", "audioUriString": null, "sourceUri": "", "mediaType": "AUDIO", "contentFingerprint": "fp1"}]"""
        // Segment references non-existent transcript 999
        val segmentsJson = """[{"oldId": 10, "transcriptOldId": 999, "startMs": 0, "endMs": 2000, "text": "Hello"}]"""

        val manifest = createZip(zipFile, transcriptsJson, segmentsJson)

        ZipFile(zipFile).use { zip ->
            try {
                validator.validate(zip, manifest)
                fail("Expected IllegalArgumentException for orphaned segment")
            } catch (e: IllegalArgumentException) {
                // Expected
            }
        }
    }

    @Test
    fun testNegativeTimingRejected() {
        val zipFile = tempFolder.newFile("negative_time.zip")
        val transcriptsJson = """[{"oldId": 1, "title": "Test", "audioFileName": "audio.m4a", "audioDurationMs": 5000, "createdAt": 1000, "fullText": "Hello", "segmentsJson": "", "modelUsed": "TINY", "audioUriString": null, "sourceUri": "", "mediaType": "AUDIO", "contentFingerprint": "fp1"}]"""
        // EndMs < startMs
        val segmentsJson = """[{"oldId": 10, "transcriptOldId": 1, "startMs": 3000, "endMs": 1000, "text": "Hello"}]"""

        val manifest = createZip(zipFile, transcriptsJson, segmentsJson)

        ZipFile(zipFile).use { zip ->
            try {
                validator.validate(zip, manifest)
                fail("Expected IllegalArgumentException for invalid segment timing")
            } catch (e: IllegalArgumentException) {
                // Expected
            }
        }
    }
}
