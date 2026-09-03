package app.offlinetranscriber.mobile.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.offlinetranscriber.mobile.TranscriberApplication
import app.offlinetranscriber.mobile.data.model.CollectionEntity
import app.offlinetranscriber.mobile.data.model.TranscriptEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CollectionDetailUiState(
    val collection: CollectionEntity? = null,
    val transcripts: List<TranscriptEntity> = emptyList()
)

class CollectionDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val collectionId: Long =
        checkNotNull(savedStateHandle["collectionId"])

    private val repository =
        (application as TranscriberApplication).knowledgeRepository

    val state = combine(
        repository.observeCollection(collectionId),
        repository.observeTranscriptsInCollection(
            collectionId
        )
    ) { collection, transcripts ->
        CollectionDetailUiState(
            collection = collection,
            transcripts = transcripts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CollectionDetailUiState()
    )

    fun removeTranscript(transcriptId: Long) {
        viewModelScope.launch {
            repository.setTranscriptInCollection(
                transcriptId = transcriptId,
                collectionId = collectionId,
                selected = false
            )
        }
    }
}
