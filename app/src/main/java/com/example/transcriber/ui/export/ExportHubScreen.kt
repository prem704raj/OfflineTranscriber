package com.example.transcriber.ui.export

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transcriber.billing.Entitlement
import com.example.transcriber.billing.ProFeature
import com.example.transcriber.export.ExportFeaturePolicy
import com.example.transcriber.export.ExportViewModel
import com.example.transcriber.export.files.ExportCreateDocumentContract
import com.example.transcriber.export.model.ExportContentType
import com.example.transcriber.export.model.ExportTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportHubScreen(
    contentType: String,
    sourceId: Long,
    onBack: () -> Unit,
    onUpgrade: (ProFeature) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parsedContentType = try {
        ExportContentType.valueOf(contentType)
    } catch (_: Exception) {
        ExportContentType.TRANSCRIPT
    }

    val target = remember(parsedContentType, sourceId) {
        ExportTarget(parsedContentType, sourceId)
    }

    val viewModel: ExportViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ExportViewModel(
                    application = context.applicationContext as Application,
                    target = target
                ) as T
            }
        }
    )

    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ExportCreateDocumentContract()
    ) { uri ->
        viewModel.onSaveDestinationSelected(uri)
    }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    val policyCheck = remember(state.target, state.options, state.entitlement) {
        ExportFeaturePolicy.check(state.target, state.options, state.entitlement)
    }
    val isProRestricted = !policyCheck.allowed

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Export & Share",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.title.isNotBlank()) {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Pro Upgrade Callout if format or feature restricted
            if (isProRestricted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onUpgrade(ProFeature.PREMIUM_EXPORT) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pro Export Feature",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = policyCheck.reason ?: "Unlock PDF, DOCX and speaker-aware exports with Pro.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Format Selector
            ExportFormatSelector(
                selectedFormat = state.options.format,
                entitlement = state.entitlement,
                onSelectFormat = { viewModel.selectFormat(it) }
            )

            // Options Section
            ExportOptionsSection(
                target = state.target,
                options = state.options,
                hasSpeakerData = state.hasSpeakerData,
                entitlement = state.entitlement,
                onUpdateMetadata = { viewModel.updateIncludeMetadata(it) },
                onUpdateTimestamps = { viewModel.updateIncludeTimestamps(it) },
                onUpdateSpeakerLabels = { viewModel.updateIncludeSpeakerLabels(it) },
                onUpdatePageSize = { viewModel.updatePageSize(it) },
                onUpdateTextScale = { viewModel.updateTextScale(it) },
                onUpdateQuizAnswers = { viewModel.updateIncludeQuizAnswers(it) },
                onUpdateAskCitations = { viewModel.updateIncludeAskCitations(it) }
            )

            // Preview Card
            ExportPreviewCard(
                format = state.options.format,
                previewText = state.previewSnippet
            )

            // Action Bar
            ExportActionBar(
                format = state.options.format,
                progressState = state.progressState,
                isProRestricted = isProRestricted,
                onSaveClick = {
                    viewModel.prepareSave { artifact ->
                        createDocumentLauncher.launch(artifact)
                    }
                },
                onShareClick = { viewModel.share(context) },
                onPrintClick = { viewModel.print(context) },
                onUpgradeClick = { onUpgrade(ProFeature.PREMIUM_EXPORT) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
