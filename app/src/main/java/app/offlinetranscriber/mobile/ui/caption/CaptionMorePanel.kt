package app.offlinetranscriber.mobile.ui.caption

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.offlinetranscriber.mobile.caption.model.CaptionPreset
import app.offlinetranscriber.mobile.caption.model.CaptionStyle
import app.offlinetranscriber.mobile.caption.style.CaptionPresetFactory

@Composable
fun CaptionMorePanel(
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
        // Drop Shadow Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Text Shadow",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Improves readability over bright video backgrounds",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = style.shadowEnabled,
                onCheckedChange = { onUpdateStyle(style.copy(shadowEnabled = it)) }
            )
        }

        // Max Lines
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "MAX LINES",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 2, 3).forEach { lines ->
                    FilterChip(
                        selected = style.maxLines == lines,
                        onClick = { onUpdateStyle(style.copy(maxLines = lines)) },
                        label = { Text("$lines Line${if (lines > 1) "s" else ""}") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        OutlinedButton(
            onClick = { onUpdateStyle(CaptionPresetFactory.style(CaptionPreset.CLASSIC)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset to Default Style")
        }
    }
}
