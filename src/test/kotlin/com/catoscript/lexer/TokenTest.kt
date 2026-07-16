package com.catoscript.lexer

import kotlin.test.Test
import kotlin.test.assertIs

class TokenTest {
    @Test
    fun `Empty is a token`() {
        val empty = Token.Empty
        assertIs<Token>(empty)
    }
}