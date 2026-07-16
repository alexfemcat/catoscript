package com.catoscript.parser

import kotlin.test.Test

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
}