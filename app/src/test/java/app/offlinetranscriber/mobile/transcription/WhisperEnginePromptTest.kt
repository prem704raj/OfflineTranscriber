package app.offlinetranscriber.mobile.transcription

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhisperEnginePromptTest {

    @Test
    fun hindiPromptContainsBilingualMarkers() {
        val prompt = WhisperEngine.buildEffectivePrompt("hi", null)
        assertNotNull(prompt)
        assertTrue(prompt!!.contains("नमस्ते"))
        assertTrue(prompt.contains("English"))
    }

    @Test
    fun punjabiPromptContainsGurmukhiMarkers() {
        val prompt = WhisperEngine.buildEffectivePrompt("pa", null)
        assertNotNull(prompt)
        assertTrue(prompt!!.contains("ਸਤਿ ਸ੍ਰੀ ਅਕਾਲ"))
        assertTrue(prompt.contains("English"))
    }

    @Test
    fun indicPromptAppendsCustomVocabulary() {
        val prompt = WhisperEngine.buildEffectivePrompt("hi", "AIIMS Cardiology OPD")
        assertNotNull(prompt)
        assertTrue(prompt!!.contains("नमस्ते"))
        assertTrue(prompt.endsWith("AIIMS Cardiology OPD"))
    }

    @Test
    fun nonIndicLanguageReturnsOnlyCustomVocabulary() {
        val promptWithVocab = WhisperEngine.buildEffectivePrompt("en", "Kubernetes Pods")
        assertEquals("Kubernetes Pods", promptWithVocab)

        val promptWithoutVocab = WhisperEngine.buildEffectivePrompt("en", null)
        assertNull(promptWithoutVocab)

        val promptBlankVocab = WhisperEngine.buildEffectivePrompt("fr", "   ")
        assertNull(promptBlankVocab)
    }

    @Test
    fun autoLanguageReturnsCustomVocabularyIfPresent() {
        val prompt = WhisperEngine.buildEffectivePrompt("auto", "Custom terminology")
        assertEquals("Custom terminology", prompt)

        val emptyPrompt = WhisperEngine.buildEffectivePrompt("auto", null)
        assertNull(emptyPrompt)
    }

    @Test
    fun promptTruncationEnforcesMaxLengthAtWordBoundary() {
        val longVocab = (1..200).joinToString(" ") { "Term$it" }
        assertTrue("Input vocabulary must be long", longVocab.length > WhisperEngine.MAX_PROMPT_CHARS_INDIC)

        val prompt = WhisperEngine.buildEffectivePrompt("hi", longVocab)
        assertNotNull(prompt)
        assertTrue("Effective prompt must not exceed MAX_PROMPT_CHARS_INDIC", prompt!!.length <= WhisperEngine.MAX_PROMPT_CHARS_INDIC)
        assertTrue("Effective prompt token count must not exceed 224", WhisperEngine.estimateTokens(prompt) <= WhisperEngine.MAX_PROMPT_TOKENS)
        assertTrue("Effective prompt should retain Hindi language hint", prompt.startsWith("नमस्ते"))
        // Word boundary check: should not end with partial word like "Ter"
        val lastWord = prompt.substringAfterLast(" ")
        assertTrue("Prompt should truncate cleanly at word boundary", lastWord.startsWith("Term"))
    }

    @Test
    fun promptTruncationWithoutLanguageHintEnforcesLimit() {
        val longVocab = (1..250).joinToString(" ") { "MedicineKeyword$it" }
        val prompt = WhisperEngine.buildEffectivePrompt("en", longVocab)
        assertNotNull(prompt)
        assertTrue(prompt!!.length <= WhisperEngine.MAX_PROMPT_CHARS_LATIN)
        assertTrue(WhisperEngine.estimateTokens(prompt) <= WhisperEngine.MAX_PROMPT_TOKENS)
        val lastWord = prompt.substringAfterLast(" ")
        assertTrue("Truncation should preserve complete word", lastWord.startsWith("MedicineKeyword"))
    }

    @Test
    fun defaultThreadCountIsSafeForMobile() {
        val threads = WhisperEngine.DEFAULT_THREAD_COUNT
        assertTrue("Thread count should be >= 2", threads >= 2)
        assertTrue("Thread count should be <= 4 for mobile thermal safety", threads <= 4)
    }
}

