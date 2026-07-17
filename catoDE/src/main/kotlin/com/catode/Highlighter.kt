package com.catode

// TokenKind is the closed set of categories a stripe of catoscript text can be.
// The Highlighter emits one of these per TokenSpan; EditorPane will `when` on this
// exhaustively to pick a color.
enum class TokenKind {
    COMMENT,
    STRING,
    LABEL,
    VARIABLE,
    NUMBER,
    KEYWORD_CONTROL,
    KEYWORD_CONTROL_FILE,
    KEYWORD_VISUAL,
    KEYWORD_SYSTEM,
    KEYWORD_NEURAL,
    KEYWORD_AUDIO,
    KEYWORD_ACTION,
    KEYWORD_MEOW,
    KEYWORD_OTHER,
}

// A TokenSpan is one colored stripe in the editor: a range of source-text
// offsets and the kind of token that lives there. Both fields travel together
// because no consumer wants one without the other.
data class TokenSpan(
    val range: IntRange,
    val kind: TokenKind,
)

object HighLighter {
    private val commentRegex: Regex = Regex("#.*")

    fun tokenize(text: String): List<TokenSpan> {
        val spans : MutableList<TokenSpan> = mutableListOf()
        for (match in commentRegex.findAll(text)) {
            spans.add(TokenSpan(match.range, TokenKind.COMMENT))
        }
        return spans
    }
}




