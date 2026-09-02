package com.example.transcriber.export

import com.example.transcriber.export.render.docx.XmlEscaper
import org.junit.Assert.assertEquals
import org.junit.Test

class XmlEscaperTest {

    @Test
    fun `escapes xml special characters`() {
        val input = "<hello & \"world\" 'test'>"
        val escaped = XmlEscaper.escape(input)
        assertEquals("&lt;hello &amp; &quot;world&quot; &apos;test&apos;&gt;", escaped)
    }

    @Test
    fun `strips non-printable control characters`() {
        val input = "Hello\u0000\u0008World\nGood"
        val escaped = XmlEscaper.escape(input)
        assertEquals("HelloWorld\nGood", escaped)
    }
}
