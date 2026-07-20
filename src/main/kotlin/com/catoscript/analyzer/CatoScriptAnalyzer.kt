package com.catoscript.analyzer

// CatoScriptAnalyzer: walks the Program AST to find semantic errors.

import com.catoscript.ast.Expr
import com.catoscript.ast.Program
import com.catoscript.ast.SourcePos
import com.catoscript.ast.Stmt

data class AnalyzerResult(val errors: List<AnalyzerError>) {
    fun hasErrors(): Boolean = errors.isNotEmpty()
}

// Represents a single error found in the code.
data class AnalyzerError(val message: String, val pos: SourcePos)

class CatoScriptAnalyzer {

    private val errors = mutableListOf<AnalyzerError>()
    private val defined = mutableSetOf<String>()

    fun analyze(program: Program): AnalyzerResult {
        errors.clear()
        defined.clear()
        analyzeProgram(program)
        return AnalyzerResult(errors.toList())
    }

    private fun analyzeStmt(stmt: Stmt) {
        when (stmt) {
            is Stmt.Set -> {
                defined.add(stmt.varName)
                analyzeExpr(stmt.expr)
            }
            else -> { /* B.3 walks Meow/Sniff/Jump/Basket/Call/etc. */ }
        }
    }

    private fun analyzeExpr(expr: Expr) {
        when (expr) {
            is Expr.Num -> {
            }
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