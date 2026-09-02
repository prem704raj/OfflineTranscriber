package com.example.transcriber.caption.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.annotation.OptIn
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import com.example.transcriber.caption.model.CaptionCue
import com.example.transcriber.caption.model.CaptionHorizontalAlignment
import com.example.transcriber.caption.model.CaptionPreset
import com.example.transcriber.caption.model.CaptionStyle
import com.example.transcriber.caption.model.CaptionTextSize
import com.example.transcriber.caption.model.CaptionVerticalPosition
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
class DynamicCaptionCanvasOverlay(
    cues: List<CaptionCue>,
    private val styleProvider: CaptionStyleProvider
) : BitmapOverlay() {

    private val index = CaptionCueIndex(cues)

    private var videoWidth = 0
    private var videoHeight = 0

    private var drawingBitmap: Bitmap? = null
    private var drawingCanvas: Canvas? = null
    private val emptyBitmap by lazy {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    private val textPaint = TextPaint(
        Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG
    )

    private val strokePaint = TextPaint(
        Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG
    )

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private data class LayoutKey(
        val segmentId: Long,
        val width: Int,
        val height: Int,
        val revision: Long,
        val speakerLabel: String?
    )

    private var cachedKey: LayoutKey? = null
    private var cachedFill: StaticLayout? = null
    private var cachedStroke: StaticLayout? = null
    private var cachedText: SpannableString? = null

    override fun configure(videoSize: Size) {
        super.configure(videoSize)
        videoWidth = videoSize.width
        videoHeight = videoSize.height

        if (videoWidth > 0 && videoHeight > 0) {
            val bmp = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.ARGB_8888)
            drawingBitmap = bmp
            drawingCanvas = Canvas(bmp)
        }

        cachedKey = null
        cachedFill = null
        cachedStroke = null
        cachedText = null
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val canvas = drawingCanvas ?: return emptyBitmap
        val bitmap = drawingBitmap ?: return emptyBitmap

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        if (videoWidth <= 0 || videoHeight <= 0) return bitmap

        val cue = index.active(presentationTimeUs) ?: return bitmap

        val state = styleProvider.current()
        val currentStyle = state.style

        val safe = currentStyle.safeAreaPercent.coerceIn(0.03f, 0.18f)
        val horizontalMargin = (videoWidth * safe).roundToInt()
        val maxTextWidth = (videoWidth - horizontalMargin * 2).coerceAtLeast(videoWidth / 3)

        val speakerToInclude = if (currentStyle.includeSpeakerLabel) cue.speakerLabel else null
        val key = LayoutKey(
            segmentId = cue.segmentId,
            width = maxTextWidth,
            height = videoHeight,
            revision = state.revision,
            speakerLabel = speakerToInclude
        )

        if (key != cachedKey) {
            buildLayouts(
                cue = cue,
                style = currentStyle,
                maxTextWidth = maxTextWidth
            )
            cachedKey = key
        }

        val fill = cachedFill ?: return bitmap
        val stroke = cachedStroke

        val boxPaddingX = (textPaint.textSize * 0.42f).coerceAtLeast(8f)
        val boxPaddingY = (textPaint.textSize * 0.24f).coerceAtLeast(5f)

        val boxWidth = fill.width + boxPaddingX * 2f
        val boxHeight = fill.height + boxPaddingY * 2f

        val left = when (currentStyle.horizontalAlignment) {
            CaptionHorizontalAlignment.START -> horizontalMargin.toFloat()
            CaptionHorizontalAlignment.CENTER -> (videoWidth - boxWidth) / 2f
            CaptionHorizontalAlignment.END -> videoWidth - horizontalMargin - boxWidth
        }

        val safeVertical = videoHeight * safe
        val top = when (currentStyle.verticalPosition) {
            CaptionVerticalPosition.TOP -> safeVertical
            CaptionVerticalPosition.CENTER -> (videoHeight - boxHeight) / 2f
            CaptionVerticalPosition.BOTTOM -> videoHeight - safeVertical - boxHeight
        }.coerceIn(0f, (videoHeight - boxHeight).coerceAtLeast(0f))

        if (currentStyle.backgroundEnabled) {
            backgroundPaint.color = currentStyle.backgroundColorArgb
            val radius = (textPaint.textSize * currentStyle.cornerRadiusFactor).coerceIn(0f, textPaint.textSize)
            canvas.drawRoundRect(
                left,
                top,
                left + boxWidth,
                top + boxHeight,
                radius,
                radius,
                backgroundPaint
            )
        }

        if (currentStyle.speakerAccentEnabled && currentStyle.includeSpeakerLabel && !cue.speakerLabel.isNullOrBlank()) {
            accentPaint.color = SpeakerCaptionAccent.color(cue.speakerOrdinal)
            val accentWidth = (textPaint.textSize * 0.10f).coerceAtLeast(4f)
            canvas.drawRoundRect(
                left,
                top,
                left + accentWidth,
                top + boxHeight,
                accentWidth,
                accentWidth,
                accentPaint
            )
        }

        canvas.save()
        canvas.translate(left + boxPaddingX, top + boxPaddingY)
        stroke?.draw(canvas)
        fill.draw(canvas)
        canvas.restore()

        return bitmap
    }

    private fun buildLayouts(
        cue: CaptionCue,
        style: CaptionStyle,
        maxTextWidth: Int
    ) {
        val size = fontSize(videoHeight, style.textSize)

        val typeface = when (style.preset) {
            CaptionPreset.BOLD -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            else -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val text = buildCaptionText(cue, style)

        textPaint.apply {
            color = style.textColorArgb
            textSize = size
            this.typeface = typeface
            this.style = Paint.Style.FILL
            clearShadowLayer()

            if (style.shadowEnabled) {
                setShadowLayer(
                    size * 0.08f,
                    0f,
                    size * 0.06f,
                    0xB3000000.toInt()
                )
            }
        }

        strokePaint.apply {
            color = style.outlineColorArgb
            textSize = size
            this.typeface = typeface
            this.style = Paint.Style.STROKE
            strokeWidth = (size * style.outlineWidthFactor).coerceAtLeast(0f)
            strokeJoin = Paint.Join.ROUND
            strokeMiter = 10f
            clearShadowLayer()
        }

        val alignment = when (style.horizontalAlignment) {
            CaptionHorizontalAlignment.START -> Layout.Alignment.ALIGN_NORMAL
            CaptionHorizontalAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
            CaptionHorizontalAlignment.END -> Layout.Alignment.ALIGN_OPPOSITE
        }

        cachedText = text
        cachedFill = createLayout(
            text = text,
            paint = textPaint,
            width = maxTextWidth,
            alignment = alignment,
            maxLines = style.maxLines
        )

        cachedStroke = if (style.outlineWidthFactor > 0f) {
            createLayout(
                text = text,
                paint = strokePaint,
                width = maxTextWidth,
                alignment = alignment,
                maxLines = style.maxLines
            )
        } else {
            null
        }
    }

    private fun buildCaptionText(
        cue: CaptionCue,
        style: CaptionStyle
    ): SpannableString {
        val body = cue.text
            .replace(Regex("""\s+"""), " ")
            .trim()

        val speaker = if (style.includeSpeakerLabel) {
            cue.speakerLabel?.takeIf { it.isNotBlank() }
        } else {
            null
        }

        val finalText = if (speaker != null) {
            "$speaker\n$body"
        } else {
            body
        }

        val span = SpannableString(finalText)
        if (speaker != null) {
            span.setSpan(
                RelativeSizeSpan(0.72f),
                0,
                speaker.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            span.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                speaker.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return span
    }

    private fun createLayout(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment,
        maxLines: Int
    ): StaticLayout =
        StaticLayout.Builder.obtain(
            text,
            0,
            text.length,
            paint,
            width
        )
            .setAlignment(alignment)
            .setIncludePad(false)
            .setLineSpacing(0f, 1.04f)
            .setMaxLines(maxLines.coerceIn(1, 4))
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()

    private fun fontSize(
        height: Int,
        value: CaptionTextSize
    ): Float {
        val factor = when (value) {
            CaptionTextSize.SMALL -> 0.038f
            CaptionTextSize.MEDIUM -> 0.048f
            CaptionTextSize.LARGE -> 0.060f
            CaptionTextSize.EXTRA_LARGE -> 0.074f
        }

        return (height * factor).coerceIn(24f, 140f)
    }
}
