package com.example.transcriber.settings

data class TranscriptionLanguage(
    val code: String,
    val label: String
)

object LanguageCatalog {
    val all = listOf(
        TranscriptionLanguage("auto", "Auto detect"),
        TranscriptionLanguage("en", "English"),
        TranscriptionLanguage("hi", "Hindi"),
        TranscriptionLanguage("es", "Spanish"),
        TranscriptionLanguage("fr", "French"),
        TranscriptionLanguage("de", "German"),
        TranscriptionLanguage("it", "Italian"),
        TranscriptionLanguage("pt", "Portuguese"),
        TranscriptionLanguage("ja", "Japanese"),
        TranscriptionLanguage("ko", "Korean"),
        TranscriptionLanguage("zh", "Chinese"),
        TranscriptionLanguage("ar", "Arabic"),
        TranscriptionLanguage("ru", "Russian"),
        TranscriptionLanguage("bn", "Bengali"),
        TranscriptionLanguage("mr", "Marathi"),
        TranscriptionLanguage("ta", "Tamil"),
        TranscriptionLanguage("te", "Telugu"),
        TranscriptionLanguage("ur", "Urdu"),
        TranscriptionLanguage("id", "Indonesian"),
        TranscriptionLanguage("tr", "Turkish")
    )

    fun byCode(code: String): TranscriptionLanguage =
        all.firstOrNull { it.code == code } ?: all.first()
}
