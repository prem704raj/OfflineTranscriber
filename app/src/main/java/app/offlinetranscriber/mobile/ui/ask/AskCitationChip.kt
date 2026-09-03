package app.offlinetranscriber.mobile.ui.ask

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.offlinetranscriber.mobile.data.model.AskCitationRow
import app.offlinetranscriber.mobile.ui.accessibility.minimumTouchTarget
import java.util.Locale

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        val remMinutes = minutes % 60
        String.format(Locale.US, "%d:%02d:%02d", hours, remMinutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

@Composable
fun AskCitationChip(
    value: AskCitationRow,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedTime = formatDuration(value.startMs)
    val labelText = if (value.transcriptTitle.isNotBlank()) {
        "${value.transcriptTitle} • $formattedTime"
    } else {
        formattedTime
    }
    val description = "Open ${value.transcriptTitle} at $formattedTime"

    AssistChip(
        onClick = onOpen,
        leadingIcon = {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        label = {
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelMedium
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .minimumTouchTarget()
            .semantics {
                contentDescription = description
            }
    )
}
