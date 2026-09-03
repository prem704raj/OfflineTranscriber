package app.offlinetranscriber.mobile.ui.queue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.offlinetranscriber.mobile.TranscriberApplication
import app.offlinetranscriber.mobile.background.BackgroundTranscriptionStarter
import app.offlinetranscriber.mobile.queue.TranscriptionJobEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QueueViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val app = application as TranscriberApplication

    val jobs: StateFlow<List<TranscriptionJobEntity>> =
        app.queueRepository
            .observeAll()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    fun retry(id: Long) {
        viewModelScope.launch {
            app.queueManager.retry(id)
            BackgroundTranscriptionStarter.start(getApplication())
        }
    }

    fun cancel(id: Long) {
        viewModelScope.launch {
            app.queueManager.cancel(id)
        }
    }

    fun clearFinished() {
        viewModelScope.launch {
            app.queueRepository
                .clearFinishedHistory()
        }
    }
}
