package com.catoscript.analyzer

// CatoScriptAnalyzer: walks the Program AST to find semantic errors before runtime.
// This is the "static check" phase of the compiler.

import com.catoscript.ast.Program

data class AnalyzerResult(val errors: List<AnalyzerError>) {
    fun hasErrors(): Boolean = errors.isNotEmpty()
}

// Represents a single error found in the code.
data class AnalyzerError(val message: String)

class CatoScriptAnalyzer {

    fun analyze(program: Program): AnalyzerResult {
        // This will be expanded in Batch 2 to actually walk the AST nodes.
        // For now, we return an empty list of errors to verify the pipeline.
        return AnalyzerResult(emptyList())
    }
}