package app.offlinetranscriber.mobile.ask

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AskQueryNormalizerTest {

    @Test
    fun normalizesWhitespace() {
        assertEquals(
            "what is normalization?",
            AskQueryNormalizer.clean(
                "  what   is normalization? "
            )
        )
    }

    @Test
    fun capsLength() {
        assertEquals(
            300,
            AskQueryNormalizer.clean(
                "a".repeat(1000)
            ).length
        )
    }

    @Test
    fun ftsQueryGeneratesSafeTokens() {
        val query = AskQueryNormalizer.ftsQuery("What is normalization?")
        assertNotNull(query)
    }

    @Test
    fun tooShortQueryReturnsNull() {
        assertNull(AskQueryNormalizer.ftsQuery("a"))
    }
}
