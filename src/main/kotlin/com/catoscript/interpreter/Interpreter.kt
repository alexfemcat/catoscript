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

    private data class CallFrame(val returnIp: Int, val callerVariables: Map<String, Value>)

    private data class BasketInfo(val body: List<Stmt>, val params: List<String>)

    fun run(program: Program): InterpreterResult {

        var stepsConsumed = 0L
        try {
            val labels = buildLabelMap(program)
            val labelParams = buildLabelParamsMap(program)
            val labelBodyEnds = buildLabelBodyEnds(program)
            val baskets = buildBasketsMap(program)
            val variables = mutableMapOf<String, Value>()
            val callStack = mutableListOf<CallFrame>()
            var lastSniff: Value.Bool? = null

            fun executeFrom(stmts: List<Stmt>, startIp: Int) {
                var ip = startIp
                while (ip < stmts.size) {
                    if (stepsConsumed >= policy.maxTotalSteps) {
                        throw ExecutionLimitReached(stepsConsumed)
                    }
                    val stmt = stmts[ip]
                when (stmt) {
                    is Stmt.Empty, is Stmt.Comment, is Stmt.Label, is Stmt.Basket -> { ip++ }
                    is Stmt.Return -> {
                        val frame = callStack.removeLastOrNull()
                            ?: throw RuntimeErrorException("return with no active call frame")
                        variables.clear()
                        variables.putAll(frame.callerVariables)
                        ip = frame.returnIp
                        return
                    }
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
                    is Stmt.Call -> {
                        val info = baskets[stmt.name] ?: throw RuntimeErrorException("unknown basket '${stmt.name}'")
                        if (stmt.args.size != info.params.size) {
                            throw RuntimeErrorException("arity mismatch on basket '${stmt.name}': expected ${info.params.size} args, got${stmt.args.size}")
                        }
                        if (callStack.size >= policy.maxCallDepth) {
                            throw RuntimeErrorException("call depth exceeded ${policy.maxCallDepth} (recursive basket calls)")
                        }
                        val returnIp = ip + 1
                        callStack.add(CallFrame(returnIp = returnIp, callerVariables = variables.toMap()))
                        for ((paramName, argExpr) in info.params.zip(stmt.args)) {
                            variables[paramName] = eval(argExpr, variables)
                        }
                        executeFrom(info.body, 0)
                        ip = returnIp
                    }

                    is Stmt.Jump -> {
                        if (stmt.label == "end") {
                            val frame = callStack.removeLastOrNull()
                                ?: throw RuntimeErrorException("jump :end with no active call frame")
                            variables.clear()
                            variables.putAll(frame.callerVariables)
                            ip = frame.returnIp
                        } else {
                            val targetIp = labels[stmt.label]
                                ?: throw RuntimeErrorException("unknown label ':${stmt.label}'")
                            val paramsList = labelParams[stmt.label] ?: emptyList()
                            if (stmt.args.isNotEmpty() || paramsList.isNotEmpty()) {
                                if (stmt.args.size != paramsList.size) {
                                    throw RuntimeErrorException("arity mismatch on ':${stmt.label}': expected ${paramsList.size} args, got ${stmt.args.size}")
                                }
                                if (callStack.size >= policy.maxCallDepth) {
                                    throw RuntimeErrorException("call depth exceeded ${policy.maxCallDepth} (recursive label calls)")
                                }
                                callStack.add(CallFrame(returnIp = labelBodyEnds[stmt.label] ?: (ip + 1), callerVariables = variables.toMap()))
                                for ((paramName, argExpr) in paramsList.zip(stmt.args)) {
                                    variables[paramName] = eval(argExpr, variables)
                                }
                            }
                            ip = targetIp
                        }
                    }
                }
                stepsConsumed++
                }
            }

            executeFrom(program.stmts, 0)
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

    private fun buildLabelParamsMap(program: Program): Map<String, List<String>> {
        val map = mutableMapOf<String, List<String>>()
        for (stmt in program.stmts) {
            if (stmt is Stmt.Label && stmt.params.isNotEmpty()) {
                map[stmt.name] = stmt.params
            }
        }
        return map
    }

    private fun buildLabelBodyEnds(program: Program): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        for ((i, stmt) in program.stmts.withIndex()) {
            if (stmt is Stmt.Label && stmt.params.isNotEmpty()) {
                var endIp = program.stmts.size
                for (j in i + 1 until program.stmts.size) {
                    if (program.stmts[j] is Stmt.Jump && (program.stmts[j] as Stmt.Jump).label == "end") {
                        endIp = j + 1
                        break
                    }
                }
                map[stmt.name] = endIp
            }
        }
        return map
    }

    private fun buildBasketsMap(program: Program): Map<String, BasketInfo> {
        val map = mutableMapOf<String, BasketInfo>()
        for (stmt in program.stmts) {
            if (stmt is Stmt.Basket) {
                map[stmt.name] = BasketInfo(body = stmt.body, params = stmt.params)
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
                    com.catoscript.ast.CompareOp.LTE -> Value.Bool(compareLess(l, r).b || compareEq(l, r).b)
                    com.catoscript.ast.CompareOp.EQ -> compareEq(l, r)
                    com.catoscript.ast.CompareOp.NEQ -> Value.Bool(!compareEq(l, r).b)
                    com.catoscript.ast.CompareOp.GTE -> Value.Bool(compareLess(r, l).b || compareEq(l, r).b)
                    com.catoscript.ast.CompareOp.GT -> compareLess(r, l)
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






























