package com.catoscript.web

import com.catoscript.runtime.CatoHost
import com.catoscript.runtime.Request
import com.catoscript.runtime.Waveform
import com.catoscript.runtime.WebHost

class WebHostImpl : WebHost {

    // internal state
    private val routes: MutableMap<String, RouteEntry> = mutableMapOf()
    private val meowBuffer: MutableList<String> = mutableListOf()
    private val currentRequest: Request? = null

    // catohost: 14 inherited methods

    override fun print(line: String) {
        meowBuffer.add(line)
    }

    override fun playTone(freq: Double, durationMs: Int, waveform: Waveform) {
    //web doesnt play audio
    }

    override fun playSample(id: String) {
        // web host doesnt play samples
    }

    override fun setCursor(x: Int, y: Int) {
        // web host has no cursor
    }

    override fun clearScreen() {
        // web host has no screen
    }

    override fun envLookup(key: String): String? {
        return null
    }

    override fun now(): Long {
        return System.currentTimeMillis()
    }

    override fun args(): List<String> {
        return emptyList()
    }

    override fun readLine(prompt: String?): String? {
        return null
    }

    override fun exit(code: Int) {
        // web host is long-live; per-request exits dont make sense
    }

    override fun printErr(line: String) {
        System.err.println(line)
    }

    override fun readFile(path: String): String? {
        return null
    }

    override fun writeFile(path: String, content: String): Boolean {
        return false
    }

    override fun fileExists(path: String): Boolean {
        return false
    }

    // 4 new methods

    override fun registerRoute(method: String, path: String, basketName: String) {
        TODO()
    }

    override fun serve(port: Int) {
        TODO()
    }

    override fun currentRequest(): Request {
        TODO()
    }

    override fun respond(status: Int, body: String) {
        TODO()
    }
}

// single registered route, key is METHOD path
private data class RouteEntry(
    val method: String,
    val path: String,
    val basketName: String,
)

