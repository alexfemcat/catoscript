package com.catode

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class HighlighterTest {

    @Test
    fun `comment at start produces one COMMENT span`() {
        val text = "# hello"
        val spans = Highlighter.tokenize(text)
        assertEquals(1, spans.size)
        assertEquals(TokenKind.COMMENT, spans[0].kind)
        assertEquals(0..6, spans[0].range)
    }

    @Test
    fun `comment after code is still found`() {
        val text = "meow # trailing"
        val spans = Highlighter.tokenize(text)
        assertEquals(2, spans.size)
        assertEquals(TokenKind.COMMENT, spans[0].kind)
        assertEquals(5..14, spans[0].range)
    }

    @Test
    fun `no comment returns no COMMENT span`() {
        val spans = Highlighter.tokenize("meow \"hi\"")
        assertEquals(true, spans.none { it.kind == TokenKind.COMMENT })
    }
    @Test
    fun `label at start of line is found`() {
        val text = ":DEAD\nmeow"
        val spans = Highlighter.tokenize(text)
        assertEquals(2, spans.size)
        assertEquals(TokenKind.LABEL, spans[0].kind)
        assertEquals(0..4, spans[0].range)
    }

    @Test
    fun `label with leading spaces is found`() {
        val text = "  :END"
        val spans = Highlighter.tokenize(text)
        assertEquals(1, spans.size)
        assertEquals(TokenKind.LABEL, spans[0].kind)
        assertEquals(0..5, spans[0].range)
    }

    @Test
    fun `no label returns no LABEL span`() {
        val spans = Highlighter.tokenize("meow")
        assertEquals(true, spans.none { it.kind == TokenKind.LABEL })
    }
    @Test
    fun `double-quoted string is one STRING span`() {
        val spans = Highlighter.tokenize("meow \"hi\"")
        val stringSpan = spans.first { it.kind == TokenKind.STRING }
        assertEquals(5..8, stringSpan.range)
    }

    @Test
    fun `meow is a KEYWORD_MEOW span`() {
        val spans = Highlighter.tokenize("meow")
        assertEquals(1, spans.size)
        assertEquals(TokenKind.KEYWORD_MEOW, spans[0].kind)
        assertEquals(0..3, spans[0].range)
    }

    @Test
    fun `jump is a KEYWORD_CONTROL span`() {
        val spans = Highlighter.tokenize("jump")
        assertEquals(1, spans.size)
        assertEquals(TokenKind.KEYWORD_CONTROL, spans[0].kind)
        assertEquals(0..3, spans[0].range)
    }

    @Test
    fun `include is a KEYWORD_CONTROL_FILE span`() {
        val spans = Highlighter.tokenize("include")
        assertEquals(1, spans.size)
        assertEquals(TokenKind.KEYWORD_CONTROL_FILE, spans[0].kind)
        assertEquals(0..6, spans[0].range)
    }
}