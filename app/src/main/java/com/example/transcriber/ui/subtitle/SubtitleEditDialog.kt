package com.example.transcriber.ui.subtitle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.transcriber.domain.model.TranscriptSegment
import com.example.transcriber.ui.accessibility.minimumTouchTarget

@Composable
fun SubtitleEditDialog(
    segment: TranscriptSegment,
    onDismiss: () -> Unit,
    onSave: (String, Long, Long) -> Unit,
    onPreview: (Long) -> Unit
) {
    var text by remember(segment.id) { mutableStateOf(segment.text) }
    var startMs by remember(segment.id) { mutableLongStateOf(segment.startMs) }
    var endMs by remember(segment.id) { mutableLongStateOf(segment.endMs) }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit subtitle",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Subtitle text") },
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 6
                )

                Spacer(Modifier.height(18.dp))
                Text("Start • ${TranscriptSegment.formatTime(startMs)}", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                TimingNudges(
                    onMinus500 = { startMs = (startMs - 500).coerceAtLeast(0); onPreview(startMs) },
                    onMinus100 = { startMs = (startMs - 100).coerceAtLeast(0); onPreview(startMs) },
                    onPlus100 = { startMs += 100; onPreview(startMs) },
                    onPlus500 = { startMs += 500; onPreview(startMs) }
                )

                Spacer(Modifier.height(18.dp))
                Text("End • ${TranscriptSegment.formatTime(endMs)}", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                TimingNudges(
                    onMinus500 = { endMs = (endMs - 500).coerceAtLeast(0) },
                    onMinus100 = { endMs = (endMs - 100).coerceAtLeast(0) },
                    onPlus100 = { endMs += 100 },
                    onPlus500 = { endMs += 500 }
                )

                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { onPreview(startMs) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumTouchTarget()
                ) { Text("Preview from start") }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(text, startMs, endMs) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.minimumTouchTarget()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumTouchTarget()
            ) { Text("Cancel") }
        }
    )
}

@Composable
private fun TimingNudges(
    onMinus500: () -> Unit,
    onMinus100: () -> Unit,
    onPlus100: () -> Unit,
    onPlus500: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TextButton(onClick = onMinus500, modifier = Modifier.weight(1f).minimumTouchTarget()) { Text("−500") }
        TextButton(onClick = onMinus100, modifier = Modifier.weight(1f).minimumTouchTarget()) { Text("−100") }
        TextButton(onClick = onPlus100, modifier = Modifier.weight(1f).minimumTouchTarget()) { Text("+100") }
        TextButton(onClick = onPlus500, modifier = Modifier.weight(1f).minimumTouchTarget()) { Text("+500") }
    }
}
