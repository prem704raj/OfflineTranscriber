package app.offlinetranscriber.mobile.backup.restore

object BackupZipSafety {

    fun requireSafeName(name: String) {
        require(name.isNotBlank()) { "Entry name cannot be blank." }
        require(!name.startsWith("/")) { "Entry name cannot start with slash: $name" }
        require(!name.contains("\\")) { "Entry name cannot contain backslash: $name" }
        require(!name.contains("\u0000")) { "Entry name cannot contain null byte: $name" }

        val parts = name.split("/")
        require(parts.none { it == ".." || it == "." }) {
            "Entry name contains path traversal component: $name"
        }
    }
}
