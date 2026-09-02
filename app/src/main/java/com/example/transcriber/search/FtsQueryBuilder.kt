package com.example.transcriber.search

object FtsQueryBuilder {

    private val tokenRegex =
        Regex("""[\p{L}\p{N}]+""")

    /**
     * Converts uncontrolled user input into safe FTS4 prefix tokens.
     *
     * "database normalization"
     * -> "database* AND normalization*"
     *
     * Raw MATCH operators and punctuation are never forwarded.
     */
    fun build(userInput: String): String? {
        val tokens = tokenRegex
            .findAll(userInput)
            .map { it.value }
            .filter { it.isNotBlank() }
            .take(12)
            .toList()

        if (tokens.isEmpty()) return null

        return tokens.joinToString(" AND ") {
            "$it*"
        }
    }

    fun displayTokens(
        userInput: String
    ): List<String> = tokenRegex
        .findAll(userInput)
        .map { it.value }
        .filter { it.isNotBlank() }
        .take(12)
        .toList()
}
