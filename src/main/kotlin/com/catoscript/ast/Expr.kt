package com.catoscript.ast

// Expr: a sealed hierarchy of things that produce a runtime Value.
// Str carries its parts as a list (Literal vs Interpolation) so the
// parser handles "$var" interpolation once at parse time, not every
// time the string is evaluated.
// Num is Long for now; Doubles come when arithmetic needs them.
// VarRef reads a variable by name from the interpreter's variable map.
// Compare is the only boolean expression this slice supports, with two
// operators (LT and EQ). Arithmetic comparisons land with arithmetic.

sealed interface Expr {
    data class Str(val parts: List<StrPart>, val pos: SourcePos) : Expr
    data class Num(val value: Long, val pos: SourcePos) : Expr
    data class VarRef(val name: String, val pos: SourcePos) : Expr
    data class Compare(val op: CompareOp, val left: Expr, val right: Expr, val pos: SourcePos) : Expr
}

enum class CompareOp { LT, EQ }

sealed interface StrPart {
    data class Literal(val text: String) : StrPart
    data class Interpolation(val varName: String) : StrPart
}


