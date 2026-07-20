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
| Analyzer: source-ordered undefined-variable checks in top-level `set` RHS expressions | **partially shipped (Phase B.2)** | `CatoScriptAnalyzer.kt`; `AnalyzerError(message, pos)`. Full `Stmt` walk plus basket/label/arity resolution is B.3 — see §6.1 |
| `cato compile <file>` core path | **partially shipped (Phase B.2)** | parses → runs the partial analyzer → prints serialized AST JSON to stdout, or prints all analyzer errors. Does **not** write a `.cato.json` sidecar; B.7 MW4 is pending — see §2.1 |
| Interpreter loop | **shipped** | `Interpreter.kt` |
| `meow`, `set`, `sniff`, `purr_at`, `hiss_at`, `jump` | **shipped** | exactly these six commands |
| `include` (parser-only inlining) | **shipped** | `RealParser.kt` line 70 |
| Labels (`:NAME`) | **shipped** | case-sensitive; parameter list parsed (`:NAME $a $b`) and bound at runtime — see §5. **Phase B.8 retires the parameter form** — labels stay as naked-goto / `purr_at` / `hiss_at` targets and fall-through markers only |
| Comments (`# ...`) | **shipped** | whole-line only, not trailing |
| Six-way comparisons (`<`, `<=`, `==`, `!=`, `>=`, `>`) | **shipped** | `Expr.Compare` + `CompareOp` |
| String interpolation (`"$var"`) and `\$` escape | **shipped** | `parseString` |
| CatoHost SPI (14 methods) | **declared** | only `print` is currently called by the interpreter |
| `NullHost` / `ConsoleHost` | **shipped** | `runtime/` |
| Step budget (`maxTotalSteps`) | **shipped** | default 1,000,000 |
| Call depth budget (`maxCallDepth`) | **shipped** | default 64, enforced on `jump :NAME args` calls — see §5.4. **Phase B.8 routes the same check through the new `name(args)` call form** |
| `maxStepsPerTick`, `seed` policy fields | **declared, never read** | scaffolding for future hosts |
| Label parameters + call stack (`:GREET $name`, `jump :GREET arg`, `jump :end`) | **shipped** | the underlying mechanism (label params + call stack + return opcode + `maxCallDepth=64`) shipped in Phase B.6. **Phase B.8 supersedes the surface syntax** — the AST/interpreter plumbing stays, but `jump :NAME args` and `jump :end` are replaced by `name(args)` / `return`, and `:NAME $a $b` is replaced by `basket name $a $b ... end_basket`. See §5, §13.2 |
| `basket` / `end_basket` / `return` / `name(args)` call form | **shipped (Phase B.8)** | replacement surface for the B.6 label-params pattern. The mechanism underneath is the B.6 plumbing; the keywords are new. See §5.5, §13.2 |
| `for` / `end_for` / `[]` / list literals | **planned** | §5.9 |
| Bracket operators (`[is]`, `[over]`, `[first]`, `[sort]`, etc.) | **planned** | §14 |
| `let` (opt-in types) | **planned** | §5.9 |
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
│   ├── Interpreter.kt                  # the loop. one Int ip, mutable call stack for label params.
│   ├── InterpreterPolicy.kt            # maxStepsPerTick / maxTotalSteps / maxCallDepth(64) / seed
│   ├── InterpreterResult.kt            # sealed Completed | BudgetExceeded | RuntimeError
│   ├── ExecutionLimit.kt               # ExecutionLimitReached exception
│   ├── ScriptContext.kt                # SCAFFOLD — not used by Interpreter.run
│   ├── ThreadHandle.kt                 # SCAFFOLD — not used by Interpreter.run
│   └── Value.kt                        # sealed Value = Num(Long) | Str | Bool
├── runtime/
│   ├── CatoHost.kt                     # 14-method SPI
│   ├── NullHost.kt                     # object — silent defaults
│   └── ConsoleHost.kt                  # class — stdout/stderr/exit
├── analyzer/
│   └── CatoScriptAnalyzer.kt           # B.2 partial checker + AnalyzerResult / AnalyzerError(message, pos)
└── cli/RunScript.kt                    # `cato run` + partial `cato compile` entry

src/test/kotlin/com/catoscript/
├── parser/RealParserTest.kt            # 16 tests covering every parser shape + 3 include tests
├── parser/AstEmitTest.kt               # 1 eyeball smoke test (no assertions)
├── interpreter/InterpreterTest.kt      # 11 tests including `RecordingHost`
├── interpreter/LabelParamsInterpreterTest.kt  # 6 tests: bind, 2-params, return, arity, recursion, naked
├── interpreter/ScriptContextTest.kt    # data-class equality only — ScriptContext is unused
├── interpreter/ThreadHandleTest.kt     # data-class equality only — ThreadHandle is unused
└── runtime/NullHostTest.kt             # verifies silent defaults

samples/
├── include-demo-v1/main.cato           # include demo (runs today)
├── include-demo-v1/library.cato        # library for include demo (runs today, top-level gated)
├── misc/1-3.cato                       # Tier 1 + Tier 2 demo (runs today)
├── misc/1-4.cato                       # Tier 3 demo (runs today)
├── misc/grade.cato                     # Tier 3 if/else-if/else (runs today)
├── misc/analyzer-undef.cato            # B.2 compile-mode eyeball test (intentionally fails with 3 errors)
└── misc/hello.cato                     # minimal Tier 1 (runs today)
```

**The six success-path sample scripts listed above run in the current library.** Verified by `InterpreterTest.include skips library top-level code on load` and the test runs of the Tier 1/2/3 scripts. `samples/misc/analyzer-undef.cato` is different: it is an intentional compile-mode failure used to eyeball B.2's three expression paths; there is no analyzer unit test yet.

### 2.1 CLI run and compile paths

`RunScript.kt` accepts `cato run <file>`, `cato compile <file>`, or `cato <file>` (which defaults to run mode). Compile mode currently does this:

1. Parse the source to `Program`.
2. Print `Compiling and analyzing...` to stdout.
3. Run the partial B.2 analyzer described in §6.1.
4. On success, print `Successfully compiled. AST:` and the serialized AST JSON to stdout.
5. On failure, print every `error: <message>` to stderr, then `compilation failed: N error(s)`, and return `InterpreterResult.RuntimeError(summary, 0)`. The CLI's common result handler then prints `runtime error: compilation failed: N error(s) (step 0)` and exits 1.

Compile mode does **not** write a `.cato.json` file next to the source; that output behavior remains Phase B.7 MW4. The repo-root `cato.bat` now passes both `run` and `compile` through to `RunScript.kt`; the repo-root `cato.sh` remains a single-file run launcher and does not expose compile mode.

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
| `jump :LABEL` | `Stmt.Jump(label, args: List<Expr> = emptyList(), pos)` | Unconditional goto. If `label` matches a basket name, throws `jump :<name> targets a basket, not a label — use <name>(args) instead`. Otherwise resolves `targetIp = labels[label]` and sets `ip = targetIp`; throws `unknown label ':<name>'` if the label is missing. Does not consult `lastSniff`. Naked `jump :NAME` to a parameterless label shares the caller's scope. The previous two-branch handler (`stmt.label == "end"` as return opcode, `jump :NAME args` as call) was retired in Phase B.8; see §5.5 for the current call/return surface. | `RuntimeErrorException("unknown label ':<name>'")`, `RuntimeErrorException("jump :<name> targets a basket, not a label — use <name>(args) instead")` |
| `basket NAME $a $b ...` / `end_basket` | `Stmt.Basket(name, params: List<String>, body: List<Stmt>, pos)` | `basket NAME $a $b` opens a body; statements are collected into `Basket.body` until `end_basket` closes it. At runtime `Basket` is a no-op — the body runs only when called via `name(args)`. `basket` names that collide with reserved keywords throw `ParseError("basket name '<n>' is a reserved keyword")`. See §5.5, §13.2. |
| `name(arg, arg, ...)` | `Stmt.Call(name, args: List<Expr>, pos)` | Disambiguated by "line starts with identifier followed by `(`". At runtime: validate `args.size == baskets[name].params.size` else throw `arity mismatch on basket '<name>': expected <n> args, got <m>`; check `callStack.size < policy.maxCallDepth` else throw `call depth exceeded <n> (recursive basket calls)`; push `CallFrame(returnIp = baskets[name].bodyEndIp, callerVariables = variables.toMap())`; bind params via `for ((paramName, argExpr) in paramsList.zip(args)) variables[paramName] = eval(argExpr, variables)`; `ip = baskets[name].bodyStartIp`. See §5.5, §13.2. |
| `return` | `Stmt.Return(pos: SourcePos)` | Pop the top `CallFrame`, restore the caller's pre-call `variables` snapshot, set `ip = frame.returnIp`. Throws `return with no active call frame` when the stack is empty (same misuse class as the old `jump :end` runtime error — only the spelling changed). See §5.5, §13.2. |
| `include "path.cato"` | (no AST node — parser inlines) | Resolves path against `basePath`, recursively parses, wraps the inlined statements with `Jump __include_skip_N` ... `Label __include_skip_N` | `ParseError` variants: missing path quoting, missing file, cyclic include, unresolvable path |

**Implicit no-op commands** (recognized by the parser, do nothing at runtime):

| Form | AST | Runtime |
|---|---|---|
| `:NAME` / `:NAME $a $b ...` | `Stmt.Label(name, params: List<String> = emptyList(), pos)` | Records `name → ip` in the label map at `run()` start. Otherwise `ip++`. **The parameter form `:NAME $a $b` is the Phase B.6 mechanism — Phase B.8 retires it.** Going forward, labels stay as naked-goto / `purr_at` / `hiss_at` targets and as fall-through markers; basket bodies (the new Tier 5 shape) are declared with `basket NAME $a $b ... end_basket` instead. See §5 and §13.2. |
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

In `cato compile`, the B.2 analyzer reports this earlier only when the reference appears inside a `Stmt.Set` RHS. It does not yet inspect `meow`, `sniff`, call arguments, basket bodies, or other statement shapes; see §6.1 and §11.19.

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

**What this section covers, in two parts.** After Phase B.6, the call/return surface was `jump :NAME args` (call) plus `jump :end` (return opcode), with `:NAME $a $b` as the function-body declaration. **Phase B.8 replaces that surface** with `name(args)` (call) + `return` (return keyword), and `basket name $a $b ... end_basket` (body declaration). Labels stay as naked-goto / `purr_at` / `hiss_at` targets and as fall-through markers. The underlying mechanism (call stack, `maxCallDepth=64`, caller-variable restore) is the B.6 plumbing — B.8 retires the *syntax*, not the *machinery*.

§5.1–§5.4 describe what ships today (the B.6 surface). §5.5 documents the planned B.8 replacement at the level a player writing catoscript needs. The two halves intentionally read differently — the planned half is shorter because the readable syntax carries more of the meaning.

### 5.1 Declaration

```catoscript
:NAME
```

Becomes `Stmt.Label(name = "NAME")`. The parser strips the leading `:`, then `.trim()`. Anything after the colon that survives `.trim()` becomes the label name. **No character validation** — letters, digits, underscores, spaces all go into the name.

```catoscript
:START                     # → Label("START")
:   START                  # → Label("START")
:GREET $name               # → Label("GREET $name") — Phase B.6 form. Phase B.8 retires this; see §5.5
:123                       # → Label("123") — valid!
```

### 5.2 Lookup is case-sensitive

`:end` and `:END` are **two different labels**. The parser does no case folding (`trimmed.substring(1).trim()`). The interpreter uses a `Map<String, Int>` (Kotlin `String` equality is exact). So:

```catoscript
:end                       # declares "end"
jump :END                  # RuntimeError: unknown label ':END' (the map has "end", not "END")
```

**Always match case exactly between the label declaration and the label reference.**

### 5.3 `jump :end` IS the return opcode (case-sensitive lowercase) — Phase B.6 surface, superseded by B.8

`jump :end` has a special status **when a call frame is active** (i.e., the most recent `jump :NAME args...` pushed a frame — see §5.4). In that case `jump :end` is the return opcode: pop the top `CallFrame`, restore the caller's pre-call `variables` snapshot, and resume at `frame.returnIp`.

The label name is case-sensitive lowercase exactly: `jump :end` returns, `jump :END` does not (it is a regular label lookup, which throws `unknown label ':END'` unless a `:END` label exists). The substring `end` is only special as a literal label name equal to `"end"`.

```catoscript
jump :DRINK "milk"                     # call: bind "milk" → $beverage, run body, return
meow "main continues"                  # runs after DRINK returns
jump :DRINK "tea"                      # second call, also returns
meow "after tea"                       # runs after the second return

:DRINK $beverage                       # Label("DRINK", params = ["beverage"])
  meow "slurp $beverage"
  jump :end                            # return — pops the frame for :DRINK
```

Output:
```
slurp milk
main continues
slurp tea
after tea
```

**`jump :end` with no active call frame** throws `RuntimeErrorException("jump :end with no active call frame")`. This is the only way to misuse it — at top-level (no preceding labeled function call) `jump :end` is a runtime error.

Note that `:end` and `:END` are still two different labels per §5.2. The two are now distinct in one more way: only the lowercase spelling, in a `jump`, with an active frame, returns. Everything else is a regular label lookup.

### 5.4 Label parameters · binding, call stack, and `jump :end` — Phase B.6 surface

This is now the core of how catoscript calls labeled snippets. The AST shape, parser, and runtime all ship together (Phase B.6, end-to-end).

**The four building blocks (all in `Interpreter.kt`):**

- `private data class CallFrame(val returnIp: Int, val callerVariables: Map<String, Value>)` — what gets pushed when a call happens. `returnIp` is the ip to resume at when `jump :end` pops the frame; `callerVariables` is the caller's pre-call variable snapshot, restored in full on return (variables are call-scoped, not block-scoped — see "naked jumps" below).
- `val labelParams = buildLabelParamsMap(program)` — built once at the start of `run()`. `Map<String, List<String>>` from label name to its declared params. Only labels with a non-empty `params` list appear in the map (parameterless labels are not function calls; see below).
- `val labelBodyEnds = buildLabelBodyEnds(program)` — `Map<String, Int>` from label name to the ip *just past* the body's `jump :end` (the resume point when the function returns). For each parameterized label, scans forward for the first `jump :end` after the label definition; the body's resume-after position is `j + 1`, or `program.stmts.size` if no `jump :end` exists in the body.
- `val callStack = mutableListOf<CallFrame>()` — the actual stack. Pushed on call, popped on return.

**Two-branch `jump` handler (paraphrased from `Interpreter.kt:85-110`):**

```kotlin
is Stmt.Jump -> {
    if (stmt.label == "end") {                                // the return opcode
        val frame = callStack.removeLastOrNull()
            ?: throw RuntimeErrorException("jump :end with no active call frame")
        variables.clear()
        variables.putAll(frame.callerVariables)
        ip = frame.returnIp
    } else {
        val targetIp = labels[stmt.label]
            ?: throw RuntimeErrorException("unknown label ':${stmt.label}'")
        val paramsList = labelParams[stmt.label] ?: emptyList()
        if (stmt.args.isNotEmpty() || paramsList.isNotEmpty()) {
            if (stmt.args.size != paramsList.size)
                throw RuntimeErrorException("arity mismatch on ':${stmt.label}': expected ${paramsList.size} args, got ${stmt.args.size}")
            if (callStack.size >= policy.maxCallDepth)
                throw RuntimeErrorException("call depth exceeded ${policy.maxCallDepth} (recursive label calls)")
            callStack.add(CallFrame(
                returnIp = labelBodyEnds[stmt.label] ?: (ip + 1),
                callerVariables = variables.toMap(),
            ))
            for ((paramName, argExpr) in paramsList.zip(stmt.args))
                variables[paramName] = eval(argExpr, variables)
        }
        ip = targetIp                                         // naked jump lands here too
    }
}
```

**Three things to know about how this behaves:**

1. **"Function call" vs "naked jump" is decided by the label.** If the *target label* declares params (or the call site supplies args), it is a function call: arity is checked, depth is checked, a frame is pushed, params are bound. If the target label has no params and the call site supplies no args, it is a regular goto — no frame is pushed, the caller's variable map is shared with the labeled body. So `:SETX` (no params) jumped to with `jump :SETX` lets the body `set` into the caller's scope; the body's `meow` after the jump sees the new value. A naked jump to a parameterless label that contains `set $x 5` does mutate the caller.
2. **`returnIp` skips the body tail.** Because `labelBodyEnds[label]` points one past the body's `jump :end`, when `jump :end` returns the labeled body is *unreachable* again — control resumes immediately after the body's return, which is the position after the body's terminal `jump :end`. This is why `meow "after"` at top-level runs after a labeled function returns: the caller is positioned past the call, the body's resume-after position is also past the body's `jump :end`, and both are still where they were.
3. **Recursion is bounded by `policy.maxCallDepth` (default 64).** Each pushed frame counts; once `callStack.size == maxCallDepth`, the next call throws `call depth exceeded <n> (recursive label calls)`. The recursion guard exists to keep the step budget honest for runaway recursion.

**Worked example:**

```catoscript
jump :DRINK "milk"
jump :DRINK "tea"
meow "done"

:DRINK $beverage
  meow "slurp $beverage"
  jump :end
```

- `ip=0` → `Jump("DRINK", [Expr.Str("milk")])`. Not `:end`, label is `"DRINK"`, target resolves, `paramsList = ["beverage"]`, `args.size = 1 == params.size`. `callStack` empty so depth check passes. Push `CallFrame(returnIp = ?, callerVariables = {})`. `labelBodyEnds["DRINK"]` is the ip one past `jump :end` inside the body. Bind `$beverage = "milk"`. `ip` → body.
- body runs `meow "slurp milk"` then `Jump("end")`. Pop frame, restore empty caller vars, `ip = frame.returnIp` (position immediately after the body's `jump :end`).
- caller resumes at `ip = 1` → `Jump("DRINK", [Expr.Str("tea")])`. Same flow with `$beverage = "tea"`.
- then `meow "done"` → end of program → `Completed`.

Output:
```
slurp milk
slurp tea
done
```

**Known limitation — args still cannot contain internal whitespace.** `jump :GREET "two words"` fails at `parseExpr` for the trailing `words"` because args are tokenized on whitespace. Pass strings through a variable: `set $msg "two words"` then `jump :GREET $msg`. (The same parser gap affects all `Expr` args and is unchanged from the parser-only era.)

### 5.5 Baskets and calls · the current surface (Phase B.8)

Phase B.8 replaces the four-primitive B.6 surface (`jump :NAME args` + `jump :end` + `:NAME $a $b`) with a three-keyword shape that reads top-to-bottom as natural language. The work splits as follows:

| B.6 form (retired) | B.8 form (current) | What it is |
|---|---|---|
| `:GREET $name` | `basket greet $name ... end_basket` | function-body declaration |
| `jump :GREET "mochi"` | `greet("mochi")` | call site |
| `jump :end` (inside the body) | `return` | return keyword |

**Worked example — the B.8 shape** (canonical copy at `samples/misc/basket-explanation.cato`):

```catoscript
greet("mochi")
  basket greet $name
    meow "hello, $name"
    return
  end_basket

greet("luna")
```

Output:
```
hello, mochi
hello, luna
```

**Coexistence with labels.** `:NAME` labels survive B.8 — they stay as `purr_at` / `hiss_at` targets, naked-goto targets, and fall-through markers. `:NAME` cannot carry a parameter list in B.8; the colon-prefix stays a label-only affordance (baskets do not use it). Naked `jump :NAME` (no args) keeps working unchanged; `jump :NAME args` and `jump :end` are removed. Tier 3 demos (`1-4.cato`, `grade.cato`) keep working because they use only naked goto.

**Basket-call semantics — what the runtime does** (same plumbing as the B.6 label-params path):

- Build `baskets: Map<String, BasketInfo>` once at `run()` start (`name → bodyStartIp + bodyEndIp + params list`).
- `Stmt.Basket` is a no-op at runtime — the body runs only when called. `basket` declares; it does not execute.
- `Stmt.Call(name, args)` validates `args.size == baskets[name].params.size` else throws `arity mismatch on basket '<name>': expected <n> args, got <m>`; checks `callStack.size < policy.maxCallDepth` else throws `call depth exceeded <n> (recursive basket calls)`; pushes `CallFrame(returnIp = baskets[name].bodyEndIp, callerVariables = variables.toMap())`; binds params positionally via `for ((p, a) in paramsList.zip(args)) variables[p] = eval(a, variables)`; jumps to `bodyStartIp`.
- `Stmt.Return` pops the top frame, restores the caller's pre-call `variables` snapshot, sets `ip = frame.returnIp`. Throws `return with no active call frame` when the stack is empty.

**What the parser must do.** Recognize `basket NAME $a $b` as the body opener; collect following statements into `Basket.body` until `end_basket`. Recognize `return` as a no-arg statement. Recognize `name(arg, arg, ...)` (line starts with identifier followed by `(`) as `Stmt.Call`. Reject basket names that match any reserved keyword (`meow`, `set`, `sniff`, `purr_at`, `hiss_at`, `jump`, `include`, `basket`, `return`, `end_basket`) with `ParseError("basket name '<n>' is a reserved keyword")`. Disambiguate `name(...)` from expressions like `meow` (a known command) by checking the leading-identifier rule.

**What the editor must do.** `editor/syntaxes/catoscript.tmLanguage.json` adds `basket`, `end_basket`, `return` to the keyword set; the mid-line label-ref grammar rule is updated for `()` call punctuation. The basket body is a block in the same sense as `for ... end_for` will be in Tier 6 — indentation is the player's choice; the parser collects until `end_basket` regardless.

**Tests and samples.** New `BasketInterpreterTest` covers declare / single-arg call / multi-arg call / return / arity mismatch / depth exceeded / naked-jump-still-goto / basket with no params. `samples/misc/tier5-demo.cato` is rewritten to use the new shape (the four sections become four `basket` examples + one naked-goto example). `samples/misc/basket-explanation.cato` is the pedagogical reference — the worked example above is taken from it. Other samples (`1-4.cato`, `grade.cato`, `include-demo-v1/main.cato`) are audited for `jump :NAME args` and `jump :end`; naked `jump :NAME` gotos stay unchanged, call/return uses rewrite.

**Version bump.** `0.3.2-LOCAL` — breaking change to the call form. Any script using `jump :NAME args` or `jump :end` must rewrite; naked `jump :NAME` gotos keep working unchanged.

### 5.6 Phase B.6 reference · the complete shape as it currently ships

Phase B.6 ships. The complete shape:

- `Stmt.Label(name, params: List<String>, pos)` — declared params parsed (each must start with `$`, else `ParseError("label params must start with $: '<token>'")`).
- `Stmt.Jump(label, args: List<Expr>, pos)` — args are tokenized on whitespace, then each passed through `parseExpr` (string literals, `$var` references, numbers, comparisons).
- `Interpreter.run()` maintains `callStack: MutableList<CallFrame>`, plus the `labelParams` and `labelBodyEnds` lookup maps built once at `run()` start.
- `jump :END` (any non-`end` label) with args → validate arity → check depth → push frame → bind params → jump.
- `jump :END` (any non-`end` label) without args to a parameterless label → plain goto, no frame.
- `jump :end` → pop frame, restore caller's variables, resume at `frame.returnIp`. Throws `jump :end with no active call frame` if the stack is empty.
- Errors: `arity mismatch on ':<name>': expected <n> args, got <m>` and `call depth exceeded <n> (recursive label calls)`.

Tests: `LabelParamsInterpreterTest.kt` has six cases — bind at call site, two-params in declaration order, return resumes caller, arity mismatch throws, recursion past `maxCallDepth` throws, naked label call shares caller scope. All pass under `./gradlew test`.

The remaining gap is the args-cannot-contain-internal-whitespace parser limit (pre-existing, not Phase B.6 work).

**Phase B.8 retires this surface** — see §5.5 above. The mechanism (call stack, maxCallDepth, caller-variable restore) survives into B.8; only the syntax changes.

### 5.7 Duplicate labels throw at runtime

```catoscript
:LOOP
meow "first"
:LOOP                       # RuntimeError: duplicate label ':LOOP'
meow "second"
```

Labels are deduplicated when the `labels` map is built at the start of `run()`. The parser does NOT check for duplicates — this surfaces as a runtime error, not a parse error.

### 5.8 `purr_at` / `hiss_at` require a prior `sniff`

```catoscript
purr_at :DEAD               # RuntimeError: purr_at has no prior sniff
meow "this never runs"
```

`lastSniff` is initialized to `null`. Every `purr_at` and `hiss_at` checks it first.

### 5.9 Unknown label crash

```catoscript
jump :NOWHERE               # RuntimeError: unknown label ':NOWHERE'
```

`jump` (unconditional) does not require a prior sniff, but does require the label to exist.

### 5.10 What works today for "call a labeled block from many places"

The B.6 surface ships today, so the full set works today. **Phase B.8 retires (1) below** — that line rewrites to `basket greet $name ... return` plus `greet("mochi")` once B.8 lands (see §5.5). (2)–(4) stay unchanged because they do not depend on the B.6 surface.

1. **Labeled function with inputs.** `:GREET $name ... jump :end` then `jump :GREET "mochi"` — binds the arg, runs the body, returns to the caller (see §5.4). Multiple call sites, one body.
2. **Naked goto (no params, no args).** `jump :SKIP` past a block that should not run, or into a block that mutates the caller's scope via `set` (no frame pushed, see §5.4). Survives B.8 unchanged.
3. **Conditional branch (if/else made of labels).** `sniff` then `purr_at :TRUE_BRANCH` / `hiss_at :FALSE_BRANCH`. Each branch runs at most once per script. Survives B.8 unchanged.
4. **Unrolled body.** If you want the body to run three times without params, write three copies inline (or use a parameterized label called three times). Survives B.8 unchanged (the unrolled variant does not use `jump :NAME args`).

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
    data class Jump(val label: String, val args: List<Expr> = emptyList(), val pos: SourcePos) : Stmt
    data class Label(val name: String, val params: List<String> = emptyList(), val pos: SourcePos) : Stmt
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

### 6.1 Analyzer result and current B.2 walk

```kotlin
data class AnalyzerResult(val errors: List<AnalyzerError>) {
    fun hasErrors(): Boolean = errors.isNotEmpty()
}
data class AnalyzerError(val message: String, val pos: SourcePos)
```

`CatoScriptAnalyzer` owns private `errors: MutableList<AnalyzerError>` and `defined: MutableSet<String>` state. Every `analyze(program)` call clears both collections, walks `Program.stmts` in source order, and returns `AnalyzerResult(errors.toList())`.

The B.2 walk is deliberately partial:

- `Stmt.Set` adds `varName` to `defined` **before** analyzing its RHS. Earlier definitions are visible to later `set` lines, and a self-reference such as `set $x $x` is therefore accepted by this pass.
- Every other `Stmt` is currently ignored. B.3 owns `Meow`, `Sniff`, jumps, labels, baskets, returns, calls, comments, and empty statements, plus basket/label/arity resolution.
- `Expr.Num` is a no-op. `Expr.VarRef(name, pos)` emits `AnalyzerError("undefined variable: $name at line N, col M", pos)` when `name` is absent. `Expr.Compare` checks left then right.
- `Expr.Str` checks each `StrPart.Interpolation` by treating it as a `VarRef`. Because `StrPart.Interpolation` has no position, the diagnostic uses the enclosing `Expr.Str.pos`.

Diagnostics accumulate; analysis does not stop after the first miss. B.2 has an eyeball sample (`samples/misc/analyzer-undef.cato`) but no analyzer unit test.

---

## 7. Interpreter model

```kotlin
fun run(program: Program): InterpreterResult {
    val labels = buildLabelMap(program)         // throws on duplicate labels
    val labelParams = buildLabelParamsMap(program)   // only labels with non-empty params
    val labelBodyEnds = buildLabelBodyEnds(program)  // ip one-past body jump :end, per parameterized label
    val variables = mutableMapOf<String, Value>()
    val callStack = mutableListOf<CallFrame>()        // pushed on labeled call, popped on jump :end
    var lastSniff: Value.Bool? = null
    var ip = 0                                        // ONE Int. + a call stack for label params.
    while (ip < program.stmts.size) {
        if (stepsConsumed >= policy.maxTotalSteps) throw ExecutionLimitReached(stepsConsumed)
        when (program.stmts[ip]) {
            is Stmt.Empty, is Stmt.Comment, is Stmt.Label -> ip++
            is Stmt.Meow -> { host.print(valueToString(eval(program.stmts[ip].expr, variables))); ip++ }
            is Stmt.Set -> { variables[program.stmts[ip].varName] = eval(program.stmts[ip].expr, variables); ip++ }
            is Stmt.Sniff -> { lastSniff = eval(...).also { if (it !is Value.Bool) throw ... }; ip++ }
            is Stmt.PurrAt -> { if (lastSniff?.b == true) ip = labels[...] ?: throw ...; else ip++ }
            is Stmt.HissAt -> { if (lastSniff?.b == false) ip = labels[...] ?: throw ...; else ip++ }
            is Stmt.Jump -> {
                if (stmt.label == "end") {
                    val frame = callStack.removeLastOrNull()
                        ?: throw RuntimeErrorException("jump :end with no active call frame")
                    variables.clear(); variables.putAll(frame.callerVariables); ip = frame.returnIp
                } else {
                    val targetIp = labels[stmt.label] ?: throw RuntimeErrorException("unknown label ':${stmt.label}'")
                    val paramsList = labelParams[stmt.label] ?: emptyList()
                    if (stmt.args.isNotEmpty() || paramsList.isNotEmpty()) {
                        if (stmt.args.size != paramsList.size) throw RuntimeErrorException("arity mismatch ...")
                        if (callStack.size >= policy.maxCallDepth) throw RuntimeErrorException("call depth exceeded ${policy.maxCallDepth} (recursive label calls)")
                        callStack.add(CallFrame(returnIp = labelBodyEnds[stmt.label] ?: (ip + 1), callerVariables = variables.toMap()))
                        for ((paramName, argExpr) in paramsList.zip(stmt.args)) variables[paramName] = eval(argExpr, variables)
                    }
                    ip = targetIp
                }
            }
        }
        stepsConsumed++                          // every iteration costs one step, including Empty/Comment/Label
    }
    return InterpreterResult.Completed
}
```

Key invariants:

- **One `ip` only.** Plus a call stack (`MutableList<CallFrame>`) for labeled function calls. Naked jumps (no args, no params) do not push.
- **One variable map per program, restored on return.** Variables are call-scoped: when `jump :end` pops a frame, the caller's pre-call snapshot (`frame.callerVariables`) replaces the current map entirely. There is no block scope, no global vs local.
- **`lastSniff` is a single-slot global.** There is no stack of `sniff` results.
- **Every loop iteration counts as one step** toward `maxTotalSteps` (default 1,000,000).
- **Every `jump :NAME args...` call counts one** toward `policy.maxCallDepth` (default 64) — checked at the point of push.
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
    val maxCallDepth: Int = 64,            // read once per call-frame push on `jump :NAME args...`
    val seed: Long? = null                // declared, never read
)
```

- The total-step check is at the top of every iteration: `if (stepsConsumed >= policy.maxTotalSteps) throw ExecutionLimitReached(stepsConsumed)`.
- The call-depth check is once per `Jump` that is a function call: `if (callStack.size >= policy.maxCallDepth) throw RuntimeErrorException("call depth exceeded <n> (recursive label calls)")` (default 64). Tests can pin a tighter depth (e.g., `InterpreterPolicy(maxCallDepth = 4)`) to exercise the guard cheaply.
- `stepsConsumed` increments once per loop iteration regardless of statement type.
- `ExecutionLimitReached` is caught and returned as `InterpreterResult.BudgetExceeded(stepsConsumed)`.
- `maxStepsPerTick` is a knob for future per-frame hosts (per `InterpreterPolicy.kt` line 6 comment); the standalone REPL doesn't have frames.
- `seed` exists for future deterministic replay; nothing reads it yet.
- The CLI (`RunScript.kt`) constructs `Interpreter(host)` with the default policy only — there is no way to thread a different policy through `cato run` today.

---

## 11. Common pitfalls · read this before writing catoscript

Each item below is a real failure mode an AI assistant will produce without checking this section.

### 11.1 `jump :end` is no longer a return opcode (Phase B.8)

After B.8, `jump :end` is a regular label lookup — it tries to find `:end` in the labels map and throws `unknown label ':end'` if missing. The "end" name has no special meaning. Use `return` to exit a basket body; see §5.5.

### 11.2 Labels are case-sensitive

`:end` and `:END` are different labels. Match case exactly. The parser does not fold case.

### 11.3 Baskets and parameterless labels (Phase B.8)

`:GREET $name` no longer parses as a parameterized label — that surface was retired in B.8. The same intent is written `basket greet $name ... end_basket` (declaration) and `greet("mochi")` (call). Naked `jump :NAME` (no args) to a parameterless label still works as a goto and shares the caller's scope. See §5.5 for the current surface and §5.6 for the retired B.6 surface kept for historical reference.

### 11.4 No loops

There is no `for`, no `while`, no `repeat`. The only iteration is the implicit interpreter loop and conditional jumps back to a label (which works, but cannot count without arithmetic). To run something N times, you write N copies or use a label that you `jump :LOOP` from inside, gated by a `sniff` against a manually-set `$count` (since `scratch` increment doesn't exist).

### 11.5 No arithmetic

No `+`, `-`, `*`, `/`, `%`. No negative literals. No floats. `Expr.Num` is `Long` only. `grade.cato` hand-computes its sum because of this; see its comment: *"Compute the sum by hand. No arithmetic in the language yet."*

### 11.6 No list, array, or bracket syntax

`[]` is not in the parser. `for ... in ...` is not in the parser. There is no `set $xs [...]` shape. Tier 6 (§5.9) is the planned landing zone.

### 11.7 The current call surface is `name(args)`; `jump :NAME args` was retired in Phase B.8

`name(arg, arg, ...)` parses as `Stmt.Call(name, args, pos)` and resolves to a basket of the same name. The deprecated `jump :NAME args` surface (Phase B.6) throws at runtime — the `jump` handler no longer takes args and no longer binds to label params. To migrate an old script, rewrite `jump :NAME a b c` as `name(a, b, c)` and `:NAME $a $b` as `basket name $a $b ... end_basket`. See §5.5 for the current surface and §5.6 for the retired B.6 surface kept for historical reference.

### 11.7a `return` requires an active call frame (Phase B.8)

After Phase B.8 lands, `return` outside a basket body throws `RuntimeErrorException("return with no active call frame")`. It is the same misuse class as the current `jump :end` pitfall (§11.1) — `return` at top-level is a runtime error, not a silent no-op. Use `return` only inside a `basket ... end_basket` body. See §5.5.

### 11.7b `()` is the call site, not a function-call punctuation token (Phase B.8)

After Phase B.8 lands, `name(arg, arg, ...)` is parsed as a `Stmt.Call` *only when* the line starts with an identifier followed by `(`. This is the call site shape. It is not a general expression-punctuation token — `meow greet("mochi")` is parsed as `Meow(greet("mochi"))` if `greet` resolves to a basket call, but `1 + (2)` does not parse because there is no `+` operator and there is no general `()` grouping. Disambiguation is by "line starts with identifier followed by `(`"; misspelled basket names surface as runtime arity-mismatch errors, not parse errors. See §5.5.

### 11.8 No `std.*` namespace

`std.length("...")`, `std.cli.args()`, etc. throw `cannot parse expression 'std.cli.args()'`. Stdlib namespaces are §12 / §14 work.

### 11.9 No `let`, no `scratch`, no `bat`

All of these throw `unknown command '<word>'`. They are planned (§5.9, §2 audit) but not in the parser. Persistence (formerly Tier 7 `bury` / `dig`) is unlocked by `std.fs.write` / `std.fs.read` at Tier 13 — see devplan §14.

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

### 11.19 The B.2 analyzer is not full compile validation

`cato compile` currently checks undefined variables only in top-level `set` RHS expressions. Undefined reads in `meow`, `sniff`, basket bodies, and call arguments are not diagnosed yet, and basket/label/arity resolution is still B.3 work. Also, the target name is marked defined before its own RHS is checked, so `set $x $x` passes this analyzer slice even though running it from an empty environment still fails. Do not treat a successful B.2 compile as proof that the script is runtime-safe.

---

## 12. Sample scripts verified today

The success-path scripts below run today. The B.2 analyzer sample in §12.6 intentionally fails compile mode with three diagnostics.

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

### 12.6 `samples/misc/analyzer-undef.cato` — intentional B.2 compile failure

```catoscript
set $ok "ready"
set $a "I want $missing_a"
set $b $missing_b
set $c $missing_c < 10
meow "ok value: $ok"
```

The three failing `set` RHS expressions exercise string interpolation, a bare `VarRef`, and the left side of `Expr.Compare`. Compile mode reports all three before its summary:

```
error: undefined variable: $missing_a at line 2, col 1
error: undefined variable: $missing_b at line 3, col 1
error: undefined variable: $missing_c at line 4, col 1
compilation failed: 3 error(s)
```

This is an eyeball test, not an automated assertion. The final `meow` is not inspected until B.3 because B.2 ignores every non-`Set` statement.

---

## 13. Future state · what the language will look like

> **⚠️ CRITICAL.** Every feature in this section is **planned, not shipped**. The current engine will throw `ParseError` or `RuntimeErrorException` on the language syntax, or omit the named future behavior where a shipped CLI path is being extended (for example, B.2 compile mode does not create a sidecar file). Use this section to understand what catoscript *will* support once the devplan phases land, but do not treat these examples as current-library contracts.
>
> Each entry below cites the devplan section and phase for the feature, gives the exact syntax shape as the devplan describes it, and includes a short example. Where the devplan is explicit (e.g., `std.cli.args()`), the syntax is exact. Where the devplan only mentions a feature in passing (e.g., the §2 audit list of pure-interpreter-work commands), the syntax shape is a *target*, not a commitment — the parser may ship a slightly different shape when the phase lands.

### 13.1 Future commands

Per devplan §2 audit, Phase C, Tier 7. These are pure interpreter work or thin wrappers over declared CatoHost methods (which are not yet routed).

| Command (planned) | Phase | Devplan | What it does |
|---|---|---|---|
| `scratch $x + N` | C | §2 | Increment a variable |
| `bat $x - N` | C | §2 | Decrement a variable |
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
| `spring` | — | §2 | **Superseded by `return` (Phase B.8)** — was "early return"; the B.8 `return` keyword makes this planned command unnecessary |
| `swat` | — | §2 | Throw |

**Current engine behavior:** every one of these throws `ParseError("unknown command '<word>'")` (or `RuntimeErrorException("unknown label ':<name>'")` for `def_asset` / `end_asset` if they are interpreted as label declarations).

### 13.2 Future syntax

These change how a script reads, not just which commands exist.

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
  jump :END_OF_TEST

:test_adds_one
  set $items []
  set $items std.list.push($items, "yarn")
  sniff std.list.length($items) == 1
  hiss_at :FAIL_adds_one
  jump :END_OF_TEST

:FAIL_starts_empty
  meow "FAIL: list should start empty"
  jump :END_OF_TEST

:FAIL_adds_one
  meow "FAIL: push should add one item"

:END_OF_TEST
```

Each `:test_*` block is a separate sub-script; failures don't stop the others. Tests are labels (not baskets) so `jump :END_OF_TEST` is a naked goto past the `FAIL_*` labels — `jump :end` was retired in Phase B.8.

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

- **Pause / resume / `Stepper`** (§5.7) — scaffold types `ScriptContext` and `ThreadHandle` already exist (see §11.17) but `Interpreter.run()` doesn't use them. The `Stepper` interface lights up LSP debug features and REPL `:step`.

### 13.5 Future CLI tools

| Command | Phase | What it does |
|---|---|---|
| `cato compile <file>` sidecar output | B.7 MW4 | Extend the shipped B.2 core path to emit `.cato.json` next to the source. Current behavior is JSON on stdout only; see §2.1 |
| `cato fmt <file>` | G | Deterministic formatter |
| `cato test <file>` | G + std.test | Run `:test_*` labels |
| `cato` (REPL, no args) | F | Interactive prompt |
| `:tutorial` | F | Walks tiers 1–16 interactively |
| `:load <file>` | F | Run a `.cato` script from REPL |
| `:step` | F + G | Single-step one instruction |

### 13.6 Reading rule for this section

**Use this section to write code targeting the future engine** — i.e., when the user's tooling has the corresponding phase flag enabled, or when planning ahead.

**Do not use this section to write code against the current library.** Planned language examples fail with the errors noted inline; planned extensions to shipped CLI paths may run without producing the future result (for example, no `.cato.json` sidecar).

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
| `0.3.1-LOCAL` | Phase B.6 | shipped · label parameters + call stack + `jump :end` return opcode + `maxCallDepth=64` (6 new tests in `LabelParamsInterpreterTest.kt`) |
| `0.3.2-LOCAL` | Phase B.8 | parked — bumps when the basket / `return` / `name(args)` surface lands. **Breaking change to the call form** — any script using `jump :NAME args` or `jump :end` rewrites; naked `jump :NAME` gotos keep working unchanged. Underlying mechanism (call stack, `maxCallDepth`, caller-variable restore) is the B.6 plumbing. |
| `0.4.0-LOCAL` | Phase E | parked — bumps when KP-side click-to-line work makes error positions user-visible |
| `0.5.0-LOCAL` | Phase F | parked — bumps when CLI REPL ships |
| `0.6.0-LOCAL` | Phase G | parked — bumps when analyzer/formatter/stepper land |

Until `0.4.0-LOCAL` ships, do not generate code that relies on column-precise error positions.

---

## 16. Where to read more

- `AGENTS.md` — repo-wide rules, coding style, what the four documents are.
- `catoscript-devplan.md` — source of truth for what catoscript is and is intended to become. §5 improvements, §10 out of scope, §11 pedagogical positioning, §12 CLI + UI stdlib, §13 implementation discipline, §14 capability surface.
- `src/main/kotlin/com/catoscript/` — the source. Read it. It is small (under 1500 lines combined).
- `src/test/kotlin/com/catoscript/` — the executable specs. `InterpreterTest.kt` shows every runtime behavior with a passing assertion. `RealParserTest.kt` shows every parser shape.

**If anything in this reference disagrees with the source, the source wins.** This file is generated from a snapshot read; if you find a discrepancy, open a PR fixing the reference (not the source — the source is correct as of the last commit).
