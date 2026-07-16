package com.catoscript.interpreter

// Value: the runtime values an Expr evaluates to. Num is Long for now
// (no decimals yet). Str is a plain string. Bool is the result of a
// Compare and the bridge into purr_at / hiss_at. Nothing else exists
// yet — lists, nulls, and doubles land when their features ship.

sealed interface Value {
    data class Num(val n: Long) : Value
    data class Str(val s: String) : Value
    data class Bool(val b: Boolean) : Value
}