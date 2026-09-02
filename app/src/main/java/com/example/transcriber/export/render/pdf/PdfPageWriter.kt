package com.example.transcriber.export.render.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.transcriber.export.model.ExportTextScale
import com.example.transcriber.export.model.PdfPageSize

class PdfPageWriter(
    private val pageSize: PdfPageSize,
    private val textScale: ExportTextScale
) {

    val pageWidth: Int = when (pageSize) {
        PdfPageSize.A4 -> 595
        PdfPageSize.LETTER -> 612
    }

    val pageHeight: Int = when (pageSize) {
        PdfPageSize.A4 -> 842
        PdfPageSize.LETTER -> 792
    }

    private val margin = 40f
    private val contentWidth = (pageWidth - 2 * margin).toInt()
    private val contentBottom = pageHeight - margin - 20f

    private val scaleMultiplier = when (textScale) {
        ExportTextScale.COMPACT -> 0.85f
        ExportTextScale.STANDARD -> 1.0f
        ExportTextScale.LARGE -> 1.2f
    }

    val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C1B1F")
        textSize = 10.5f * scaleMultiplier
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    val boldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C1B1F")
        textSize = 10.5f * scaleMultiplier
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        textSize = 18f * scaleMultiplier
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#475569")
        textSize = 12f * scaleMultiplier
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
    }

    val h1Paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        textSize = 14f * scaleMultiplier
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val h2Paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155")
        textSize = 12f * scaleMultiplier
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val metaLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748B")
        textSize = 9.5f * scaleMultiplier
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val metaValPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155")
        textSize = 9.5f * scaleMultiplier
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    val timeBadgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2563EB")
        textSize = 9.5f * scaleMultiplier
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    val speakerBadgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        textSize = 10f * scaleMultiplier
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E2E8F0")
        strokeWidth = 1f
    }

    val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 8.5f * scaleMultiplier
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private var document: PdfDocument? = null
    private var currentPage: PdfDocument.Page? = null
    private var currentCanvas: Canvas? = null
    private var currentY: Float = margin
    private var pageNumber: Int = 0

    fun start(pdfDoc: PdfDocument) {
        this.document = pdfDoc
        this.pageNumber = 0
        newPage()
    }

    fun finish() {
        currentPage?.let { page ->
            drawFooter(pageNumber)
            document?.finishPage(page)
        }
        currentPage = null
        currentCanvas = null
    }

    private fun newPage() {
        currentPage?.let { page ->
            drawFooter(pageNumber)
            document?.finishPage(page)
        }
        pageNumber++
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = document?.startPage(pageInfo) ?: return
        currentPage = page
        currentCanvas = page.canvas
        currentY = margin
    }

    private fun ensureSpace(neededHeight: Float) {
        if (currentY + neededHeight > contentBottom) {
            newPage()
        }
    }

    private fun drawFooter(pageNum: Int) {
        val canvas = currentCanvas ?: return
        val footerY = pageHeight - margin + 10f
        canvas.drawLine(margin, footerY - 10f, pageWidth - margin, footerY - 10f, linePaint)
        val text = "Offline Transcriber  •  Page $pageNum"
        val textWidth = footerPaint.measureText(text)
        canvas.drawText(text, (pageWidth - textWidth) / 2f, footerY, footerPaint)
    }

    fun drawTitle(title: String, subtitle: String?) {
        val layout = createStaticLayout(title, titlePaint, contentWidth)
        ensureSpace(layout.height + 30f)
        currentCanvas?.let { canvas ->
            canvas.save()
            canvas.translate(margin, currentY)
            layout.draw(canvas)
            canvas.restore()
        }
        currentY += layout.height + 6f

        if (subtitle != null) {
            val subLayout = createStaticLayout(subtitle, subtitlePaint, contentWidth)
            ensureSpace(subLayout.height + 10f)
            currentCanvas?.let { canvas ->
                canvas.save()
                canvas.translate(margin, currentY)
                subLayout.draw(canvas)
                canvas.restore()
            }
            currentY += subLayout.height + 12f
        } else {
            currentY += 10f
        }
    }

    fun drawMetadata(items: List<Pair<String, String>>) {
        if (items.isEmpty()) return
        val rowHeight = 16f * scaleMultiplier
        val totalHeight = items.size * rowHeight + 14f
        ensureSpace(totalHeight)

        currentCanvas?.let { canvas ->
            var y = currentY
            items.forEach { (label, value) ->
                val labelText = "$label: "
                canvas.drawText(labelText, margin, y + 10f, metaLabelPaint)
                val labelWidth = metaLabelPaint.measureText(labelText)
                canvas.drawText(value, margin + labelWidth, y + 10f, metaValPaint)
                y += rowHeight
            }
            canvas.drawLine(margin, y + 4f, pageWidth - margin, y + 4f, linePaint)
        }
        currentY += totalHeight
    }

    fun drawHeading(level: Int, text: String) {
        val paint = if (level == 1) h1Paint else h2Paint
        val layout = createStaticLayout(text, paint, contentWidth)
        val spacingBefore = if (level == 1) 16f else 10f
        val spacingAfter = if (level == 1) 8f else 4f
        ensureSpace(layout.height + spacingBefore + spacingAfter)

        currentY += spacingBefore
        currentCanvas?.let { canvas ->
            canvas.save()
            canvas.translate(margin, currentY)
            layout.draw(canvas)
            canvas.restore()
        }
        currentY += layout.height + spacingAfter
    }

    fun drawParagraph(text: String) {
        val layout = createStaticLayout(text, bodyPaint, contentWidth)
        ensureSpace(layout.height + 8f)

        currentCanvas?.let { canvas ->
            canvas.save()
            canvas.translate(margin, currentY)
            layout.draw(canvas)
            canvas.restore()
        }
        currentY += layout.height + 8f
    }

    fun drawTranscriptSegment(text: String, timestamp: String?, speaker: String?) {
        val prefix = buildString {
            if (timestamp != null) append("[$timestamp] ")
            if (speaker != null) append("$speaker: ")
        }

        val fullText = "$prefix$text"
        val layout = createStaticLayout(fullText, bodyPaint, contentWidth)
        ensureSpace(layout.height + 6f)

        currentCanvas?.let { canvas ->
            canvas.save()
            canvas.translate(margin, currentY)
            layout.draw(canvas)
            canvas.restore()
        }
        currentY += layout.height + 6f
    }

    fun drawBullet(text: String, timestamp: String?) {
        val timePrefix = if (timestamp != null) "[$timestamp] " else ""
        val bulletText = "• $timePrefix$text"
        val layout = createStaticLayout(bulletText, bodyPaint, contentWidth - 10)
        ensureSpace(layout.height + 4f)

        currentCanvas?.let { canvas ->
            canvas.save()
            canvas.translate(margin + 10f, currentY)
            layout.draw(canvas)
            canvas.restore()
        }
        currentY += layout.height + 4f
    }

    fun drawChecklist(checked: Boolean, text: String, detail: String?, timestamp: String?) {
        val mark = if (checked) "☑ " else "☐ "
        val time = if (timestamp != null) " [$timestamp]" else ""
        val detailStr = if (detail != null) " ($detail)" else ""
        val full = "$mark$text$detailStr$time"
        val layout = createStaticLayout(full, bodyPaint, contentWidth - 10)
        ensureSpace(layout.height + 4f)

        currentCanvas?.let { canvas ->
            canvas.save()
            canvas.translate(margin + 10f, currentY)
            layout.draw(canvas)
            canvas.restore()
        }
        currentY += layout.height + 4f
    }

    fun drawKeyValue(key: String, value: String) {
        val keyText = "$key: "
        val keyLayout = createStaticLayout(keyText, boldPaint, contentWidth)
        val valLayout = createStaticLayout(value, bodyPaint, contentWidth)
        val height = keyLayout.height + valLayout.height + 6f
        ensureSpace(height)

        currentCanvas?.let { canvas ->
            canvas.save()
            canvas.translate(margin, currentY)
            keyLayout.draw(canvas)
            canvas.restore()

            canvas.save()
            canvas.translate(margin, currentY + keyLayout.height)
            valLayout.draw(canvas)
            canvas.restore()
        }
        currentY += height
    }

    fun drawDivider() {
        ensureSpace(16f)
        currentCanvas?.drawLine(margin, currentY + 8f, pageWidth - margin, currentY + 8f, linePaint)
        currentY += 16f
    }

    fun drawSpacer() {
        ensureSpace(10f)
        currentY += 10f
    }

    @Suppress("DEPRECATION")
    private fun createStaticLayout(
        text: CharSequence,
        paint: TextPaint,
        width: Int
    ): StaticLayout {
        val safeWidth = width.coerceAtLeast(50)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, safeWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1.15f)
                .setIncludePad(false)
                .build()
        } else {
            StaticLayout(
                text,
                paint,
                safeWidth,
                Layout.Alignment.ALIGN_NORMAL,
                1.15f,
                2f,
                false
            )
        }
    }
}
