package com.example.transcriber.settings

data class AppSettings(
    val selectedModelId: String = "",
    val languageCode: String = "auto",
    val onboardingComplete: Boolean = false
)
