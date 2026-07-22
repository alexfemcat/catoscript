package com.catoscript.analyzer

import com.catoscript.ast.Expr
import com.catoscript.ast.Program
import com.catoscript.ast.SourcePos
import com.catoscript.ast.Stmt

data class AnalyzerResult(val errors: List<AnalyzerError>) {
    fun hasErrors(): Boolean = errors.isNotEmpty()
}

data class AnalyzerError(val message: String, val pos: SourcePos)

data class BasketInfo(val body: List<Stmt>, val params: List<String>)

class CatoScriptAnalyzer {

    private val errors = mutableListOf<AnalyzerError>()
    private val defined = mutableSetOf<String>()
    private val labelMap = mutableMapOf<String, Int>()
    private val basketsMap = mutableMapOf<String, BasketInfo>()
    private val reservedKeywords = setOf("meow", "set", "sniff", "purr_at", "hiss_at", "jump", "basket", "return")

    fun analyze(program: Program): AnalyzerResult {
        errors.clear()
        defined.clear()
        labelMap.clear()
        basketsMap.clear()

        for (stmt in program.stmts) {
            when (stmt) {
                is Stmt.Label -> labelMap[stmt.name] = stmt.pos.line
                is Stmt.Basket -> basketsMap[stmt.name] = BasketInfo(stmt.body, stmt.params)
                else -> {}
            }
        }

        analyzeProgram(program)
        return AnalyzerResult(errors.toList())
    }

    private fun analyzeStmt(stmt: Stmt) {
        when (stmt) {
            is Stmt.Set -> {
                defined.add(stmt.varName)
                analyzeExpr(stmt.expr)
            }
            is Stmt.Meow -> {
                analyzeExpr(stmt.expr)
            }
            is Stmt.Sniff -> {
                analyzeExpr(stmt.cond)
            }
            is Stmt.PurrAt -> {
                if (labelMap[stmt.label] == null) {
                    errors.add(AnalyzerError("target label '${stmt.label}' not found", stmt.pos))
                }
            }
            is Stmt.HissAt -> {
                if (labelMap[stmt.label] == null) {
                    errors.add(AnalyzerError("target label '${stmt.label}' not found", stmt.pos))
                }
            }
            is Stmt.Jump -> {
                if (labelMap[stmt.label] == null) {
                    errors.add(AnalyzerError("jump target label '${stmt.label}' not found", stmt.pos))
                }
                for (arg in stmt.args) {
                    analyzeExpr(arg)
                }
            }
            is Stmt.Label -> {}
            is Stmt.Basket -> {
                if (reservedKeywords.contains(stmt.name)) {
                    errors.add(AnalyzerError("basket name '${stmt.name}' conflicts with a reserved keyword", stmt.pos))
                } else {
                    for (param in stmt.params) { defined.add(param) }
                    for (bodyStmt in stmt.body) { analyzeStmt(bodyStmt) }
                    for (param in stmt.params) { defined.remove(param) }
                }
            }
            is Stmt.Call -> {
                val info = basketsMap[stmt.name]
                if (info == null) {
                    errors.add(AnalyzerError("call to undefined basket '${stmt.name}'", stmt.pos))
                } else {
                    if (info.params.size != stmt.args.size) {
                        errors.add(
                            AnalyzerError(
                                "basket call '${stmt.name}' expected ${info.params.size} arguments, but received ${stmt.args.size}",
                                stmt.pos
                            )
                        )
                    }
                    for (argExpr in stmt.args) {
                        analyzeExpr(argExpr)
                    }
                }
            }
            is Stmt.Return -> {}
            is Stmt.Comment -> {}
            is Stmt.Empty -> {}
        }
    }

    private fun analyzeExpr(expr: Expr) {
        when (expr) {
            is Expr.Num -> {}
            is Expr.VarRef -> {
                if (expr.name !in defined) {
                    errors.add(
                        AnalyzerError(
                            "undefined variable: \$${expr.name} at ${expr.pos.format()}",
                            expr.pos
                        )
                    )
                }
            }
            is Expr.Str -> {
                for (part in expr.parts) {
                    if (part is com.catoscript.ast.StrPart.Interpolation) {
                        analyzeExpr(Expr.VarRef(part.varName, expr.pos))
                    }
                }
            }
            is Expr.Compare -> {
                analyzeExpr(expr.left)
                analyzeExpr(expr.right)
            }
        }
    }

    private fun analyzeProgram(p: Program) {
        for (stmt in p.stmts) {
            analyzeStmt(stmt)
        }
    }
}
