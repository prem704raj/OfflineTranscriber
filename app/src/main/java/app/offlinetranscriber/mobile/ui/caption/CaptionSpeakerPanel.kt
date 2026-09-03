package app.offlinetranscriber.mobile.ui.caption

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.offlinetranscriber.mobile.caption.model.CaptionStyle

@Composable
fun CaptionSpeakerPanel(
    style: CaptionStyle,
    hasSpeakerData: Boolean,
    onUpdateStyle: (CaptionStyle) -> Unit,
    onManageSpeakers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!hasSpeakerData) {
            Text(
                text = "No speaker labels detected for this video yet. Detect speakers first to add speaker names and color accents to your captions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onManageSpeakers,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Detect Speakers")
            }
        } else {
            // Include Speaker Labels Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Include Speaker Labels",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Displays speaker name above each caption block",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = style.includeSpeakerLabel,
                    onCheckedChange = { onUpdateStyle(style.copy(includeSpeakerLabel = it)) }
                )
            }

            // Speaker Accent Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Speaker Color Accent",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Adds a unique color bar beside each speaker's label",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = style.speakerAccentEnabled,
                    enabled = style.includeSpeakerLabel,
                    onCheckedChange = { onUpdateStyle(style.copy(speakerAccentEnabled = it)) }
                )
            }

            Spacer(Modifier.height(4.dp))

            OutlinedButton(
                onClick = onManageSpeakers,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage Speaker Names")
            }
        }
    }
}
