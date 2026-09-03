package app.offlinetranscriber.mobile.caption.render

import app.offlinetranscriber.mobile.caption.model.CaptionStyle

data class CaptionRenderState(
    val style: CaptionStyle,
    val revision: Long
)
