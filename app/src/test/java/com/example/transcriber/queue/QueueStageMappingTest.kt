package com.example.transcriber.queue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueStageMappingTest {

    @Test
    fun rawVideoJobInitializesInPreparingStage() {
        val job = TranscriptionJobEntity(
            id = 1L,
            inputUri = "content://media/external/video/media/101",
            sourceUri = "content://media/external/video/media/101",
            preparedInputUri = null,
            sourceType = TranscriptionSourceType.VIDEO.name,
            displayName = "Lecture Video",
            modelId = "base-q5",
            languageCode = "en",
            stage = TranscriptionJobStage.PREPARING.name
        )

        assertEquals(TranscriptionJobStage.PREPARING.name, job.stage)
        assertNull(job.preparedInputUri)
        assertEquals(TranscriptionJobStatus.QUEUED.name, job.status)
    }

    @Test
    fun audioJobInitializesInTranscribingStage() {
        val uri = "content://media/external/audio/media/202"
        val job = TranscriptionJobEntity(
            id = 2L,
            inputUri = uri,
            sourceUri = uri,
            preparedInputUri = uri,
            sourceType = TranscriptionSourceType.AUDIO.name,
            displayName = "Voice Memo",
            modelId = "base-q5",
            languageCode = "auto",
            stage = TranscriptionJobStage.TRANSCRIBING.name
        )

        assertEquals(TranscriptionJobStage.TRANSCRIBING.name, job.stage)
        assertEquals(uri, job.preparedInputUri)
    }
}
