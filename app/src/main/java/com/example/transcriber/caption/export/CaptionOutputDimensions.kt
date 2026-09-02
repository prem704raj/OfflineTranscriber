package com.example.transcriber.caption.export

import com.example.transcriber.caption.model.CaptionExportResolution
import kotlin.math.roundToInt

data class OutputDimensions(
    val width: Int,
    val height: Int
)

object CaptionOutputDimensions {

    fun calculate(
        sourceWidth: Int,
        sourceHeight: Int,
        resolution: CaptionExportResolution
    ): OutputDimensions {
        require(sourceWidth > 0 && sourceHeight > 0)

        if (resolution == CaptionExportResolution.ORIGINAL) {
            return OutputDimensions(
                even(sourceWidth),
                even(sourceHeight)
            )
        }

        val maxLongEdge = when (resolution) {
            CaptionExportResolution.P1080 -> 1920
            CaptionExportResolution.P720 -> 1280
            CaptionExportResolution.ORIGINAL -> Int.MAX_VALUE
        }

        val maxShortEdge = when (resolution) {
            CaptionExportResolution.P1080 -> 1080
            CaptionExportResolution.P720 -> 720
            CaptionExportResolution.ORIGINAL -> Int.MAX_VALUE
        }

        val longEdge = maxOf(sourceWidth, sourceHeight)
        val shortEdge = minOf(sourceWidth, sourceHeight)

        if (longEdge <= maxLongEdge && shortEdge <= maxShortEdge) {
            return OutputDimensions(
                even(sourceWidth),
                even(sourceHeight)
            )
        }

        val scaleLong = maxLongEdge.toFloat() / longEdge.toFloat()
        val scaleShort = maxShortEdge.toFloat() / shortEdge.toFloat()
        val scale = minOf(scaleLong, scaleShort, 1f)

        return OutputDimensions(
            width = even((sourceWidth * scale).roundToInt()),
            height = even((sourceHeight * scale).roundToInt())
        )
    }

    private fun even(value: Int): Int =
        (value.coerceAtLeast(2) / 2) * 2
}
