package app.offlinetranscriber.mobile.speaker.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakerPrototypeBuilderTest {

    @Test
    fun testCosineSimilarityCalculations() {
        val builder = SpeakerPrototypeBuilder()
        val vecA = floatArrayOf(1.0f, 0.0f, 0.0f)
        val vecB = floatArrayOf(1.0f, 0.0f, 0.0f)
        val vecC = floatArrayOf(0.0f, 1.0f, 0.0f)

        assertEquals(1.0f, builder.cosineSimilarity(vecA, vecB), 0.001f)
        assertEquals(0.0f, builder.cosineSimilarity(vecA, vecC), 0.001f)
    }

    @Test
    fun testAddSpeakerAndFindMatch() {
        val builder = SpeakerPrototypeBuilder()
        val embAlice = floatArrayOf(0.8f, 0.6f, 0.0f)
        val embBob = floatArrayOf(0.0f, 0.6f, 0.8f)

        builder.addOrUpdateSpeaker(0, embAlice)
        builder.addOrUpdateSpeaker(1, embBob)

        assertEquals(2, builder.speakerCount())

        // Query with vector close to Alice
        val queryAlice = floatArrayOf(0.79f, 0.61f, 0.0f)
        val matchAlice = builder.findBestMatch(queryAlice, minSimilarityThreshold = 0.8f)
        assertNotNull(matchAlice)
        assertEquals(0, matchAlice!!.first)
        assertTrue(matchAlice.second > 0.95f)

        // Query with vector dissimilar to both
        val queryUnknown = floatArrayOf(-1.0f, 0.0f, 0.0f)
        val matchUnknown = builder.findBestMatch(queryUnknown, minSimilarityThreshold = 0.5f)
        assertNull(matchUnknown)
    }
}
