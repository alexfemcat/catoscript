package com.catoscript.lexer

sealed class Token {
    data object Empty : Token()
    data object Comment : Token()
    data class Command(val commandName: String, val args: List<String>) : Token()
    data class Label(val name: String) : Token()
}