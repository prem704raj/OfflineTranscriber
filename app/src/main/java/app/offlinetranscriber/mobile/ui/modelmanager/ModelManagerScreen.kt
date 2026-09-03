package app.offlinetranscriber.mobile.ui.modelmanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.offlinetranscriber.mobile.billing.Entitlement
import app.offlinetranscriber.mobile.billing.ProFeature
import app.offlinetranscriber.mobile.modelmanager.ModelDownloadState
import app.offlinetranscriber.mobile.ui.components.ProFeatureBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(
    onBack: () -> Unit,
    onOpenPaywall: (ProFeature) -> Unit,
    viewModel: ModelManagerViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.openPaywallFeature) {
        state.openPaywallFeature?.let { feature ->
            onOpenPaywall(feature)
            viewModel.clearPaywallTrigger()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Transcription models")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Choose how your phone balances speed, storage and accuracy. All processing is 100% offline.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }

            items(
                state.rows,
                key = { it.spec.id }
            ) { row ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (row.selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Memory,
                                null,
                                tint = if (row.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.weight(1f))

                            if (row.proRequired) {
                                ProFeatureBadge(
                                    onClick = {
                                        if (state.entitlement != Entitlement.PRO) {
                                            onOpenPaywall(ProFeature.ACCURATE_MODEL)
                                        }
                                    }
                                )
                                Spacer(Modifier.padding(2.dp))
                            }

                            if (row.recommended) {
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text("Recommended")
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "${row.spec.label} (${row.spec.formattedApproximateSize})",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text(
                            row.spec.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(14.dp))

                        val download = state.download
                        val thisDownloading =
                            download is ModelDownloadState.Downloading &&
                                download.modelId == row.spec.id

                        if (thisDownloading) {
                            val fraction = download.fraction

                            if (fraction != null) {
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = viewModel::cancelDownload
                            ) {
                                Text("Cancel")
                            }
                        } else if (row.proRequired && state.entitlement != Entitlement.PRO && !row.installed) {
                            Button(
                                onClick = {
                                    onOpenPaywall(ProFeature.ACCURATE_MODEL)
                                }
                            ) {
                                Text("Unlock Pro (Accurate Model)")
                            }
                        } else if (row.installed) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!row.selected) {
                                    Button(
                                        onClick = {
                                            viewModel.select(row.spec)
                                        }
                                    ) {
                                        Text("Use model")
                                    }
                                } else {
                                    AssistChip(
                                        onClick = {},
                                        label = {
                                            Text("Active")
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                null
                                            )
                                        }
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.delete(row.spec)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        null
                                    )
                                    Text("Delete")
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.download(row.spec)
                                }
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    null
                                )
                                Text("Download")
                            }
                        }
                    }
                }
            }
        }
    }
}
