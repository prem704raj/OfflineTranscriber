package app.offlinetranscriber.mobile.search

object SearchQueryPolicy {
    fun shouldSearch(raw: String): Boolean {
        val tokens =
            FtsQueryBuilder.displayTokens(raw)

        if (tokens.isEmpty()) return false

        return tokens.any { token ->
            token.length >= 2 ||
                token.all { it.isDigit() }
        }
    }
}
