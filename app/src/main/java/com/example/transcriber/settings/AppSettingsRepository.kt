package com.example.transcriber.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(
    name = "app_settings"
)

class AppSettingsRepository(
    private val context: Context
) {
    private object Keys {
        val selectedModel =
            stringPreferencesKey("selected_whisper_model")
        val language =
            stringPreferencesKey("transcription_language")
        val onboarding =
            booleanPreferencesKey("onboarding_complete")
    }

    val settings: Flow<AppSettings> =
        context.appSettingsDataStore.data.map { prefs ->
            AppSettings(
                selectedModelId =
                    prefs[Keys.selectedModel].orEmpty(),
                languageCode =
                    prefs[Keys.language] ?: "auto",
                onboardingComplete =
                    prefs[Keys.onboarding] ?: false
            )
        }

    suspend fun setSelectedModel(id: String) {
        context.appSettingsDataStore.edit {
            it[Keys.selectedModel] = id
        }
    }

    suspend fun setLanguage(code: String) {
        require(
            LanguageCatalog.all.any { it.code == code }
        ) { "Unsupported language code: $code" }
        context.appSettingsDataStore.edit {
            it[Keys.language] = code
        }
    }

    suspend fun setOnboardingComplete(
        complete: Boolean
    ) {
        context.appSettingsDataStore.edit {
            it[Keys.onboarding] = complete
        }
    }
}
