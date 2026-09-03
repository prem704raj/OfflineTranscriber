package app.offlinetranscriber.mobile.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryPolicyTest {

    @Test
    fun blankOrPunctuationOnlyRejected() {
        assertFalse(SearchQueryPolicy.shouldSearch(""))
        assertFalse(SearchQueryPolicy.shouldSearch("   "))
        assertFalse(SearchQueryPolicy.shouldSearch("!@#$%^&*()"))
    }

    @Test
    fun singleLetterRejected() {
        assertFalse(SearchQueryPolicy.shouldSearch("a"))
        assertFalse(SearchQueryPolicy.shouldSearch("z"))
        assertFalse(SearchQueryPolicy.shouldSearch(" K "))
    }

    @Test
    fun singleDigitAccepted() {
        assertTrue(SearchQueryPolicy.shouldSearch("1"))
        assertTrue(SearchQueryPolicy.shouldSearch("9"))
        assertTrue(SearchQueryPolicy.shouldSearch(" 5 "))
    }

    @Test
    fun multiLetterWordsAccepted() {
        assertTrue(SearchQueryPolicy.shouldSearch("hi"))
        assertTrue(SearchQueryPolicy.shouldSearch("audio"))
        assertTrue(SearchQueryPolicy.shouldSearch("offline transcription"))
    }

    @Test
    fun mixedTokensAcceptedIfAnyMeetsThreshold() {
        assertTrue(SearchQueryPolicy.shouldSearch("a 42"))
        assertTrue(SearchQueryPolicy.shouldSearch("a test"))
    }
}
