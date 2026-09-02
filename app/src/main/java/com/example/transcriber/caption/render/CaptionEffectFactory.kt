package com.example.transcriber.caption.render

import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextureOverlay
import com.example.transcriber.caption.model.CaptionCue
import com.google.common.collect.ImmutableList

@OptIn(UnstableApi::class)
object CaptionEffectFactory {

    fun create(
        cues: List<CaptionCue>,
        styleProvider: CaptionStyleProvider
    ): Effect {
        val overlay: TextureOverlay = DynamicCaptionCanvasOverlay(
            cues = cues,
            styleProvider = styleProvider
        )

        return OverlayEffect(
            ImmutableList.of(overlay)
        )
    }
}
