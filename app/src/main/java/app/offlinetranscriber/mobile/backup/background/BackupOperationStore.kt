package app.offlinetranscriber.mobile.backup.background

import app.offlinetranscriber.mobile.backup.BackupOperationState
import app.offlinetranscriber.mobile.backup.BackupOperationStatus
import app.offlinetranscriber.mobile.backup.BackupOperationType
import app.offlinetranscriber.mobile.backup.RestorePreview
import app.offlinetranscriber.mobile.backup.RestoreResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BackupOperationStore {

    private val _state = MutableStateFlow<BackupOperationState?>(null)
    val state: StateFlow<BackupOperationState?> = _state.asStateFlow()

    private val _lastInspection = MutableStateFlow<RestorePreview?>(null)
    val lastInspection: StateFlow<RestorePreview?> = _lastInspection.asStateFlow()

    private val _lastRestoreResult = MutableStateFlow<RestoreResult?>(null)
    val lastRestoreResult: StateFlow<RestoreResult?> = _lastRestoreResult.asStateFlow()

    fun updateState(
        operationId: String,
        type: BackupOperationType,
        status: BackupOperationStatus,
        progress: Int = 0,
        stage: String = "",
        restoreSessionId: String? = null,
        errorCode: String? = null
    ) {
        _state.update {
            BackupOperationState(
                operationId = operationId,
                type = type,
                status = status,
                progress = progress,
                stage = stage,
                restoreSessionId = restoreSessionId,
                errorCode = errorCode,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun setInspection(preview: RestorePreview?) {
        _lastInspection.value = preview
    }

    fun setRestoreResult(result: RestoreResult?) {
        _lastRestoreResult.value = result
    }

    fun clear() {
        _state.value = null
    }
}
