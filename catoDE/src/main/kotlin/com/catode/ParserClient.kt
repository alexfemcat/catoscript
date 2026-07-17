package com.catode

import com.catoscript.parser.Parser
import com.catoscript.parser.ParseError
import com.catoscript.ast.SourcePos
import com.catoscript.ast.Program

data class Squiggle(
    val line: Int,
    val column: Int,
    val length: Int,
    val message: String,
)

data class ParseResult(
    val program: Program?,
    val errors: List<Squiggle>,
)

fun parse(text: String): ParseResult {
    return try {
        val program = Parser.parse(text)
        ParseResult(program, emptyList())
    } catch (error: ParseError) {
        val squiggle = Squiggle(error.pos.line, error.pos.column, 1, error.message ?: "parse error")
        ParseResult(null, listOf(squiggle))
    }
}
