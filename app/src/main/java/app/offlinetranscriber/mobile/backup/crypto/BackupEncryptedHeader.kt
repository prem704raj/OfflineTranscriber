package app.offlinetranscriber.mobile.backup.crypto

data class BackupEncryptedHeader(
    val envelopeVersion: Int,
    val kdfId: Int,
    val iterations: Int,
    val salt: ByteArray,
    val nonce: ByteArray,
    val encodedHeader: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BackupEncryptedHeader

        if (envelopeVersion != other.envelopeVersion) return false
        if (kdfId != other.kdfId) return false
        if (iterations != other.iterations) return false
        if (!salt.contentEquals(other.salt)) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (!encodedHeader.contentEquals(other.encodedHeader)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = envelopeVersion
        result = 31 * result + kdfId
        result = 31 * result + iterations
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + encodedHeader.contentHashCode()
        return result
    }
}
