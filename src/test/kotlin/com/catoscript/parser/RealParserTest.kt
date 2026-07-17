package com.catoscript.parser

import com.catoscript.ast.CompareOp
import com.catoscript.ast.Expr
import com.catoscript.ast.Program
import com.catoscript.ast.SourcePos
import com.catoscript.ast.Stmt
import com.catoscript.ast.StrPart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertTrue

class RealParserTest {
    @Test
    fun `blank line parses to Empty`() {
        val program = Parser.parse("")
        assertEquals(1, program.stmts.size)
        assertEquals(Stmt.Empty, program.stmts[0])
    }
    @Test
    fun `comment line parses to Comment`() {
        val program = Parser.parse("# a note")
        val stmt = assertIs<Stmt.Comment>(program.stmts[0])
        assertEquals("a note", stmt.text)
    }
    @Test
    fun `label parses to Label without colon`() {
        val program = Parser.parse(":START")
        val stmt = assertIs<Stmt.Label>(program.stmts[0])
        assertEquals("START", stmt.name)
    }
    @Test
    fun `meow with plain string literal`() {
        val program = Parser.parse("""meow "hello"""")
        val stmt = assertIs<Stmt.Meow>(program.stmts[0])
        val str = assertIs<Expr.Str>(stmt.expr)
        assertEquals(listOf<StrPart>(StrPart.Literal("hello")), str.parts)
    }
    @Test
    fun `meow with $var interpolation`() {
        val program = Parser.parse("meow \"hi, \$name\"")
        val stmt = assertIs<Stmt.Meow>(program.stmts[0])
        val str = assertIs<Expr.Str>(stmt.expr)
        assertEquals(
            listOf<StrPart>(StrPart.Literal("hi, "), StrPart.Interpolation("name")),
            str.parts
        )
    }
    @Test
    fun `set with number value`() {
        val program = Parser.parse("set \$x 10")
        val stmt = assertIs<Stmt.Set>(program.stmts[0])
        assertEquals("x", stmt.varName)
        val num = assertIs<Expr.Num>(stmt.expr)
        assertEquals(10L, num.value)
    }
    @Test
    fun `sniff with less-than comparison`() {
        val program = Parser.parse("sniff \$hp < 1")
        val stmt = assertIs<Stmt.Sniff>(program.stmts[0])
        val cmp = assertIs<Expr.Compare>(stmt.cond)
        assertEquals(CompareOp.LT, cmp.op)
        assertIs<Expr.VarRef>(cmp.left)
        assertEquals("hp", (cmp.left as Expr.VarRef).name)
        val num = assertIs<Expr.Num>(cmp.right)
        assertEquals(1L, num.value)
    }
    @Test
    fun `purr_at and hiss_at carry the label name without colon`() {
        val program = Parser.parse("purr_at :DEAD\nhiss_at :ALIVE")
        val purr = assertIs<Stmt.PurrAt>(program.stmts[0])
        assertEquals("DEAD", purr.label)
        val hiss = assertIs<Stmt.HissAt>(program.stmts[1])
        assertEquals("ALIVE", hiss.label)
    }
    @Test
    fun `unknown command throws ParseError`() {
        assertFailsWith<ParseError> {
            Parser.parse("frobnicate 1 2")
        }
    }
    @Test
    fun `sniff with greater-than comparison`() {
        val program = Parser.parse("sniff \$hp > 1")
        val stmt = assertIs<Stmt.Sniff>(program.stmts[0])
        val cmp = assertIs<Expr.Compare>(stmt.cond)
        assertEquals(CompareOp.GT, cmp.op)
        val left = assertIs<Expr.VarRef>(cmp.left)
        assertEquals("hp", left.name)
        val right = assertIs<Expr.Num>(cmp.right)
        assertEquals(1L, right.value)
    }
    @Test
    fun `sniff with greater-than-or-equal comparison`() {
        val program = Parser.parse("sniff \$hp >= 1")
        val stmt = assertIs<Stmt.Sniff>(program.stmts[0])
        val cmp = assertIs<Expr.Compare>(stmt.cond)
        assertEquals(CompareOp.GTE, cmp.op)
    }
    @Test
    fun `sniff with less-than-or-equal comparison`() {
        val program = Parser.parse("sniff \$hp <= 1")
        val stmt = assertIs<Stmt.Sniff>(program.stmts[0])
        val cmp = assertIs<Expr.Compare>(stmt.cond)
        assertEquals(CompareOp.LTE, cmp.op)
    }
    @Test
    fun `sniff with not-equal comparison`() {
        val program = Parser.parse("sniff \$hp != 1")
        val stmt = assertIs<Stmt.Sniff>(program.stmts[0])
        val cmp = assertIs<Expr.Compare>(stmt.cond)
        assertEquals(CompareOp.NEQ, cmp.op)
    }
    @Test
    fun `ParseError without originPath omits the file prefix`() {
        val err = ParseError("oops", SourcePos(3, 5))
        assertEquals("oops at line 3, col 5", err.message)
    }
    @Test
    fun `ParseError with originPath includes the file in the message`() {
        val err = ParseError("oops", SourcePos(3, 5), "library.cato")
        assertEquals("oops in library.cato at line 3, col 5", err.message)
    }
    @Test
    fun `include with relative path resolves against basePath`(@TempDir tmp: Path) {
        val lib = tmp.resolve("lib.cato").toFile()
        lib.writeText(":GREET\n  meow \"hi from lib\"\n  jump :end\n:end\n")
        val main = tmp.resolve("main.cato").toFile()
        main.writeText("include \"lib.cato\"\nmeow \"main done\"")
        val program = Parser.parse(main.readText(), main.absolutePath)
        val labels = program.stmts.filterIsInstance<Stmt.Label>().map { it.name }
        assertTrue("GREET" in labels, "expected GREET label from included file, got $labels")
        assertTrue("end" in labels, "expected end label from included file")
        assertTrue(labels.any { it.startsWith("__include_skip_") }, "expected skip-guard label from include wrap, got $labels")
    }
    @Test
    fun `include with missing file throws ParseError with originPath`(@TempDir tmp: Path) {
        val main = tmp.resolve("main.cato").toFile()
        main.writeText("include \"nope.cato\"")
        val ex = assertFailsWith<ParseError> {
            Parser.parse(main.readText(), main.absolutePath)
        }
        assertTrue("nope.cato" in (ex.message ?: ""), "expected missing path in message, got: ${ex.message}")
        assertEquals(main.absolutePath, ex.originPath)
    }
    @Test
    fun `cyclic include is detected with chain message`(@TempDir tmp: Path) {
        val a = tmp.resolve("a.cato").toFile()
        val b = tmp.resolve("b.cato").toFile()
        a.writeText("include \"b.cato\"")
        b.writeText("include \"a.cato\"")
        val ex = assertFailsWith<ParseError> {
            Parser.parse(a.readText(), a.absolutePath)
        }
        val msg = ex.message ?: ""
        assertTrue("a.cato" in msg && "b.cato" in msg, "expected cycle chain in message, got: $msg")
    }

}