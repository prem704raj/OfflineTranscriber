package com.example.transcriber.ui.speaker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.transcriber.speaker.model.SpeakerCountMode

@Composable
fun SpeakerCountDialog(
    onDismiss: () -> Unit,
    onConfirm: (SpeakerCountMode, Int?) -> Unit
) {
    val options = listOf(
        Pair(SpeakerCountMode.AUTO, "Auto-detect speakers (recommended)"),
        Pair(SpeakerCountMode.TWO, "2 Speakers (Interview, Podcast, 1-on-1)"),
        Pair(SpeakerCountMode.THREE, "3 Speakers"),
        Pair(SpeakerCountMode.FOUR, "4 Speakers"),
        Pair(SpeakerCountMode.FIVE, "5 Speakers"),
        Pair(SpeakerCountMode.SIX, "6 Speakers")
    )

    var selectedMode by remember { mutableStateOf(SpeakerCountMode.AUTO) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How many speakers?") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
            ) {
                Text(
                    "Choosing an exact count improves separation accuracy when known.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                options.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (mode == selectedMode),
                                onClick = { selectedMode = mode },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (mode == selectedMode),
                            onClick = null
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val count = when (selectedMode) {
                        SpeakerCountMode.AUTO -> null
                        SpeakerCountMode.TWO -> 2
                        SpeakerCountMode.THREE -> 3
                        SpeakerCountMode.FOUR -> 4
                        SpeakerCountMode.FIVE -> 5
                        SpeakerCountMode.SIX -> 6
                        SpeakerCountMode.CUSTOM -> null
                    }
                    onConfirm(selectedMode, count)
                }
            ) {
                Text("Start Analysis")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
