package app.offlinetranscriber.mobile.backup.crypto

object BackupPasswordPolicy {

    const val MIN_LENGTH = 8

    fun validate(password: String, confirmation: String): String? = when {
        password.length < MIN_LENGTH -> "Use at least 8 characters."
        password != confirmation -> "Passwords do not match."
        else -> null
    }
}
