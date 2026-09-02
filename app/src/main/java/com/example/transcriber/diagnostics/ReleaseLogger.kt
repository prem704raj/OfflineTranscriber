package com.example.transcriber.diagnostics

import android.util.Log
import com.example.transcriber.BuildConfig

object ReleaseLogger {

    fun d(
        tag: String,
        message: String
    ) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, sanitize(message))
        }
    }

    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        if (BuildConfig.DEBUG) {
            Log.w(
                tag,
                sanitize(message),
                throwable
            )
        }
    }

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        if (BuildConfig.DEBUG) {
            Log.e(
                tag,
                sanitize(message),
                throwable
            )
        }
    }

    /**
     * Do not pass transcript text, prompts, purchase tokens,
     * source URIs, file names, emails, or user content here.
     *
     * This sanitizer is a secondary guard.
     */
    fun sanitize(
        value: String
    ): String =
        value
            .replace(
                Regex(
                    """(?i)(purchaseToken|token|uri|path|prompt|transcript)\s*[:=]\s*\S+"""
                ),
                "$1=[redacted]"
            )
            .take(700)
}
