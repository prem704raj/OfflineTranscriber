package app.offlinetranscriber.mobile.meeting

import app.offlinetranscriber.mobile.meeting.nano.MeetingProtocolParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MeetingProtocolParserTest {

    @Test
    fun validParseExtractsAllSections() {
        val raw = """
            SUMMARY|Team discussed launch roadmap and database migration.
            SUMMARY_SOURCES|S1,S2
            ACTION|Send final report|Rahul|Friday|S3
            DECISION|Use Room for local database|S1
            QUESTION|Who owns migration testing?|S4
            TOPIC|Launch Roadmap|S1
            TOPIC|Database Migration|S3
        """.trimIndent()

        val parsed = MeetingProtocolParser.parse(raw)
        assertNotNull(parsed)
        assertEquals("Team discussed launch roadmap and database migration.", parsed!!.summary)
        assertEquals(listOf("S1", "S2"), parsed.summarySources)
        assertEquals(1, parsed.actions.size)
        assertEquals("Send final report", parsed.actions[0].text)
        assertEquals("Rahul", parsed.actions[0].assignee)
        assertEquals("Friday", parsed.actions[0].dueText)
        assertEquals("S3", parsed.actions[0].sourceLabel)
        assertEquals(1, parsed.decisions.size)
        assertEquals(1, parsed.questions.size)
        assertEquals(2, parsed.topics.size)
    }

    @Test
    fun missingSummaryReturnsNull() {
        val raw = """
            ACTION|Task|Rahul|Friday|S1
            DECISION|Decided|S1
        """.trimIndent()

        assertNull(MeetingProtocolParser.parse(raw))
    }

    @Test
    fun malformedLineDoesNotCrashParser() {
        val raw = """
            SUMMARY|Overview of discussion.
            MALFORMED LINE WITHOUT PIPES
            ACTION|Only two parts|S1
            ACTION|Valid task|||S2
            DECISION|Valid decision|S1
        """.trimIndent()

        val parsed = MeetingProtocolParser.parse(raw)
        assertNotNull(parsed)
        assertEquals("Overview of discussion.", parsed!!.summary)
        assertEquals(1, parsed.actions.size)
        assertEquals("Valid task", parsed.actions[0].text)
        assertEquals(1, parsed.decisions.size)
    }
}
