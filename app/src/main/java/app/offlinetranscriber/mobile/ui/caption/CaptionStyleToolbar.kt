package app.offlinetranscriber.mobile.ui.caption

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class CaptionToolPanel {
    NONE,
    PRESETS,
    TEXT,
    POSITION,
    SPEAKERS,
    MORE
}

@Composable
fun CaptionStyleToolbar(
    selectedPanel: CaptionToolPanel,
    hasSpeakerData: Boolean,
    onSelectPanel: (CaptionToolPanel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = selectedPanel == CaptionToolPanel.PRESETS,
            onClick = {
                onSelectPanel(
                    if (selectedPanel == CaptionToolPanel.PRESETS) CaptionToolPanel.NONE else CaptionToolPanel.PRESETS
                )
            },
            label = { Text("Styles") },
            leadingIcon = { Icon(Icons.Default.FormatPaint, contentDescription = null) },
            colors = FilterChipDefaults.filterChipColors()
        )

        FilterChip(
            selected = selectedPanel == CaptionToolPanel.TEXT,
            onClick = {
                onSelectPanel(
                    if (selectedPanel == CaptionToolPanel.TEXT) CaptionToolPanel.NONE else CaptionToolPanel.TEXT
                )
            },
            label = { Text("Text") },
            leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null) },
            colors = FilterChipDefaults.filterChipColors()
        )

        FilterChip(
            selected = selectedPanel == CaptionToolPanel.POSITION,
            onClick = {
                onSelectPanel(
                    if (selectedPanel == CaptionToolPanel.POSITION) CaptionToolPanel.NONE else CaptionToolPanel.POSITION
                )
            },
            label = { Text("Position") },
            leadingIcon = { Icon(Icons.Default.FormatAlignLeft, contentDescription = null) },
            colors = FilterChipDefaults.filterChipColors()
        )

        FilterChip(
            selected = selectedPanel == CaptionToolPanel.SPEAKERS,
            onClick = {
                onSelectPanel(
                    if (selectedPanel == CaptionToolPanel.SPEAKERS) CaptionToolPanel.NONE else CaptionToolPanel.SPEAKERS
                )
            },
            label = { Text("Speakers") },
            leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null) },
            colors = FilterChipDefaults.filterChipColors()
        )

        FilterChip(
            selected = selectedPanel == CaptionToolPanel.MORE,
            onClick = {
                onSelectPanel(
                    if (selectedPanel == CaptionToolPanel.MORE) CaptionToolPanel.NONE else CaptionToolPanel.MORE
                )
            },
            label = { Text("More") },
            leadingIcon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
            colors = FilterChipDefaults.filterChipColors()
        )
    }
}
