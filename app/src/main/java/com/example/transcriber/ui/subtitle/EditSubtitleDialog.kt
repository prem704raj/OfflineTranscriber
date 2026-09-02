package com.example.transcriber.ui.subtitle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transcriber.domain.model.TranscriptSegment

@Composable
fun EditSubtitleDialog(
    segment: TranscriptSegment,
    onDismiss: () -> Unit,
    onSave: (text: String, startMs: Long, endMs: Long) -> Unit
) {
    var text by remember(segment) { mutableStateOf(segment.text) }
    var startMs by remember(segment) { mutableLongStateOf(segment.startMs) }
    var endMs by remember(segment) { mutableLongStateOf(segment.endMs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Caption", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Caption Text") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                // Start Timing Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Start Time", style = MaterialTheme.typography.labelMedium)
                            Text(formatTiming(startMs), fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(
                                onClick = { startMs = (startMs - 500L).coerceAtLeast(0L) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("-0.5s", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { startMs = (startMs - 100L).coerceAtLeast(0L) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("-0.1s", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { startMs = (startMs + 100L).coerceAtMost(endMs - 100L) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+0.1s", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { startMs = (startMs + 500L).coerceAtMost(endMs - 100L) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+0.5s", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // End Timing Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("End Time", style = MaterialTheme.typography.labelMedium)
                            Text(formatTiming(endMs), fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(
                                onClick = { endMs = (endMs - 500L).coerceAtLeast(startMs + 100L) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("-0.5s", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { endMs = (endMs - 100L).coerceAtLeast(startMs + 100L) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("-0.1s", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { endMs += 100L },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+0.1s", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { endMs += 500L },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+0.5s", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(text, startMs, endMs) },
                enabled = text.isNotBlank() && endMs > startMs
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTiming(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    val millis = ms % 1000
    return String.format("%02d:%02d.%03d", min, sec, millis)
}
