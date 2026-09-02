package com.example.transcriber.playback

import androidx.media3.common.C
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerPositionTicker(
    private val scope: CoroutineScope,
    private val player: Player,
    private val intervalMs: Long = 250L,
    private val onTick: (Long, Long) -> Unit
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        publish()

        job = scope.launch {
            while (isActive && player.isPlaying) {
                delay(intervalMs)
                publish()
            }
            job = null
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        publish()
    }

    fun publishNow() = publish()

    private fun publish() {
        val rawDuration = player.duration
        val duration =
            if (rawDuration == C.TIME_UNSET || rawDuration < 0L) 0L
            else rawDuration

        onTick(
            player.currentPosition.coerceAtLeast(0L),
            duration
        )
    }
}
