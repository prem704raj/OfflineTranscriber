package app.offlinetranscriber.mobile.meeting

import app.offlinetranscriber.mobile.data.repository.MeetingPackState

object MeetingNotesFormatter {

    fun format(
        title: String,
        value: MeetingPackState
    ): String = buildString {
        appendLine(title)
        appendLine("=".repeat(title.length.coerceIn(3, 60)))
        appendLine()

        appendLine("SUMMARY")
        appendLine(value.pack.summary)
        appendLine()

        appendLine("ACTION ITEMS")
        if (value.actions.isEmpty()) {
            appendLine("- None identified")
        } else {
            value.actions.forEach { action ->
                val marker = if (action.status == "DONE") "[x]" else "[ ]"
                append("- $marker ${action.text}")
                if (action.assignee.isNotBlank()) {
                    append(" — ${action.assignee}")
                }
                if (action.dueText.isNotBlank()) {
                    append(" — Due: ${action.dueText}")
                }
                appendLine(" @ ${formatTimestamp(action.startMs)}")
            }
        }
        appendLine()

        appendLine("DECISIONS")
        if (value.decisions.isEmpty()) {
            appendLine("- None identified")
        } else {
            value.decisions.forEach { item ->
                appendLine("- ${item.text} @ ${formatTimestamp(item.startMs)}")
            }
        }
        appendLine()

        appendLine("OPEN QUESTIONS")
        if (value.questions.isEmpty()) {
            appendLine("- None identified")
        } else {
            value.questions.forEach { item ->
                appendLine("- ${item.text} @ ${formatTimestamp(item.startMs)}")
            }
        }
        appendLine()

        appendLine("TOPIC TIMELINE")
        if (value.topics.isEmpty()) {
            appendLine("- None identified")
        } else {
            value.topics.forEach { item ->
                appendLine("- ${formatTimestamp(item.startMs)} — ${item.title}")
            }
        }
        appendLine()

        appendLine("Generated locally by Offline Transcriber.")
    }

    fun formatTimestamp(ms: Long): String {
        val total = (ms / 1000L).coerceAtLeast(0L)
        val h = total / 3600L
        val m = (total % 3600L) / 60L
        val s = total % 60L

        return if (h > 0) {
            "%d:%02d:%02d".format(h, m, s)
        } else {
            "%d:%02d".format(m, s)
        }
    }
}
