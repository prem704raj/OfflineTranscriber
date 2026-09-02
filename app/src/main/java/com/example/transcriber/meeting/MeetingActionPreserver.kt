package com.example.transcriber.meeting

import com.example.transcriber.data.model.MeetingActionEntity
import com.example.transcriber.meeting.model.GeneratedMeetingAction
import com.example.transcriber.meeting.model.MeetingActionStatus
import java.util.Locale

data class PersistableGeneratedAction(
    val generated: GeneratedMeetingAction,
    val status: MeetingActionStatus,
    val manuallyEdited: Boolean
)

object MeetingActionPreserver {

    fun merge(
        old: List<MeetingActionEntity>,
        generated: List<GeneratedMeetingAction>
    ): List<PersistableGeneratedAction> {
        val usedOld = mutableSetOf<Long>()

        val output = generated.map { fresh ->
            val match = old
                .filterNot { it.id in usedOld }
                .map { previous -> previous to similarity(previous.text, fresh.text) }
                .maxByOrNull { it.second }
                ?.takeIf { it.second >= 0.72 }
                ?.first

            if (match != null) {
                usedOld += match.id
            }

            PersistableGeneratedAction(
                generated = fresh,
                status = if (match?.status == MeetingActionStatus.DONE.name) {
                    MeetingActionStatus.DONE
                } else {
                    MeetingActionStatus.OPEN
                },
                manuallyEdited = match?.manuallyEdited ?: false
            )
        }.toMutableList()

        old.filter { it.manuallyEdited && it.id !in usedOld }
            .forEach { manual ->
                output += PersistableGeneratedAction(
                    generated = GeneratedMeetingAction(
                        text = manual.text,
                        assignee = manual.assignee,
                        dueText = manual.dueText,
                        sourceSegmentId = manual.sourceSegmentId,
                        startMs = manual.startMs
                    ),
                    status = runCatching {
                        MeetingActionStatus.valueOf(manual.status)
                    }.getOrDefault(MeetingActionStatus.OPEN),
                    manuallyEdited = true
                )
            }

        return output.take(20)
    }

    private fun similarity(
        a: String,
        b: String
    ): Double {
        val one = tokenSet(a)
        val two = tokenSet(b)

        if (one.isEmpty() || two.isEmpty()) {
            return 0.0
        }

        return one.intersect(two).size.toDouble() / one.union(two).size.toDouble()
    }

    private fun tokenSet(
        value: String
    ): Set<String> =
        Regex("""[\p{L}\p{N}]{2,}""")
            .findAll(value.lowercase(Locale.ROOT))
            .map { it.value }
            .toSet()
}
