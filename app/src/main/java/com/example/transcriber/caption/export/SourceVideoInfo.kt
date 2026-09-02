package com.example.transcriber.caption.export

data class SourceVideoInfo(
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val durationMs: Long,
    val hasAudio: Boolean
) {
    val orientedWidth: Int
        get() = if (rotationDegrees == 90 || rotationDegrees == 270) height else width

    val orientedHeight: Int
        get() = if (rotationDegrees == 90 || rotationDegrees == 270) width else height
}
