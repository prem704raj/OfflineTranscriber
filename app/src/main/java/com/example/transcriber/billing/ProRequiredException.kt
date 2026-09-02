package com.example.transcriber.billing

class ProRequiredException(
    val feature: ProFeature
) : IllegalStateException(
    "$feature requires Offline Transcriber Pro."
)
