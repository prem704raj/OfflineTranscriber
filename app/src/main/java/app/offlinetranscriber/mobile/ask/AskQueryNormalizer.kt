package app.offlinetranscriber.mobile.ask

import app.offlinetranscriber.mobile.search.FtsQueryBuilder

object AskQueryNormalizer {

    const val MAX_QUESTION_CHARS = 300

    fun clean(
        raw: String
    ): String =
        raw
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()
            .take(MAX_QUESTION_CHARS)

    fun ftsQuery(
        raw: String
    ): String? {
        val value = clean(raw)

        if (value.length < 2) return null

        return FtsQueryBuilder
            .build(value)
            ?.takeIf {
                it.isNotBlank()
            }
    }
}
