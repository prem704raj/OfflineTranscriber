package com.example.transcriber.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LanguageCatalogTest {

    @Test
    fun returnsCorrectLanguageForKnownCode() {
        val en = LanguageCatalog.byCode("en")
        assertEquals("English", en.label)

        val hi = LanguageCatalog.byCode("hi")
        assertEquals("Hindi", hi.label)

        val es = LanguageCatalog.byCode("es")
        assertEquals("Spanish", es.label)
    }

    @Test
    fun returnsAutoDetectForUnknownCode() {
        val fallback = LanguageCatalog.byCode("unknown_xyz")
        assertEquals("auto", fallback.code)
        assertEquals("Auto detect", fallback.label)
    }

    @Test
    fun containsAllRequiredMajorLanguages() {
        val codes = LanguageCatalog.all.map { it.code }
        val required = listOf("auto", "en", "hi", "es", "fr", "de", "it", "ja", "ko", "zh", "ar", "ru", "bn", "mr", "ta", "te", "ur")
        required.forEach { code ->
            val found = LanguageCatalog.byCode(code)
            assertNotNull(found)
            assertEquals(code, found.code)
        }
    }
}
