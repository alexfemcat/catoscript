package com.catoscript.ast

// Stmt: a sealed hierarchy of statements the interpreter executes.
// Meow prints a value. Set writes a variable. Sniff records a boolean
// (the bridge between a condition and the next purr_at / hiss_at).
// PurrAt jumps to a label if the last sniff was true; HissAt jumps if
// false. Jump always jumps. Label is a pass-through at runtime — it
// exists only so the parser's label map knows the target ip.
// Comment and Empty are no-ops kept so the interpreter can advance
// past them without special-casing.
// Program is the top-level node: an ordered list of statements.

sealed interface Stmt {
    data class Meow(val expr: Expr, val pos: SourcePos) : Stmt
    data class Set(val varName: String, val expr: Expr, val pos: SourcePos) : Stmt
    data class Sniff(val cond: Expr, val pos: SourcePos) : Stmt
    data class PurrAt(val label: String, val pos: SourcePos) : Stmt
    data class HissAt(val label: String, val pos: SourcePos) : Stmt
    data class Jump(val label: String, val pos: SourcePos) : Stmt
    data class Label(val name: String, val pos: SourcePos) : Stmt
    data class Comment(val text: String, val pos: SourcePos) : Stmt
    data object Empty : Stmt
}

data class Program(val stmts: List<Stmt>)