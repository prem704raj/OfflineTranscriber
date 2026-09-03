package app.offlinetranscriber.mobile.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class Sha256Test {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testEmptyFileHash() {
        val emptyFile = tempFolder.newFile("empty.bin")
        // SHA-256 of empty string is e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        val expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        val actual = Sha256.of(emptyFile)
        assertEquals(expected, actual)
        assertTrue(Sha256.matches(emptyFile, expected))
        assertTrue(Sha256.matches(emptyFile, expected.uppercase()))
    }

    @Test
    fun testKnownDataHash() {
        val file = tempFolder.newFile("hello.txt")
        file.writeText("Hello, World!", Charsets.UTF_8)
        // SHA-256 of "Hello, World!" is dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f
        val expected = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f"
        val actual = Sha256.of(file)
        assertEquals(expected, actual)
        assertTrue(Sha256.matches(file, expected))
        assertFalse(Sha256.matches(file, "0000000000000000000000000000000000000000000000000000000000000000"))
    }
}
