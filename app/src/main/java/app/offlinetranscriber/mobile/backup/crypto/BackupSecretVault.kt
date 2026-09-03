package app.offlinetranscriber.mobile.backup.crypto

import java.util.concurrent.ConcurrentHashMap

class BackupSecretVault {

    private val values = ConcurrentHashMap<String, CharArray>()

    fun put(operationId: String, password: CharArray) {
        remove(operationId)
        values[operationId] = password.copyOf()
    }

    fun take(operationId: String): CharArray? =
        values.remove(operationId)

    fun remove(operationId: String) {
        values.remove(operationId)?.fill('\u0000')
    }

    fun clear() {
        values.values.forEach { it.fill('\u0000') }
        values.clear()
    }
}
