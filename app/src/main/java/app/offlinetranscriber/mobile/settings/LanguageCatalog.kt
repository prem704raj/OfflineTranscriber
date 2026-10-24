package app.offlinetranscriber.mobile.settings

data class TranscriptionLanguage(
    val code: String,
    val label: String
)

object LanguageCatalog {
    val all = listOf(
        TranscriptionLanguage("auto", "Auto detect"),
        TranscriptionLanguage("en", "English"),
        TranscriptionLanguage("hi", "Hindi"),
        TranscriptionLanguage("pa", "Punjabi"),
        TranscriptionLanguage("bn", "Bengali"),
        TranscriptionLanguage("mr", "Marathi"),
        TranscriptionLanguage("ta", "Tamil"),
        TranscriptionLanguage("te", "Telugu"),
        TranscriptionLanguage("gu", "Gujarati"),
        TranscriptionLanguage("kn", "Kannada"),
        TranscriptionLanguage("ml", "Malayalam"),
        TranscriptionLanguage("ur", "Urdu"),
        TranscriptionLanguage("ne", "Nepali"),
        TranscriptionLanguage("sa", "Sanskrit"),
        TranscriptionLanguage("as", "Assamese"),
        TranscriptionLanguage("sd", "Sindhi"),
        TranscriptionLanguage("si", "Sinhala"),
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
        TranscriptionLanguage("id", "Indonesian"),
        TranscriptionLanguage("tr", "Turkish")
    )

    private val indicCodes = setOf(
        "hi", "pa", "bn", "mr", "ta", "te", "gu", "kn", "ml", "ur", "ne", "sa", "as", "sd", "si"
    )

    fun isIndic(code: String): Boolean =
        code.lowercase() in indicCodes

    fun byCode(code: String): TranscriptionLanguage =
        all.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: all.first()
}
