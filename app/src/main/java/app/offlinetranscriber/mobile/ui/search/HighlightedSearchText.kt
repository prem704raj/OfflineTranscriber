package app.offlinetranscriber.mobile.ui.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import app.offlinetranscriber.mobile.search.FtsQueryBuilder

@Composable
fun HighlightedSearchText(
    text: String,
    query: String
) {
    val tokens = FtsQueryBuilder
        .displayTokens(query)
        .map { it.lowercase() }
        .filter { it.isNotBlank() }

    if (tokens.isEmpty()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
        return
    }

    val annotated = buildHighlightedString(
        text = text,
        tokens = tokens,
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            background = MaterialTheme.colorScheme.primaryContainer
        )
    )

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge
    )
}

private fun buildHighlightedString(
    text: String,
    tokens: List<String>,
    style: SpanStyle
): AnnotatedString {
    val lower = text.lowercase()

    val matches = mutableListOf<IntRange>()

    tokens.forEach { token ->
        var start = 0

        while (start < lower.length) {
            val index = lower.indexOf(
                string = token,
                startIndex = start
            )

            if (index < 0) break

            matches += index until (index + token.length)
            start = index + token.length
        }
    }

    return buildAnnotatedString {
        append(text)

        matches.forEach { range ->
            if (
                range.first >= 0 &&
                range.last < text.length
            ) {
                addStyle(
                    style = style,
                    start = range.first,
                    end = range.last + 1
                )
            }
        }
    }
}
