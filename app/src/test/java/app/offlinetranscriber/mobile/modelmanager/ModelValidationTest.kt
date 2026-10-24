package app.offlinetranscriber.mobile.modelmanager

import app.offlinetranscriber.mobile.security.Sha256
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile

class ModelValidationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun smallFileFailsValidation() {
        val modelFile = tempFolder.newFile("ggml-base-q5_1.bin")
        // Write 1 KB only (less than 40 MB threshold)
        modelFile.writeBytes(ByteArray(1024))

        val spec = ModelCatalog.balanced
        val isValid = modelFile.exists() && modelFile.isFile && modelFile.length() >= spec.minimumValidBytes
        assertFalse("Model file with size under minimum threshold must fail validation", isValid)
    }

    @Test
    fun validSizeFilePassesValidation() {
        val modelFile = tempFolder.newFile("ggml-tiny-q5_1.bin")
        val raf = RandomAccessFile(modelFile, "rw")
        raf.setLength(25L * 1024L * 1024L) // 25 MB (>= 20 MB minimumValidBytes)
        raf.close()

        val spec = ModelCatalog.fast
        val isValid = modelFile.exists() && modelFile.isFile && modelFile.length() >= spec.minimumValidBytes
        assertTrue("Model file meeting minimum size must pass validation", isValid)
    }

    @Test
    fun corruptedFileFailsSha256Check() {
        val modelFile = tempFolder.newFile("ggml-tiny-corrupt.bin")
        modelFile.writeBytes("corrupted whisper model content".toByteArray())

        val spec = ModelCatalog.fast
        val matches = Sha256.matches(modelFile, spec.sha256)
        assertFalse("Corrupted file must fail SHA-256 match", matches)
    }

    @Test
    fun accurateShaComputationMatchesKnownBytes() {
        val testFile = tempFolder.newFile("test-sha.bin")
        testFile.writeBytes("Hello Whisper".toByteArray())
        val computed = Sha256.of(testFile)
        val expected = "19c8ff8ca54cf5b65623a523cc16d962df81f084de9782eed898fe3164bdcfb9"
        assertEquals(expected, computed)
        assertTrue(Sha256.matches(testFile, expected))
    }

    @Test
    fun modelCatalogAllSpecsHaveStrictSha256AndSizeBounds() {
        val all = ModelCatalog.all
        assertTrue(all.isNotEmpty())

        for (spec in all) {
            assertEquals("SHA-256 hash must be exactly 64 hex characters for ${spec.label}", 64, spec.sha256.length)
            assertTrue("SHA-256 hash must be valid hexadecimal for ${spec.label}", spec.sha256.all { it in "0123456789abcdefABCDEF" })
            assertTrue("minimumValidBytes must be positive for ${spec.label}", spec.minimumValidBytes > 0)
            assertTrue("approximateBytes must be greater than minimumValidBytes for ${spec.label}", spec.approximateBytes >= spec.minimumValidBytes)
        }

        // Verify tier hierarchy
        assertTrue(ModelCatalog.fast.approximateBytes < ModelCatalog.balanced.approximateBytes)
        assertTrue(ModelCatalog.balanced.approximateBytes < ModelCatalog.accurate.approximateBytes)
    }
}
