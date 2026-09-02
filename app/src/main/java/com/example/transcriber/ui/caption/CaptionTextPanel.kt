package com.example.transcriber.ui.caption

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.transcriber.caption.model.CaptionStyle
import com.example.transcriber.caption.model.CaptionTextSize

@Composable
fun CaptionTextPanel(
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
        // Text Size
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "TEXT SIZE",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CaptionTextSize.entries.forEach { size ->
                    val label = when (size) {
                        CaptionTextSize.SMALL -> "Small"
                        CaptionTextSize.MEDIUM -> "Medium"
                        CaptionTextSize.LARGE -> "Large"
                        CaptionTextSize.EXTRA_LARGE -> "XL"
                    }
                    FilterChip(
                        selected = style.textSize == size,
                        onClick = { onUpdateStyle(style.copy(textSize = size)) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Text Color
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "TEXT COLOR",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val colorOptions = listOf(
                    "White" to 0xFFFFFFFF.toInt(),
                    "Yellow" to 0xFFFFF176.toInt(),
                    "Black" to 0xFF000000.toInt()
                )

                colorOptions.forEach { (name, colorArgb) ->
                    val isSelected = style.textColorArgb == colorArgb
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(colorArgb))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable {
                                onUpdateStyle(style.copy(textColorArgb = colorArgb))
                            }
                    )
                }
            }
        }

        // Outline
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "OUTLINE",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val outlineOptions = listOf(
                    "Off" to 0f,
                    "Thin" to 0.025f,
                    "Medium" to 0.040f,
                    "Thick" to 0.060f
                )

                outlineOptions.forEach { (label, widthFactor) ->
                    FilterChip(
                        selected = style.outlineWidthFactor == widthFactor,
                        onClick = { onUpdateStyle(style.copy(outlineWidthFactor = widthFactor)) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Background Box Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Background Box",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Adds a dark translucent backdrop behind text",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = style.backgroundEnabled,
                onCheckedChange = { onUpdateStyle(style.copy(backgroundEnabled = it)) }
            )
        }
    }
}
