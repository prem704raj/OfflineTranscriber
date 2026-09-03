package app.offlinetranscriber.mobile.transcription

import java.io.File

data class PreparedPcmAudio(
    val file: File,
    val durationMs: Long,
    val totalSamples: Long,
    val sampleRate: Int = AudioProcessor.TARGET_SAMPLE_RATE,
    val channels: Int = 1
)
