package app.offlinetranscriber.mobile.shortcuts

import app.offlinetranscriber.mobile.billing.ProFeature

sealed interface ExternalAppCommand {
    data object Record : ExternalAppCommand
    data object ImportAudio : ExternalAppCommand
    data object OpenQueue : ExternalAppCommand
    data object OpenModels : ExternalAppCommand
    data class OpenPaywall(val feature: ProFeature?) : ExternalAppCommand
}
