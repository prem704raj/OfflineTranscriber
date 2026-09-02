package com.example.transcriber.meeting.nano

object MeetingNanoPrompt {

    val system =
        """
You are a private on-device meeting analysis engine.

Use ONLY supplied transcript evidence.
Never add outside knowledge.
Never infer identity from voice.
Only return an assignee when explicitly stated.
Only return a due phrase when explicitly stated.
A decision must be actually agreed, approved, selected, confirmed or finalized.
An action must be actionable.
Every insight must cite a valid supplied source label.
When evidence is weak, omit the item instead of guessing.
        """.trimIndent()

    fun prompt(
        evidence: String
    ) =
        """
Analyze this meeting transcript chunk.

Extract:
- concise summary
- explicit decisions
- action items
- unresolved questions
- major topics

Evidence:
$evidence
        """.trimIndent()

    fun protocolPrompt(
        evidence: String
    ) =
        """
Analyze this meeting transcript chunk and output in strict protocol format:
SUMMARY|concise overview of the meeting chunk
SUMMARY_SOURCES|S1,S2
ACTION|task text|assignee-or-empty|due-or-empty|S3
DECISION|agreed decision|S4
QUESTION|unresolved question|S5
TOPIC|major topic title|S6

Rules:
- Use ONLY evidence from the transcript below.
- Do not infer identity or speakers.
- Assignee and due date must be empty if not explicitly stated.
- Cite valid source labels (e.g. S1, S2) matching the evidence.

Evidence:
$evidence
        """.trimIndent()
}
