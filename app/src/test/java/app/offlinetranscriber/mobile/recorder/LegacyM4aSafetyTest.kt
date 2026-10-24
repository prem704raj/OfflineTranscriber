package app.offlinetranscriber.mobile.recorder

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream

class LegacyM4aSafetyTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testDir: File

    @Before
    fun setUp() {
        testDir = tempFolder.newFolder("recordings")
    }

    @Test
    fun legacyM4aFileIsNeverModifiedOrCorruptedByWavRepair() {
        val m4aFile = File(testDir, "lecture_legacy.m4a")

        // Construct realistic M4A header: 4-byte size (0x00, 0x00, 0x00, 0x20), "ftypM4A " (box type & major brand)
        val m4aHeader = byteArrayOf(
            0x00, 0x00, 0x00, 0x20, // box size: 32 bytes
            'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(), // "ftyp"
            'M'.code.toByte(), '4'.code.toByte(), 'A'.code.toByte(), ' '.code.toByte(), // major brand
            0x00, 0x00, 0x00, 0x00  // minor version
        )
        val dummyAacData = ByteArray(16384) { (it % 255).toByte() }
        val originalBytes = m4aHeader + dummyAacData

        FileOutputStream(m4aFile).use { fos ->
            fos.write(originalBytes)
        }

        val originalLength = m4aFile.length()
        val originalPrefix = m4aFile.readBytes().take(44).toByteArray()

        // Attempting repairWavHeader on .m4a MUST return false and do nothing
        val repaired = WavAudioWriter.repairWavHeader(m4aFile)
        assertFalse("repairWavHeader must refuse to touch non-WAV container formats", repaired)

        // Verify byte-level identity: not a single byte was altered
        val postCheckBytes = m4aFile.readBytes()
        assertEquals(originalLength, m4aFile.length())
        assertArrayEquals("M4A ftyp header must be preserved byte-for-byte", originalPrefix, postCheckBytes.take(44).toByteArray())
        assertArrayEquals("Entire file contents must remain identical", originalBytes, postCheckBytes)

        // isValidWav must also return false
        assertFalse("M4A file must never report isValidWav = true", WavAudioWriter.isValidWav(m4aFile))
    }

    @Test
    fun recoverableDraftPreservesLegacyM4aWithoutWavCorruption() {
        val m4aFile = File(testDir, "legacy_interrupted.m4a")
        // Write 10KB M4A file
        val m4aHeader = byteArrayOf(
            0x00, 0x00, 0x00, 0x20,
            'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte()
        )
        m4aFile.writeBytes(m4aHeader + ByteArray(10240))

        RecordingDraftJournal.start(testDir, m4aFile.absolutePath, 1000L)
        RecordingDraftJournal.updateHeartbeat(testDir, 5000L, isPaused = false)

        val draft = RecordingDraftJournal.getRecoverableDraft(testDir)
        assertNotNull("Legacy M4A draft must be recoverable if file > 8192 bytes", draft)
        assertEquals(m4aFile.absolutePath, draft?.filePath)
        assertEquals(5000L, draft?.durationMs)

        // Verify the file's first bytes were NOT overwritten with RIFF/WAV
        val bytes = m4aFile.readBytes()
        assertEquals('f'.code.toByte(), bytes[4])
        assertEquals('t'.code.toByte(), bytes[5])
        assertEquals('y'.code.toByte(), bytes[6])
        assertEquals('p'.code.toByte(), bytes[7])
    }
}

