package com.catoscript.cli

// RunScript: minimal CLI entry point. Reads a .cato file, parses it,
// runs it through the interpreter with NullHost, prints the result.
// Used to smoke-test catoscript end to end from the terminal.
//
// Usage:   ./gradlew run -PappArgs="samples/01_first_script/hello.cato"

import com.catoscript.interpreter.Interpreter
import com.catoscript.interpreter.InterpreterResult
import com.catoscript.parser.Parser
import com.catoscript.runtime.ConsoleHost
import kotlin.system.exitProcess
import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("usage: run <file.cato>")
        exitProcess(2)
    }
    val path = args[0]
    val source = File(path).readText()
    val program = Parser.parse(source)
    val host = ConsoleHost()
    val result = Interpreter(host).run(program)
    when (result) {
        is InterpreterResult.Completed -> exitProcess(0)
        is InterpreterResult.BudgetExceeded -> {
            System.err.println("script exceeded step budget at step ${result.stepsConsumed}")
            exitProcess(1)
        }
        is InterpreterResult.RuntimeError -> {
            System.err.println("runtime error: ${result.message} (step ${result.stepsConsumed})")
            exitProcess(1)
        }
    }
}