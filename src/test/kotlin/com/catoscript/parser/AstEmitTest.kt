package com.catoscript.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.catoscript.ast.Program
import kotlinx.serialization.json.Json

class AstEmitTest {
    @Test
    fun `emit prints sample 3 cato JSON for eyeball`() {
        val source = """
            set ${'$'}hp 5
            sniff ${'$'}hp > 1
            purr_at :DEAD
            meow "still alive"
            jump :END
            :DEAD
            meow "game over"
            :END
        """.trimIndent()
        val program = Parser.parse(source)
        val json = emit(program)
        println("--- begin sample/3.cato.json ---")
        println(json)
        println("--- end ---")
    }
    @Test
    fun `emit round-trips through parse`() {
        val source = """
            set ${'$'}hp 5
            sniff ${'$'}hp > 1
            purr_at :DEAD
            meow "still alive"
            jump :END
            :DEAD
            meow "game over"
            :END
        """.trimIndent()
        val original = Parser.parse(source)
        val json = emit(original)
        val decoded = Json.decodeFromString<Program>(json)
        assertEquals(original, decoded)
    }
    @Test
    fun `emit produces a known JSON shape for hello`() {
        val source = """meow "hello""""
        val program = Parser.parse(source)
        val json = emit(program)
        assertTrue(json.contains("\"Meow\""))
        assertTrue(json.contains("\"hello\""))
        assertTrue(json.startsWith("{"))
    }
}