package com.catoscript.interpreter

// InterpreterPolicy: controls how the interpreter loop runs.
// maxStepsPerTick is the per-frame budget Kernel Panic cares about
// (the loop is also a cooperative scheduler). The standalone REPL
// doesn't have frames, so this is just a knob for future hosts.
// maxTotalSteps is a hard ceiling on total instructions executed
// across the whole script run. Throws ExecutionLimitReached when hit.
// seed is here so the type is in place when something needs it;
// nothing reads it yet.

data class InterpreterPolicy(
    val maxStepsPerTick: Int = 100,
    val maxTotalSteps: Long = 1_000_000,
    val seed: Long? = null
)