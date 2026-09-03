package app.offlinetranscriber.mobile.billing

class ProRequiredException(
    val feature: ProFeature
) : IllegalStateException(
    "$feature requires Offline Transcriber Pro."
)
