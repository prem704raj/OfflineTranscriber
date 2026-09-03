package app.offlinetranscriber.mobile.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.offlinetranscriber.mobile.data.model.CollectionSummaryRow

@Composable
fun CollectionPickerDialog(
    collections: List<CollectionSummaryRow>,
    selectedIds: Set<Long>,
    onToggle: (collectionId: Long, selected: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add to collections", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            if (collections.isEmpty()) {
                Text(
                    "Create a collection from the Library first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(
                        items = collections,
                        key = { it.id }
                    ) { collection ->
                        val checked = collection.id in selectedIds

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { selected ->
                                    onToggle(
                                        collection.id,
                                        selected
                                    )
                                }
                            )

                            Column {
                                Text(collection.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${collection.transcriptCount} transcripts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
