package com.example.transcriber.caption.style

import com.example.transcriber.caption.model.CaptionHorizontalAlignment
import com.example.transcriber.caption.model.CaptionPreset
import com.example.transcriber.caption.model.CaptionStyle
import com.example.transcriber.caption.model.CaptionTextSize
import com.example.transcriber.caption.model.CaptionVerticalPosition

object CaptionPresetFactory {

    fun style(
        preset: CaptionPreset
    ): CaptionStyle =
        when (preset) {
            CaptionPreset.CLASSIC ->
                CaptionStyle(
                    preset = preset,
                    textSize = CaptionTextSize.MEDIUM,
                    verticalPosition = CaptionVerticalPosition.BOTTOM,
                    horizontalAlignment = CaptionHorizontalAlignment.CENTER,
                    textColorArgb = 0xFFFFFFFF.toInt(),
                    outlineColorArgb = 0xFF000000.toInt(),
                    outlineWidthFactor = 0.040f,
                    backgroundEnabled = false,
                    shadowEnabled = true,
                    cornerRadiusFactor = 0.18f,
                    safeAreaPercent = 0.08f,
                    maxLines = 2
                )

            CaptionPreset.CLEAN ->
                CaptionStyle(
                    preset = preset,
                    textSize = CaptionTextSize.MEDIUM,
                    verticalPosition = CaptionVerticalPosition.BOTTOM,
                    horizontalAlignment = CaptionHorizontalAlignment.CENTER,
                    textColorArgb = 0xFFFFFFFF.toInt(),
                    outlineColorArgb = 0xFF000000.toInt(),
                    outlineWidthFactor = 0f,
                    backgroundEnabled = true,
                    backgroundColorArgb = 0x99000000.toInt(),
                    shadowEnabled = false,
                    cornerRadiusFactor = 0.18f,
                    safeAreaPercent = 0.08f,
                    maxLines = 2
                )

            CaptionPreset.BOLD ->
                CaptionStyle(
                    preset = preset,
                    textSize = CaptionTextSize.LARGE,
                    verticalPosition = CaptionVerticalPosition.BOTTOM,
                    horizontalAlignment = CaptionHorizontalAlignment.CENTER,
                    textColorArgb = 0xFFFFFFFF.toInt(),
                    outlineColorArgb = 0xFF000000.toInt(),
                    outlineWidthFactor = 0.055f,
                    backgroundEnabled = false,
                    shadowEnabled = true,
                    cornerRadiusFactor = 0.18f,
                    safeAreaPercent = 0.08f,
                    maxLines = 2
                )

            CaptionPreset.BOX ->
                CaptionStyle(
                    preset = preset,
                    textSize = CaptionTextSize.MEDIUM,
                    verticalPosition = CaptionVerticalPosition.BOTTOM,
                    horizontalAlignment = CaptionHorizontalAlignment.CENTER,
                    textColorArgb = 0xFFFFFFFF.toInt(),
                    outlineColorArgb = 0xFF000000.toInt(),
                    outlineWidthFactor = 0f,
                    backgroundEnabled = true,
                    backgroundColorArgb = 0xCC000000.toInt(),
                    shadowEnabled = false,
                    cornerRadiusFactor = 0.24f,
                    safeAreaPercent = 0.08f,
                    maxLines = 2
                )

            CaptionPreset.MINIMAL ->
                CaptionStyle(
                    preset = preset,
                    textSize = CaptionTextSize.SMALL,
                    verticalPosition = CaptionVerticalPosition.BOTTOM,
                    horizontalAlignment = CaptionHorizontalAlignment.CENTER,
                    textColorArgb = 0xFFFFFFFF.toInt(),
                    outlineColorArgb = 0xFF000000.toInt(),
                    outlineWidthFactor = 0.028f,
                    backgroundEnabled = false,
                    shadowEnabled = true,
                    cornerRadiusFactor = 0.18f,
                    safeAreaPercent = 0.08f,
                    maxLines = 2
                )
        }
}
