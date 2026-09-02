package com.example.transcriber.ask

data class ParsedNanoAnswer(
    val answer: String,
    val citationLabels: List<String>
)

object NanoAskParser {

    fun parse(
        raw: String
    ): ParsedNanoAnswer? {

        val answer =
            raw
                .lineSequence()
                .firstOrNull {
                    it.startsWith("ANSWER|")
                }
                ?.substringAfter("ANSWER|")
                ?.trim()
                ?.takeIf {
                    it.length >= 3
                }
                ?: return null

        val labels =
            raw
                .lineSequence()
                .firstOrNull {
                    it.startsWith("CITE|")
                }
                ?.substringAfter("CITE|")
                ?.split(",")
                ?.map {
                    it.trim().uppercase()
                }
                ?.filter {
                    it.matches(Regex("""S[0-9]{1,2}"""))
                }
                ?.distinct()
                .orEmpty()

        return ParsedNanoAnswer(
            answer = answer
                .replace(Regex("""\s+"""), " ")
                .take(2_500),
            citationLabels = labels.take(8)
        )
    }
}
