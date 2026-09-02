package com.example.transcriber.ui.export

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transcriber.billing.Entitlement
import com.example.transcriber.export.model.ExportFormat

@Composable
fun ExportFormatSelector(
    selectedFormat: ExportFormat,
    entitlement: Entitlement,
    onSelectFormat: (ExportFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Export Format",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FormatCard(
                format = ExportFormat.TXT,
                title = "TXT",
                desc = "Plain text",
                icon = Icons.AutoMirrored.Filled.TextSnippet,
                selected = selectedFormat == ExportFormat.TXT,
                isPro = false,
                userHasPro = entitlement == Entitlement.PRO,
                onClick = { onSelectFormat(ExportFormat.TXT) },
                modifier = Modifier.weight(1f)
            )

            FormatCard(
                format = ExportFormat.MARKDOWN,
                title = "Markdown",
                desc = ".md document",
                icon = Icons.Default.Description,
                selected = selectedFormat == ExportFormat.MARKDOWN,
                isPro = false,
                userHasPro = entitlement == Entitlement.PRO,
                onClick = { onSelectFormat(ExportFormat.MARKDOWN) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FormatCard(
                format = ExportFormat.PDF,
                title = "PDF",
                desc = "Formatted PDF",
                icon = Icons.Default.PictureAsPdf,
                selected = selectedFormat == ExportFormat.PDF,
                isPro = true,
                userHasPro = entitlement == Entitlement.PRO,
                onClick = { onSelectFormat(ExportFormat.PDF) },
                modifier = Modifier.weight(1f)
            )

            FormatCard(
                format = ExportFormat.DOCX,
                title = "DOCX",
                desc = "Word document",
                icon = Icons.Default.Description,
                selected = selectedFormat == ExportFormat.DOCX,
                isPro = true,
                userHasPro = entitlement == Entitlement.PRO,
                onClick = { onSelectFormat(ExportFormat.DOCX) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FormatCard(
    format: ExportFormat,
    title: String,
    desc: String,
    icon: ImageVector,
    selected: Boolean,
    isPro: Boolean,
    userHasPro: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor)
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )

                    if (isPro) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (userHasPro) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!userHasPro) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.size(3.dp))
                                }
                                Text(
                                    text = "PRO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (userHasPro) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
