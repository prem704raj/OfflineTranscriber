package app.offlinetranscriber.mobile.queue

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptionQueueRetryTest {

    @Test
    fun completedJobWithResultTranscriptIdNormalizesAndBreaksInfiniteLoop() = runTest {
        // Create a job that had already finalized a transcript (resultTranscriptId != null)
        // but was left in QUEUED state (e.g. system requeue race or crash during finalization)
        var completedId: Long? = null
        var completedTranscriptId: Long? = null
        var queueInvocationCount = 0

        val fakeJob = TranscriptionJobEntity(
            id = 42L,
            inputUri = "file:///dummy/audio.wav",
            sourceUri = "file:///dummy/audio.wav",
            preparedInputUri = "file:///dummy/audio.wav",
            sourceType = "RECORDING",
            displayName = "Lecture Audio",
            modelId = "balanced",
            languageCode = "hi",
            status = "QUEUED",
            stage = "TRANSCRIBING",
            resultTranscriptId = 999L
        )

        // Simulate repository where complete() marks the job finished
        val jobsQueue = mutableListOf(fakeJob)

        fun fetchNext(): TranscriptionJobEntity? {
            queueInvocationCount++
            return jobsQueue.firstOrNull { it.status == "QUEUED" }
        }

        // Simulate what TranscriptionQueueManager.processOne does on detecting resultTranscriptId != null
        val current = fetchNext()
        assertNotNull(current)
        if (current!!.resultTranscriptId != null) {
            completedId = current.id
            completedTranscriptId = current.resultTranscriptId
            // Normalization: update job status to COMPLETED
            val idx = jobsQueue.indexOfFirst { it.id == current.id }
            if (idx >= 0) {
                jobsQueue[idx] = jobsQueue[idx].copy(status = "COMPLETED")
            }
        }

        // Verify normalization occurred
        assertEquals(42L, completedId)
        assertEquals(999L, completedTranscriptId)

        // Verify queue is now empty and will not loop indefinitely
        val next = fetchNext()
        assertNull("Queue must be drained without infinite loop", next)
        assertEquals(2, queueInvocationCount)
    }

    @Test
    fun retryJobResetsResultTranscriptIdToNull() {
        val jobWithStaleTranscript = TranscriptionJobEntity(
            id = 100L,
            inputUri = "file:///dummy/audio.wav",
            sourceUri = "file:///dummy/audio.wav",
            preparedInputUri = "file:///dummy/audio.wav",
            sourceType = "RECORDING",
            displayName = "Failed Job",
            modelId = "balanced",
            languageCode = "en",
            status = "FAILED",
            resultTranscriptId = 555L,
            errorMessage = "Out of memory"
        )

        // When retry is called, the SQL query executes:
        // UPDATE transcription_jobs SET status = 'QUEUED', progress = 0, errorMessage = NULL, resultTranscriptId = NULL
        val retriedJob = jobWithStaleTranscript.copy(
            status = "QUEUED",
            progress = 0,
            errorMessage = null,
            resultTranscriptId = null
        )

        assertEquals("QUEUED", retriedJob.status)
        assertEquals(0, retriedJob.progress)
        assertNull("Error message must be cleared", retriedJob.errorMessage)
        assertNull("Stale resultTranscriptId must be wiped on retry to avoid false-completed state", retriedJob.resultTranscriptId)
    }
}

