package com.example.transcriber.ui.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.transcriber.TranscriberApplication
import com.example.transcriber.backup.BackupOperationState
import com.example.transcriber.backup.BackupOptions
import com.example.transcriber.backup.RestoreMode
import com.example.transcriber.backup.RestorePreview
import com.example.transcriber.backup.RestoreResult
import com.example.transcriber.backup.crypto.BackupPasswordPolicy
import com.example.transcriber.backup.estimate.BackupEstimate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BackupViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val app = application as TranscriberApplication

    val operationState: StateFlow<BackupOperationState?> = app.backupOperationStore.state
    val lastInspection: StateFlow<RestorePreview?> = app.backupOperationStore.lastInspection
    val lastRestoreResult: StateFlow<RestoreResult?> = app.backupOperationStore.lastRestoreResult

    private val _estimate = MutableStateFlow<BackupEstimate?>(null)
    val estimate: StateFlow<BackupEstimate?> = _estimate.asStateFlow()

    // Backup creation options
    private val _includeSettings = MutableStateFlow(true)
    val includeSettings: StateFlow<Boolean> = _includeSettings.asStateFlow()

    private val _includeMedia = MutableStateFlow(false)
    val includeMedia: StateFlow<Boolean> = _includeMedia.asStateFlow()

    private val _passwordProtected = MutableStateFlow(false)
    val passwordProtected: StateFlow<Boolean> = _passwordProtected.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _passwordConfirmation = MutableStateFlow("")
    val passwordConfirmation: StateFlow<String> = _passwordConfirmation.asStateFlow()

    // Restore inspection / prompt state
    private val _pendingInspectUri = MutableStateFlow<Uri?>(null)
    val pendingInspectUri: StateFlow<Uri?> = _pendingInspectUri.asStateFlow()

    private val _showPasswordSheet = MutableStateFlow(false)
    val showPasswordSheet: StateFlow<Boolean> = _showPasswordSheet.asStateFlow()

    private val _inspectPasswordInput = MutableStateFlow("")
    val inspectPasswordInput: StateFlow<String> = _inspectPasswordInput.asStateFlow()

    private val _inspectPasswordError = MutableStateFlow<String?>(null)
    val inspectPasswordError: StateFlow<String?> = _inspectPasswordError.asStateFlow()

    // Restore Preview State
    private val _selectedRestoreMode = MutableStateFlow(RestoreMode.MERGE)
    val selectedRestoreMode: StateFlow<RestoreMode> = _selectedRestoreMode.asStateFlow()

    private val _restoreSettingsOption = MutableStateFlow(true)
    val restoreSettingsOption: StateFlow<Boolean> = _restoreSettingsOption.asStateFlow()

    private val _showReplaceConfirmDialog = MutableStateFlow(false)
    val showReplaceConfirmDialog: StateFlow<Boolean> = _showReplaceConfirmDialog.asStateFlow()

    init {
        loadEstimate()
    }

    fun loadEstimate() {
        viewModelScope.launch {
            _estimate.value = app.backupSizeEstimator.estimate()
        }
    }

    fun setIncludeSettings(value: Boolean) {
        _includeSettings.value = value
    }

    fun setIncludeMedia(value: Boolean) {
        _includeMedia.value = value
    }

    fun setPasswordProtected(value: Boolean) {
        _passwordProtected.value = value
        if (!value) {
            _password.value = ""
            _passwordConfirmation.value = ""
        }
    }

    fun setPassword(value: String) {
        _password.value = value
    }

    fun setPasswordConfirmation(value: String) {
        _passwordConfirmation.value = value
    }

    fun validateCreationPassword(): String? {
        if (!_passwordProtected.value) return null
        return BackupPasswordPolicy.validate(_password.value, _passwordConfirmation.value)
    }

    fun createBackup(destination: Uri) {
        val pw = if (_passwordProtected.value) _password.value.toCharArray() else null
        val options = BackupOptions(
            includeSettings = _includeSettings.value,
            includeMedia = _includeMedia.value,
            passwordProtected = _passwordProtected.value
        )
        app.backupOperationCoordinator.startCreateBackup(
            destination = destination,
            options = options,
            password = pw
        )
    }

    fun onRestoreFileSelected(uri: Uri) {
        _pendingInspectUri.value = uri
        viewModelScope.launch {
            val isEncrypted = app.contentResolver.openInputStream(uri)?.buffered(64 * 1024)?.use { stream ->
                app.backupCrypto.isEncrypted(stream)
            } ?: false

            if (isEncrypted) {
                _inspectPasswordInput.value = ""
                _inspectPasswordError.value = null
                _showPasswordSheet.value = true
            } else {
                app.backupOperationCoordinator.startInspectBackup(uri, null)
            }
        }
    }

    fun setInspectPasswordInput(value: String) {
        _inspectPasswordInput.value = value
        _inspectPasswordError.value = null
    }

    fun submitInspectPassword() {
        val uri = _pendingInspectUri.value ?: return
        val pass = _inspectPasswordInput.value
        if (pass.isBlank()) {
            _inspectPasswordError.value = "Password cannot be blank"
            return
        }
        _showPasswordSheet.value = false
        app.backupOperationCoordinator.startInspectBackup(uri, pass.toCharArray())
    }

    fun dismissPasswordSheet() {
        _showPasswordSheet.value = false
        _pendingInspectUri.value = null
        _inspectPasswordInput.value = ""
        _inspectPasswordError.value = null
    }

    fun setRestoreMode(mode: RestoreMode) {
        _selectedRestoreMode.value = mode
    }

    fun setRestoreSettingsOption(value: Boolean) {
        _restoreSettingsOption.value = value
    }

    fun requestRestoreExecution() {
        if (_selectedRestoreMode.value == RestoreMode.REPLACE) {
            _showReplaceConfirmDialog.value = true
        } else {
            confirmAndExecuteRestore()
        }
    }

    fun confirmAndExecuteRestore() {
        _showReplaceConfirmDialog.value = false
        val preview = lastInspection.value ?: return
        app.backupOperationCoordinator.startRestoreBackup(
            sessionId = preview.sessionId,
            mode = _selectedRestoreMode.value,
            restoreSettings = _restoreSettingsOption.value && preview.includesSettings
        )
    }

    fun dismissReplaceDialog() {
        _showReplaceConfirmDialog.value = false
    }

    fun cancelActiveOperation() {
        val state = operationState.value ?: return
        app.backupOperationCoordinator.cancel(state.operationId)
    }

    fun clearOperationState() {
        app.backupOperationStore.clear()
    }
}
