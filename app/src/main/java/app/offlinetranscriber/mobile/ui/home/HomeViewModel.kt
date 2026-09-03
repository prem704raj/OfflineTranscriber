package app.offlinetranscriber.mobile.ui.home

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.offlinetranscriber.mobile.TranscriberApplication
import app.offlinetranscriber.mobile.billing.Entitlement
import app.offlinetranscriber.mobile.billing.ProFeature
import app.offlinetranscriber.mobile.billing.ProRequiredException
import app.offlinetranscriber.mobile.data.model.TranscriptEntity
import app.offlinetranscriber.mobile.modelmanager.ModelCatalog
import app.offlinetranscriber.mobile.modelmanager.WhisperModelSpec
import app.offlinetranscriber.mobile.queue.TranscriptionJobEntity
import app.offlinetranscriber.mobile.queue.TranscriptionSourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val app = application as TranscriberApplication

    val repository = app.transcriptRepository
    val modelManager = app.modelManager
    val settingsRepository = app.settingsRepository
    val entitlementRepository = app.entitlementRepository
    private val enqueueUseCase = app.enqueueTranscriptionUseCase

    val entitlement: StateFlow<Entitlement> = entitlementRepository.entitlement
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Entitlement.FREE)

    val transcripts: StateFlow<List<TranscriptEntity>> = repository.allTranscripts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queueJobs: StateFlow<List<TranscriptionJobEntity>> = app.queueRepository
        .observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedModelSpec: StateFlow<WhisperModelSpec?> = settingsRepository.settings
        .map { prefs ->
            ModelCatalog.byId(prefs.selectedModelId) ?: modelManager.installedModels().firstOrNull() ?: ModelCatalog.balanced
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _openPaywallFeature = MutableStateFlow<ProFeature?>(null)
    val openPaywallFeature: StateFlow<ProFeature?> = _openPaywallFeature.asStateFlow()

    fun enqueueAudio(uri: Uri, onEnqueued: ((needsModel: Boolean) -> Unit)? = null) {
        val fileName = getFileNameFromUri(uri) ?: "Audio_${System.currentTimeMillis()}"
        viewModelScope.launch {
            runCatching {
                enqueueUseCase(
                    inputUri = uri,
                    sourceUri = uri,
                    sourceType = TranscriptionSourceType.AUDIO,
                    displayName = fileName.substringBeforeLast('.')
                )
            }.onSuccess { result ->
                onEnqueued?.invoke(result.needsModel)
            }.onFailure { error ->
                if (error is ProRequiredException) {
                    _openPaywallFeature.value = error.feature
                } else {
                    _message.value = error.message ?: "Failed to enqueue audio"
                }
            }
        }
    }

    fun enqueueRawVideo(uri: Uri, onEnqueued: ((needsModel: Boolean) -> Unit)? = null) {
        val fileName = getFileNameFromUri(uri) ?: "Video_${System.currentTimeMillis()}"
        viewModelScope.launch {
            runCatching {
                enqueueUseCase(
                    inputUri = uri,
                    sourceUri = uri,
                    sourceType = TranscriptionSourceType.VIDEO,
                    displayName = fileName.substringBeforeLast('.'),
                    isPrepared = false
                )
            }.onSuccess { result ->
                onEnqueued?.invoke(result.needsModel)
            }.onFailure { error ->
                if (error is ProRequiredException) {
                    _openPaywallFeature.value = error.feature
                } else {
                    _message.value = error.message ?: "Failed to enqueue video"
                }
            }
        }
    }

    fun enqueueRecording(recordedUri: Uri, title: String, onEnqueued: ((needsModel: Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            runCatching {
                enqueueUseCase(
                    inputUri = recordedUri,
                    sourceUri = recordedUri,
                    sourceType = TranscriptionSourceType.RECORDING,
                    displayName = title,
                    isPrepared = true
                )
            }.onSuccess { result ->
                onEnqueued?.invoke(result.needsModel)
            }.onFailure { error ->
                if (error is ProRequiredException) {
                    _openPaywallFeature.value = error.feature
                } else {
                    _message.value = error.message ?: "Failed to enqueue recording"
                }
            }
        }
    }

    fun enqueueVideo(
        sourceVideoUri: Uri,
        extractedAudioUri: Uri,
        displayName: String,
        onEnqueued: ((needsModel: Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            runCatching {
                enqueueUseCase(
                    inputUri = extractedAudioUri,
                    sourceUri = sourceVideoUri,
                    sourceType = TranscriptionSourceType.VIDEO,
                    displayName = displayName.substringBeforeLast('.'),
                    isPrepared = true
                )
            }.onSuccess { result ->
                onEnqueued?.invoke(result.needsModel)
            }.onFailure { error ->
                if (error is ProRequiredException) {
                    _openPaywallFeature.value = error.feature
                } else {
                    _message.value = error.message ?: "Failed to enqueue video"
                }
            }
        }
    }

    fun enqueueSampleTest(onEnqueued: ((needsModel: Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            runCatching {
                enqueueUseCase(
                    inputUri = Uri.parse("asset://jfk.wav"),
                    sourceUri = Uri.parse("asset://jfk.wav"),
                    sourceType = TranscriptionSourceType.AUDIO,
                    displayName = "JFK Historic Inaugural Address Sample",
                    isPrepared = true
                )
            }.onSuccess { result ->
                onEnqueued?.invoke(result.needsModel)
            }.onFailure { error ->
                if (error is ProRequiredException) {
                    _openPaywallFeature.value = error.feature
                } else {
                    _message.value = error.message ?: "Failed to enqueue sample"
                }
            }
        }
    }

    fun clearPaywallTrigger() {
        _openPaywallFeature.value = null
    }

    fun clearMessage() {
        _message.value = null
    }

    fun renameTranscript(id: Long, newTitle: String) {
        viewModelScope.launch {
            repository.updateTitle(id, newTitle)
        }
    }

    fun deleteTranscript(id: Long) {
        viewModelScope.launch {
            repository.deleteTranscript(id)
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        val context = getApplication<Application>()
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        return cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting file name from content URI", e)
            }
        }
        return uri.lastPathSegment
    }
}
