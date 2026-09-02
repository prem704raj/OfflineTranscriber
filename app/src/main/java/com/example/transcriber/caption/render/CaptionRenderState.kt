package com.example.transcriber.caption.render

import com.example.transcriber.caption.model.CaptionStyle

data class CaptionRenderState(
    val style: CaptionStyle,
    val revision: Long
)
