package com.catoscript.interpreter

import com.catoscript.parser.Parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue



class LabelParamsInterpreterTest {
    @Test
    fun `param binds at call site`() {
        val source = """
              jump :ECHO "hi"
              :ECHO ${'$'}msg
                meow ${'$'}msg
                jump :end
          """.trimIndent()
        val host = RecordingHost()
        val result = Interpreter(host).run(Parser.parse(source))
        assertEquals(InterpreterResult.Completed, result)
        assertEquals(listOf("hi"), host.printed)
    }

    @Test
    fun `two params bind in declaration order`() {
        val source = """
              jump :TWO "x" "y"
              :TWO ${'$'}a ${'$'}b
                meow "${'$'}a ${'$'}b"
                jump :end
          """.trimIndent()
        val host = RecordingHost()
        val result = Interpreter(host).run(Parser.parse(source))
        assertEquals(InterpreterResult.Completed, result)
        assertEquals(listOf("x y"), host.printed)
    }

    @Test
    fun `jump to end returns from caller and continues`() {
        val source = """
              jump :DEEP "ignored"
              :DEEP ${'$'}unused
                meow "in"
                jump :end
              meow "after"
          """.trimIndent()
        val host = RecordingHost()
        val result = Interpreter(host).run(Parser.parse(source))
        assertEquals(InterpreterResult.Completed, result)
        assertEquals(listOf("in", "after"), host.printed)
    }

    @Test
    fun `arity mismatch throws RuntimeError`() {
        val source = """
              jump :F "anything"
              :F
                meow "no params"
                jump :end
          """.trimIndent()
        val host = RecordingHost()
        val result = Interpreter(host).run(Parser.parse(source))
        val err = assertIs<InterpreterResult.RuntimeError>(result)
        assertTrue(
            "arity" in err.message || "expected" in err.message,
            "expected arity-mismatch message, got: ${err.message}",
        )
    }

    @Test
    fun `recursion past maxCallDepth throws RuntimeError`() {
        val source = """
              jump :LOOP "x"
              :LOOP ${'$'}unused
                jump :LOOP "x"
          """.trimIndent()
        val host = RecordingHost()
        val policy = InterpreterPolicy(maxCallDepth = 4)
        val result = Interpreter(host, policy).run(Parser.parse(source))
        val err = assertIs<InterpreterResult.RuntimeError>(result)
        assertTrue(
            "depth" in err.message || "recursive" in err.message,
            "expected depth/recursive message, got: ${err.message}",
        )
    }

    @Test
    fun `naked label call shares caller scope`() {
        // Naked jump (no args, no params on target) is a goto, not a function
        // call — no frame is pushed. set inside the label mutates the caller's
        // scope, so the meow after the jump sees the new value.
        val source = """
              jump :SETX
              :SETX
                set ${'$'}x 5
              meow ${'$'}x
          """.trimIndent()
        val host = RecordingHost()
        val result = Interpreter(host).run(Parser.parse(source))
        assertEquals(InterpreterResult.Completed, result)
        assertEquals(listOf("5"), host.printed)
    }
}