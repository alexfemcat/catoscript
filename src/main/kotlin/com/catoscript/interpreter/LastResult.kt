package com.catoscript.interpreter

sealed interface LastResult {
    data class Success(val value: Any) : LastResult
    data class Failure(val value: Any) : LastResult
}