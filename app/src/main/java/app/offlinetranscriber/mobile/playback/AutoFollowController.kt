package app.offlinetranscriber.mobile.playback

class AutoFollowController(
    private val suppressionMs: Long = 4_000L,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private var suppressedUntil = 0L

    fun onUserScroll() {
        suppressedUntil = timeProvider() + suppressionMs
    }

    fun shouldAutoFollow(): Boolean =
        timeProvider() >= suppressedUntil

    fun forceResume() {
        suppressedUntil = 0L
    }
}
