package com.catode

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

fun colorFor(kind: TokenKind): Color = when (kind) {
    TokenKind.COMMENT -> Color(0xFF6A9955)
    TokenKind.STRING -> Color(0xFFCE9178)
    TokenKind.LABEL -> Color(0xFFDCDCAA)
    TokenKind.VARIABLE -> Color(0xFF9CDCFE)
    TokenKind.NUMBER -> Color(0xFFB5CEA8)
    TokenKind.KEYWORD_CONTROL -> Color(0xFFC586C0)
    TokenKind.KEYWORD_CONTROL_FILE -> Color(0xFFC586C0)
    TokenKind.KEYWORD_VISUAL -> Color(0xFF569CD6)
    TokenKind.KEYWORD_SYSTEM -> Color(0xFF569CD6)
    TokenKind.KEYWORD_NEURAL -> Color(0xFF569CD6)
    TokenKind.KEYWORD_AUDIO -> Color(0xFF569CD6)
    TokenKind.KEYWORD_ACTION -> Color(0xFF569CD6)
    TokenKind.KEYWORD_MEOW -> Color(0xFF4EC9B0)
    TokenKind.KEYWORD_OTHER -> Color(0xFF569CD6)
}

fun highlight(text: String): AnnotatedString {
    val tokens = Highlighter.tokenize(text)
    return buildAnnotatedString {
        append(text)
        for (token in tokens) {
            addStyle(
                SpanStyle(color = colorFor(token.kind)),
                token.range.first,
                token.range.last + 1,
            )
        }
    }
}