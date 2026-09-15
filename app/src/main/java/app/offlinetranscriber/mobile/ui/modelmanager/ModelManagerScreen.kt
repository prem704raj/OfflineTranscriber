package app.offlinetranscriber.mobile.ui.modelmanager

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.offlinetranscriber.mobile.billing.Entitlement
import app.offlinetranscriber.mobile.billing.ProFeature
import app.offlinetranscriber.mobile.modelmanager.ModelDownloadState
import app.offlinetranscriber.mobile.theme.OtMeasuredDataStyle
import app.offlinetranscriber.mobile.ui.accessibility.accessibleAction
import app.offlinetranscriber.mobile.ui.components.ProFeatureBadge
import app.offlinetranscriber.mobile.ui.design.AppDimens
import app.offlinetranscriber.mobile.ui.design.AppShapes
import app.offlinetranscriber.mobile.ui.layout.ReadingWidthContainer
import app.offlinetranscriber.mobile.ui.system.OtStatusKind
import app.offlinetranscriber.mobile.ui.system.OtStatusLabel

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
                    Text(
                        "Transcription models",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.accessibleAction("Back")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        ReadingWidthContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = AppDimens.ScreenHorizontal,
                    vertical = AppDimens.Space4
                ),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Space3)
            ) {
                item {
                    Text(
                        "Choose how your phone balances speed, storage and accuracy. All processing is 100% offline.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(AppDimens.Space2))
                }

                items(
                    state.rows,
                    key = { it.spec.id }
                ) { row ->
                    val borderModifier = if (row.selected) {
                        Modifier.border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = AppShapes.Button
                        )
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = AppShapes.Button
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(borderModifier),
                        color = MaterialTheme.colorScheme.surface,
                        shape = AppShapes.Button
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = if (row.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    text = row.spec.label,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = row.spec.formattedApproximateSize,
                                    style = OtMeasuredDataStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (row.proRequired) {
                                    Spacer(Modifier.width(8.dp))
                                    ProFeatureBadge(
                                        onClick = {
                                            if (state.entitlement != Entitlement.PRO) {
                                                onOpenPaywall(ProFeature.ACCURATE_MODEL)
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = row.spec.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (row.recommended || row.selected) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (row.selected) {
                                        OtStatusLabel(
                                            text = "ACTIVE",
                                            kind = OtStatusKind.LOCAL
                                        )
                                    }
                                    if (row.recommended) {
                                        OtStatusLabel(
                                            text = "RECOMMENDED",
                                            kind = OtStatusKind.LOCAL
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            val download = state.download
                            val thisDownloading =
                                download is ModelDownloadState.Downloading &&
                                    download.modelId == row.spec.id

                            if (thisDownloading) {
                                val fraction = download.fraction
                                if (fraction != null) {
                                    LinearProgressIndicator(
                                        progress = { fraction },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        trackColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        trackColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = viewModel::cancelDownload,
                                    shape = AppShapes.Control
                                ) {
                                    Text("Cancel")
                                }
                            } else if (row.proRequired && state.entitlement != Entitlement.PRO && !row.installed) {
                                Button(
                                    onClick = {
                                        onOpenPaywall(ProFeature.ACCURATE_MODEL)
                                    },
                                    shape = AppShapes.Control
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
                                            },
                                            shape = AppShapes.Control
                                        ) {
                                            Text("Use model")
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.delete(row.spec)
                                        },
                                        shape = AppShapes.Control
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = null
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Delete")
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.download(row.spec)
                                    },
                                    shape = AppShapes.Control
                                ) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Download")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
