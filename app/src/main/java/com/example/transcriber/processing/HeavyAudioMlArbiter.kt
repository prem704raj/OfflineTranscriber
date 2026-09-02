package com.example.transcriber.processing

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class HeavyProcessingOwner {
    WHISPER_TRANSCRIPTION,
    SPEAKER_DIARIZATION,
    VIDEO_EXPORT,
    BACKUP_RESTORE
}

typealias HeavyMlTaskType = HeavyProcessingOwner

object HeavyProcessingArbiter {
    private val mutex = Mutex()

    @Volatile
    var currentOwner: HeavyProcessingOwner? = null
        private set

    val currentTaskType: HeavyProcessingOwner?
        get() = currentOwner

    suspend fun <T> withLease(
        owner: HeavyProcessingOwner,
        block: suspend () -> T
    ): T {
        return mutex.withLock {
            currentOwner = owner
            try {
                block()
            } finally {
                currentOwner = null
            }
        }
    }

    fun isBusy(): Boolean = mutex.isLocked
}

typealias HeavyAudioMlArbiter = HeavyProcessingArbiter
