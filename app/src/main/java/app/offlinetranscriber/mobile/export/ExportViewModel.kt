package app.offlinetranscriber.mobile.export

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.offlinetranscriber.mobile.TranscriberApplication
import app.offlinetranscriber.mobile.billing.Entitlement
import app.offlinetranscriber.mobile.export.document.ExportBlock
import app.offlinetranscriber.mobile.export.format.ExportTimestampFormatter
import app.offlinetranscriber.mobile.export.model.ExportArtifact
import app.offlinetranscriber.mobile.export.model.ExportContentType
import app.offlinetranscriber.mobile.export.model.ExportFormat
import app.offlinetranscriber.mobile.export.model.ExportOptions
import app.offlinetranscriber.mobile.export.model.ExportTarget
import app.offlinetranscriber.mobile.export.model.ExportTextScale
import app.offlinetranscriber.mobile.export.model.PdfPageSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExportViewModel(
    application: Application,
    target: ExportTarget
) : AndroidViewModel(application) {

    private val app = application as TranscriberApplication
    private val coordinator = app.exportCoordinator
    private val metadataProvider = app.exportSnapshotMetadataProvider
    private val assembler = app.exportDocumentAssembler

    private val _uiState = MutableStateFlow(
        ExportUiState(
            target = target,
            options = ExportOptions(format = ExportFormat.TXT),
            entitlement = app.billingRepository.state.value.entitlement
        )
    )
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            app.entitlementRepository.entitlement.collect { entitlement: Entitlement ->
                _uiState.value = _uiState.value.copy(entitlement = entitlement)
            }
        }
        loadMetadata()
    }

    fun loadMetadata() {
        viewModelScope.launch {
            try {
                val metadata = metadataProvider.load(_uiState.value.target)
                _uiState.value = _uiState.value.copy(
                    title = metadata.title,
                    hasSpeakerData = metadata.hasSpeakerData
                )
                refreshPreviewSnippet()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to load document information"
                )
            }
        }
    }

    private suspend fun refreshPreviewSnippet() {
        try {
            val doc = assembler.assemble(_uiState.value.target, _uiState.value.options)
            val preview = buildString {
                append("Title: ${doc.title}\n")
                if (doc.subtitle != null) append("Type: ${doc.subtitle}\n")
                if (doc.metadata.isNotEmpty()) {
                    append("\n--- Overview ---\n")
                    doc.metadata.forEach { append("${it.label}: ${it.value}\n") }
                }
                append("\n--- Content Sample ---\n")
                val sampleBlocks = doc.blocks.take(8)
                sampleBlocks.forEach { block ->
                    when (block) {
                        is ExportBlock.Heading -> append("\n[${block.text.uppercase()}]\n")
                        is ExportBlock.Paragraph -> append("${block.text.take(120)}...\n")
                        is ExportBlock.TranscriptSegment -> {
                            val time = block.timestampMs?.let { "[${ExportTimestampFormatter.format(it)}] " } ?: ""
                            val speaker = block.speakerLabel?.let { "$it: " } ?: ""
                            append("$time$speaker${block.text.take(80)}...\n")
                        }
                        is ExportBlock.Bullet -> append("• ${block.text.take(80)}\n")
                        is ExportBlock.Checklist -> {
                            val mark = if (block.checked) "[X]" else "[ ]"
                            append("$mark ${block.text.take(80)}\n")
                        }
                        is ExportBlock.KeyValue -> append("${block.key}: ${block.value.take(60)}\n")
                        else -> {}
                    }
                }
            }
            _uiState.value = _uiState.value.copy(previewSnippet = preview)
        } catch (_: Exception) {
            // Keep existing preview on error
        }
    }

    fun selectFormat(format: ExportFormat) {
        _uiState.value = _uiState.value.copy(
            options = _uiState.value.options.copy(format = format),
            statusMessage = null,
            errorMessage = null
        )
        viewModelScope.launch { refreshPreviewSnippet() }
    }

    fun updateIncludeMetadata(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            options = _uiState.value.options.copy(includeMetadata = enabled)
        )
        viewModelScope.launch { refreshPreviewSnippet() }
    }

    fun updateIncludeTimestamps(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            options = _uiState.value.options.copy(includeTimestamps = enabled)
        )
        viewModelScope.launch { refreshPreviewSnippet() }
    }

    fun updateIncludeSpeakerLabels(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            options = _uiState.value.options.copy(includeSpeakerLabels = enabled)
        )
        viewModelScope.launch { refreshPreviewSnippet() }
    }

    fun updatePageSize(pageSize: PdfPageSize) {
        _uiState.value = _uiState.value.copy(
            options = _uiState.value.options.copy(pageSize = pageSize)
        )
    }

    fun updateTextScale(scale: ExportTextScale) {
        _uiState.value = _uiState.value.copy(
            options = _uiState.value.options.copy(textScale = scale)
        )
    }

    fun updateIncludeQuizAnswers(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            options = _uiState.value.options.copy(includeQuizAnswers = enabled)
        )
        viewModelScope.launch { refreshPreviewSnippet() }
    }

    fun updateIncludeAskCitations(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            options = _uiState.value.options.copy(includeAskCitations = enabled)
        )
        viewModelScope.launch { refreshPreviewSnippet() }
    }

    fun prepareSave(onLaunchPicker: (ExportArtifact) -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(
                progressState = ExportProgressState.GENERATING,
                statusMessage = "Generating ${state.options.format.name} document...",
                errorMessage = null
            )
            try {
                val artifact = coordinator.generateArtifact(
                    target = state.target,
                    options = state.options,
                    entitlement = state.entitlement,
                    title = state.title
                )
                _uiState.value = _uiState.value.copy(
                    progressState = ExportProgressState.SAVING,
                    statusMessage = "Select destination to save...",
                    pendingArtifactForSave = artifact
                )
                onLaunchPicker(artifact)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    progressState = ExportProgressState.ERROR,
                    errorMessage = e.message ?: "Failed to generate export document",
                    statusMessage = null
                )
            }
        }
    }

    fun onSaveDestinationSelected(uri: Uri?) {
        val artifact = _uiState.value.pendingArtifactForSave
        if (uri == null || artifact == null) {
            _uiState.value = _uiState.value.copy(
                progressState = ExportProgressState.IDLE,
                statusMessage = null,
                pendingArtifactForSave = null
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                progressState = ExportProgressState.SAVING,
                statusMessage = "Writing document..."
            )
            try {
                coordinator.saveArtifact(artifact, uri)
                _uiState.value = _uiState.value.copy(
                    progressState = ExportProgressState.SUCCESS,
                    statusMessage = "Saved ${artifact.fileName} successfully!",
                    pendingArtifactForSave = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    progressState = ExportProgressState.ERROR,
                    errorMessage = e.message ?: "Failed to save file to chosen destination",
                    statusMessage = null,
                    pendingArtifactForSave = null
                )
            }
        }
    }

    fun share(context: Context) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(
                progressState = ExportProgressState.GENERATING,
                statusMessage = "Preparing ${state.options.format.name} for share...",
                errorMessage = null
            )
            try {
                val artifact = coordinator.generateArtifact(
                    target = state.target,
                    options = state.options,
                    entitlement = state.entitlement,
                    title = state.title
                )
                val shareIntent = coordinator.createShareIntent(artifact)
                context.startActivity(shareIntent)
                _uiState.value = _uiState.value.copy(
                    progressState = ExportProgressState.IDLE,
                    statusMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    progressState = ExportProgressState.ERROR,
                    errorMessage = e.message ?: "Failed to share document",
                    statusMessage = null
                )
            }
        }
    }

    fun print(context: Context) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(
                progressState = ExportProgressState.PRINTING,
                statusMessage = "Preparing document for printing...",
                errorMessage = null
            )
            try {
                coordinator.printPdf(
                    context = context,
                    target = state.target,
                    options = state.options,
                    entitlement = state.entitlement,
                    title = state.title
                )
                _uiState.value = _uiState.value.copy(
                    progressState = ExportProgressState.IDLE,
                    statusMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    progressState = ExportProgressState.ERROR,
                    errorMessage = e.message ?: "Failed to start print job",
                    statusMessage = null
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            progressState = ExportProgressState.IDLE
        )
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(
            statusMessage = null,
            progressState = ExportProgressState.IDLE
        )
    }
}
