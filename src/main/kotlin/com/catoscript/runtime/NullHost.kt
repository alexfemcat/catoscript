package com.catoscript.runtime

object NullHost : CatoHost {
    override fun print(line: String) {}
    override fun playTone(freq: Double, durationMs: Int, waveform: Waveform) {}
    override fun playSample(id: String) {}
    override fun setCursor(x: Int, y: Int) {}
    override fun clearScreen() {}
    override fun envLookup(key: String): String? = null
    override fun now(): Long = System.currentTimeMillis()

    override fun args(): List<String> = emptyList()
    override fun readLine(prompt: String?): String? = null
    override fun exit(code: Int) {}
    override fun printErr(line: String) {}

    override fun readFile(path: String): String? = null
    override fun writeFile(path: String, content: String): Boolean = false
    override fun fileExists(path: String): Boolean = false
}