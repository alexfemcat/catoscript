package com.catoscript.parser

import com.catoscript.lexer.Token
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertEquals


class ParserTest {

    @Test
    fun `parseLine returns Empty for blank input`() {
        assertEquals(Token.Empty, parseLine(""))
    }
    @Test
    fun `parseLine returns Comment for a comment line`() {
        assertEquals(Token.Comment, parseLine("# a comment"))
    }
    @Test
    fun `parseLine returns Command for a command line`() {
        val result = parseLine("meow 1 2")
        assertEquals(Token.Command("meow", listOf("1", "2")), result)
    }
}