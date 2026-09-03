package app.offlinetranscriber.mobile.backup.restore

object BackupSafetyLimits {

    const val MAX_ZIP_ENTRIES = 20_000

    const val MAX_MANIFEST_BYTES = 4L * 1024L * 1024L

    const val MAX_SINGLE_JSON_BYTES = 1L * 1024L * 1024L * 1024L

    const val MAX_TOTAL_JSON_BYTES = 5L * 1024L * 1024L * 1024L

    const val MAX_SECTION_ROWS = 5_000_000L

    const val MAX_MEDIA_ENTRIES = 10_000

    const val MAX_COMPRESSION_RATIO = 1_000.0
}
