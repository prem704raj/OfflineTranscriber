package com.example.transcriber.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtsQueryBuilderTest {

    @Test
    fun testSimpleTokens() {
        val query = FtsQueryBuilder.build("database normalization")
        assertEquals("database* AND normalization*", query)
    }

    @Test
    fun testPunctuationAndSpecialChars() {
        val query = FtsQueryBuilder.build("what's BCNF??? (3NF)")
        assertEquals("what* AND s* AND BCNF* AND 3NF*", query)
    }

    @Test
    fun testUnicodeLetters() {
        val query = FtsQueryBuilder.build("münchen café 123")
        assertEquals("münchen* AND café* AND 123*", query)
    }

    @Test
    fun testBlankOrPunctuationOnly() {
        assertNull(FtsQueryBuilder.build(""))
        assertNull(FtsQueryBuilder.build("   "))
        assertNull(FtsQueryBuilder.build("???---***"))
    }

    @Test
    fun testDisplayTokens() {
        val tokens = FtsQueryBuilder.displayTokens("Hello, World!")
        assertEquals(listOf("Hello", "World"), tokens)
    }
}
