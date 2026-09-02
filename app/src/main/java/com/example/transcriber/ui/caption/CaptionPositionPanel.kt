package com.example.transcriber.ui.caption

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.transcriber.caption.model.CaptionHorizontalAlignment
import com.example.transcriber.caption.model.CaptionStyle
import com.example.transcriber.caption.model.CaptionVerticalPosition
import kotlin.math.roundToInt

@Composable
fun CaptionPositionPanel(
    style: CaptionStyle,
    onUpdateStyle: (CaptionStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Vertical Position
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "VERTICAL POSITION",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CaptionVerticalPosition.entries.forEach { pos ->
                    val label = when (pos) {
                        CaptionVerticalPosition.TOP -> "Top"
                        CaptionVerticalPosition.CENTER -> "Center"
                        CaptionVerticalPosition.BOTTOM -> "Bottom"
                    }
                    FilterChip(
                        selected = style.verticalPosition == pos,
                        onClick = { onUpdateStyle(style.copy(verticalPosition = pos)) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Horizontal Alignment
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "ALIGNMENT",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CaptionHorizontalAlignment.entries.forEach { align ->
                    val label = when (align) {
                        CaptionHorizontalAlignment.START -> "Left"
                        CaptionHorizontalAlignment.CENTER -> "Center"
                        CaptionHorizontalAlignment.END -> "Right"
                    }
                    FilterChip(
                        selected = style.horizontalAlignment == align,
                        onClick = { onUpdateStyle(style.copy(horizontalAlignment = align)) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Safe Area Margin Slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SAFE AREA MARGIN",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${(style.safeAreaPercent * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Slider(
                value = style.safeAreaPercent,
                onValueChange = { onUpdateStyle(style.copy(safeAreaPercent = it)) },
                valueRange = 0.04f..0.16f,
                steps = 5
            )
        }
    }
}
