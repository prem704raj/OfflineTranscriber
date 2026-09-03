package app.offlinetranscriber.mobile.backup

import app.offlinetranscriber.mobile.backup.format.BackupFormat
import app.offlinetranscriber.mobile.backup.format.BackupManifest
import app.offlinetranscriber.mobile.backup.format.BackupManifestCodec
import app.offlinetranscriber.mobile.backup.format.BackupManifestEntry
import app.offlinetranscriber.mobile.backup.restore.BackupZipSafety
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupManifestCodecTest {

    private val codec = BackupManifestCodec()

    @Test
    fun testEncodeAndDecodeManifest() {
        val manifest = BackupManifest(
            product = BackupFormat.PRODUCT,
            formatVersion = BackupFormat.FORMAT_VERSION,
            backupId = "backup-12345",
            createdAtEpochMs = 1700000000000L,
            appVersionName = "1.2.0",
            appVersionCode = 12L,
            sourceRoomSchemaVersion = 9,
            includesSettings = true,
            includesMedia = false,
            sectionCounts = mapOf("data/transcripts.json" to 5L),
            entries = listOf(
                BackupManifestEntry(
                    name = "data/transcripts.json",
                    uncompressedBytes = 1024L,
                    sha256 = "abcdef123456"
                )
            ),
            media = emptyList(),
            warnings = emptyList()
        )

        val out = ByteArrayOutputStream()
        codec.write(manifest, out)

        val decoded = codec.read(ByteArrayInputStream(out.toByteArray()))
        assertEquals(manifest.product, decoded.product)
        assertEquals(manifest.formatVersion, decoded.formatVersion)
        assertEquals(manifest.backupId, decoded.backupId)
        assertEquals(manifest.entries.size, decoded.entries.size)
        assertEquals("data/transcripts.json", decoded.entries[0].name)
    }

    @Test
    fun testIncompatibleProductRejected() {
        val manifest = BackupManifest(
            product = "other-product",
            formatVersion = 1,
            backupId = "123",
            createdAtEpochMs = 0L,
            appVersionName = "1.0",
            appVersionCode = 1L,
            sourceRoomSchemaVersion = 1,
            includesSettings = false,
            includesMedia = false,
            sectionCounts = emptyMap(),
            entries = emptyList(),
            media = emptyList(),
            warnings = emptyList()
        )

        try {
            codec.validateCompatibility(manifest)
            fail("Expected IllegalArgumentException for incompatible product")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun testFutureFormatVersionRejected() {
        val manifest = BackupManifest(
            product = BackupFormat.PRODUCT,
            formatVersion = 999,
            backupId = "123",
            createdAtEpochMs = 0L,
            appVersionName = "99.0",
            appVersionCode = 999L,
            sourceRoomSchemaVersion = 99,
            includesSettings = false,
            includesMedia = false,
            sectionCounts = emptyMap(),
            entries = emptyList(),
            media = emptyList(),
            warnings = emptyList()
        )

        try {
            codec.validateCompatibility(manifest)
            fail("Expected IllegalArgumentException for newer format version")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }
}

class BackupZipSafetyTest {

    @Test
    fun testValidNamesAccepted() {
        BackupZipSafety.requireSafeName("manifest.json")
        BackupZipSafety.requireSafeName("data/transcripts.json")
        BackupZipSafety.requireSafeName("media/audio_123.m4a")
    }

    @Test
    fun testUnsafeNamesRejected() {
        val unsafeNames = listOf(
            "",
            "   ",
            "/absolute/path.json",
            "data\\transcripts.json",
            "../secret.txt",
            "data/../secret.txt",
            "data/./transcripts.json",
            "data/file\u0000name.json"
        )

        for (name in unsafeNames) {
            try {
                BackupZipSafety.requireSafeName(name)
                fail("Expected rejection of unsafe name: $name")
            } catch (e: IllegalArgumentException) {
                // Expected
            }
        }
    }
}
