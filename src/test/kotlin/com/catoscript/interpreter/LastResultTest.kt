package com.catoscript.interpreter

import kotlin.test.Test
import kotlin.test.assertIs

class LastResultTest {

    @Test
    fun `Success is a Last Result`() {
        val success = LastResult.Success("it worked")
        assertIs<LastResult.Success>(success)
    }

    @Test
    fun `Failure is a Last Result`() {
        val failure = LastResult.Failure("it broke")
        assertIs<LastResult.Failure>(failure)
    }
}