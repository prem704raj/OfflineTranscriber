package app.offlinetranscriber.mobile.ui.meeting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.offlinetranscriber.mobile.data.model.MeetingActionEntity
import app.offlinetranscriber.mobile.meeting.model.MeetingActionStatus
import app.offlinetranscriber.mobile.ui.accessibility.accessibleAction
import app.offlinetranscriber.mobile.ui.accessibility.minimumTouchTarget

@Composable
fun MeetingActionsTab(
    actions: List<MeetingActionEntity>,
    onToggleAction: (actionId: Long, done: Boolean) -> Unit,
    onEditAction: (actionId: Long, text: String, assignee: String, dueText: String) -> Unit,
    onOpenSource: (startMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingAction by remember { mutableStateOf<MeetingActionEntity?>(null) }

    editingAction?.let { action ->
        EditMeetingActionDialog(
            action = action,
            onDismiss = { editingAction = null },
            onSave = { text, assignee, dueText ->
                onEditAction(action.id, text, assignee, dueText)
                editingAction = null
            }
        )
    }

    if (actions.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No explicit action items were found.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        val sortedActions = remember(actions) {
            actions.sortedWith(
                compareBy<MeetingActionEntity> {
                    if (it.status == MeetingActionStatus.OPEN.name) 0 else 1
                }.thenBy { it.startMs }
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(
                items = sortedActions,
                key = { it.id }
            ) { action ->
                val isDone = action.status == MeetingActionStatus.DONE.name
                val toggleLabel = if (isDone) "Mark action open" else "Mark action complete"

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDone) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Checkbox(
                            checked = isDone,
                            onCheckedChange = { checked ->
                                onToggleAction(action.id, checked)
                            },
                            modifier = Modifier
                                .minimumTouchTarget()
                                .semantics { contentDescription = toggleLabel }
                        )

                        Spacer(Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = action.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isDone) FontWeight.Normal else FontWeight.Medium,
                                    textDecoration = if (isDone) TextDecoration.LineThrough else null
                                ),
                                color = if (isDone) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )

                            if (action.assignee.isNotBlank() || action.dueText.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (action.assignee.isNotBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Assigned: ${action.assignee}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (action.dueText.isNotBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Due: ${action.dueText}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MeetingCitationChip(
                                    startMs = action.startMs,
                                    onClick = { onOpenSource(action.startMs) }
                                )

                                IconButton(
                                    onClick = { editingAction = action },
                                    modifier = Modifier
                                        .minimumTouchTarget()
                                        .accessibleAction("Edit action item")
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit action item",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
