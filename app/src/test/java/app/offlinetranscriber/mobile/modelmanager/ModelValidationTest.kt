package app.offlinetranscriber.mobile.modelmanager

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
}
