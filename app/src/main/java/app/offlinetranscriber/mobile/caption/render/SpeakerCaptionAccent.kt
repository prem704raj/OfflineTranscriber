package app.offlinetranscriber.mobile.caption.render

object SpeakerCaptionAccent {

    private val colors = intArrayOf(
        0xFF64B5F6.toInt(),
        0xFF81C784.toInt(),
        0xFFFFB74D.toInt(),
        0xFFBA68C8.toInt(),
        0xFF4DD0E1.toInt(),
        0xFFF06292.toInt()
    )

    fun color(ordinal: Int?): Int =
        colors[(ordinal ?: 0).mod(colors.size)]
}
