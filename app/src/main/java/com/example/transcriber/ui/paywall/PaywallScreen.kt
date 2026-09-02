package com.example.transcriber.ui.paywall

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transcriber.billing.BillingConnectionState
import com.example.transcriber.billing.BillingViewModel
import com.example.transcriber.billing.Entitlement
import com.example.transcriber.billing.ProFeature
import com.example.transcriber.billing.PurchaseUiStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    reason: ProFeature?,
    onBack: () -> Unit,
    onUnlocked: () -> Unit,
    viewModel: BillingViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val activity = LocalContext.current as? Activity
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.connect()
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.entitlement) {
        if (state.entitlement == Entitlement.PRO) {
            onUnlocked()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Pro") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(30.dp)
            ) {
                Icon(
                    Icons.Rounded.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.padding(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Unlock Offline Transcriber Pro",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "One payment. Keep it.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            reason?.let {
                Spacer(Modifier.height(12.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(reasonCopy(it)) }
                )
            }

            Spacer(Modifier.height(24.dp))

            Benefit(Icons.Rounded.AutoAwesome, "Accurate transcription model")
            Benefit(Icons.Rounded.Movie, "Video transcription + Subtitle Studio")
            Benefit(Icons.Rounded.Subtitles, "SRT & VTT subtitle export")
            Benefit(Icons.AutoMirrored.Filled.Chat, "Ask questions across your transcript library")
            Benefit(Icons.Rounded.School, "Study packs, flashcards and quizzes")
            Benefit(Icons.Rounded.Workspaces, "Unlimited collections and batch queue")
            Benefit(Icons.Rounded.AutoAwesome, "Turn meetings into decisions, action items and follow-ups")
            Benefit(Icons.Rounded.AutoAwesome, "See who spoke when with private on-device speaker detection")
            Benefit(Icons.Rounded.AutoAwesome, "Export polished PDF and DOCX documents")
            Benefit(Icons.Rounded.Subtitles, "Export finished videos with captions permanently rendered")

            Spacer(Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("✓ No subscription")
                    Text("✓ No ads")
                    Text("✓ Unlimited core audio transcription stays free")
                    Text("✓ Core processing stays 100% local")
                }
            }

            Spacer(Modifier.height(24.dp))

            when {
                state.entitlement == Entitlement.PRO -> {
                    AssistChip(
                        onClick = {},
                        label = { Text("Pro unlocked") },
                        leadingIcon = {
                            Icon(Icons.Rounded.CheckCircle, null)
                        }
                    )
                }

                state.purchaseStatus == PurchaseUiStatus.PENDING -> {
                    Text(
                        "Purchase pending",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Google Play will unlock Pro after the payment completes.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    val product = state.product

                    Button(
                        onClick = { activity?.let(viewModel::buy) },
                        enabled = product != null && activity != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            if (product != null) {
                                "Get Pro • ${product.formattedPrice}"
                            } else {
                                "Get Pro"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = viewModel::restore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Restore purchase")
                    }

                    if (state.connection is BillingConnectionState.Connecting) {
                        Spacer(Modifier.height(14.dp))
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }

                    if (state.connection is BillingConnectionState.Unavailable) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Google Play billing is unavailable right now.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = viewModel::connect) {
                            Text("Retry connection")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Benefit(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.padding(7.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun reasonCopy(feature: ProFeature): String =
    when (feature) {
        ProFeature.ACCURATE_MODEL -> "Accurate model is a Pro feature"
        ProFeature.VIDEO_TRANSCRIPTION -> "Video transcription is a Pro feature"
        ProFeature.SUBTITLE_EXPORT -> "Subtitle export is a Pro feature"
        ProFeature.STUDY_MODE -> "Study Mode is a Pro feature"
        ProFeature.UNLIMITED_COLLECTIONS -> "Unlimited collections are a Pro feature"
        ProFeature.BATCH_QUEUE -> "Unlimited batch queue is a Pro feature"
        ProFeature.ASK_TRANSCRIPTS -> "Ask your transcripts is a Pro feature"
        ProFeature.MEETING_INTELLIGENCE -> "Meeting Intelligence is a Pro feature"
        ProFeature.SPEAKER_INTELLIGENCE -> "Speaker Intelligence is a Pro feature"
        ProFeature.PREMIUM_EXPORT -> "PDF, DOCX and advanced exports are Pro features"
        ProFeature.BURNED_IN_CAPTIONS -> "Burned-in video captions are a Pro feature"
    }
