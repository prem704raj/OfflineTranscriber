package com.example.transcriber.meeting.nano

data class ParsedProtocolAction(
    val text: String,
    val assignee: String,
    val dueText: String,
    val sourceLabel: String
)

data class ParsedProtocolDecision(
    val text: String,
    val sourceLabel: String
)

data class ParsedProtocolQuestion(
    val text: String,
    val sourceLabel: String
)

data class ParsedProtocolTopic(
    val title: String,
    val sourceLabel: String
)

data class ParsedProtocolResult(
    val summary: String,
    val summarySources: List<String>,
    val actions: List<ParsedProtocolAction>,
    val decisions: List<ParsedProtocolDecision>,
    val questions: List<ParsedProtocolQuestion>,
    val topics: List<ParsedProtocolTopic>
)

object MeetingProtocolParser {

    private val labelPattern = Regex("""^S\d+$""", RegexOption.IGNORE_CASE)

    fun parse(raw: String): ParsedProtocolResult? {
        if (raw.isBlank()) return null

        var summary: String? = null
        val summarySources = mutableListOf<String>()
        val actions = mutableListOf<ParsedProtocolAction>()
        val decisions = mutableListOf<ParsedProtocolDecision>()
        val questions = mutableListOf<ParsedProtocolQuestion>()
        val topics = mutableListOf<ParsedProtocolTopic>()

        raw.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank()) return@forEach

            val parts = line.split("|").map { it.trim() }
            val tag = parts.firstOrNull()?.uppercase() ?: return@forEach

            when (tag) {
                "SUMMARY" -> {
                    if (parts.size >= 2 && summary == null) {
                        val text = parts.subList(1, parts.size).joinToString("|").trim()
                        if (text.isNotBlank()) {
                            summary = text.take(1500)
                        }
                    }
                }
                "SUMMARY_SOURCES" -> {
                    if (parts.size >= 2) {
                        parts[1].split(",")
                            .map { it.trim().uppercase() }
                            .filter { labelPattern.matches(it) }
                            .forEach {
                                if (it !in summarySources && summarySources.size < 4) {
                                    summarySources.add(it)
                                }
                            }
                    }
                }
                "ACTION" -> {
                    if (parts.size >= 5 && actions.size < 8) {
                        val task = parts[1].take(300)
                        val assignee = parts[2].take(80)
                        val due = parts[3].take(80)
                        val label = parts[4].uppercase()
                        if (task.isNotBlank() && labelPattern.matches(label)) {
                            actions.add(
                                ParsedProtocolAction(
                                    text = task,
                                    assignee = assignee,
                                    dueText = due,
                                    sourceLabel = label
                                )
                            )
                        }
                    }
                }
                "DECISION" -> {
                    if (parts.size >= 3 && decisions.size < 6) {
                        val text = parts[1].take(320)
                        val label = parts[2].uppercase()
                        if (text.isNotBlank() && labelPattern.matches(label)) {
                            decisions.add(
                                ParsedProtocolDecision(
                                    text = text,
                                    sourceLabel = label
                                )
                            )
                        }
                    }
                }
                "QUESTION" -> {
                    if (parts.size >= 3 && questions.size < 6) {
                        val text = parts[1].take(320)
                        val label = parts[2].uppercase()
                        if (text.isNotBlank() && labelPattern.matches(label)) {
                            questions.add(
                                ParsedProtocolQuestion(
                                    text = text,
                                    sourceLabel = label
                                )
                            )
                        }
                    }
                }
                "TOPIC" -> {
                    if (parts.size >= 3 && topics.size < 6) {
                        val title = parts[1].take(80)
                        val label = parts[2].uppercase()
                        if (title.isNotBlank() && labelPattern.matches(label)) {
                            topics.add(
                                ParsedProtocolTopic(
                                    title = title,
                                    sourceLabel = label
                                )
                            )
                        }
                    }
                }
            }
        }

        val resolvedSummary = summary ?: return null

        return ParsedProtocolResult(
            summary = resolvedSummary,
            summarySources = summarySources,
            actions = actions,
            decisions = decisions,
            questions = questions,
            topics = topics
        )
    }
}
