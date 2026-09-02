package com.example.transcriber.caption.preview

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.transcriber.caption.model.CaptionCue
import com.example.transcriber.caption.model.CaptionStyle
import com.example.transcriber.caption.render.CaptionEffectFactory
import com.example.transcriber.caption.render.CaptionStyleProvider

@OptIn(UnstableApi::class)
class CaptionPreviewController(
    private val player: ExoPlayer
) {

    private var styleProvider: CaptionStyleProvider? = null

    fun attach(
        cues: List<CaptionCue>,
        initialStyle: CaptionStyle
    ) {
        val provider = CaptionStyleProvider(initialStyle)
        styleProvider = provider

        player.setVideoEffects(
            listOf(
                CaptionEffectFactory.create(
                    cues = cues,
                    styleProvider = provider
                )
            )
        )
    }

    fun updateStyle(
        style: CaptionStyle
    ) {
        styleProvider?.update(style)
    }

    fun detach() {
        player.setVideoEffects(emptyList())
        styleProvider = null
    }
}
