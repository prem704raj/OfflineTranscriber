package com.example.transcriber.ui.caption

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transcriber.caption.model.CaptionPreset
import com.example.transcriber.caption.model.CaptionStyle

@Composable
fun CaptionPresetPanel(
    selectedPreset: CaptionPreset,
    onSelectPreset: (CaptionPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "PRESETS",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CaptionPreset.entries) { preset ->
                PresetCard(
                    preset = preset,
                    isSelected = selectedPreset == preset,
                    onSelect = { onSelectPreset(preset) }
                )
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: CaptionPreset,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val title = when (preset) {
        CaptionPreset.CLASSIC -> "Classic"
        CaptionPreset.CLEAN -> "Clean"
        CaptionPreset.BOLD -> "Bold"
        CaptionPreset.BOX -> "Box"
        CaptionPreset.MINIMAL -> "Minimal"
    }

    val previewBg = when (preset) {
        CaptionPreset.CLEAN -> Color.Black.copy(alpha = 0.6f)
        CaptionPreset.BOX -> Color.Black.copy(alpha = 0.8f)
        else -> Color.Transparent
    }

    val previewWeight = when (preset) {
        CaptionPreset.BOLD -> FontWeight.ExtraBold
        CaptionPreset.MINIMAL -> FontWeight.Normal
        else -> FontWeight.Bold
    }

    Card(
        modifier = Modifier
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = previewBg,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(2.dp)
            ) {
                Text(
                    text = "Sample",
                    fontSize = 12.sp,
                    fontWeight = previewWeight,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}
