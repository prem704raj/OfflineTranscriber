package app.offlinetranscriber.mobile.settings

data class AppSettings(
    val selectedModelId: String = "",
    val languageCode: String = "auto",
    val onboardingComplete: Boolean = false
)
