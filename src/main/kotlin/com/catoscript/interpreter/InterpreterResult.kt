package com.catoscript.interpreter

// InterpreterResult: what Interpreter.run() returns to its caller.
// Completed means the ip walked off the end of the program normally.
// BudgetExceeded means the step cap was hit (the script ran too long).
// RuntimeError means a real failure: undefined variable, bad label
// jump, type mismatch. The message is human-readable and meant for
// the CLI REPL or the host's error pane. stepsConsumed on every
// variant is so a debugger or Stepper can report "you got to step N."

sealed interface InterpreterResult {
    data object Completed : InterpreterResult
    data class BudgetExceeded(val stepsConsumed: Long) : InterpreterResult
    data class RuntimeError(val message: String, val stepsConsumed: Long) :
        InterpreterResult
}