package com.catoscript.interpreter

// ExecutionLimitReached: thrown by the interpreter when the step
// counter exceeds InterpreterPolicy.maxTotalSteps. The interpreter
// catches this and turns it into an InterpreterResult.BudgetExceeded
// instead of letting it propagate as a crash. stepsConsumed is the
// total instructions executed before the cap was hit, useful for
// debugging "which script ran away."

class ExecutionLimitReached(val stepsConsumed: Long) : RuntimeException("execution limit reached at step $stepsConsumed")
