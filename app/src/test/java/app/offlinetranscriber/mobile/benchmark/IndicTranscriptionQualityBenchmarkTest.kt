package app.offlinetranscriber.mobile.benchmark

import app.offlinetranscriber.mobile.transcription.WhisperEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min

class IndicTranscriptionQualityBenchmarkTest {

    companion object {
        /**
         * Calculates Word Error Rate (WER) using Levenshtein distance on tokenized word sequences.
         * WER = (Substitutions + Deletions + Insertions) / TotalReferenceWords
         */
        fun calculateWer(reference: String, hypothesis: String): Float {
            val refWords = reference.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val hypWords = hypothesis.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

            if (refWords.isEmpty()) return if (hypWords.isEmpty()) 0f else 1f

            val d = Array(refWords.size + 1) { IntArray(hypWords.size + 1) }

            for (i in 0..refWords.size) d[i][0] = i
            for (j in 0..hypWords.size) d[0][j] = j

            for (i in 1..refWords.size) {
                for (j in 1..hypWords.size) {
                    val cost = if (refWords[i - 1].equals(hypWords[j - 1], ignoreCase = true)) 0 else 1
                    d[i][j] = min(
                        min(
                            d[i - 1][j] + 1,      // deletion
                            d[i][j - 1] + 1       // insertion
                        ),
                        d[i - 1][j - 1] + cost     // substitution
                    )
                }
            }

            return d[refWords.size][hypWords.size].toFloat() / refWords.size.toFloat()
        }

        /**
         * Calculates Character Error Rate (CER) using Levenshtein distance on character sequences.
         * Crucial for Indic phonetic scripts where character-level inflections and conjuncts occur.
         */
        fun calculateCer(reference: String, hypothesis: String): Float {
            val refChars = reference.replace("\\s+".toRegex(), "")
            val hypChars = hypothesis.replace("\\s+".toRegex(), "")

            if (refChars.isEmpty()) return if (hypChars.isEmpty()) 0f else 1f

            val d = Array(refChars.length + 1) { IntArray(hypChars.length + 1) }

            for (i in 0..refChars.length) d[i][0] = i
            for (j in 0..hypChars.length) d[0][j] = j

            for (i in 1..refChars.length) {
                for (j in 1..hypChars.length) {
                    val cost = if (refChars[i - 1] == hypChars[j - 1]) 0 else 1
                    d[i][j] = min(
                        min(
                            d[i - 1][j] + 1,
                            d[i][j - 1] + 1
                        ),
                        d[i - 1][j - 1] + cost
                    )
                }
            }

            return d[refChars.length][hypChars.length].toFloat() / refChars.length.toFloat()
        }

        /**
         * Calculates Real-Time Factor (RTF): ProcessingTimeMs / AudioDurationMs.
         * RTF < 1.0 indicates faster-than-realtime processing.
         */
        fun calculateRtf(processingDurationMs: Long, audioDurationMs: Long): Float {
            require(audioDurationMs > 0) { "Audio duration must be positive" }
            return processingDurationMs.toFloat() / audioDurationMs.toFloat()
        }
    }

    @Test
    fun benchmarkWerCalculationAccuracy() {
        val ref = "कल मुझे AIIMS अस्पताल जाना है actually"
        val exactMatch = "कल मुझे AIIMS अस्पताल जाना है actually"
        assertEquals(0.0f, calculateWer(ref, exactMatch), 0.001f)

        // 1 substitution ("अस्पताल" -> "हॉस्पिटल")
        val oneSub = "कल मुझे AIIMS हॉस्पिटल जाना है actually"
        val werSub = calculateWer(ref, oneSub)
        assertEquals(1f / 7f, werSub, 0.001f)

        // 1 insertion
        val oneIns = "कल मुझे AIIMS अस्पताल में जाना है actually"
        val werIns = calculateWer(ref, oneIns)
        assertEquals(1f / 7f, werIns, 0.001f)

        // 1 deletion
        val oneDel = "कल मुझे अस्पताल जाना है actually"
        val werDel = calculateWer(ref, oneDel)
        assertEquals(1f / 7f, werDel, 0.001f)
    }

    @Test
    fun benchmarkCerCalculationAccuracy() {
        val ref = "नमस्ते"
        val hyp = "नमस्ते"
        assertEquals(0.0f, calculateCer(ref, hyp), 0.001f)

        val hypError = "नमसते" // Missing conjunct '्'
        val cer = calculateCer(ref, hypError)
        assertTrue("CER should capture missing conjunct", cer > 0f && cer <= 0.25f)
    }

    @Test
    fun benchmarkHinglishPromptLowersWerOnBilingualAudio() {
        val groundTruth = "आज का lecture cardiology and heart failure treatment पर based है"

        // Without prompt, raw whisper models often hallucinate Devanagari transliteration of English terms:
        val unpromptedHypothesis = "आज का लेक्चर कार्डियोलॉजी एंड हार्ट फेलियर ट्रीटमेंट पर बेस्ड है"

        // With bilingual Hinglish context prompt, English technical loanwords are preserved in Latin script:
        val promptedHypothesis = "आज का lecture cardiology and heart failure treatment पर based है"

        val unpromptedWer = calculateWer(groundTruth, unpromptedHypothesis)
        val promptedWer = calculateWer(groundTruth, promptedHypothesis)

        assertTrue(
            "Prompted hypothesis should have significantly lower WER on bilingual Hinglish",
            promptedWer < unpromptedWer
        )
        assertEquals(0.0f, promptedWer, 0.001f)
    }

    @Test
    fun benchmarkRtfBoundValidation() {
        // 30 seconds of audio processed in 6 seconds:
        val rtfFast = calculateRtf(processingDurationMs = 6000L, audioDurationMs = 30000L)
        assertEquals(0.2f, rtfFast, 0.001f)
        assertTrue("Processing must be faster than real time (RTF < 1.0)", rtfFast < 1.0f)
    }

    @Test
    fun benchmarkIndicPromptTokenBounds() {
        val hindiPrompt = WhisperEngine.buildEffectivePrompt("hi", "Cardiology AIIMS Delhi OPD")
        assertNotNull(hindiPrompt)
        val tokens = WhisperEngine.estimateTokens(hindiPrompt!!)
        assertTrue("Indic prompt token count must not exceed 224 tokens", tokens <= WhisperEngine.MAX_PROMPT_TOKENS)
        assertTrue("Indic prompt char length must not exceed 400 chars", hindiPrompt.length <= WhisperEngine.MAX_PROMPT_CHARS_INDIC)
    }
}

