package com.catoscript.ast

// SourcePos: a 1-indexed line/column position in a catoscript source file.
// Attached to every Expr and Stmt node so error messages can point at
// the exact spot in the script where something went wrong.

data class SourcePos(val line: Int, val column: Int) {
    fun format(): String = "line $line, col $column"
}
