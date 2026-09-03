package app.offlinetranscriber.mobile.caption.preview

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import app.offlinetranscriber.mobile.caption.model.CaptionCue
import app.offlinetranscriber.mobile.caption.model.CaptionStyle
import app.offlinetranscriber.mobile.caption.render.CaptionEffectFactory
import app.offlinetranscriber.mobile.caption.render.CaptionStyleProvider

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
