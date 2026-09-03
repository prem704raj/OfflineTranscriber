package app.offlinetranscriber.mobile.ui.caption

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.offlinetranscriber.mobile.billing.Entitlement
import app.offlinetranscriber.mobile.billing.ProFeature
import app.offlinetranscriber.mobile.caption.model.CaptionExportResolution
import app.offlinetranscriber.mobile.caption.model.CaptionStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptionExportSheet(
    style: CaptionStyle,
    resolution: CaptionExportResolution,
    cuesCount: Int,
    durationMs: Long,
    entitlement: Entitlement,
    onSelectResolution: (CaptionExportResolution) -> Unit,
    onStartExport: () -> Unit,
    onOpenPaywall: (ProFeature) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isPro = entitlement == Entitlement.PRO

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Export Captioned Video",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            // Quality Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "VIDEO QUALITY",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val resolutions = listOf(
                        CaptionExportResolution.ORIGINAL to "Original",
                        CaptionExportResolution.P1080 to "1080p",
                        CaptionExportResolution.P720 to "720p"
                    )

                    resolutions.forEach { (res, label) ->
                        FilterChip(
                            selected = resolution == res,
                            onClick = { onSelectResolution(res) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val durationSec = durationMs / 1000
                    val minutes = durationSec / 60
                    val seconds = durationSec % 60
                    val durationText = String.format("%d:%02d", minutes, seconds)

                    val estimatedMb = when (resolution) {
                        CaptionExportResolution.P720 -> (durationSec * 5L / 8L).coerceAtLeast(2L)
                        CaptionExportResolution.P1080 -> (durationSec * 10L / 8L).coerceAtLeast(3L)
                        CaptionExportResolution.ORIGINAL -> (durationSec * 12L / 8L).coerceAtLeast(4L)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Style Preset", style = MaterialTheme.typography.bodyMedium)
                        Text(style.preset.name.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speaker Labels", style = MaterialTheme.typography.bodyMedium)
                        Text(if (style.includeSpeakerLabel) "Included" else "Off", fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Captions", style = MaterialTheme.typography.bodyMedium)
                        Text("$cuesCount segments", fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Duration", style = MaterialTheme.typography.bodyMedium)
                        Text(durationText, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Estimated File Size", style = MaterialTheme.typography.bodyMedium)
                        Text("~$estimatedMb MB", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Text(
                text = "Captions will be permanently rendered directly into the video frames.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // CTA Button
            if (isPro) {
                Button(
                    onClick = {
                        onDismiss()
                        onStartExport()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Export Captioned Video", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        onDismiss()
                        onOpenPaywall(ProFeature.BURNED_IN_CAPTIONS)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Unlock Video Export (Pro)", fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "Processed 100% locally on your device.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
