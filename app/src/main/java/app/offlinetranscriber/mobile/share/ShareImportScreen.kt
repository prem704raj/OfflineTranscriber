package app.offlinetranscriber.mobile.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ShareImportScreen(
    state: StateFlow<ShareImportUiState>,
    onContinue: (ShareImportResult) -> Unit,
    onClose: () -> Unit
) {
    val value by state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.Share,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(Modifier.height(18.dp))

        when (val current = value) {
            ShareImportUiState.Importing -> {
                Text(
                    "Importing shared media",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(14.dp))
                CircularProgressIndicator()
                Spacer(Modifier.height(10.dp))
                Text(
                    "Keeping a safe local reference so background transcription can continue.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is ShareImportUiState.Done -> {
                Text(
                    "${current.result.enqueuedCount} item(s) added",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(Modifier.height(8.dp))

                if (current.result.totalRejected > 0) {
                    Text(
                        "${current.result.totalRejected} item(s) could not be added.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = { onContinue(current.result) }
                ) {
                    Text("Continue")
                }
            }

            is ShareImportUiState.Error -> {
                Text(
                    "Couldn't import shared media",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    current.message,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(18.dp))

                OutlinedButton(
                    onClick = onClose
                ) {
                    Text("Close")
                }
            }
        }
    }
}
