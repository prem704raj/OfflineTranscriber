package app.offlinetranscriber.mobile.performance

class ProgressThrottler(
    private val minIntervalMs: Long = 250L,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private var lastPercent = -1
    private var lastAt = 0L

    fun shouldEmit(percent: Int): Boolean {
        val safe = percent.coerceIn(0, 100)
        val now = timeProvider()

        if (
            safe != lastPercent &&
            (
                now - lastAt >= minIntervalMs ||
                safe == 0 ||
                safe == 100
            )
        ) {
            lastPercent = safe
            lastAt = now
            return true
        }

        return false
    }

    fun reset() {
        lastPercent = -1
        lastAt = 0L
    }
}
