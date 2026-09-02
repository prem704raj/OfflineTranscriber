package com.example.transcriber.ui.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transcriber.billing.Entitlement
import com.example.transcriber.export.model.ExportContentType
import com.example.transcriber.export.model.ExportFormat
import com.example.transcriber.export.model.ExportOptions
import com.example.transcriber.export.model.ExportTarget
import com.example.transcriber.export.model.ExportTextScale
import com.example.transcriber.export.model.PdfPageSize

@Composable
fun ExportOptionsSection(
    target: ExportTarget,
    options: ExportOptions,
    hasSpeakerData: Boolean,
    entitlement: Entitlement,
    onUpdateMetadata: (Boolean) -> Unit,
    onUpdateTimestamps: (Boolean) -> Unit,
    onUpdateSpeakerLabels: (Boolean) -> Unit,
    onUpdatePageSize: (PdfPageSize) -> Unit,
    onUpdateTextScale: (ExportTextScale) -> Unit,
    onUpdateQuizAnswers: (Boolean) -> Unit,
    onUpdateAskCitations: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPro = entitlement == Entitlement.PRO

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Export Options",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Include Metadata
                OptionToggleRow(
                    title = "Include Header & Metadata",
                    subtitle = "Title, date, duration and audio properties",
                    checked = options.includeMetadata,
                    onCheckedChange = onUpdateMetadata
                )

                // Include Timestamps (for Transcripts & Meetings)
                if (target.contentType == ExportContentType.TRANSCRIPT || target.contentType == ExportContentType.MEETING_PACK) {
                    OptionToggleRow(
                        title = "Include Timestamps",
                        subtitle = "Show [00:00] time codes alongside content",
                        checked = options.includeTimestamps,
                        onCheckedChange = onUpdateTimestamps
                    )
                }

                // Speaker Labels (for Transcripts)
                if (target.contentType == ExportContentType.TRANSCRIPT && hasSpeakerData) {
                    OptionToggleRow(
                        title = "Speaker Labels",
                        subtitle = "Distinguish who spoke when",
                        checked = options.includeSpeakerLabels,
                        onCheckedChange = onUpdateSpeakerLabels,
                        isProBadge = !isPro
                    )
                }

                // Quiz Answers (for Study Pack)
                if (target.contentType == ExportContentType.STUDY_PACK) {
                    OptionToggleRow(
                        title = "Include Quiz Answer Key",
                        subtitle = "Show correct answers & explanations",
                        checked = options.includeQuizAnswers,
                        onCheckedChange = onUpdateQuizAnswers
                    )
                }

                // Ask Citations (for Ask AI)
                if (target.contentType == ExportContentType.ASK_CONVERSATION) {
                    OptionToggleRow(
                        title = "Include Citations",
                        subtitle = "Show grounded transcript references",
                        checked = options.includeAskCitations,
                        onCheckedChange = onUpdateAskCitations
                    )
                }

                // PDF Specific controls
                if (options.format == ExportFormat.PDF) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Page Size",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = options.pageSize == PdfPageSize.A4,
                            onClick = { onUpdatePageSize(PdfPageSize.A4) },
                            label = { Text("A4 Standard") }
                        )
                        FilterChip(
                            selected = options.pageSize == PdfPageSize.LETTER,
                            onClick = { onUpdatePageSize(PdfPageSize.LETTER) },
                            label = { Text("US Letter") }
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Text Scale",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = options.textScale == ExportTextScale.COMPACT,
                            onClick = { onUpdateTextScale(ExportTextScale.COMPACT) },
                            label = { Text("Compact") }
                        )
                        FilterChip(
                            selected = options.textScale == ExportTextScale.STANDARD,
                            onClick = { onUpdateTextScale(ExportTextScale.STANDARD) },
                            label = { Text("Standard") }
                        )
                        FilterChip(
                            selected = options.textScale == ExportTextScale.LARGE,
                            onClick = { onUpdateTextScale(ExportTextScale.LARGE) },
                            label = { Text("Large") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isProBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isProBadge) {
                    Spacer(Modifier.size(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(9.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.size(2.dp))
                            Text(
                                "PRO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
