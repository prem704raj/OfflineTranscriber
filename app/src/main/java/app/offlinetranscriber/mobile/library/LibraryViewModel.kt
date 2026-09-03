package app.offlinetranscriber.mobile.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.offlinetranscriber.mobile.TranscriberApplication
import app.offlinetranscriber.mobile.billing.FeatureAccessPolicy
import app.offlinetranscriber.mobile.billing.ProFeature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LibraryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        (application as TranscriberApplication).knowledgeRepository

    private val _state =
        MutableStateFlow(LibraryUiState())

    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeCollectionSummaries(),
                repository.observeRecentBookmarks(12),
                repository.observeLibraryTranscripts()
            ) { collections, bookmarks, transcripts ->
                Triple(collections, bookmarks, transcripts)
            }.collect { (collections, bookmarks, transcripts) ->
                _state.value = _state.value.copy(
                    collections = collections,
                    recentBookmarks = bookmarks,
                    transcripts = transcripts
                )
            }
        }
    }

    fun createCollection(name: String) {
        viewModelScope.launch {
            val app = getApplication<Application>() as TranscriberApplication
            val entitlement = app.entitlementRepository.entitlement.first()
            val currentCount = _state.value.collections.size

            if (!FeatureAccessPolicy.canCreateCollection(entitlement, currentCount)) {
                _state.value = _state.value.copy(
                    openPaywallFeature = ProFeature.UNLIMITED_COLLECTIONS
                )
                return@launch
            }

            runCatching {
                repository.createCollection(name)
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    errorMessage = error.message
                )
            }
        }
    }

    fun clearPaywallTrigger() {
        _state.value = _state.value.copy(
            openPaywallFeature = null
        )
    }

    fun deleteCollection(collectionId: Long) {
        viewModelScope.launch {
            repository.deleteCollection(collectionId)
        }
    }

    fun openCollectionPicker(transcriptId: Long) {
        viewModelScope.launch {
            val selected =
                repository.collectionIdsForTranscript(
                    transcriptId
                )

            _state.value = _state.value.copy(
                collectionPicker = CollectionPickerState(
                    transcriptId = transcriptId,
                    selectedCollectionIds = selected
                )
            )
        }
    }

    fun setCollectionMembership(
        collectionId: Long,
        selected: Boolean
    ) {
        val picker = _state.value.collectionPicker
            ?: return

        viewModelScope.launch {
            repository.setTranscriptInCollection(
                transcriptId = picker.transcriptId,
                collectionId = collectionId,
                selected = selected
            )

            val newSet = picker.selectedCollectionIds
                .toMutableSet()
                .apply {
                    if (selected) add(collectionId)
                    else remove(collectionId)
                }
                .toSet()

            _state.value = _state.value.copy(
                collectionPicker = picker.copy(
                    selectedCollectionIds = newSet
                )
            )
        }
    }

    fun closeCollectionPicker() {
        _state.value = _state.value.copy(
            collectionPicker = null
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(
            errorMessage = null
        )
    }
}
