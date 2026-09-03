package app.offlinetranscriber.mobile.export.render.docx

import app.offlinetranscriber.mobile.export.document.ExportBlock
import app.offlinetranscriber.mobile.export.document.ExportDocument
import app.offlinetranscriber.mobile.export.format.ExportTimestampFormatter
import app.offlinetranscriber.mobile.export.model.ExportOptions

object DocxPackageParts {

    val contentTypesXml: String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
          <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
        </Types>
    """.trimIndent()

    val packageRelsXml: String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
        </Relationships>
    """.trimIndent()

    val documentRelsXml: String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    val stylesXml: String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
          <w:docDefaults>
            <w:rPrDefault>
              <w:rPr>
                <w:rFonts w:ascii="Segoe UI" w:hAnsi="Segoe UI" w:cs="Segoe UI"/>
                <w:sz w:val="22"/>
                <w:color w:val="1C1B1F"/>
              </w:rPr>
            </w:rPrDefault>
          </w:docDefaults>
          <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
            <w:name w:val="Normal"/>
            <w:pPr>
              <w:spacing w:after="160" w:line="260" w:lineRule="auto"/>
            </w:pPr>
          </w:style>
          <w:style w:type="paragraph" w:styleId="Title">
            <w:name w:val="Title"/>
            <w:pPr>
              <w:spacing w:before="240" w:after="120"/>
            </w:pPr>
            <w:rPr>
              <w:b/>
              <w:sz w:val="36"/>
              <w:color w:val="0F172A"/>
            </w:rPr>
          </w:style>
          <w:style w:type="paragraph" w:styleId="Subtitle">
            <w:name w:val="Subtitle"/>
            <w:pPr>
              <w:spacing w:before="0" w:after="200"/>
            </w:pPr>
            <w:rPr>
              <w:i/>
              <w:sz w:val="24"/>
              <w:color w:val="475569"/>
            </w:rPr>
          </w:style>
          <w:style w:type="paragraph" w:styleId="Heading1">
            <w:name w:val="heading 1"/>
            <w:pPr>
              <w:spacing w:before="320" w:after="140"/>
            </w:pPr>
            <w:rPr>
              <w:b/>
              <w:sz w:val="28"/>
              <w:color w:val="0F172A"/>
            </w:rPr>
          </w:style>
          <w:style w:type="paragraph" w:styleId="Heading2">
            <w:name w:val="heading 2"/>
            <w:pPr>
              <w:spacing w:before="240" w:after="100"/>
            </w:pPr>
            <w:rPr>
              <w:b/>
              <w:sz w:val="24"/>
              <w:color w:val="334155"/>
            </w:rPr>
          </w:style>
        </w:styles>
    """.trimIndent()

    fun buildDocumentXml(document: ExportDocument, options: ExportOptions): String {
        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
            append("<w:body>")

            // Title
            append("<w:p><w:pPr><w:pStyle w:val=\"Title\"/></w:pPr>")
            append("<w:r><w:t>${XmlEscaper.escape(document.title)}</w:t></w:r></w:p>")

            // Subtitle
            if (document.subtitle != null) {
                append("<w:p><w:pPr><w:pStyle w:val=\"Subtitle\"/></w:pPr>")
                append("<w:r><w:t>${XmlEscaper.escape(document.subtitle)}</w:t></w:r></w:p>")
            }

            // Metadata
            if (document.metadata.isNotEmpty()) {
                document.metadata.forEach { item ->
                    append("<w:p><w:pPr><w:spacing w:after=\"60\"/></w:pPr>")
                    append("<w:r><w:rPr><w:b/><w:sz w:val=\"18\"/><w:color w:val=\"64748B\"/></w:rPr><w:t>${XmlEscaper.escape(item.label)}: </w:t></w:r>")
                    append("<w:r><w:rPr><w:sz w:val=\"18\"/><w:color w:val=\"334155\"/></w:rPr><w:t>${XmlEscaper.escape(item.value)}</w:t></w:r></w:p>")
                }
                append("<w:p><w:pPr><w:pBdr><w:bottom w:val=\"single\" w:sz=\"6\" w:space=\"4\" w:color=\"E2E8F0\"/></w:pBdr><w:spacing w:after=\"180\"/></w:pPr></w:p>")
            }

            // Blocks
            document.blocks.forEach { block ->
                when (block) {
                    is ExportBlock.Heading -> {
                        val style = if (block.level == 1) "Heading1" else "Heading2"
                        append("<w:p><w:pPr><w:pStyle w:val=\"$style\"/></w:pPr>")
                        append("<w:r><w:t>${XmlEscaper.escape(block.text)}</w:t></w:r></w:p>")
                    }
                    is ExportBlock.Paragraph -> {
                        append("<w:p><w:r><w:t xml:space=\"preserve\">${XmlEscaper.escape(block.text)}</w:t></w:r></w:p>")
                    }
                    is ExportBlock.TranscriptSegment -> {
                        append("<w:p><w:pPr><w:spacing w:after=\"100\"/></w:pPr>")
                        if (block.timestampMs != null) {
                            val timeStr = "[${ExportTimestampFormatter.format(block.timestampMs)}] "
                            append("<w:r><w:rPr><w:b/><w:color w:val=\"2563EB\"/><w:rFonts w:ascii=\"Courier New\" w:hAnsi=\"Courier New\"/></w:rPr><w:t xml:space=\"preserve\">${XmlEscaper.escape(timeStr)}</w:t></w:r>")
                        }
                        if (block.speakerLabel != null) {
                            append("<w:r><w:rPr><w:b/><w:color w:val=\"0F172A\"/></w:rPr><w:t xml:space=\"preserve\">${XmlEscaper.escape(block.speakerLabel)}: </w:t></w:r>")
                        }
                        append("<w:r><w:t xml:space=\"preserve\">${XmlEscaper.escape(block.text)}</w:t></w:r></w:p>")
                    }
                    is ExportBlock.Bullet -> {
                        append("<w:p><w:pPr><w:ind w:left=\"360\"/><w:spacing w:after=\"80\"/></w:pPr>")
                        append("<w:r><w:t xml:space=\"preserve\">• </w:t></w:r>")
                        if (block.timestampMs != null) {
                            val timeStr = "[${ExportTimestampFormatter.format(block.timestampMs)}] "
                            append("<w:r><w:rPr><w:b/><w:color w:val=\"2563EB\"/><w:rFonts w:ascii=\"Courier New\" w:hAnsi=\"Courier New\"/></w:rPr><w:t xml:space=\"preserve\">${XmlEscaper.escape(timeStr)}</w:t></w:r>")
                        }
                        append("<w:r><w:t xml:space=\"preserve\">${XmlEscaper.escape(block.text)}</w:t></w:r></w:p>")
                    }
                    is ExportBlock.Checklist -> {
                        append("<w:p><w:pPr><w:ind w:left=\"360\"/><w:spacing w:after=\"80\"/></w:pPr>")
                        val mark = if (block.checked) "☑ " else "☐ "
                        append("<w:r><w:rPr><w:b/></w:rPr><w:t xml:space=\"preserve\">$mark</w:t></w:r>")
                        append("<w:r><w:t xml:space=\"preserve\">${XmlEscaper.escape(block.text)}</w:t></w:r>")
                        if (block.detail != null) {
                            append("<w:r><w:rPr><w:i/><w:color w:val=\"64748B\"/></w:rPr><w:t xml:space=\"preserve\"> (${XmlEscaper.escape(block.detail)})</w:t></w:r>")
                        }
                        if (block.timestampMs != null) {
                            val timeStr = " [${ExportTimestampFormatter.format(block.timestampMs)}]"
                            append("<w:r><w:rPr><w:color w:val=\"2563EB\"/></w:rPr><w:t xml:space=\"preserve\">${XmlEscaper.escape(timeStr)}</w:t></w:r>")
                        }
                        append("</w:p>")
                    }
                    is ExportBlock.KeyValue -> {
                        append("<w:p><w:pPr><w:spacing w:after=\"80\"/></w:pPr>")
                        append("<w:r><w:rPr><w:b/></w:rPr><w:t xml:space=\"preserve\">${XmlEscaper.escape(block.key)}: </w:t></w:r>")
                        append("<w:r><w:t xml:space=\"preserve\">${XmlEscaper.escape(block.value)}</w:t></w:r></w:p>")
                    }
                    is ExportBlock.Divider -> {
                        append("<w:p><w:pPr><w:pBdr><w:bottom w:val=\"single\" w:sz=\"6\" w:space=\"8\" w:color=\"E2E8F0\"/></w:pBdr><w:spacing w:before=\"140\" w:after=\"140\"/></w:pPr></w:p>")
                    }
                    is ExportBlock.Spacer -> {
                        append("<w:p><w:pPr><w:spacing w:after=\"140\"/></w:pPr></w:p>")
                    }
                }
            }

            append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/><w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\"/></w:sectPr>")
            append("</w:body></w:document>")
        }
    }
}
