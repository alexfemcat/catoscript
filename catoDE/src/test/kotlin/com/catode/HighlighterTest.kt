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
        assertEquals(1, spans.size)
        assertEquals(TokenKind.COMMENT, spans[0].kind)
        assertEquals(5..14, spans[0].range)
    }

    @Test
    fun `no comment returns empty list`() {
        val spans = Highlighter.tokenize("meow \"hi\"")
        assertEquals(emptyList<TokenSpan>(), spans)
    }
    @Test
    fun `label at start of line is found`() {
        val text = ":DEAD\nmeow"
        val spans = Highlighter.tokenize(text)
        assertEquals(1, spans.size)
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
        assertEquals(emptyList<TokenSpan>(), spans)
    }
}