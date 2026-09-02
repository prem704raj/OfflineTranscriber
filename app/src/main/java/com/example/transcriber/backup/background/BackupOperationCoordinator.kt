package com.example.transcriber.backup.background

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.example.transcriber.backup.BackupOperationStatus
import com.example.transcriber.backup.BackupOperationType
import com.example.transcriber.backup.BackupOptions
import com.example.transcriber.backup.RestoreMode
import com.example.transcriber.backup.crypto.BackupSecretVault
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class BackupCreateRequest(
    val operationId: String,
    val destination: Uri,
    val options: BackupOptions
)

data class BackupInspectRequest(
    val operationId: String,
    val sourceUri: Uri
)

data class BackupRestoreRequest(
    val operationId: String,
    val sessionId: String,
    val mode: RestoreMode,
    val restoreSettings: Boolean
)

class BackupOperationCoordinator(
    private val context: Context,
    val store: BackupOperationStore,
    val vault: BackupSecretVault
) {

    private val createRequests = ConcurrentHashMap<String, BackupCreateRequest>()
    private val inspectRequests = ConcurrentHashMap<String, BackupInspectRequest>()
    private val restoreRequests = ConcurrentHashMap<String, BackupRestoreRequest>()

    fun takeCreateRequest(id: String): BackupCreateRequest? = createRequests.remove(id)
    fun takeInspectRequest(id: String): BackupInspectRequest? = inspectRequests.remove(id)
    fun takeRestoreRequest(id: String): BackupRestoreRequest? = restoreRequests.remove(id)

    fun startCreateBackup(
        destination: Uri,
        options: BackupOptions,
        password: CharArray?
    ): String {
        val opId = UUID.randomUUID().toString()
        if (password != null) {
            vault.put(opId, password)
        }
        createRequests[opId] = BackupCreateRequest(opId, destination, options)
        store.updateState(opId, BackupOperationType.CREATE, BackupOperationStatus.QUEUED, 0, "Starting backup...")

        val intent = Intent(context, BackupRestoreForegroundService::class.java).apply {
            action = BackupRestoreForegroundService.ACTION_CREATE
            putExtra(BackupRestoreForegroundService.EXTRA_OPERATION_ID, opId)
        }
        ContextCompat.startForegroundService(context, intent)
        return opId
    }

    fun startInspectBackup(
        sourceUri: Uri,
        password: CharArray?
    ): String {
        val opId = UUID.randomUUID().toString()
        if (password != null) {
            vault.put(opId, password)
        }
        inspectRequests[opId] = BackupInspectRequest(opId, sourceUri)
        store.updateState(opId, BackupOperationType.INSPECT, BackupOperationStatus.QUEUED, 0, "Inspecting backup...")

        val intent = Intent(context, BackupRestoreForegroundService::class.java).apply {
            action = BackupRestoreForegroundService.ACTION_INSPECT
            putExtra(BackupRestoreForegroundService.EXTRA_OPERATION_ID, opId)
        }
        ContextCompat.startForegroundService(context, intent)
        return opId
    }

    fun startRestoreBackup(
        sessionId: String,
        mode: RestoreMode,
        restoreSettings: Boolean
    ): String {
        val opId = UUID.randomUUID().toString()
        restoreRequests[opId] = BackupRestoreRequest(opId, sessionId, mode, restoreSettings)
        store.updateState(opId, BackupOperationType.RESTORE, BackupOperationStatus.QUEUED, 0, "Preparing restore...", sessionId)

        val intent = Intent(context, BackupRestoreForegroundService::class.java).apply {
            action = BackupRestoreForegroundService.ACTION_RESTORE
            putExtra(BackupRestoreForegroundService.EXTRA_OPERATION_ID, opId)
        }
        ContextCompat.startForegroundService(context, intent)
        return opId
    }

    fun cancel(operationId: String) {
        val intent = Intent(context, BackupRestoreForegroundService::class.java).apply {
            action = BackupRestoreForegroundService.ACTION_CANCEL
            putExtra(BackupRestoreForegroundService.EXTRA_OPERATION_ID, operationId)
        }
        context.startService(intent)
    }
}
