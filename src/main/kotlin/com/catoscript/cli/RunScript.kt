package com.catoscript.cli

// RunScript: minimal CLI entry point. Reads a .cato file, parses it,
// runs it through the interpreter with ConsoleHost, prints the result.
// Used to smoke-test catoscript end to end from the terminal.
//
// Usage:   cato run <file.cato>
//          cato <file.cato>

import com.catoscript.interpreter.Interpreter
import com.catoscript.interpreter.InterpreterResult
import com.catoscript.parser.Parser
import com.catoscript.runtime.ConsoleHost
import kotlin.system.exitProcess
import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("usage: cato run <file.cato>")
        exitProcess(2)
    }
    // Strip the optional "run" subcommand so both `cato run foo.cato`
    // and `cato foo.cato` work. The launcher passes args verbatim.
    val scriptArgs = if (args[0] == "run") args.drop(1) else args.toList()
    if (scriptArgs.isEmpty()) {
        System.err.println("usage: cato run <file.cato>")
        exitProcess(2)
    }
    val path = scriptArgs[0]
    val source = File(path).readText()
    val program = Parser.parse(source, File(path).absolutePath)
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