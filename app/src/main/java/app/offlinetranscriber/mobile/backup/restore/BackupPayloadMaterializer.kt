package app.offlinetranscriber.mobile.backup.restore

import android.content.Context
import android.net.Uri
import app.offlinetranscriber.mobile.backup.crypto.BackupCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream

data class MaterializedBackupPayload(
    val zipFile: File,
    val encrypted: Boolean
)

class BackupPayloadMaterializer(
    private val context: Context,
    private val workspace: RestoreWorkspace,
    private val crypto: BackupCrypto
) {

    suspend fun materialize(
        sessionId: String,
        sourceUri: Uri,
        password: CharArray?,
        onProgressBytes: (Long) -> Unit = {}
    ): MaterializedBackupPayload = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val output = workspace.payloadZip(sessionId)

        if (output.exists()) {
            output.delete()
        }

        val isEncrypted = resolver.openInputStream(sourceUri)?.buffered(64 * 1024)?.use { probe ->
            crypto.isEncrypted(probe)
        } ?: error("Unable to open backup source: $sourceUri")

        if (isEncrypted) {
            val secret = password ?: error("This backup requires a password.")
            try {
                resolver.openInputStream(sourceUri)?.use { input ->
                    crypto.decryptToFile(
                        input = input,
                        password = secret,
                        outputZip = output,
                        onBytes = onProgressBytes
                    )
                } ?: error("Unable to read encrypted backup.")
            } finally {
                secret.fill('\u0000')
            }
        } else {
            resolver.openInputStream(sourceUri)?.buffered(64 * 1024)?.use { input ->
                FileOutputStream(output).buffered(64 * 1024).use { destination ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L

                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue

                        destination.write(buffer, 0, read)
                        total += read
                        onProgressBytes(total)
                    }
                    destination.flush()
                }
            } ?: error("Unable to read unencrypted backup.")
        }

        require(output.isFile && output.length() > 0L) {
            "Backup contains no data."
        }

        MaterializedBackupPayload(
            zipFile = output,
            encrypted = isEncrypted
        )
    }
}
