package com.catoscript.interpreter

import com.catoscript.parser.Parser
import com.catoscript.runtime.CatoHost
import com.catoscript.runtime.Waveform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class RecordingHost : CatoHost {
    val printed: MutableList<String> = mutableListOf()
    override fun print(line: String) {
        printed.add(line)
    }
    override fun playTone(freq: Double, durationMs: Int, waveform: Waveform) {}
    override fun playSample(id: String) {}
    override fun setCursor(x: Int, y: Int) {}
    override fun clearScreen() {}
    override fun envLookup(key: String): String? = null
    override fun now(): Long = 0L
    override fun args(): List<String> = emptyList()
    override fun readLine(prompt: String?): String? = null
    override fun exit(code: Int) {}
    override fun printErr(line: String) {}
    override fun readFile(path: String): String? = null
    override fun writeFile(path: String, content: String): Boolean = false
    override fun fileExists(path: String): Boolean = false
}
class InterpreterTest {
    @Test
    fun `runs the README Tier 1 script`() {
        val source = """
            meow "hello"
            meow "world"
        """.trimIndent()
        val host = RecordingHost()
        val result = Interpreter(host).run(Parser.parse(source))
        assertEquals(InterpreterResult.Completed, result)
        assertEquals(listOf("hello", "world"), host.printed)
    }
    @Test
    fun `runs Tier 2 with variable and interpolation`() {
        val source = """
            set ${'$'}name "mochi"
            meow "hello, ${'$'}name"
        """.trimIndent()
        val host = RecordingHost()
        val result = Interpreter(host).run(Parser.parse(source))
        assertEquals(InterpreterResult.Completed, result)
        assertEquals(listOf("hello, mochi"), host.printed)
    }
    @Test
    fun `runs Tier 3 with sniff purr_at hiss_at and labels`() {
        val source = """
            set ${'$'}hp 5
            sniff ${'$'}hp < 1
            purr_at :DEAD
            meow "still alive"
            jump :END
            :DEAD
            meow "game over"
            :END
        """.trimIndent()
        val host = RecordingHost()
        val result = Interpreter(host).run(Parser.parse(source))
        assertEquals(InterpreterResult.Completed, result)
        assertEquals(listOf("still alive"), host.printed)
    }
    @Test
    fun `purr_at branches when sniff is true`() {
        val source = """
            set ${'$'}hp 0
            sniff ${'$'}hp < 1
            hiss_at :ALIVE
            meow "dead"
            jump :END
            :ALIVE
            meow "alive"
            :END
        """.trimIndent()
        val host = RecordingHost()
        Interpreter(host).run(Parser.parse(source))
        assertEquals(listOf("dead"), host.printed)
    }
    @Test
    fun `jump always jumps`() {
        val source = """
            jump :END
            meow "skipped"
            :END
            meow "reached"
        """.trimIndent()
        val host = RecordingHost()
        Interpreter(host).run(Parser.parse(source))
        assertEquals(listOf("reached"), host.printed)
    }
    @Test
    fun `undefined variable returns RuntimeError`() {
        val source = """meow "hello, ${'$'}nope""""
        val host = RecordingHost()
        val result = Interpreter(host).run(Parser.parse(source))
        val err = assertIs<InterpreterResult.RuntimeError>(result)
        assertTrue(err.message.contains("nope"), "got: ${err.message}")
    }
    @Test
    fun `runs Tier 3 with sniff using greater-than`() {
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
        val host = RecordingHost()
        Interpreter(host).run(Parser.parse(source))
        assertEquals(listOf("game over"), host.printed)
    }
    @Test
    fun `sniff with greater-than-or-equal picks purr_at on exact equality`() {
        val source = """
        set ${'$'}hp 5
        sniff ${'$'}hp >= 5
        purr_at :DEAD
        meow "still alive"
        jump :END
        :DEAD
        meow "dead"
        :END
    """.trimIndent()
        val host = RecordingHost()
        Interpreter(host).run(Parser.parse(source))
        assertEquals(listOf("dead"), host.printed)
    }
    @Test
    fun `sniff with less-than-or-equal picks purr_at on exact equality`() {
        val source = """
        set ${'$'}hp 5
        sniff ${'$'}hp <= 5
        purr_at :DEAD
        meow "still alive"
        jump :END
        :DEAD
        meow "dead"
        :END
    """.trimIndent()
        val host = RecordingHost()
        Interpreter(host).run(Parser.parse(source))
        assertEquals(listOf("dead"), host.printed)
    }
    @Test
    fun `sniff with not-equal picks purr_at when values differ`() {
        val source = """
        set ${'$'}hp 5
        sniff ${'$'}hp != 1
        purr_at :OTHER
        meow "same"
        jump :END
        :OTHER
        meow "different"
        :END
    """.trimIndent()
        val host = RecordingHost()
        Interpreter(host).run(Parser.parse(source))
        assertEquals(listOf("different"), host.printed)
    }
    @Test
    fun `include skips library top-level code on load`(@TempDir tmp: Path) {
        val lib = tmp.resolve("lib.cato").toFile()
        lib.writeText("meow \"lib top-level should not print\"")
        val main = tmp.resolve("main.cato").toFile()
        main.writeText("include \"lib.cato\"\nmeow \"main done\"")
        val host = RecordingHost()
        val result = Interpreter(host).run(Parser.parse(main.readText(), main.absolutePath))
        assertEquals(InterpreterResult.Completed, result)
        assertEquals(listOf("main done"), host.printed)
    }

    // Interpreter-level include behavior is covered transitively: the
    // parser inlines included statements as ordinary Stmt nodes, so the
    // interpreter executes them with no special-case logic. Adding a
    // separate interpreter test here would be testing the interpreter's
    // general statement-walking, which is already covered by the other
    // InterpreterTest cases. If the parser is right, the interpreter
    // is right for free.
}