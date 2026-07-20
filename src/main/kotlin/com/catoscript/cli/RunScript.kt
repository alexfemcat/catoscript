package com.catoscript.cli

// RunScript: minimal CLI entry point. Reads a .cato file, parses it,
// runs it through the interpreter with ConsoleHost, prints the result.
// Used to smoke-test catoscript end to end from the terminal.
//
// Usage:   cato [run|compile] <file.cato>
//          cato <file.cato>

import com.catoscript.interpreter.Interpreter
import com.catoscript.interpreter.InterpreterResult
import com.catoscript.parser.Parser
import com.catoscript.runtime.ConsoleHost
import kotlin.system.exitProcess
import java.io.File
import com.catoscript.parser.emit

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("usage: cato run <file.cato>")
        exitProcess(2)
    }
    // Strip the optional "run" or "compile" subcommand so both `cato run foo.cato`,
    // `cato compile foo.cato`, and `cato foo.cato` work. The launcher passes args verbatim.
    // Determine mode and extract path
    var mode = "run" // Default mode
    val scriptArgs: List<String> = if (args.size > 1 && args[0] == "compile") {
        mode = "compile"
        args.drop(1)
    } else if (args.size > 1 && args[0] == "run") {
        mode = "run"
        args.drop(1)
    } else {
        args.toList()
    }
    if (scriptArgs.isEmpty()) {
        System.err.println("usage: cato [run|compile] <file.cato>")
        exitProcess(2)
    }
    val path = scriptArgs[0]
    val source = File(path).readText()
    val program = Parser.parse(source, File(path).absolutePath)
    val host = ConsoleHost()
    val result: InterpreterResult = if (mode == "compile") {
        compileScript(program)
    } else {
        Interpreter(host).run(program)
    }
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

fun compileScript(program: com.catoscript.ast.Program): InterpreterResult {
    println("Compiling and analyzing...")
    val analyzerResult = com.catoscript.analyzer.CatoScriptAnalyzer().analyze(program)
    if (analyzerResult.hasErrors()) {
        // Phase B.2 MW8 — report every error, not just the first.
        for (error in analyzerResult.errors) {
            System.err.println("error: ${error.message}")
        }
        val count = analyzerResult.errors.size
        val summary = "compilation failed: $count error(s)"
        System.err.println(summary)
        return InterpreterResult.RuntimeError(summary, 0)
    }
    val jsonString = emit(program)
    println("Successfully compiled. AST:")
    println(jsonString)
    return InterpreterResult.Completed
}