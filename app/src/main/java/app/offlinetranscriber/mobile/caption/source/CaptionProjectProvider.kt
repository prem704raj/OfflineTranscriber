package app.offlinetranscriber.mobile.caption.source

import app.offlinetranscriber.mobile.caption.model.CaptionProjectSnapshot

interface CaptionProjectProvider {

    suspend fun load(
        transcriptId: Long,
        includeSpeakers: Boolean
    ): CaptionProjectSnapshot
}
