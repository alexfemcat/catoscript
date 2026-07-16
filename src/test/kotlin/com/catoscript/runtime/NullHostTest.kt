package com.catoscript.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class NullHostTest {

    @Test
    fun printDoesNothing() {
        NullHost.print("hello")
    }

    @Test
    fun nowReturnsMonotonicClock() {
        val a = NullHost.now()
        val b = NullHost.now()
        assertEquals(true, b >= a)
    }

    @Test
    fun envLookupIsNull() {
        assertNull(NullHost.envLookup("anything"))
    }

    @Test
    fun argsIsEmpty() {
        assertEquals(emptyList(), NullHost.args())
    }

    @Test
    fun readLineIsNull() {
        assertNull(NullHost.readLine("prompt?"))
    }

    @Test
    fun fileAccessorsAreNullOrFalse() {
        assertNull(NullHost.readFile("nope.txt"))
        assertFalse(NullHost.fileExists("nope.txt"))
        assertFalse(NullHost.writeFile("nope.txt", "data"))
    }
}