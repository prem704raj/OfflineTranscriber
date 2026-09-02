package com.example.transcriber.ui.speaker

import androidx.compose.foundation.clickable
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
import com.example.transcriber.data.model.SpeakerClusterEntity

@Composable
fun MergeSpeakerDialog(
    sourceCluster: SpeakerClusterEntity,
    allClusters: List<SpeakerClusterEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    val candidates = allClusters.filter { it.id != sourceCluster.id }
    var selectedTargetId by remember { mutableStateOf<Long?>(candidates.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge Speaker") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
            ) {
                Text(
                    "Merge \"${sourceCluster.customName}\" into another speaker. All turns and segment assignments will be combined.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))

                candidates.forEach { target ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (target.id == selectedTargetId),
                                onClick = { selectedTargetId = target.id },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (target.id == selectedTargetId),
                            onClick = null
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = target.customName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedTargetId?.let { targetId ->
                        onConfirm(sourceCluster.id, targetId)
                    }
                },
                enabled = selectedTargetId != null
            ) {
                Text("Merge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
