package com.catoscript.interpreter

import com.catoscript.parser.Parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BasketInterpreterTest {

    @Test
    fun `declare and single-arg call`() {
        val source = """
              greet("mochi")
              basket greet ${'$'}name
                meow "hello, ${'$'}name"
                return
              end_basket
          """.trimIndent()
        val host = RecordingHost()
        val result = Interpreter(host).run(Parser.parse(source))
        assertEquals(InterpreterResult.Completed, result)
        assertEquals(listOf("hello, mochi"), host.printed)
    }

    @Test
    fun `multi-arg call binds in declaration order`() {
        val source = """
              combo("x", "y")
              basket combo ${'$'}a ${'$'}b
                meow "${'$'}a ${'$'}b"
                return
              end_basket
          """.trimIndent()
        val host = RecordingHost()
        val result = Interpreter(host).run(Parser.parse(source))
        assertEquals(InterpreterResult.Completed, result)
        assertEquals(listOf("x y"), host.printed)
    }

    @Test
    fun `return resumes caller`() {
        val source = """
              greet("mochi")
              meow "between"
              greet("luna")
              meow "done"
              basket greet ${'$'}name
                meow "hello, ${'$'}name"
                return
              end_basket
          """.trimIndent()
        val host = RecordingHost()
        val result = Interpreter(host).run(Parser.parse(source))
        assertEquals(InterpreterResult.Completed, result)
        assertEquals(listOf("hello, mochi", "between", "hello, luna", "done"), host.printed)
    }

    @Test
    fun `arity mismatch throws RuntimeError`() {
        val source = """
              greet("mochi", "extra")
              basket greet ${'$'}name
                meow "hello, ${'$'}name"
                return
              end_basket
          """.trimIndent()
        val host = RecordingHost()
        val result = Interpreter(host).run(Parser.parse(source))
        val err = assertIs<InterpreterResult.RuntimeError>(result)
        assertTrue(
            "arity" in err.message || "expected" in err.message,
            "expected arity-mismatch message, got: ${err.message}",
        )
    }
}