package com.catoscript.parser

import com.catoscript.ast.CompareOp
import com.catoscript.ast.Expr
import com.catoscript.ast.Program
import com.catoscript.ast.SourcePos
import com.catoscript.ast.Stmt
import com.catoscript.ast.StrPart

// RealParser: recursive-descent parser from catoscript source text to
// a Program (List<Stmt>). Replaces the old line-splitter Parser.kt,
// which returned flat Tokens and couldn't represent structure
// (interpolated strings, comparisons, label targets).
//
// Shape:
//   Parser.parse(source) splits source into lines and parses each
//   into one Stmt. Per-line dispatch is on the first non-space char:
//     blank line  -> Empty
//     "#..."      -> Comment
//     ":NAME"     -> Label
//     keyword     -> shape-specific (meow/set/sniff/purr_at/hiss_at/jump)
//   Anything else throws ParseError with a SourcePos.
//
// parseExpr handles four shapes:
//   "..."   -> Str with StrPart list (handles $var interpolation, \$ escape)
//   $NAME   -> VarRef
//   digits  -> Num
//   a <op> b (where op is < or ==) -> Compare

class ParseError(message: String, val pos: SourcePos) :RuntimeException("$message at ${pos.format()}")

object Parser {
    fun parse(source: String): Program {
        val lines = source.lines()
        val stmts = mutableListOf<Stmt>()
        for ((index, line) in lines.withIndex()) {
            val lineNumber = index + 1
            stmts.add(parseLine(line, lineNumber))
        }
        return Program(stmts)
    }

    private fun parseLine(line: String, lineNumber: Int): Stmt {
        val pos = SourcePos(lineNumber, 1)
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return Stmt.Empty
        return when {
            trimmed.startsWith("#") -> Stmt.Comment(trimmed.substring(1).trim(), pos)
            trimmed.startsWith(":") -> Stmt.Label(trimmed.substring(1).trim(), pos)
            else -> {
                val spaceAt = trimmed.indexOf(' ')
                val keyword = if (spaceAt == -1) trimmed else trimmed.substring(0, spaceAt)
                val rest = if (spaceAt == -1) "" else trimmed.substring(spaceAt + 1).trim()
                parseKeyword(keyword, rest, pos)

            }

        }
    }

    private fun parseKeyword(keyword: String, rest: String, pos: SourcePos): Stmt {
        return when (keyword) {
            "meow" -> Stmt.Meow(parseExpr(rest, pos), pos)
            "set" -> {
                if (!rest.startsWith("$")) throw ParseError("set expects a variable name like \$name", pos)
                val spaceAt = rest.indexOf(' ')
                if (spaceAt == -1) throw ParseError("set expects \$name followed by a value", pos)
                val name = rest.substring(1, spaceAt)
                val valueText = rest.substring(spaceAt + 1).trim()
                Stmt.Set(name, parseExpr(valueText, pos), pos)
            }
            "sniff" -> Stmt.Sniff(parseExpr(rest, pos), pos)
            "purr_at" -> {
                if (!rest.startsWith(":")) throw ParseError("purr_at expects a label like :NAME", pos)
                Stmt.PurrAt(rest.substring(1).trim(), pos)
            }
            "hiss_at" -> {
                if (!rest.startsWith(":")) throw ParseError("hiss_at expects a label like :NAME", pos)
                Stmt.HissAt(rest.substring(1).trim(), pos)
            }
            "jump" -> {
                if (!rest.startsWith(":")) throw ParseError("jump expects a label like :NAME", pos)
                Stmt.Jump(rest.substring(1).trim(), pos)
            }
            else -> throw ParseError("unknown command '$keyword'", pos)
        }
    }

    private fun parseExpr(text: String, pos: SourcePos): Expr {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) throw ParseError("expected an expression", pos)
        if (trimmed.contains(" < ") || trimmed.contains(" == ")) {
            return parseCompare(trimmed, pos)
        }
        return when {
            trimmed.startsWith("\"") -> parseString(trimmed, pos)
            trimmed.startsWith("$") -> Expr.VarRef(trimmed.substring(1), pos)
            trimmed.all { it.isDigit() } -> Expr.Num(trimmed.toLong(), pos)
            else -> parseCompare(trimmed, pos)
        }
    }

    private fun parseString(text: String, pos: SourcePos): Expr.Str {
        val parts = mutableListOf<StrPart>()
        var i = 1
        val current = StringBuilder()
        while (i < text.length && text[i] != '"') {
            val c = text[i]
            if (c == '\\' && i + 1 < text.length && text[i + 1] == '$') {
                current.append('$')
                i += 2
                continue
            }
            if (c == '$') {
                if (current.isNotEmpty()) {
                    parts.add(StrPart.Literal(current.toString()))
                    current.clear()
                }
                val nameStart = i + 1
                var nameEnd = nameStart
                while (nameEnd < text.length && (text[nameEnd].isLetterOrDigit() || text[nameEnd] == '_')) {
                    nameEnd++
                }
                if (nameEnd == nameStart) throw ParseError("'\$' must be followed by a variable name", pos)
                parts.add(StrPart.Interpolation(text.substring(nameStart, nameEnd)))
                i = nameEnd
                continue
            }
            current.append(c)
            i++
        }
        if (i >= text.length) throw ParseError("unterminated string literal", pos)
        if (current.isNotEmpty()) parts.add(StrPart.Literal(current.toString()))
        return Expr.Str(parts, pos)
    }

    private fun parseCompare(text: String, pos: SourcePos): Expr {
        val ltAt = text.indexOf(" < ")
        val eqAt = text.indexOf(" == ")
        return when {
            ltAt != -1 -> Expr.Compare(
                CompareOp.LT,
                parseExpr(text.substring(0, ltAt), pos),
                parseExpr(text.substring(ltAt + 3), pos),
                pos
            )
            eqAt != -1 -> Expr.Compare(
                CompareOp.EQ,
                parseExpr(text.substring(0, eqAt), pos),
                parseExpr(text.substring(eqAt + 4), pos),
                pos
            )
            else -> throw ParseError("cannot parse expression '$text'", pos)
        }
    }
}