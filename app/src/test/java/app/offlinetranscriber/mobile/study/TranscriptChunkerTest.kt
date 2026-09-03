package app.offlinetranscriber.mobile.study

import app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptChunkerTest {

    @Test
    fun chunksPreserveSegmentBoundaries() {
        val segments = (0 until 30).map { i ->
            TranscriptSegmentEntity(
                id = i.toLong() + 1L,
                transcriptId = 1L,
                startMs = i * 5_000L,
                endMs = i * 5_000L + 4_000L,
                text = "Database normalization explanation ".repeat(8)
            )
        }

        val chunks = TranscriptChunker.build(
            segments = segments,
            maxChunks = 8
        )

        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.size <= 8)

        chunks.forEach { chunk ->
            assertTrue(chunk.segmentIds.isNotEmpty())
            assertTrue(chunk.text.contains("["))
        }
    }

    @Test
    fun emptyInputReturnsEmpty() {
        assertEquals(
            emptyList<TranscriptChunk>(),
            TranscriptChunker.build(emptyList())
        )
    }
}
