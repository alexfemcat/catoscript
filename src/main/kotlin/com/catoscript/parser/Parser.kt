package com.catoscript.parser

import com.catoscript.lexer.Token

fun parseLine(line: String): Token? {
    val trimmed = line.trim()
    return when {
        trimmed.isEmpty() -> Token.Empty
        trimmed.startsWith("#") -> Token.Comment
        else -> {
            val parts = trimmed.split(" ")
            val commandName = parts[0]
            val args = if (parts.size > 1) parts.drop(1) else emptyList()
            Token.Command(commandName, args)
        }

    }
}