package com.example.transcriber.caption.source

import com.example.transcriber.caption.model.CaptionProjectSnapshot

interface CaptionProjectProvider {

    suspend fun load(
        transcriptId: Long,
        includeSpeakers: Boolean
    ): CaptionProjectSnapshot
}
