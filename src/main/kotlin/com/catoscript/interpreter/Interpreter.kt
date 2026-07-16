package com.catoscript.interpreter


// Interpreter: the catoscript execution loop. Takes a parsed Program
// and runs it statement by statement against a CatoHost.
//
// Flow:
//   1. Walk the program once to build a Map<String, Int> from label
//      name to its index in the stmts list. A label appearing twice
//      is a RuntimeError.
//   2. Maintain a MutableMap<String, Value> for variables.
//   3. Maintain a `lastSniff: Value.Bool?` that records the result
//      of the most recent Sniff statement. purr_at uses it; hiss_at
//      uses it. A purr_at with no prior sniff is a RuntimeError.
//   4. ip walks 0, 1, 2, ... through stmts. Empty and Comment and
//      Label are no-ops at runtime (they affect ip but produce no
//      output). Meow evaluates its Expr to a String and calls
//      host.print(line). Set evaluates and stores. Sniff evaluates
//      and stores into lastSniff. PurrAt/HissAt/Jump set ip to the
//      label's index. Any unknown label is a RuntimeError.
//   5. Every iteration increments stepsConsumed. If it exceeds
//      policy.maxTotalSteps, throw ExecutionLimitReached. The outer
//      try/catch turns it into BudgetExceeded.
//   6. Walk off the end: return Completed.
//   7. Any exception not handled above is wrapped as RuntimeError
//      with the exception message and current stepsConsumed.

import com.catoscript.ast.Expr
import com.catoscript.ast.Program
import com.catoscript.ast.Stmt
import com.catoscript.ast.StrPart
import com.catoscript.runtime.CatoHost


class RuntimeErrorException(message: String) : RuntimeException(message)

class Interpreter(val host: CatoHost, val policy: InterpreterPolicy = InterpreterPolicy()) {

    fun run(program: Program): InterpreterResult {
        var stepsConsumed = 0L
        try {
            val labels = buildLabelMap(program)
            val variables = mutableMapOf<String, Value>()
            var lastSniff: Value.Bool? = null
            var ip = 0

            while (ip < program.stmts.size) {
                if (stepsConsumed >= policy.maxTotalSteps) {
                    throw ExecutionLimitReached(stepsConsumed)
                }
                val stmt = program.stmts[ip]
                when (stmt) {
                    is Stmt.Empty, is Stmt.Comment, is Stmt.Label -> { ip++ }
                    is Stmt.Meow -> {
                        host.print(valueToString(eval(stmt.expr, variables)))
                        ip++
                    }
                    is Stmt.HissAt -> {
                        val last = lastSniff ?: throw RuntimeErrorException("hiss_at has no prior sniff")
                        if (!last.b) ip = labels[stmt.label] ?: throw RuntimeErrorException("unknown label ':${stmt.label}'")
                        else ip++
                    }

                    is Stmt.Set -> {
                        variables[stmt.varName] = eval(stmt.expr, variables)
                        ip++
                    }
                    is Stmt.Sniff -> {
                        val v = eval(stmt.cond, variables)
                        if (v !is Value.Bool) throw RuntimeErrorException("sniff expects a boolean, got $v")
                        lastSniff = v
                        ip++
                    }
                    is Stmt.PurrAt -> {
                        val last = lastSniff ?: throw RuntimeErrorException("purr_at has no prior sniff")
                        if (last.b) ip = labels[stmt.label] ?: throw RuntimeErrorException("unknown label ':${stmt.label}'")
                        else ip++
                    }
                    is Stmt.Jump -> {
                        ip = labels[stmt.label] ?: throw RuntimeErrorException("unknown label ':${stmt.label}'")
                    }
                }
                stepsConsumed++
            }
            return InterpreterResult.Completed
        } catch (e: ExecutionLimitReached) {
            return InterpreterResult.BudgetExceeded(e.stepsConsumed)
        } catch (e: RuntimeErrorException) {
            return InterpreterResult.RuntimeError(e.message ?: "runtime error", stepsConsumed)
        }
    }
    private fun buildLabelMap(program: Program): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        for ((index, stmt) in program.stmts.withIndex()) {
            if (stmt is Stmt.Label) {
                if (map.containsKey(stmt.name)) throw RuntimeErrorException("duplicate label ':${stmt.name}'")
                map[stmt.name] = index
            }
        }
        return map
    }

    private fun eval(expr: Expr, variables: Map<String, Value>): Value {
        return when (expr) {
            is Expr.Num -> Value.Num(expr.value)
            is Expr.Str -> Value.Str(renderString(expr.parts, variables))
            is Expr.VarRef -> variables[expr.name] ?: throw RuntimeErrorException("undefined variable '\$${expr.name}'")
            is Expr.Compare -> {
                val l =eval(expr.left, variables)
                val r = eval(expr.right, variables)
                when (expr.op) {
                    com.catoscript.ast.CompareOp.LT -> compareLess(l, r)
                    com.catoscript.ast.CompareOp.EQ -> compareEq(l, r)
                }
            }
        }
    }

    private fun renderString(parts: List<StrPart>, variables: Map<String, Value>): String {
        val sb = StringBuilder()
        for (part in parts) {
            when (part) {
                is StrPart.Literal -> sb.append(part.text)
                is StrPart.Interpolation -> {
                    val v = variables[part.varName] ?: throw RuntimeErrorException("undefined variable '\$${part.varName}'")
                    sb.append(valueToString(v))
                }
            }
        }
        return sb.toString()
    }

    private fun valueToString(v: Value): String = when (v) {
        is Value.Num -> v.n.toString()
        is Value.Str -> v.s
        is Value.Bool -> v.b.toString()
    }

    private fun compareLess(l: Value, r: Value): Value.Bool {
        if (l is Value.Num && r is Value.Num) return Value.Bool(l.n < r.n)
        if (l is Value.Str && r is Value.Str) return Value.Bool(l.s < r.s)
        throw RuntimeErrorException("cannot compare $l and $r with <")
    }

    private fun compareEq(l: Value, r: Value): Value.Bool {
        if (l is Value.Num && r is Value.Num) return Value.Bool(l.n == r.n)
        if (l is Value.Str && r is Value.Str) return Value.Bool(l.s == r.s)
        if (l is Value.Bool && r is Value.Bool) return Value.Bool(l.b == r.b)
        throw RuntimeErrorException("cannot compare $l and $r with ==")
    }
}






























