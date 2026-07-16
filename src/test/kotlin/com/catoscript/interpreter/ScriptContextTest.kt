package com.catoscript.interpreter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull

class ScriptContextTest {

    @Test
    fun `two contexts with the same variables are equal`() {
        val variables: MutableMap<String, Any> = mutableMapOf("name" to "alice")
        val labels: Map<String, Int> = mapOf("loop_start" to 5)

        val a = ScriptContext(variables, labels)
        val b = ScriptContext(variables, labels)

        assertEquals(a, b)
    }

    @Test
    fun `two contexts with different variables are not equal`() {
        val variables1: MutableMap<String, Any> = mutableMapOf("name" to "alice")
        val variables2: MutableMap<String, Any> = mutableMapOf("name" to "bob")
        val labels: Map<String, Int> = mapOf()

        val a = ScriptContext(variables1, labels)
        val b = ScriptContext(variables2, labels)

        assertNotEquals(a, b)
    }

    @Test
    fun `copy creates a new instance with the same data`() {
        val variables: MutableMap<String, Any> = mutableMapOf("count" to 0)
        val labels: Map<String, Int> = mapOf()

        val original = ScriptContext(variables, labels)
        val copied = original.copy()

        assertEquals(original, copied)
        assertNotSame(original, copied)
    }

    @Test
    fun `paused defaults to null when not specified`() {
        val variables: MutableMap<String, Any> = mutableMapOf()
        val labels: Map<String, Int> = mapOf()

        val ctx = ScriptContext(variables, labels)

        assertNull(ctx.paused)
    }
}