package com.catode

import com.catoscript.interpreter.Interpreter
import com.catoscript.interpreter.InterpreterResult
import com.catoscript.runtime.CatoHost
import com.catoscript.runtime.Waveform

class CapturingHost : CatoHost {
    val lines = mutableListOf<String>()

    override fun print(line: String) {
        lines.add(line)
    }

    // every other CatoHost method: no-op, like ConsoleHost
    override fun playTone(freq: Double, durationMs: Int, waveform: Waveform) {}
    override fun playSample(id: String) {}
    override fun setCursor(x: Int, y: Int) {}
    override fun clearScreen() {}
    override fun envLookup(key: String): String? = null
    override fun now(): Long = System.currentTimeMillis()
    override fun args(): List<String> = emptyList()
    override fun readLine(prompt: String?): String? = null
    override fun exit(code: Int) { kotlin.system.exitProcess(code) }
    override fun printErr(line: String) { System.err.println(line) }
    override fun readFile(path: String): String? = null
    override fun writeFile(path: String, content: String): Boolean = false
    override fun fileExists(path: String): Boolean = false
}

data class RunResult(
    val lines: List<String>,
    val result: InterpreterResult?,
    val errors: List<Squiggle>,
)

fun run(text: String): RunResult {
    val host = CapturingHost()
    val parseResult = parse(text)
    val program = parseResult.program
    if (program == null) {
        return RunResult(emptyList(), null, parseResult.errors)
    }
    val result = Interpreter(host).run(program)
    return RunResult(host.lines, result, emptyList())
}