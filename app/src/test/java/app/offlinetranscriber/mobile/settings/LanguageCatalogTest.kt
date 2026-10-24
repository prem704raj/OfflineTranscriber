package app.offlinetranscriber.mobile.settings

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

        val pa = LanguageCatalog.byCode("pa")
        assertEquals("Punjabi", pa.label)

        val gu = LanguageCatalog.byCode("gu")
        assertEquals("Gujarati", gu.label)

        val kn = LanguageCatalog.byCode("kn")
        assertEquals("Kannada", kn.label)

        val ml = LanguageCatalog.byCode("ml")
        assertEquals("Malayalam", ml.label)
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
        val required = listOf("auto", "en", "hi", "pa", "gu", "kn", "ml", "es", "fr", "de", "it", "ja", "ko", "zh", "ar", "ru", "bn", "mr", "ta", "te", "ur")
        required.forEach { code ->
            val found = LanguageCatalog.byCode(code)
            assertNotNull(found)
            assertEquals(code, found.code)
        }
    }

    @Test
    fun detectsIndicLanguagesCorrectly() {
        org.junit.Assert.assertTrue(LanguageCatalog.isIndic("hi"))
        org.junit.Assert.assertTrue(LanguageCatalog.isIndic("pa"))
        org.junit.Assert.assertTrue(LanguageCatalog.isIndic("bn"))
        org.junit.Assert.assertTrue(LanguageCatalog.isIndic("ta"))
        org.junit.Assert.assertTrue(LanguageCatalog.isIndic("gu"))
        org.junit.Assert.assertFalse(LanguageCatalog.isIndic("en"))
        org.junit.Assert.assertFalse(LanguageCatalog.isIndic("fr"))
    }
}
