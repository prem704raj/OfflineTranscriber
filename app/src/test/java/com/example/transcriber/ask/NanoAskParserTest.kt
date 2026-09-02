package com.example.transcriber.ask

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NanoAskParserTest {

    @Test
    fun parsesAnswerAndCitations() {
        val parsed = NanoAskParser.parse(
            """
            ANSWER|Normalization reduces data redundancy.
            CITE|S1,S3
            """.trimIndent()
        )

        assertNotNull(parsed)
        assertEquals(
            "Normalization reduces data redundancy.",
            parsed!!.answer
        )
        assertEquals(
            listOf("S1", "S3"),
            parsed.citationLabels
        )
    }

    @Test
    fun missingAnswerFails() {
        assertNull(
            NanoAskParser.parse(
                "CITE|S1"
            )
        )
    }

    @Test
    fun parsesExtraWhitespaceGracefully() {
        val parsed = NanoAskParser.parse(
            """
            Random text before
            ANSWER|  Data structures organize elements. 
            CITE| S1 , S2, INVALID, S10 
            """.trimIndent()
        )

        assertNotNull(parsed)
        assertEquals(
            "Data structures organize elements.",
            parsed!!.answer
        )
        assertEquals(
            listOf("S1", "S2", "S10"),
            parsed.citationLabels
        )
    }
}
