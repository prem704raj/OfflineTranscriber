package app.offlinetranscriber.mobile.ui.speaker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.offlinetranscriber.mobile.data.database.SpeakerTurnRow
import java.util.Locale

import app.offlinetranscriber.mobile.theme.SpeakerBlue
import app.offlinetranscriber.mobile.theme.SpeakerOchre
import app.offlinetranscriber.mobile.theme.SpeakerPlum
import app.offlinetranscriber.mobile.theme.SpeakerTeal

// Speaker palette: restrained categorical markers, never a rainbow UI.
val SpeakerColors = listOf(
    SpeakerBlue,
    SpeakerTeal,
    SpeakerPlum,
    SpeakerOchre
)

fun getSpeakerColor(speakerIndex: Int): Color {
    val idx = (speakerIndex % SpeakerColors.size + SpeakerColors.size) % SpeakerColors.size
    return SpeakerColors[idx]
}

@Composable
fun SpeakerTimeline(
    turns: List<SpeakerTurnRow>,
    onSeekToMs: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (turns.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No speaker turns recorded",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(turns, key = { it.id }) { turn ->
            val color = getSpeakerColor(turn.speakerIndex)
            val durationSec = (turn.endMs - turn.startMs) / 1000f

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeekToMs(turn.startMs) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = turn.customName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${formatTime(turn.startMs)} – ${formatTime(turn.endMs)} (${String.format(Locale.US, "%.1fs", durationSec)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", min, sec)
}
