package com.catoscript.interpreter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ThreadHandleTest {

    @Test
    fun `two handles with the same fields are equal`() {
        val a = ThreadHandle("t1", "10.0.0.5", active = true)
        val b = ThreadHandle("t1", "10.0.0.5", active = true)

        assertEquals(a, b)
    }

    @Test
    fun `handles with different active flags are not equal`() {
        val running = ThreadHandle("t1", "10.0.0.5", active = true)
        val paused = ThreadHandle("t1", "10.0.0.5", active = false)

        assertNotEquals(running, paused)
    }

    @Test
    fun `copy with a new active value creates a different handle`() {
        val original = ThreadHandle("t1", "10.0.0.5", active = true)
        val paused = original.copy(active = false)

        assertEquals("t1", paused.id)
        assertEquals("10.0.0.5", paused.ip)
        assertNotEquals(original, paused)
    }
}