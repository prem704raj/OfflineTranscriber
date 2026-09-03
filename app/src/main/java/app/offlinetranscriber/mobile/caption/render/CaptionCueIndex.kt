package app.offlinetranscriber.mobile.caption.render

import app.offlinetranscriber.mobile.caption.model.CaptionCue

class CaptionCueIndex(
    cues: List<CaptionCue>
) {

    private val values = cues.sortedBy { it.startUs }

    fun active(
        presentationTimeUs: Long
    ): CaptionCue? {
        if (values.isEmpty()) return null

        var low = 0
        var high = values.lastIndex
        var candidate = -1

        while (low <= high) {
            val mid = (low + high) ushr 1
            if (values[mid].startUs <= presentationTimeUs) {
                candidate = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        if (candidate < 0) return null

        /*
         * Search a bounded neighborhood backwards because timing edits may overlap.
         * Prefer the latest-starting cue active at this timestamp.
         */
        var index = candidate
        var checked = 0

        while (index >= 0 && checked < 8) {
            val cue = values[index]
            if (presentationTimeUs >= cue.startUs && presentationTimeUs < cue.endUs) {
                return cue
            }
            index--
            checked++
        }

        return null
    }
}
