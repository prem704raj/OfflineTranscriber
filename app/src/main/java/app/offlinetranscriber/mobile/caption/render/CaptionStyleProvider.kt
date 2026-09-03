package app.offlinetranscriber.mobile.caption.render

import app.offlinetranscriber.mobile.caption.model.CaptionStyle
import java.util.concurrent.atomic.AtomicReference

class CaptionStyleProvider(
    initial: CaptionStyle
) {

    private data class Holder(
        val style: CaptionStyle,
        val revision: Long
    )

    private val ref = AtomicReference(Holder(initial, 0L))

    fun current(): CaptionRenderState {
        val value = ref.get()
        return CaptionRenderState(
            style = value.style,
            revision = value.revision
        )
    }

    fun update(value: CaptionStyle) {
        while (true) {
            val old = ref.get()
            val next = Holder(
                style = value,
                revision = old.revision + 1L
            )
            if (ref.compareAndSet(old, next)) {
                return
            }
        }
    }
}
