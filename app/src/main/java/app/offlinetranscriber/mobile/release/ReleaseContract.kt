package app.offlinetranscriber.mobile.release

import app.offlinetranscriber.mobile.BuildConfig

object ReleaseContract {

    fun requireSafeReleaseBuild() {
        if (BuildConfig.DEBUG) {
            return
        }

        check(!BuildConfig.APPLICATION_ID.contains(".debug", ignoreCase = true)) {
            "Release applicationId contains a debug suffix."
        }

        check(BuildConfig.BUILD_TYPE == "release") {
            "Expected release build type but found: ${BuildConfig.BUILD_TYPE}"
        }
    }
}
