# catoscript · AI coding assistant reference

> The authoritative shape of catoscript as it actually exists in this repo. **Verified by reading every source file end to end.** Companion to `catoscript-devplan.md` (which describes what catoscript is *intended* to become). This file describes what catoscript *does today*.
>
> **Use this file before writing any catoscript.** The sections that matter most for avoiding hallucinated code are **§1 Implementation status**, **§5 Labels and jumps**, and **§11 Common pitfalls**.

---

## 1. Implementation status · what actually ships

| Feature | Status | Notes |
|---|---|---|
| Lexer + parser + recursive-descent shape | **shipped** | `RealParser.kt` |
| Typed AST (`Stmt` / `Expr` / `StrPart`) | **shipped** | `Stmt.kt`, `Expr.kt`, `SourcePos.kt` |
| Interpreter loop | **shipped** | `Interpreter.kt` |
| `meow`, `set`, `sniff`, `purr_at`, `hiss_at`, `jump` | **shipped** | exactly these six commands |
| `include` (parser-only inlining) | **shipped** | `RealParser.kt` line 70 |
| Labels (`:NAME`) | **shipped** | case-sensitive, no parameters |
| Comments (`# ...`) | **shipped** | whole-line only, not trailing |
| Six-way comparisons (`<`, `<=`, `==`, `!=`, `>=`, `>`) | **shipped** | `Expr.Compare` + `CompareOp` |
| String interpolation (`"$var"`) and `\$` escape | **shipped** | `parseString` |
| CatoHost SPI (14 methods) | **declared** | only `print` is currently called by the interpreter |
| `NullHost` / `ConsoleHost` | **shipped** | `runtime/` |
| Step budget (`maxTotalSteps`) | **shipped** | default 1,000,000 |
| `maxStepsPerTick`, `seed` policy fields | **declared, never read** | scaffolding for future hosts |
| Label parameters (`:GREET $name`, `jump :GREET arg`) | **planned** | Phase B.6 |
| `for` / `end_for` / `[]` / list literals | **planned** | §5.9 |
| `()` call punctuation | **planned** | §5.9 |
| Bracket operators (`[is]`, `[over]`, `[first]`, `[sort]`, etc.) | **planned** | §14 |
| `let` (opt-in types) | **planned** | §5.9 |
| `bury` / `dig` (persistence) | **planned** | Tier 7 |
| `std.*` (cli, ui, time, fs, test, json, web, random, path, list, str) | **planned** | §12, §14 |
| Audio commands (`chirp`, `purr`, `hiss`, `vibrato`, `sample`) | **planned** | Phase C — `CatoHost` methods exist but are not routed |
| Screen commands (`scurry`, `groom`) | **planned** | Phase C |
| `sniff_env` (env lookup) | **planned** | Phase C |
| `scratch` / `bat` / `swat` / `nap` / `tumble` / `zoomies` / `knead` / `nine_lives` / `crouch` / `spring` / `def` / `def_asset` / `end_asset` | **planned** | mentioned in devplan §2 audit, not in parser |
| Arithmetic (`+`, `-`, `*`, `/`) | **not in scope** | §10 — see also "what catoscript will never add" |
| Loops beyond labels (`while`, `repeat`) | **not in scope** | §10 |
| Closures, lambdas, classes, async, regex, match | **not in scope** | §10 |
| `for ... in ...` (only after Phase B.6 + §5.9 land) | **planned** | |

> **Rule for AI assistants:** If a feature is marked **planned** or **not in scope**, do **not** generate code that uses it. If the user asks for it, surface the status and point at the devplan section. Do not invent syntax to make it "look like" the planned feature — there is no way to fake these without crashing the parser.

---

## 2. File and source layout

```
src/main/kotlin/com/catoscript/
├── lexer/                              # legacy/scratch — the active parser does NOT use a separate lexer
├── parser/
│   ├── RealParser.kt                   # the parser. 249 lines. line-by-line character parsing.
│   └── AstEmit.kt                      # JSON emit via kotlinx.serialization
├── ast/
│   ├── Stmt.kt                         # sealed Stmt (9 implementations)
│   ├── Expr.kt                         # sealed Expr (4 implementations) + CompareOp enum + StrPart
│   └── SourcePos.kt                    # SourcePos(line, column)
├── interpreter/
│   ├── Interpreter.kt                  # the loop. 90-ish lines. one Int ip, no call stack.
│   ├── InterpreterPolicy.kt            # maxStepsPerTick / maxTotalSteps / seed
│   ├── InterpreterResult.kt            # sealed Completed | BudgetExceeded | RuntimeError
│   ├── ExecutionLimit.kt               # ExecutionLimitReached exception
│   ├── ScriptContext.kt                # SCAFFOLD — not used by Interpreter.run
│   ├── ThreadHandle.kt                 # SCAFFOLD — not used by Interpreter.run
│   └── Value.kt                        # sealed Value = Num(Long) | Str | Bool
├── runtime/
│   ├── CatoHost.kt                     # 14-method SPI
│   ├── NullHost.kt                     # object — silent defaults
│   └── ConsoleHost.kt                  # class — stdout/stderr/exit
└── cli/RunScript.kt                    # `cato run <file.cato>` entry

src/test/kotlin/com/catoscript/
├── parser/RealParserTest.kt            # 16 tests covering every parser shape + 3 include tests
├── parser/AstEmitTest.kt               # 1 eyeball smoke test (no assertions)
├── interpreter/InterpreterTest.kt      # 11 tests including `RecordingHost`
├── interpreter/ScriptContextTest.kt    # data-class equality only — ScriptContext is unused
├── interpreter/ThreadHandleTest.kt     # data-class equality only — ThreadHandle is unused
└── runtime/NullHostTest.kt             # verifies silent defaults

samples/
├── include-demo-v1/main.cato           # include demo (runs today)
├── include-demo-v1/library.cato        # library for include demo (runs today, top-level gated)
├── misc/1-3.cato                       # Tier 1 + Tier 2 demo (runs today)
├── misc/1-4.cato                       # Tier 3 demo (runs today)
├── misc/grade.cato                     # Tier 3 if/else-if/else (runs today)
└── misc/hello.cato                     # minimal Tier 1 (runs today)
```

**All six sample scripts run in the current library.** Verified by `InterpreterTest.include skips library top-level code on load` and the test runs of the Tier 1/2/3 scripts.

---

## 3. Commands · shipped

The parser recognizes **exactly seven keywords**. Anything else is `ParseError("unknown command '<word>'")`.

| Keyword | AST | Runtime semantics | Errors thrown |
|---|---|---|---|
| `meow <expr>` | `Stmt.Meow(expr, pos)` | `host.print(valueToString(eval(expr)))`, then `ip++` | `RuntimeErrorException` if expr references undefined variable |
| `set $name <expr>` | `Stmt.Set(varName, expr, pos)` | `variables[varName] = eval(expr)`, then `ip++` | `ParseError("set expects a variable name like $name")` if no `$`; `ParseError("set expects $name followed by a value")` if no value after the name |
| `sniff <expr>` | `Stmt.Sniff(cond, pos)` | `eval(cond)` → must be `Value.Bool`, stored in `lastSniff`, then `ip++` | `RuntimeErrorException("sniff expects a boolean, got <v>")` if result is not Bool |
| `purr_at :LABEL` | `Stmt.PurrAt(label, pos)` | If `lastSniff?.b == true`: `ip = labels[label]`. Else `ip++` | `RuntimeErrorException("purr_at has no prior sniff")` if no sniff yet; `RuntimeErrorException("unknown label ':<name>'")` if label missing |
| `hiss_at :LABEL` | `Stmt.HissAt(label, pos)` | Mirror of `purr_at`. Jump if `lastSniff?.b == false` | `RuntimeErrorException("hiss_at has no prior sniff")` if no sniff yet; `RuntimeErrorException("unknown label ':<name>'")` if label missing |
| `jump :LABEL` | `Stmt.Jump(label, pos)` | Unconditional `ip = labels[label]`. **Does not consult `lastSniff`.** | `RuntimeErrorException("unknown label ':<name>'")` if label missing |
| `include "path.cato"` | (no AST node — parser inlines) | Resolves path against `basePath`, recursively parses, wraps the inlined statements with `Jump __include_skip_N` ... `Label __include_skip_N` | `ParseError` variants: missing path quoting, missing file, cyclic include, unresolvable path |

**Implicit no-op commands** (recognized by the parser, do nothing at runtime):

| Form | AST | Runtime |
|---|---|---|
| `:NAME` | `Stmt.Label(name, pos)` | Records `name → ip` in the label map at `run()` start. Otherwise `ip++`. |
| `# comment text` | `Stmt.Comment(text, pos)` | `ip++`. The comment text is never inspected at runtime. |
| blank line | `Stmt.Empty` (singleton) | `ip++`. The only `Stmt` without a `pos` field. |

---

## 4. Expressions · shipped

### 4.1 Number literals

`Expr.Num(value: Long, pos)`. Parser accepts only `0-9` digits (`trimmed.all { it.isDigit() }`). **No negatives, no floats, no decimals.**

```catoscript
set $x 10
meow 42
```

```catoscript
set $x -5      # ParseError: cannot parse expression '-5'
meow 1.5      # ParseError: cannot parse expression '1.5'
meow 0x10     # ParseError: cannot parse expression '0x10'
```

### 4.2 String literals and interpolation

`Expr.Str(parts: List<StrPart>, pos)` where `StrPart` is `Literal(text)` or `Interpolation(varName)`.

- Variables interpolate via `$name` (letter/digit/underscore chars).
- Literal `$` is `\$`.
- `$` not followed by a name char → `ParseError("'$' must be followed by a variable name")`.
- Unterminated `"` → `ParseError("unterminated string literal")`.
- No nested expressions inside interpolation.

```catoscript
set $name "mochi"
meow "hello, $name"           # → "hello, mochi"
meow "cost is \$5"            # → "cost is $5"
meow "a" "b"                  # ParseError: cannot parse expression '"a" "b"'
```

### 4.3 Variable references

`Expr.VarRef(name: String, pos)`. The `$` prefix is mandatory. `VarRef("x")` reads `variables["x"]`; missing → `RuntimeErrorException("undefined variable '$x'")`.

```catoscript
set $x 10
meow $x                       # → "10"
meow $y                       # RuntimeError: undefined variable '$y'
```

### 4.4 Comparisons

`Expr.Compare(op: CompareOp, left, right, pos)`. Six operators, parsed by **first occurrence** in the text (no precedence, no chaining):

| Source | Op |
|---|---|
| `$a < $b` | `LT` |
| `$a <= $b` | `LTE` |
| `$a == $b` | `EQ` |
| `$a != $b` | `NEQ` |
| `$a >= $b` | `GTE` |
| `$a > $b` | `GT` |

Comparison rules:

- `Num < Num`, `Num <= Num`, `Num == Num`, etc. → Long comparison
- `Str < Str` etc. → lexicographic (`String.compareTo < 0`)
- `Bool == Bool`, `Bool != Bool` → equality
- **Anything else** → `RuntimeErrorException("cannot compare <l> and <r> with <")` (or `==`)

```catoscript
sniff $hp < 10
sniff $name == "mochi"
sniff $alive != 1
```

**Chained comparisons do not work the way you'd expect.** `$a < $b < $c` parses as `Compare(LT, $a, $b)` then `parseExpr` is called on `" $b < $c"` again, which becomes the right-hand expression — i.e. it nests rather than chains. There is no short-circuit.

---

## 5. Labels and jumps · the gotcha section

This is the section that matters most. The bug we hit was here, and several other common AI mistakes live here too.

### 5.1 Declaration

```catoscript
:NAME
```

Becomes `Stmt.Label(name = "NAME")`. The parser strips the leading `:`, then `.trim()`. Anything after the colon that survives `.trim()` becomes the label name. **No character validation** — letters, digits, underscores, spaces all go into the name.

```catoscript
:START                     # → Label("START")
:   START                  # → Label("START")
:GREET $name               # → Label("GREET $name") — see §5.5
:123                       # → Label("123") — valid!
```

### 5.2 Lookup is case-sensitive

`:end` and `:END` are **two different labels**. The parser does no case folding (`trimmed.substring(1).trim()`). The interpreter uses a `Map<String, Int>` (Kotlin `String` equality is exact). So:

```catoscript
:end                       # declares "end"
jump :END                  # RuntimeError: unknown label ':END' (the map has "end", not "END")
```

**Always match case exactly between the label declaration and the label reference.**

### 5.3 `jump :end` has NO special meaning

This is the most common hallucination. `jump :end` is exactly equivalent to `jump :DEAD` or `jump :ANY_OTHER_LABEL`. The interpreter does a plain map lookup. The substring `end` has no special status.

```catoscript
:SAY_HELLO
  meow "hello!"
  jump :end                # looks up "end" in the label map

jump :SAY_HELLO            # never reached if no :end label exists
:END                       # declares "END" — does NOT match "end"
```

If no `:end` label exists, `jump :end` throws `RuntimeErrorException("unknown label ':end'")`. The script does not loop forever and does not exit cleanly.

### 5.4 There is no call stack · labels cannot return to caller

Every jump teleports `ip` to the target. **No caller is recorded.** This means:

```catoscript
:SAY_HELLO
  meow "hello!"
  jump :end                # teleports to wherever :end lives. does not "return".

jump :SAY_HELLO            # dead code (jumped past)
meow "between"
jump :SAY_HELLO            # dead code
:END                       # where :end lives — at the very end → script exits
```

Output: just `hello!`. The "between" meow never runs.

**There is no `def` keyword, no function-call mechanism, no parameters.** The "function called from multiple places, each call returns" pattern is exactly what Phase B.6 (label parameters) ships. Until then, the pattern cannot be expressed in valid catoscript.

### 5.5 Labels with parameters don't work yet

```catoscript
:DRINK $beverage           # parses as Label(name = "DRINK $beverage")
  meow "slurp $beverage"
  jump :end

jump :DRINK "milk"         # parses as Jump(label = "DRINK \"milk\"") — no args
```

The parser does not treat `$name` after the label as a parameter declaration. It treats the whole line as one label name. Phase B.6 fixes this by:

1. Changing `Stmt.Label` to `Label(name, params: List<String>, pos)`.
2. Changing `Stmt.Jump` to `Jump(label, args: List<Expr>, pos)`.
3. Adding a call stack to the interpreter.
4. Making `jump :end` pop the call stack and return to caller.

Until B.6 lands, do not write `:DRINK $beverage` or `jump :DRINK "milk"`. The parser accepts the syntax but the runtime will crash on undefined `$beverage` (or fail at lookup time).

### 5.6 Duplicate labels throw at runtime

```catoscript
:LOOP
meow "first"
:LOOP                       # RuntimeError: duplicate label ':LOOP'
meow "second"
```

Labels are deduplicated when the `labels` map is built at the start of `run()`. The parser does NOT check for duplicates — this surfaces as a runtime error, not a parse error.

### 5.7 `purr_at` / `hiss_at` require a prior `sniff`

```catoscript
purr_at :DEAD               # RuntimeError: purr_at has no prior sniff
meow "this never runs"
```

`lastSniff` is initialized to `null`. Every `purr_at` and `hiss_at` checks it first.

### 5.8 Unknown label crash

```catoscript
jump :NOWHERE               # RuntimeError: unknown label ':NOWHERE'
```

`jump` (unconditional) does not require a prior sniff, but does require the label to exist.

### 5.9 What works today for "call a labeled block from many places"

Without call-stack support, the only repeatable patterns are:

1. **Conditional branch (if/else made of labels).** `sniff` then `purr_at :TRUE_BRANCH` / `hiss_at :FALSE_BRANCH`. Each branch runs at most once per script.
2. **Unrolled body.** If you want the body to run three times, write three copies inline.
3. **Skip pattern.** `jump :SKIP` past a block that should not run. The block runs zero or one time depending on whether you jumped past it.

The "called from many places, returns to caller" pattern is B.6 work.

---

## 6. AST shapes · exact

```kotlin
// ast/Stmt.kt
sealed interface Stmt {
    data class Meow(val expr: Expr, val pos: SourcePos) : Stmt
    data class Set(val varName: String, val expr: Expr, val pos: SourcePos) : Stmt
    data class Sniff(val cond: Expr, val pos: SourcePos) : Stmt
    data class PurrAt(val label: String, val pos: SourcePos) : Stmt
    data class HissAt(val label: String, val pos: SourcePos) : Stmt
    data class Jump(val label: String, val pos: SourcePos) : Stmt
    data class Label(val name: String, val pos: SourcePos) : Stmt
    data class Comment(val text: String, val pos: SourcePos) : Stmt
    data object Empty : Stmt                       // the only Stmt without a pos field
}
data class Program(val stmts: List<Stmt>)

// ast/Expr.kt
sealed interface Expr {
    data class Str(val parts: List<StrPart>, val pos: SourcePos) : Expr
    data class Num(val value: Long, val pos: SourcePos) : Expr
    data class VarRef(val name: String, val pos: SourcePos) : Expr
    data class Compare(val op: CompareOp, val left: Expr, val right: Expr, val pos: SourcePos) : Expr
}
enum class CompareOp { LT, LTE, EQ, NEQ, GTE, GT }    // parser checks in this order
sealed interface StrPart {
    data class Literal(val text: String) : StrPart
    data class Interpolation(val varName: String) : StrPart
}

// ast/SourcePos.kt
data class SourcePos(val line: Int, val column: Int) {
    fun format(): String = "line $line, col $column"
}
```

**Every node except `Stmt.Empty` carries `pos: SourcePos`.** Column is intentionally coarse — every command on a line has `column = 1`. Don't rely on column for error messages.

---

## 7. Interpreter model

```kotlin
fun run(program: Program): InterpreterResult {
    val labels = buildLabelMap(program)         // throws on duplicate labels
    val variables = mutableMapOf<String, Value>()
    var lastSniff: Value.Bool? = null
    var ip = 0                                   // ONE Int. No call stack.
    while (ip < program.stmts.size) {
        if (stepsConsumed >= policy.maxTotalSteps) throw ExecutionLimitReached(stepsConsumed)
        when (program.stmts[ip]) {
            is Stmt.Empty, is Stmt.Comment, is Stmt.Label -> ip++
            is Stmt.Meow -> { host.print(valueToString(eval(program.stmts[ip].expr, variables))); ip++ }
            is Stmt.Set -> { variables[program.stmts[ip].varName] = eval(program.stmts[ip].expr, variables); ip++ }
            is Stmt.Sniff -> { lastSniff = eval(...).also { if (it !is Value.Bool) throw ... }; ip++ }
            is Stmt.PurrAt -> { if (lastSniff?.b == true) ip = labels[...] ?: throw ...; else ip++ }
            is Stmt.HissAt -> { if (lastSniff?.b == false) ip = labels[...] ?: throw ...; else ip++ }
            is Stmt.Jump -> { ip = labels[...] ?: throw ... }
        }
        stepsConsumed++                          // every iteration costs one step, including Empty/Comment/Label
    }
    return InterpreterResult.Completed
}
```

Key invariants:

- **One `ip` only.** No call stack. No return-to-caller.
- **One variable map per program.** No scoping. No globals vs locals.
- **`lastSniff` is a single-slot global.** There is no stack of `sniff` results.
- **Every loop iteration counts as one step** toward `maxTotalSteps` (default 1,000,000).
- **`maxStepsPerTick` and `seed` are declared but never read.**
- **Only `host.print` is wired up today.** All 13 other CatoHost methods are interface declarations with implementations in `NullHost`/`ConsoleHost` but no interpreter call site.

---

## 8. `CatoHost` SPI

```kotlin
package com.catoscript.runtime

enum class Waveform { SQUARE, SINE, NOISE, SAMPLE }

interface CatoHost {
    fun print(line: String)
    fun playTone(freq: Double, durationMs: Int, waveform: Waveform)
    fun playSample(id: String)
    fun setCursor(x: Int, y: Int)
    fun clearScreen()
    fun envLookup(key: String): String?
    fun now(): Long

    fun args(): List<String>
    fun readLine(prompt: String?): String?
    fun exit(code: Int)
    fun printErr(line: String)

    fun readFile(path: String): String?
    fun writeFile(path: String, content: String): Boolean
    fun fileExists(path: String): Boolean
}
```

### Per-method status

| Method | Called by interpreter? | Use case |
|---|---|---|
| `print(line)` | **YES** (the only one) | `meow` output |
| `playTone(...)` | no | `chirp` / `purr` / `hiss` / `vibrato` (Phase C) |
| `playSample(id)` | no | `sample` (Phase C) |
| `setCursor(x, y)` | no | `scurry` (Phase C) |
| `clearScreen()` | no | `groom` (Phase C) |
| `envLookup(key)` | no | `sniff_env` / `env` (Phase C) |
| `now()` | no | `nap` (Phase C) |
| `args()` | no | `std.cli.args()` (§12) |
| `readLine(prompt)` | no | `ask` (§12) |
| `exit(code)` | no (CLI calls `kotlin.system.exitProcess` directly) | `std.cli.exit` (§12) |
| `printErr(line)` | no (CLI calls `System.err.println` directly) | `std.cli.print_err` (§12) |
| `readFile(path)` | no | `std.fs.read` (§14) |
| `writeFile(path, content)` | no | `std.fs.write` (§14) |
| `fileExists(path)` | no | `std.fs.exists` (§14) |

### Implemented hosts

| Host | `print` | `printErr` | `exit` | `now()` | Notes |
|---|---|---|---|---|---|
| `NullHost` | silent | silent | silent | `System.currentTimeMillis()` | the default for tests; nothing is observable |
| `ConsoleHost` | `println` | `System.err.println` | `kotlin.system.exitProcess(code)` | `System.currentTimeMillis()` | what `cato run` uses via `RunScript.kt` |
| `RecordingHost` | appends to a list | n/a | n/a | n/a | test fixture in `InterpreterTest.kt` only |

**`ReplHost`, `KernelPanicHost`, `WebHost` do not exist in source.** They appear in the devplan as targets for future work. Don't reference them as if they exist.

---

## 9. `Value` types

```kotlin
sealed interface Value {
    data class Num(val n: Long) : Value
    data class Str(val s: String) : Value
    data class Bool(val b: Boolean) : Value
}
```

**Only three.** No `Value.Null`, no `Value.List`, no `Value.Double`. The comment in `Value.kt` makes the gap explicit:

> Num is Long for now (no decimals yet). Bool comes out of Compare. Nothing else exists yet — lists, nulls, and doubles land when their features ship.

`meow` converts via `valueToString` which exhaustively handles all three. Comparisons on mixed types throw.

---

## 10. Step budget and policy

```kotlin
data class InterpreterPolicy(
    val maxStepsPerTick: Int = 100,        // declared, never read
    val maxTotalSteps: Long = 1_000_000,  // read once per loop iteration
    val seed: Long? = null                // declared, never read
)
```

- The check is at the top of every iteration: `if (stepsConsumed >= policy.maxTotalSteps) throw ExecutionLimitReached(stepsConsumed)`.
- `stepsConsumed` increments once per loop iteration regardless of statement type.
- `ExecutionLimitReached` is caught and returned as `InterpreterResult.BudgetExceeded(stepsConsumed)`.
- `maxStepsPerTick` is a knob for future per-frame hosts (per `InterpreterPolicy.kt` line 6 comment); the standalone REPL doesn't have frames.
- `seed` exists for future deterministic replay; nothing reads it yet.
- The CLI (`RunScript.kt`) constructs `Interpreter(host)` with the default policy only — there is no way to thread a different policy through `cato run` today.

---

## 11. Common pitfalls · read this before writing catoscript

Each item below is a real failure mode an AI assistant will produce without checking this section.

### 11.1 `jump :end` does NOT mean "return to caller"

`jump :end` is a regular label lookup. There is no special handling of the substring `end`. If no `:end` label exists, the script crashes. If `:end` exists at the bottom of the script, `jump :end` exits the script. **There is no call stack and no implicit return.**

### 11.2 Labels are case-sensitive

`:end` and `:END` are different labels. Match case exactly. The parser does not fold case.

### 11.3 Label parameters don't work yet

`:GREET $name` is parsed as `Label("GREET $name")` — a single label name with a literal `$` in it. `jump :GREET "mochi"` is parsed as `Jump("GREET \"mochi\"")`. Neither has any parameter mechanism. The call-stack + label-params work is Phase B.6.

### 11.4 No loops

There is no `for`, no `while`, no `repeat`. The only iteration is the implicit interpreter loop and conditional jumps back to a label (which works, but cannot count without arithmetic). To run something N times, you write N copies or use a label that you `jump :LOOP` from inside, gated by a `sniff` against a manually-set `$count` (since `scratch` increment doesn't exist).

### 11.5 No arithmetic

No `+`, `-`, `*`, `/`, `%`. No negative literals. No floats. `Expr.Num` is `Long` only. `grade.cato` hand-computes its sum because of this; see its comment: *"Compute the sum by hand. No arithmetic in the language yet."*

### 11.6 No list, array, or bracket syntax

`[]` is not in the parser. `for ... in ...` is not in the parser. There is no `set $xs [...]` shape. Tier 6 (§5.9) is the planned landing zone.

### 11.7 No function-call syntax

`()` is not in the parser. `greet("mochi")` would throw `cannot parse expression 'greet("mochi")'`. Tier 5 with `()` punctuation ships via §5.9 + B.6.

### 11.8 No `std.*` namespace

`std.length("...")`, `std.cli.args()`, etc. throw `cannot parse expression 'std.cli.args()'`. Stdlib namespaces are §12 / §14 work.

### 11.9 No `let`, no `bury`, no `dig`, no `scratch`, no `bat`

All of these throw `unknown command '<word>'`. They are planned (§5.9, Tier 7, §2 audit) but not in the parser.

### 11.10 `purr_at` / `hiss_at` need a prior sniff

Without a preceding `sniff`, the script throws `purr_at has no prior sniff` or `hiss_at has no prior sniff`. There is no `lastSniff` stack — only the most recent sniff is remembered.

### 11.11 Duplicate labels throw at runtime, not parse time

The parser accepts duplicate `:NAME` lines. The interpreter's `buildLabelMap` throws `duplicate label ':NAME'` at the start of `run()`. Don't rely on the parser to catch this.

### 11.12 Comments must be on their own line

`meow "hello" # comment` does NOT work. The `#` is only recognized as a comment marker when it's the first non-whitespace character of the line. Anything after a `#` on a non-comment line is part of the command's `rest` and will likely throw.

### 11.13 Number literals are strict

`set $x 10` works. `set $x 10.5` throws. `set $x -5` throws. `set $x 0x10` throws. Only decimal non-negative integers.

### 11.14 Only `print` is wired up on the host

Any other `CatoHost` method exists in the interface and has a `NullHost`/`ConsoleHost` implementation, but the interpreter does not call them. Audio, screen, env, CLI, FS, network — all declared, none routed.

### 11.15 Column positions are always 1

`SourcePos.column` is intentionally coarse — every command on a line has `column = 1`. Don't write error messages that rely on column precision.

### 11.16 No trailing `//` Kotlin comments in source

(Not a runtime pitfall but a coding rule per AGENTS.md §5.10.) Use `·`, `/`, or rewrite the sentence. Avoids breaking when text surfaces in player-facing output.

### 11.17 `ScriptContext` and `ThreadHandle` are scaffold types

They exist in source but `Interpreter.run()` does not construct or consume them. They are placeholders for future pause/resume and multi-threading work. Tests for them (`ScriptContextTest`, `ThreadHandleTest`) prove only that the data class shape is correct.

### 11.18 The kernel-panic-host isn't in the repo

`KernelPanicHost` is described in the devplan but does not exist in `src/main/`. Don't reference it as if it exists.

---

## 12. Sample scripts that actually run

These are the scripts verified to work today. The `InterpreterTest` suite runs variants of all of them.

### 12.1 `samples/misc/hello.cato` — Tier 1 minimum

```catoscript
meow "hello"
meow "world"
```

Output:
```
hello
world
```

### 12.2 `samples/misc/1-3.cato` — Tier 2 (variables + interpolation)

```catoscript
meow "hello"
meow "world"


set $name "deez nuts"

meow $name
```

Output:
```
hello
world
deez nuts
```

### 12.3 `samples/misc/1-4.cato` — Tier 3 (sniff + purr_at + jump + labels)

```catoscript
set $hp 5
sniff $hp < 10
purr_at :DEAD
meow "still alive"
jump :END
:DEAD
meow "game over"
:END
```

Output:
```
game over
```

### 12.4 `samples/misc/grade.cato` — Tier 3 if/else-if/else

See `samples/misc/grade.cato` for the full 83-line script. Output with `$avg = 91`:
```
score 1: 92
score 2: 87
score 3: 91
average: 91
grade: A
```

The script uses `sniff $avg >= 90` / `purr_at :A` / `hiss_at :NOT_A` then a sequence of fall-through labels (`:NOT_A` / `:NOT_B` / `:NOT_C` / `jump :F`). This is the canonical shape of multi-branch dispatch today.

### 12.5 `samples/include-demo-v1/main.cato` — `include`

```catoscript
include "library.cato"

meow "main: include skipped library top-level, now main runs"
```

Output (the library's top-level `meow` is gated by the synthetic skip label):
```
main: include skipped library top-level, now main runs
```

The library script (top-level skipped when included):
```catoscript
# samples/include-demo/library.cato
meow "lib: top-level should NOT print"

:UNREACHABLE_FROM_MAIN
  meow "lib: this is unreachable until Tier 5 lands"
```

---

## 13. Future state · what the language will look like

> **⚠️ CRITICAL.** Every feature in this section is **planned, not shipped**. The current engine will throw `ParseError` or `RuntimeErrorException` on any of it. Use this section to understand what catoscript *will* support once the devplan phases land, but do not run these examples against the current library — they will fail.
>
> Each entry below cites the devplan section and phase for the feature, gives the exact syntax shape as the devplan describes it, and includes a short example. Where the devplan is explicit (e.g., `std.cli.args()`), the syntax is exact. Where the devplan only mentions a feature in passing (e.g., the §2 audit list of pure-interpreter-work commands), the syntax shape is a *target*, not a commitment — the parser may ship a slightly different shape when the phase lands.

### 13.1 Future commands

Per devplan §2 audit, Phase C, Tier 7. These are pure interpreter work or thin wrappers over declared CatoHost methods (which are not yet routed).

| Command (planned) | Phase | Devplan | What it does |
|---|---|---|---|
| `scratch $x + N` | C | §2 | Increment a variable |
| `bat $x - N` | C | §2 | Decrement a variable |
| `bury $x` | — | §11 Tier 7 | Persist to disk |
| `dig $x` | — | §11 Tier 7 | Load from disk |
| `let $name: type = value` | — | §5.9, Tier 8 | Opt-in typed bind |
| `scurry X Y` | C | §2 | Move cursor (via `host.setCursor`) |
| `groom` | C | §2 | Clear screen (via `host.clearScreen`) |
| `chirp F D` | C | §2 | Square-wave tone (via `host.playTone(Waveform.SQUARE, ...)`) |
| `purr F D` | C | §2 | Sine-wave tone (via `host.playTone(Waveform.SINE, ...)`) |
| `hiss F D` | C | §2 | Noise tone (via `host.playTone(Waveform.NOISE, ...)`) |
| `vibrato F D` | C | §2 | Sine + LFO (via `host.playTone(Waveform.SINE, ...)` plus lib-side LFO) |
| `sample ID` | C | §2 | Play sample (via `host.playSample`) |
| `nap MS` | C | §2 | Sleep (via `host.now()` deltas) |
| `tempo N` | C | §2 | Set interpreter policy (steps per tick) |
| `sniff_env KEY` | C | §5.3 | Env lookup (via `host.envLookup`) |
| `def_asset` / `end_asset` | — | §2 | Heredoc string |
| `knead` | — | §2 | String concatenation |
| `tumble` | — | §2 | Reverse a string |
| `zoomies` | — | §2 | Random int |
| `nine_lives` | — | §2 | Try/catch |
| `crouch` | — | §2 | Conditional (low priority) |
| `spring` | — | §2 | Early return |
| `swat` | — | §2 | Throw |

**Current engine behavior:** every one of these throws `ParseError("unknown command '<word>'")` (or `RuntimeErrorException("unknown label ':<name>'")` for `def_asset` / `end_asset` if they are interpreted as label declarations).

### 13.2 Future syntax

These change how a script reads, not just which commands exist.

#### Label parameters + call stack (Phase B.6, Tier 5)

The Tier 5 pattern per devplan §11:

```catoscript
:DRINK $beverage                # declare a labeled snippet with a $beverage slot
  meow "slurp $beverage"
  jump :end                     # return — pops back to the caller

jump :DRINK "milk"              # call: bind "milk" → $beverage, run the label, return
jump :DRINK "tea"
```

Mechanism per devplan §6 Phase B.6:

1. `Stmt.Label` becomes `Label(name, params: List<String>, pos)`.
2. `Stmt.Jump` becomes `Jump(label, args: List<Expr>, pos)`.
3. Interpreter maintains `ArrayDeque<CallFrame>` (each frame holds `returnIp` + `bindings`).
4. Recursion guard (max depth 64).

**Current engine behavior:** `:DRINK $beverage` parses as `Label(name = "DRINK $beverage")` (one big label name). `jump :DRINK "milk"` parses as `Jump(label = "DRINK \"milk\"")`. Runtime crashes with `unknown label` or `undefined variable '$beverage'`. See §5.5 for the full trace.

#### `for $x in $items ... end_for` (Tier 6, §5.9)

```catoscript
set $toys ["ball", "mouse", "yarn"]
for $toy in $toys
  meow "$toy!"
end_for
```

`for` desugars to label-based dispatch per devplan §5.9 — *"Ten lines in the parser."*

**Current engine behavior:** `for` throws `ParseError("unknown command 'for'")`.

#### `[]` list literals (Tier 6, §5.9)

```catoscript
set $nums [1, 2, 3]
set $toys ["ball", "mouse", "yarn"]
```

**Current engine behavior:** `set $nums [1, 2, 3]` throws `cannot parse expression '[1, 2, 3]'`.

#### Bracket operators (Tier 6, §14)

Eleven bracket operators, grouped by intent per devplan §14:

| Category | Operators | Example |
|---|---|---|
| Declaration | `[is]` | `$state [is] loading` |
| Order | `[sort]`, `[shuffle]`, `[reverse]` | `$names [sort] $sorted` |
| Selection | `[first]`, `[last]` | `$deck [first] $top_card` |
| Shape | `[count]`, `[join "sep"]` | `$list [count] $n` |
| Test | `[empty?]`, `[contains? $x]` | `$list [empty?] $is_empty` |
| Walker | `[over]` | `$xs [over] $x` (sugar for `for`) |

Each bracket is sugar for a `std.list.*` or `std.random.*` function. The bracket family is **closed** after this round per devplan §14; further expansion goes through §10.

**Current engine behavior:** every one throws `cannot parse expression`.

#### `()` call punctuation (Tier 5, §14)

```catoscript
:GREET($name)                   # sugar for :GREET $name
  meow "hello, $name"
  jump :end

greet("mochi")                  # sugar for jump :GREET "mochi"
greet("mochi") greet("luna")    # two calls in a row
```

Underlying mechanism is the same as label parameters (§13.2.1). `()` is the readable surface; the underlying label mechanism is unchanged.

**Current engine behavior:** `greet("mochi")` throws `cannot parse expression 'greet("mochi")'`.

### 13.3 Future stdlib

Each namespace is a thin wrapper over the host seam or a small catoscript script in `samples/std/`. None ship today.

#### `std.cli` (Tier 9, §12)

```catoscript
set $args std.cli.args()                  # → ["--name", "mochi"]
set $verbose std.cli.flag("verbose")      # → true / false
std.cli.usage("usage: cato-run <file> [args]")
std.cli.exit(0)
std.cli.exit_fail("oops")                 # printErr + exit(1) in one
```

Backs onto `host.args()`, `host.exit()`, `host.printErr()` (declared in CatoHost, not routed today).

#### `std.ui` (Tier 10–11, §12)

| Cat word | What the player is doing | Syntax |
|---|---|---|
| `ask` | Asking for input | `ask "Volume (0-100)" $volume` |
| `choose` | Asking the player to pick one | `choose "Theme?" ["ocean", "midnight"] $theme` |
| `confirm` | Asking yes/no | `confirm "Save and restart?"` |
| `show` | Showing information | `show "Settings" "Tweak your terminal"` |
| `progress` | Showing progress | `progress 5 10` |
| `pause` | Waiting for Enter | `pause` |
| `menu` | Showing a full-screen menu | `menu :MAIN_MENU` |

Per devplan §12, no widget primitives (`box`, `text`, `button`). The seven `std.ui` words cover everything a CLI / terminal app needs.

#### `std.time` (Tier 12, §14)

```catoscript
set $now   std.time.now()              # unix millis, via host.now()
set $iso   std.time.iso($now)          # → "2026-07-16T14:30:00Z"
set $year  std.time.year($now)         # → 2026
set $after std.time.add($now, "1h")    # → $now + 3600000
```

Backs onto `host.now()` (declared, not routed).

#### `std.random` (Tier 12, §14)

```catoscript
set $die      std.random.dice("3d6")    # → 11
set $pick     std.random.pick($toys)    # → random element
set $shuffled std.random.shuffle($toys)
```

Pure in-process. `std.random.dice("3d6")` collapses the manual dice-rolling loop every script writes.

#### `std.fs` (Tier 13, §14)

```catoscript
set $text  std.fs.read("~/.config/cato/settings.cato")
set $lines std.fs.read_lines("log.txt")
std.fs.write("out.txt", $lines)
std.fs.append("log.txt", $line)
sniff std.fs.exists("~/.cato/init.cato")
purr_at :RUN_INIT
```

Backs onto `host.readFile`, `host.writeFile`, `host.fileExists` (declared, not routed).

#### `std.path` (Tier 13, §14)

```catoscript
set $parts std.path.split("/usr/local/bin/cato")    # → ["usr", "local", "bin", "cato"]
set $dir   std.path.folder("/usr/local/bin/cato")   # → "/usr/local/bin"  (was: dirname — folder reads better)
set $name  std.path.basename("/usr/local/bin/cato") # → "cato"
set $ext   std.path.extname("config.cato")          # → ".cato"
```

`folder` is the canonical name per devplan §14 (`dirname` was rejected as too jargony; `folder` reads better).

#### `std.test` (Tier 14, §14)

Run with `cato test script.cato`. The CLI REPL gets `:test` that runs all labels prefixed `:test_`:

```catoscript
:test_starts_empty
  set $items []
  sniff std.list.length($items) == 0
  hiss_at :FAIL_starts_empty
  jump :end

:test_adds_one
  set $items []
  set $items std.list.push($items, "yarn")
  sniff std.list.length($items) == 1
  hiss_at :FAIL_adds_one
  jump :end
```

Each `:test_*` block is a separate sub-script; failures don't stop the others.

#### `std.json` (Tier 15, §14)

```catoscript
set $text  std.fs.read("config.json")
set $cfg   std.json.parse($text)
set $theme $cfg.theme                          # → "ocean"
set $cfg   std.json.set($cfg, "lives", 9)
set $out   std.json.stringify($cfg)
std.fs.write("config.json", $out)
```

Pure in-process (JSON is text manipulation, no host call).

#### `std.web` (Tier 16, §14)

```catoscript
set $body std.web.fetch("https://api.example.com/cats")
set $cfg  std.json.parse($body)
set $cats std.json.get($cfg, "cats")
for $cat in $cats
  meow "$cat.name has $cat.lives lives"
end_for
```

Backs onto `host.fetchUrl(url: String): String?` — one new method on `CatoHost` (declared in §8 / §16 of this reference, not routed today).

**Per-host behavior (planned):**

| Host | `fetchUrl` |
|---|---|
| `NullHost` | `null` |
| `KernelPanicHost` | `null` (KP isn't a network host) |
| `ReplHost` | real HTTP via `java.net.http.HttpClient` |
| `WebHost` | real browser `fetch()` |

#### `std.list` (Tier 6, §14)

Backs the bracket operators. Per devplan §14, the namespace includes at minimum:

```catoscript
std.list.length($xs)              # → Int
std.list.push($xs, $x)            # → list with $x appended
std.list.first($xs)               # → first item
std.list.last($xs)                # → last item
std.list.sort($xs)                # → sorted
std.list.shuffle($xs)             # → randomized
std.list.reverse($xs)             # → reversed
std.list.contains($xs, $x)        # → Bool
std.list.join($xs, $sep)          # → String
std.list.is_empty($xs)            # → Bool
```

Bracket operators are sugar over this set. Additional functions may land as scripts need them.

#### `std.str` (§10 alternative table, §14)

Per devplan §10 alternative list and §14:

```catoscript
std.str.length($s)                # → Int
std.str.split($s, $sep)           # → list of strings
std.str.starts_with($s, $prefix)  # → Bool
std.str.ends_with($s, $suffix)    # → Bool
std.str.contains($s, $substr)     # → Bool
std.str.count($s, $substr)        # → Int (how many times $substr appears)
std.str.index_of($s, $substr)     # → Int (first index, or -1)
std.str.replace($s, $from, $to)   # → String
std.str.trim($s)                  # → String (no leading/trailing whitespace)
std.str.pad_left($s, $n)          # → String
std.str.pad_right($s, $n)         # → String
std.str.concat($a, $b)            # → String
```

Per devplan §10, regex is **not** in scope — these verbs cover 95% of what scripts reach for regex to do.

### 13.4 Future interpreter features

- **Call stack** (Phase B.6) — see §13.2.1 for the mechanism.
- **Recursion guard** (Phase B.6) — max depth 64.
- **Pause / resume / `Stepper`** (§5.7) — scaffold types `ScriptContext` and `ThreadHandle` already exist (see §11.17) but `Interpreter.run()` doesn't use them. The `Stepper` interface lights up LSP debug features and REPL `:step`.

### 13.5 Future CLI tools

| Command | Phase | What it does |
|---|---|---|
| `cato run <file>` | shipped | Run a `.cato` script |
| `cato compile <file>` | B.7 MW4 | Parse + emit `.cato.json` next to source |
| `cato fmt <file>` | G | Deterministic formatter |
| `cato test <file>` | G + std.test | Run `:test_*` labels |
| `cato` (REPL, no args) | F | Interactive prompt |
| `:tutorial` | F | Walks tiers 1–16 interactively |
| `:load <file>` | F | Run a `.cato` script from REPL |
| `:step` | F + G | Single-step one instruction |

### 13.6 Reading rule for this section

**Use this section to write code targeting the future engine** — i.e., when the user's tooling has the corresponding phase flag enabled, or when planning ahead.

**Do not use this section to write code against the current library.** Every example here will fail against the current engine with the errors noted inline.

**Cross-check:** if a feature here lands in the current engine, move its entry from this section into the appropriate current-state section (commands, syntax, stdlib, CLI). The reference and the devplan both stay source of truth for current state; this section is for the *target* state.

**Uncertainty markers:** where the devplan is explicit (e.g., `std.cli.args()`, `[sort]`, `for ... in ...`), the syntax is exact. Where the devplan only mentions a feature in passing (e.g., `def_asset`/`end_asset`, `knead`, `nine_lives`), the syntax shape is a *target* the parser may evolve toward when the phase lands, not a contract.

---

## 14. What catoscript will NEVER add (per devplan §10)

For AI assistants tempted to generate code with these features:

- Closures, lambdas, higher-order functions
- Classes / objects / inheritance
- Coroutines / `async` / `await`
- Operator overloading
- Module system / `import` (use `include`)
- Macros / compile-time evaluation
- Bytecode compiler / AOT to JVM
- Generic type parameters / variance (`list<str>` is sugar for the analyzer only)
- Pattern matching beyond `sniff`
- String interpolation with code execution (use `$var` interpolation + `meow`)
- Arithmetic (`+`, `-`, `*`, `/`) — explicitly NOT planned. Use stdlib if added; not currently in the language.
- A versioned-migration target forever — breaking changes between minors are fine if mechanical
- Turing-complete-in-practice for the player — the step budget is a feature

**The rule (devplan §10):** "Add a feature only if it collapses something the player currently has to write. Never add a feature for parity with another language."

---

## 15. Versioning

| Version | Phase | Notes |
|---|---|---|
| `0.1.0-LOCAL` | Phase A | empty library, package only, first publish |
| `0.2.0-LOCAL` | Phase B | lexer/parser/script-context moved |
| `0.3.0-LOCAL` | Phase B.5 + Phase C partial | real parser, AST, interpreter loop, `CatoHost`, `meow` routing. Audio/screen/env not yet routed. |
| `0.3.1-LOCAL` | Phase B.6 | parked — lands when label parameters + call stack land |
| `0.4.0-LOCAL` | Phase E | parked — bumps when KP-side click-to-line work makes error positions user-visible |
| `0.5.0-LOCAL` | Phase F | parked — bumps when CLI REPL ships |
| `0.6.0-LOCAL` | Phase G | parked — bumps when analyzer/formatter/stepper land |

Until `0.3.1-LOCAL` ships, do not generate code that uses label parameters. Until `0.4.0-LOCAL` ships, do not generate code that relies on column-precise error positions.

---

## 16. Where to read more

- `AGENTS.md` — repo-wide rules, coding style, what the four documents are.
- `catoscript-devplan.md` — source of truth for what catoscript is and is intended to become. §5 improvements, §10 out of scope, §11 pedagogical positioning, §12 CLI + UI stdlib, §13 implementation discipline, §14 capability surface.
- `src/main/kotlin/com/catoscript/` — the source. Read it. It is small (under 1500 lines combined).
- `src/test/kotlin/com/catoscript/` — the executable specs. `InterpreterTest.kt` shows every runtime behavior with a passing assertion. `RealParserTest.kt` shows every parser shape.

**If anything in this reference disagrees with the source, the source wins.** This file is generated from a snapshot read; if you find a discrepancy, open a PR fixing the reference (not the source — the source is correct as of the last commit).
