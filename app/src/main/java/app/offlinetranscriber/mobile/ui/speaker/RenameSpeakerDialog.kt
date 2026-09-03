package app.offlinetranscriber.mobile.ui.speaker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.offlinetranscriber.mobile.data.model.SpeakerClusterEntity

@Composable
fun RenameSpeakerDialog(
    cluster: SpeakerClusterEntity,
    onDismiss: () -> Unit,
    onConfirm: (Long, String) -> Unit
) {
    var name by remember { mutableStateOf(cluster.customName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Speaker") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Enter a custom name for Speaker ${cluster.speakerIndex + 1}:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text("e.g. Alice, Interviewer") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(cluster.id, name) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
