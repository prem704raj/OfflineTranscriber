package app.offlinetranscriber.mobile.backup.settings

import android.util.JsonReader
import app.offlinetranscriber.mobile.settings.AppSettingsRepository
import java.util.zip.ZipFile

class PortableSettingsRestorer(
    private val settingsRepository: AppSettingsRepository
) {

    suspend fun restoreWhitelisted(zip: ZipFile) {
        val entry = zip.getEntry("settings/portable_settings.json") ?: return

        runCatching {
            var languageCode: String? = null
            var onboardingComplete: Boolean? = null

            zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginObject()
                while (json.hasNext()) {
                    when (json.nextName()) {
                        "languageCode" -> {
                            if (json.peek() != android.util.JsonToken.NULL) {
                                languageCode = json.nextString()
                            } else json.nextNull()
                        }
                        "onboardingComplete" -> {
                            if (json.peek() != android.util.JsonToken.NULL) {
                                onboardingComplete = json.nextBoolean()
                            } else json.nextNull()
                        }
                        else -> json.skipValue()
                    }
                }
                json.endObject()
            }

            if (!languageCode.isNullOrBlank()) {
                val code = languageCode
                if (code != null) {
                    runCatching { settingsRepository.setLanguage(code) }
                }
            }
            if (onboardingComplete != null) {
                val complete = onboardingComplete
                if (complete != null) {
                    settingsRepository.setOnboardingComplete(complete)
                }
            }
        }
    }
}
