package com.example.transcriber.study

import com.example.transcriber.data.model.MediaType
import com.example.transcriber.data.model.TranscriptEntity
import com.example.transcriber.data.model.TranscriptSegmentEntity
import com.example.transcriber.study.model.StudyEngineType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicStudyEngineTest {

    @Test
    fun generatesKeyPointsChaptersAndCardsFromTranscript() = runTest {
        val transcript = TranscriptEntity(
            id = 1L,
            title = "Database Normalization Lecture",
            audioFileName = "lecture.mp3",
            audioDurationMs = 120_000L,
            segmentsJson = "",
            fullText = "",
            modelUsed = "whisper-base",
            mediaType = MediaType.AUDIO
        )

        val segments = listOf(
            TranscriptSegmentEntity(
                id = 1L,
                transcriptId = 1L,
                startMs = 0L,
                endMs = 15_000L,
                text = "Welcome everyone. Today we discuss database normalization and functional dependencies."
            ),
            TranscriptSegmentEntity(
                id = 2L,
                transcriptId = 1L,
                startMs = 15_000L,
                endMs = 35_000L,
                text = "Normalization is a technique used to organize relational tables and minimize redundancy."
            ),
            TranscriptSegmentEntity(
                id = 3L,
                transcriptId = 1L,
                startMs = 35_000L,
                endMs = 60_000L,
                text = "Boyce-Codd Normal Form or BCNF is a stricter version of Third Normal Form."
            ),
            TranscriptSegmentEntity(
                id = 4L,
                transcriptId = 1L,
                startMs = 60_000L,
                endMs = 90_000L,
                text = "A functional dependency means a relationship between attributes in a relational database table."
            ),
            TranscriptSegmentEntity(
                id = 5L,
                transcriptId = 1L,
                startMs = 90_000L,
                endMs = 120_000L,
                text = "In conclusion, applying normalization ensures database consistency and avoids update anomalies."
            )
        )

        val engine = ClassicStudyEngine()
        val draft = engine.generate(transcript, segments)

        assertEquals(StudyEngineType.CLASSIC, draft.engine)
        assertTrue("Key points should not be empty", draft.keyPoints.isNotEmpty())
        assertTrue("Chapters should not be empty", draft.chapters.isNotEmpty())
        assertTrue("Flashcards should not be empty", draft.flashcards.isNotEmpty())

        // Chapter timestamps must match one of the actual segment startMs values
        val validStartMs = segments.map { it.startMs }.toSet()
        draft.chapters.forEach { chapter ->
            assertTrue(
                "Chapter startMs ${chapter.startMs} must be in real segments",
                chapter.startMs in validStartMs
            )
        }
    }
}
