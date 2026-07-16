package com.catoscript.runtime

enum class Waveform { SQUARE, SINE, NOISE, SAMPLE }

interface CatoHost {
    fun print(line: String)
    fun playTone(freq: Double, durationMs: Int, waveform: Waveform)
    fun playSample(id: String)
    fun setCursor(x: Int, y: Int)
    fun clearScreen()
    fun envLookup(key: String): String?
    fun now(): Long

    fun args(): List<String>
    fun readLine(prompt: String?): String?
    fun exit(code: Int)
    fun printErr(line: String)

    fun readFile(path: String): String?
    fun writeFile(path: String, content: String): Boolean
    fun fileExists(path: String): Boolean
}