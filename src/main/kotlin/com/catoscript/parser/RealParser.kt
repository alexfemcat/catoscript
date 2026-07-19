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

class ParseError(message: String, val pos: SourcePos, val originPath: String? = null) :
    RuntimeException(buildMessage(message, pos, originPath)) {
    companion object {
        private fun buildMessage(message: String, pos: SourcePos, originPath: String?): String {
            val where = pos.format()
            return if (originPath != null) "$message in $originPath at $where" else "$message at $where"
        }
    }
}

object Parser {
    private val includeSkipCounter = java.util.concurrent.atomic.AtomicInteger(0)

    private val RESERVED_BASKET_NAMES = setOf(
        "meow", "set", "sniff", "purr_at", "hiss_at", "jump", "include", "basket", "return", "end_basket"
    )

    fun parse(
        source: String,
        basePath: String? = null,
        inProgress: Set<String> = emptySet(),
    ): Program {


        val lines = source.lines()
        val stmts = mutableListOf<Stmt>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val lineNumber = i + 1
            val trimmed = line.trim()
            if (trimmed.startsWith("basket ") || trimmed == "basket") {
                val (basket, consumed) = parseBasketBlock(trimmed, lineNumber, lines, i, basePath, inProgress)
                stmts.add(basket)
                i += consumed
            } else {
                stmts.addAll(parseLine(line, lineNumber, basePath, inProgress))
                i++
            }
        }
        return Program(stmts)
    }

    private fun parseLine(line: String, lineNumber: Int, basePath: String?, inProgress: Set<String>): List<Stmt> {
        val pos = SourcePos(lineNumber, 1)
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return listOf(Stmt.Empty)
        return when {
            trimmed.startsWith("#") -> listOf(Stmt.Comment(trimmed.substring(1).trim(), pos))
            trimmed.startsWith(":") -> {
                val body = trimmed.substring(1).trim()
                val tokens = body.split(Regex("\\s+")).filter { it.isNotEmpty() }
                val labelName = tokens.first()
                val params = tokens.drop(1).map { token ->
                    if (!token.startsWith("$")) {
                        throw ParseError("label params must start with \$: '$token'", pos)
                    }
                    token.substring(1)
                }
                listOf(Stmt.Label(labelName, params, pos = pos))
            }
            else -> {
                val spaceAt = trimmed.indexOf(' ')
                val keyword = if (spaceAt == -1) trimmed else trimmed.substring(0, spaceAt)
                val rest = if (spaceAt == -1) "" else trimmed.substring(spaceAt + 1).trim()
                if (keyword == "include") {
                    if (!rest.startsWith("\"") || !rest.endsWith("\"") || rest.length < 2) {
                        throw ParseError("include expects a quoted path like include \"lib.cato\"", pos, basePath)
                    }
                    val path = rest.substring(1, rest.length - 1)
                    parseInclude(path, basePath, inProgress, pos)
                } else {
                    listOf(parseKeyword(keyword, rest, pos))
                }
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
                val body = rest.substring(1).trim()
                val tokens = body.split(Regex("\\s+")).filter { it.isNotEmpty() }
                val target = tokens.first()
                val args = tokens.drop(1).map { arg -> parseExpr(arg, pos) }
                Stmt.Jump(target, args, pos = pos)
            }
            "return" -> Stmt.Return(pos)

            else -> throw ParseError("unknown command '$keyword'", pos)
        }
    }

    private fun parseBasketBlock(
        firstLine: String,
        startLineNumber: Int,
        lines: List<String>,
        startIndex: Int,
        basePath: String?,
        inProgress: Set<String>,
    ): Pair<Stmt.Basket, Int> {
        val pos = SourcePos(startLineNumber, 1)
        val body = firstLine.substring("basket".length).trim()
        val tokens = body.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) throw ParseError("basket expects a name like 'basket greet'", pos)
        val name = tokens.first()
        if (name in RESERVED_BASKET_NAMES) {
            throw ParseError("basket name '$name' is a reserved keyword", pos)
        }
        val params = tokens.drop(1).map { token ->
            if (!token.startsWith("$")) throw ParseError("basket params must start with \$: '$token'", pos)
            token.substring(1)
        }
        val bodyStmts = mutableListOf<Stmt>()
        var i = startIndex + 1
        while (i < lines.size) {
            val bodyLine = lines[i]
            val bodyTrimmed = bodyLine.trim()
            if (bodyTrimmed == "end_basket") {
                val consumed = i - startIndex + 1
                return Pair(Stmt.Basket(name, params, bodyStmts, pos), consumed)
            }
            bodyStmts.addAll(parseLine(bodyLine, i + 1, basePath, inProgress))
            i++
        }
        throw ParseError("basket '$name' missing end_basket", pos)
    }


    private fun parseInclude(
        path: String,
        basePath: String?,
        inProgress: Set<String>,
        pos: SourcePos
    ): List<Stmt> {
        val resolvedPath = if (java.io.File(path).isAbsolute) path
        else if (basePath != null) java.io.File(basePath).parentFile?.let { java.io.File(it, path).path }
            ?: throw ParseError("cannot resolve include path '$path'", pos, basePath)
        else path
        val file = java.io.File(resolvedPath)
        if (resolvedPath in inProgress) {
            val chain = (inProgress + resolvedPath).joinToString(" -> ")
            throw ParseError("cyclic include: $chain", pos, basePath)
        }
        if (!file.exists()) throw ParseError("included file not found: $resolvedPath", pos, basePath)
        val source = file.readText()
        val innerStmts = parse(source, resolvedPath, inProgress + resolvedPath).stmts
        val skipLabel = "__include_skip_${includeSkipCounter.incrementAndGet()}"
        val wrapped = mutableListOf<Stmt>()
        wrapped.add(Stmt.Jump(skipLabel, pos = pos))
        wrapped.addAll(innerStmts)
        wrapped.add(Stmt.Label(skipLabel, pos = pos))
        return wrapped


    }
}

private fun parseExpr(text: String, pos: SourcePos): Expr {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) throw ParseError("expected an expression", pos)
    if (trimmed.contains(" < ") || trimmed.contains(" <= ") || trimmed.contains(" > ") || trimmed.contains(" >= ") || trimmed.contains(
            " != "
        ) || trimmed.contains(" == ")
    ) {
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
    val gtAt = text.indexOf(" > ")
    val gteAt = text.indexOf(" >= ")
    val neqAt = text.indexOf(" != ")
    val eqAt = text.indexOf(" == ")
    val lteAt = text.indexOf(" <= ")

    return when {
        lteAt != -1 -> Expr.Compare(
            CompareOp.LTE,
            parseExpr(text.substring(0, lteAt), pos),
            parseExpr(text.substring(lteAt + 4), pos),
            pos
        )

        ltAt != -1 -> Expr.Compare(
            CompareOp.LT,
            parseExpr(text.substring(0, ltAt), pos),
            parseExpr(text.substring(ltAt + 3), pos),
            pos
        )

        gteAt != -1 -> Expr.Compare(
            CompareOp.GTE,
            parseExpr(text.substring(0, gteAt), pos),
            parseExpr(text.substring(gteAt + 4), pos),
            pos
        )

        gtAt != -1 -> Expr.Compare(
            CompareOp.GT,
            parseExpr(text.substring(0, gtAt), pos),
            parseExpr(text.substring(gtAt + 3), pos),
            pos
        )

        neqAt != -1 -> Expr.Compare(
            CompareOp.NEQ,
            parseExpr(text.substring(0, neqAt), pos),
            parseExpr(text.substring(neqAt + 4), pos),
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
