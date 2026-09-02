package com.example.transcriber.ui.meeting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.transcriber.meeting.MeetingNotesFormatter
import com.example.transcriber.ui.accessibility.minimumTouchTarget

@Composable
fun MeetingCitationChip(
    startMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatted = MeetingNotesFormatter.formatTimestamp(startMs)
    val totalSeconds = (startMs / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val talkBackLabel = "Open source at $minutes minutes $seconds seconds"

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .minimumTouchTarget()
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                role = Role.Button
                contentDescription = talkBackLabel
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = formatted,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}
