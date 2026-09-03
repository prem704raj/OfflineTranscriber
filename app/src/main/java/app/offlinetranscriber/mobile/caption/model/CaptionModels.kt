package app.offlinetranscriber.mobile.caption.model

import kotlinx.serialization.Serializable

@Serializable
enum class CaptionPreset {
    CLASSIC,
    CLEAN,
    BOLD,
    BOX,
    MINIMAL
}

@Serializable
enum class CaptionVerticalPosition {
    TOP,
    CENTER,
    BOTTOM
}

@Serializable
enum class CaptionHorizontalAlignment {
    START,
    CENTER,
    END
}

@Serializable
enum class CaptionTextSize {
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE
}

@Serializable
enum class CaptionExportResolution {
    ORIGINAL,
    P1080,
    P720
}

@Serializable
data class CaptionStyle(
    val preset: CaptionPreset = CaptionPreset.CLASSIC,
    val textSize: CaptionTextSize = CaptionTextSize.MEDIUM,
    val verticalPosition: CaptionVerticalPosition = CaptionVerticalPosition.BOTTOM,
    val horizontalAlignment: CaptionHorizontalAlignment = CaptionHorizontalAlignment.CENTER,
    val textColorArgb: Int = 0xFFFFFFFF.toInt(),
    val outlineColorArgb: Int = 0xFF000000.toInt(),
    val outlineWidthFactor: Float = 0.040f,
    val backgroundColorArgb: Int = 0xB3000000.toInt(),
    val backgroundEnabled: Boolean = false,
    val cornerRadiusFactor: Float = 0.18f,
    val shadowEnabled: Boolean = true,
    val safeAreaPercent: Float = 0.08f,
    val maxLines: Int = 2,
    val includeSpeakerLabel: Boolean = false,
    val speakerAccentEnabled: Boolean = false
)

@Serializable
data class CaptionCue(
    val segmentId: Long,
    val startUs: Long,
    val endUs: Long,
    val text: String,
    val speakerClusterId: Long? = null,
    val speakerLabel: String? = null,
    val speakerOrdinal: Int? = null
)

@Serializable
data class CaptionProjectSnapshot(
    val transcriptId: Long,
    val sourceVideoUri: String,
    val durationMs: Long,
    val cues: List<CaptionCue>
)

@Serializable
data class CaptionExportRequest(
    val transcriptId: Long,
    val sourceVideoUri: String,
    val style: CaptionStyle,
    val resolution: CaptionExportResolution,
    val outputFileName: String,
    val cues: List<CaptionCue> = emptyList()
)
