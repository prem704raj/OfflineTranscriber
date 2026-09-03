package app.offlinetranscriber.mobile.study.nano

sealed interface NanoFeatureState {
    data object Checking : NanoFeatureState
    data object Unavailable : NanoFeatureState
    data object Downloadable : NanoFeatureState
    data object Downloading : NanoFeatureState
    data object Available : NanoFeatureState
    data class Error(val message: String) : NanoFeatureState
}
