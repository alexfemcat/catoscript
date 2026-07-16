package com.catoscript.parser

import com.catoscript.ast.Program
import kotlinx.serialization.json.Json

// AstEmit: serialize a parsed Program to a JSON string. The output is
// the canonical form of "what the engine thinks the script means."
// Used by `cato compile <file.cato>` to write a .cato.json next to the
// source. Consumers (Phase G's analyzer, future LSP, debug tooling)
// read the JSON back via Json.decodeFromString<Program>(...).

private val json = Json {
    prettyPrint = true
}

fun emit(program: Program): String {
    return json.encodeToString(Program.serializer(), program)
}