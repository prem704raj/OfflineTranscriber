package com.example.transcriber.ui.caption

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SplitCaptionDialog(
    segmentText: String,
    onConfirmSplit: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val cleanText = segmentText.trim()
    val words = cleanText.split(Regex("""\s+"""))
    val defaultWordSplit = (words.size / 2).coerceAtLeast(1)

    // Calculate default character split point at word boundary near middle
    val defaultSplitIndex = words.take(defaultWordSplit).joinToString(" ").length.coerceIn(1, (cleanText.length - 1).coerceAtLeast(1))

    var splitProgress by remember {
        mutableFloatStateOf(defaultSplitIndex.toFloat() / cleanText.length.toFloat().coerceAtLeast(1f))
    }

    val actualSplitIndex = (cleanText.length * splitProgress).toInt().coerceIn(1, (cleanText.length - 1).coerceAtLeast(1))
    val leftPart = cleanText.substring(0, actualSplitIndex).trim()
    val rightPart = cleanText.substring(actualSplitIndex).trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Split Caption", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Adjust split position between the two parts:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Slider(
                    value = splitProgress,
                    onValueChange = { splitProgress = it },
                    valueRange = 0.1f..0.9f
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "PART 1:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = leftPart.ifBlank { "..." },
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "PART 2:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = rightPart.ifBlank { "..." },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmSplit(actualSplitIndex) },
                enabled = leftPart.isNotBlank() && rightPart.isNotBlank()
            ) {
                Text("Split")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
