package com.catoscript.runtime

class ConsoleHost : CatoHost {
    override fun print(line: String) {
        println(line)
    }
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
