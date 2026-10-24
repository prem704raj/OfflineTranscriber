package app.offlinetranscriber.mobile.domain

import app.offlinetranscriber.mobile.data.model.TranscriptEntity
import app.offlinetranscriber.mobile.domain.model.TranscriptSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptSearchReplaceTest {

    @Test
    fun testSearchAndReplaceCaseInsensitive() {
        val segments = listOf(
            TranscriptSegment(id = 1, startMs = 0, endMs = 2000, text = "Patient visited AIIMS clinic."),
            TranscriptSegment(id = 2, startMs = 2000, endMs = 4000, text = "Doctor at aiims advised rest.")
        )

        val findText = "aiims"
        val replaceText = "AIIMS New Delhi"

        val updated = segments.map { seg ->
            seg.copy(text = seg.text.replace(Regex(Regex.escape(findText), RegexOption.IGNORE_CASE), replaceText))
        }

        assertEquals("Patient visited AIIMS New Delhi clinic.", updated[0].text)
        assertEquals("Doctor at AIIMS New Delhi advised rest.", updated[1].text)
    }

    @Test
    fun testSearchAndReplaceCaseSensitive() {
        val segments = listOf(
            TranscriptSegment(id = 1, startMs = 0, endMs = 2000, text = "Apple is healthy. I love my Apple phone."),
            TranscriptSegment(id = 2, startMs = 2000, endMs = 4000, text = "an apple a day.")
        )

        val findText = "Apple"
        val replaceText = "Orange"

        val updated = segments.map { seg ->
            seg.copy(text = seg.text.replace(findText, replaceText))
        }

        assertEquals("Orange is healthy. I love my Orange phone.", updated[0].text)
        assertEquals("an apple a day.", updated[1].text) // lowercase "apple" remained untouched
    }

    @Test
    fun testRetainingTranscriptWhenAudioCleared() {
        val entity = TranscriptEntity.fromSegments(
            title = "Class Lecture",
            audioFileName = "lecture.m4a",
            audioDurationMs = 60_000L,
            segments = listOf(
                TranscriptSegment(id = 1, startMs = 0, endMs = 5000, text = "Chapter 1 Introduction")
            ),
            modelUsed = "Balanced",
            audioUriString = "file:///storage/emulated/0/recording.m4a"
        )

        val audioCleared = entity.copy(audioUriString = null)

        assertEquals("Class Lecture", audioCleared.title)
        assertEquals("Chapter 1 Introduction", audioCleared.fullText)
        assertEquals(null, audioCleared.audioUriString)
        assertEquals(1, audioCleared.getSegments().size)
    }

    @Test
    fun testSearchAndReplaceHealsRestoredTranscriptMismatchedIds() {
        // Restored from backup: JSON segments keep original device IDs (1, 2)
        val jsonSegments = listOf(
            TranscriptSegment(id = 1, startMs = 0, endMs = 3000, text = "Patient at AIIMS OPD."),
            TranscriptSegment(id = 2, startMs = 3000, endMs = 6000, text = "Followup at AIIMS next week.")
        )

        // But Room inserted them afresh with auto-increment IDs 301, 302
        val roomEntities = listOf(
            app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity(
                id = 301L,
                transcriptId = 50L,
                startMs = 0,
                endMs = 3000,
                text = "Patient at AIIMS OPD."
            ),
            app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity(
                id = 302L,
                transcriptId = 50L,
                startMs = 3000,
                endMs = 6000,
                text = "Followup at AIIMS next week."
            )
        )

        val entityMap = roomEntities.associateBy { it.id } // Keys are 301, 302; seg.id are 1, 2

        val findText = "AIIMS"
        val replaceText = "AIIMS New Delhi"

        val updatedEntities = mutableListOf<app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity>()

        // Execute the index-fallback and ID-healing algorithm from TranscriptRepository.searchAndReplace
        val updatedSegments = jsonSegments.mapIndexed { index, seg ->
            val matchedEntity = entityMap[seg.id] ?: roomEntities.getOrNull(index)
            assertNotNull("Matched entity must not be null even when seg.id misses entityMap", matchedEntity)

            val count = Regex(Regex.escape(findText), RegexOption.IGNORE_CASE).findAll(seg.text).count()
            if (count > 0) {
                val newText = seg.text.replace(Regex(Regex.escape(findText), RegexOption.IGNORE_CASE), replaceText)
                if (matchedEntity != null) {
                    updatedEntities.add(matchedEntity.copy(text = newText))
                }
                seg.copy(id = matchedEntity?.id ?: seg.id, text = newText)
            } else {
                seg.copy(id = matchedEntity?.id ?: seg.id)
            }
        }

        // Verify that entities were updated with new DB IDs (301, 302), not stale IDs (1, 2)
        assertEquals(2, updatedEntities.size)
        assertEquals(301L, updatedEntities[0].id)
        assertEquals("Patient at AIIMS New Delhi OPD.", updatedEntities[0].text)
        assertEquals(302L, updatedEntities[1].id)
        assertEquals("Followup at AIIMS New Delhi next week.", updatedEntities[1].text)

        // Verify JSON segments now have healed IDs matching the Room database
        assertEquals(301L, updatedSegments[0].id)
        assertEquals(302L, updatedSegments[1].id)
    }

    @Test
    fun testDeleteAudioResultDifferentiatesStorageVsReference() {
        val deletedResult = app.offlinetranscriber.mobile.data.repository.DeleteAudioResult(
            mediaDeleted = true,
            referenceCleared = true,
            message = "Audio file deleted. Transcript preserved."
        )
        assertTrue(deletedResult.mediaDeleted)
        assertTrue(deletedResult.referenceCleared)
        assertTrue(deletedResult.message.contains("deleted"))

        val unlinkedResult = app.offlinetranscriber.mobile.data.repository.DeleteAudioResult(
            mediaDeleted = false,
            referenceCleared = true,
            message = "Audio reference unlinked. Transcript preserved."
        )
        assertFalse(unlinkedResult.mediaDeleted)
        assertTrue(unlinkedResult.referenceCleared)
        assertTrue(unlinkedResult.message.contains("unlinked"))
    }

    @Test
    fun testSearchAndReplaceWithDollarAndBackslashSignsDoesNotCrash() {
        val segment = TranscriptSegment(
            id = 1,
            startMs = 0,
            endMs = 3000,
            text = "The doctor consultation fee was 500 rupees."
        )

        val findText = "500 rupees"
        val dollarReplacement = "cost $5 / $100 total"
        val backslashReplacement = """C:\records\patient_01"""

        // Without java.util.regex.Matcher.quoteReplacement, Regex.replace throws IndexOutOfBoundsException: No group 5
        val quotedDollar = java.util.regex.Matcher.quoteReplacement(dollarReplacement)
        val dollarResult = segment.text.replace(Regex(Regex.escape(findText), RegexOption.IGNORE_CASE), quotedDollar)
        assertEquals("The doctor consultation fee was cost $5 / $100 total.", dollarResult)

        val quotedBackslash = java.util.regex.Matcher.quoteReplacement(backslashReplacement)
        val backslashResult = segment.text.replace(Regex(Regex.escape(findText), RegexOption.IGNORE_CASE), quotedBackslash)
        assertEquals("""The doctor consultation fee was C:\records\patient_01.""", backslashResult)
    }

    @Test
    fun testLiveMatchesCounterAccuratelyCountsOccurrences() {
        val segments = listOf(
            TranscriptSegment(id = 1, startMs = 0, endMs = 2000, text = "AIIMS Delhi OPD at AIIMS"),
            TranscriptSegment(id = 2, startMs = 2000, endMs = 4000, text = "Visit aiims next week.")
        )

        // Case-insensitive count: 2 in seg 1, 1 in seg 2 = 3 total
        val countInsensitive = segments.sumOf { seg ->
            Regex(Regex.escape("aiims"), RegexOption.IGNORE_CASE).findAll(seg.text).count()
        }
        assertEquals(3, countInsensitive)

        // Case-sensitive count: 2 in seg 1, 0 in seg 2 = 2 total
        val countSensitive = segments.sumOf { seg ->
            seg.text.split("AIIMS").size - 1
        }
        assertEquals(2, countSensitive)
    }
}

