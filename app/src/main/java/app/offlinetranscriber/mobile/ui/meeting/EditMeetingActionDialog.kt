package app.offlinetranscriber.mobile.ui.meeting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import app.offlinetranscriber.mobile.data.model.MeetingActionEntity

@Composable
fun EditMeetingActionDialog(
    action: MeetingActionEntity,
    onDismiss: () -> Unit,
    onSave: (text: String, assignee: String, dueText: String) -> Unit
) {
    var text by remember { mutableStateOf(action.text) }
    var assignee by remember { mutableStateOf(action.assignee) }
    var dueText by remember { mutableStateOf(action.dueText) }

    val isSavable = text.trim().length >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Action Item", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 300) text = it },
                    label = { Text("Action item *") },
                    placeholder = { Text("e.g. Send final report") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = assignee,
                    onValueChange = { if (it.length <= 80) assignee = it },
                    label = { Text("Assignee (optional)") },
                    placeholder = { Text("e.g. Rahul") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueText,
                    onValueChange = { if (it.length <= 80) dueText = it },
                    label = { Text("Due date/time (optional)") },
                    placeholder = { Text("e.g. Friday") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSavable) {
                        onSave(text.trim(), assignee.trim(), dueText.trim())
                    }
                },
                enabled = isSavable
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
