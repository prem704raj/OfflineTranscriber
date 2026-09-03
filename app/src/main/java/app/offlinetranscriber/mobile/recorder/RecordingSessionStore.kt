package app.offlinetranscriber.mobile.recorder

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RecordingSessionStore {
    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    fun update(transform: (RecordingState) -> RecordingState) {
        _state.value = transform(_state.value)
    }

    fun set(newState: RecordingState) {
        _state.value = newState
    }

    fun reset() {
        _state.value = RecordingState()
    }
}
