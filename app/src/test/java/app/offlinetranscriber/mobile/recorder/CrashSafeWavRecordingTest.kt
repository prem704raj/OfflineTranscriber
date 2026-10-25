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
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CrashSafeWavRecordingTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testDir: File

    @Before
    fun setUp() {
        testDir = tempFolder.newFolder("wav_recordings")
    }

    @Test
    fun writeInitialHeaderCreatesValid44ByteHeader() {
        val file = File(testDir, "test_init.wav")
        WavAudioWriter.writeInitialHeader(file)

        assertEquals(44L, file.length())

        // Verify canonical header fields
        val headerBytes = file.readBytes()
        val riff = String(headerBytes, 0, 4, Charsets.US_ASCII)
        val wave = String(headerBytes, 8, 4, Charsets.US_ASCII)
        val fmt = String(headerBytes, 12, 4, Charsets.US_ASCII)
        val data = String(headerBytes, 36, 4, Charsets.US_ASCII)

        assertEquals("RIFF", riff)
        assertEquals("WAVE", wave)
        assertEquals("fmt ", fmt)
        assertEquals("data", data)

        val buffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(16, buffer.getInt(16)) // fmt subchunk size
        assertEquals(1.toShort(), buffer.getShort(20)) // Audio format (1 = PCM)
        assertEquals(1.toShort(), buffer.getShort(22)) // Num channels (1 = mono)
        assertEquals(16000, buffer.getInt(24)) // Sample rate (16 kHz)
        assertEquals(32000, buffer.getInt(28)) // Byte rate (16000 * 2)
        assertEquals(2.toShort(), buffer.getShort(32)) // Block align
        assertEquals(16.toShort(), buffer.getShort(34)) // Bits per sample
        assertEquals(0, buffer.getInt(40)) // Data size initialized to 0
    }

    @Test
    fun abruptCrashRecoveryRepairsWavHeaderAccurately() {
        val file = File(testDir, "crash_simulated.wav")

        // 1. Simulate process start: write initial placeholder header (totalPcmBytes = 0)
        WavAudioWriter.writeInitialHeader(file)

        // 2. Simulate continuous recording of 5 seconds of 16kHz 16-bit mono PCM (16000 samples/sec * 2 bytes = 32000 bytes/sec)
        // 5 seconds = 160,000 bytes
        val samplePcmData = ByteArray(160_000) { (it % 127).toByte() }
        FileOutputStream(file, true).use { fos ->
            fos.write(samplePcmData)
            fos.flush()
        }

        // At this point, simulate abrupt SIGKILL or crash: header still has data size = 0, but file length is 160,044 bytes.
        assertEquals(160_044L, file.length())

        // 3. System reboots / restarts app: RecordingDraftJournal recovers draft and repairs header
        val repaired = WavAudioWriter.repairWavHeader(file)
        assertTrue("WAV repair must succeed for uncorrupted PCM payload", repaired)

        // 4. Verify repaired header
        assertTrue("File must be valid WAV after crash repair", WavAudioWriter.isValidWav(file))
        val durationMs = WavAudioWriter.getDurationMs(file)
        assertEquals("5 seconds of PCM must equal 5000 ms duration", 5000L, durationMs)

        // Verify data chunk size matches actual PCM length
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(40)
            val buffer = ByteArray(4)
            raf.readFully(buffer)
            val dataSize = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).int
            assertEquals(160_000, dataSize)

            raf.seek(4)
            raf.readFully(buffer)
            val riffChunkSize = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).int
            assertEquals(160_044 - 8, riffChunkSize)
        }
    }

    @Test
    fun nonRiffWavIsNeverRewrittenWithSyntheticHeader() {
        val file = File(testDir, "not_really_wav.wav")
        val original = ByteArray(256) { (it % 251).toByte() }
        file.writeBytes(original)

        assertFalse("Repair must reject files without a canonical RIFF PCM header", WavAudioWriter.repairWavHeader(file))
        assertFalse("Non-RIFF content is not a valid WAV", WavAudioWriter.isValidWav(file))
        assertArrayEquals("Rejected file must remain byte-for-byte unchanged", original, file.readBytes())
    }

    @Test
    fun repairRejectsHeaderWithUnexpectedFormatFields() {
        val file = File(testDir, "wrong_format.wav")
        val header = WavAudioWriter.createWavHeader(100L, sampleRate = 8_000)
        file.writeBytes(header + ByteArray(100))
        val original = file.readBytes()

        assertFalse(
            "Repair must not reinterpret a WAV recorded with non-canonical format fields",
            WavAudioWriter.repairWavHeader(file)
        )
        assertArrayEquals(original, file.readBytes())
    }

    @Test
    fun overclaimedDataChunkSizeIsRejected() {
        val file = File(testDir, "overclaimed.wav")
        WavAudioWriter.writeInitialHeader(file)
        FileOutputStream(file, true).use { it.write(ByteArray(1_000)) }

        RandomAccessFile(file, "rw").use { raf ->
            val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            raf.seek(40)
            buf.clear()
            raf.write(buf.putInt(2_000).array())
            raf.seek(4)
            buf.clear()
            raf.write(buf.putInt(2_036).array())
        }

        assertFalse("Data chunk larger than the physical payload must be rejected", WavAudioWriter.isValidWav(file))
    }

    @Test
    fun corruptedOrEmptyDraftRejectedByRecovery() {
        val emptyFile = File(testDir, "empty.wav").apply { createNewFile() }
        assertFalse("Empty file is not a valid WAV", WavAudioWriter.isValidWav(emptyFile))
        assertFalse("Cannot repair 0-byte file", WavAudioWriter.repairWavHeader(emptyFile))
        assertEquals(0L, WavAudioWriter.getDurationMs(emptyFile))

        val partialHeaderFile = File(testDir, "partial.wav").apply {
            writeBytes(ByteArray(20))
        }
        assertFalse("Partial header is not a valid WAV", WavAudioWriter.isValidWav(partialHeaderFile))
        assertFalse("Cannot repair partial header without PCM", WavAudioWriter.repairWavHeader(partialHeaderFile))
    }

    @Test
    fun recoverableDraftRepairsAudioAndCalculatesTrueDuration() {
        val crashFile = File(testDir, "crashed_draft.wav")
        // Write header with 0 data
        WavAudioWriter.writeInitialHeader(crashFile)
        // Append 10 seconds of PCM (320,000 bytes)
        FileOutputStream(crashFile, true).use { fos ->
            fos.write(ByteArray(320_000))
        }

        // Journal started with 1000L
        RecordingDraftJournal.start(testDir, crashFile.absolutePath, 1000L)
        // Heartbeat was recorded as only 3000L before sudden kill
        RecordingDraftJournal.updateHeartbeat(testDir, 3000L, isPaused = false)

        // On app launch, journal recovers draft and uses true audio duration
        val recovered = RecordingDraftJournal.getRecoverableDraft(testDir)
        assertNotNull(recovered)
        assertEquals(crashFile.absolutePath, recovered?.filePath)
        // Real duration from 320,000 bytes is 10,000 ms, overriding stale 3,000 ms heartbeat
        assertEquals(10_000L, recovered?.durationMs)
        assertTrue("Recovered audio must be verified valid WAV", WavAudioWriter.isValidWav(File(recovered!!.filePath)))
    }
}
