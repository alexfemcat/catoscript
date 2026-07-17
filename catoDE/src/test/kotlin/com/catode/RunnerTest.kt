package com.catode

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import com.catoscript.interpreter.InterpreterResult

class RunnerTest {
    @Test
    fun `run captures meow output`() {
        val result = run("meow \"hi\"")
        assertEquals(listOf("hi"), result.lines)
        assertIs<InterpreterResult.Completed>(result.result)
    }
}