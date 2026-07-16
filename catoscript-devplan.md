# catoscript extraction · dev plan

> The catoscript language is leaving the building. Today its engine lives inside `:cato-kotlin` in `KernelPanic-Kotlin-Port`. The plan is to extract it into a standalone Kotlin/JVM library, `catoscript`, published to `mavenLocal()` during the port and consumed by Kernel Panic like any other dependency. This document is the source of truth for that work. Cross-referenced from `AGENTS.md` §2 and §8b.

**Naming (locked in):** `catoscript` is one word, camelcase. Not `cato-script`, not `KatoScript`, not `CATOScript`. The package root is `com.catoscript`. The in-repo module `:cato-kotlin` keeps the interim namespace `com.kp.cato.*` until the standalone repo's first commit renames packages in one sweep.

---

## 1. Why extract

Today, the language is *embedded* in a game repo. That has three costs:

1. **Improvements fight the game.** Better error messages, a real parser, a formatter, a debug-mode single-stepper — these are language concerns. In-repo they get deprioritized against "make the dashboard pixel-clone the OG."
2. **The language surface leaks.** `sniff_env` knows about Kernel Panic game state. `chirp` knows about KP audio. The boundary is in the wrong place.
3. **Reuse is impossible.** A CLI REPL, a VS Code extension, a web playground, a teaching tool — none can use catoscript today without dragging in the whole game.

Extraction fixes all three. The language becomes a library; KP becomes a host.

---

## 2. The seam — `CatoHost` SPI

Every command that currently reaches into game state goes through this interface. The interpreter takes a host in its constructor. The library ships a `NullHost` for tests and the standalone CLI REPL; KP provides `KernelPanicHost` in `:desktop`.

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
}

object NullHost : CatoHost {
    override fun print(line: String) {}
    override fun playTone(freq: Double, durationMs: Int, waveform: Waveform) {}
    override fun playSample(id: String) {}
    override fun setCursor(x: Int, y: Int) {}
    override fun clearScreen() {}
    override fun envLookup(key: String): String? = null
    override fun now(): Long = System.currentTimeMillis()
}
```

**Command → method mapping (initial audit):**

| catoscript command | CatoHost method | Notes |
|---|---|---|
| `meow` | `print` | KP prepends cursor position; lib leaves layout to host |
| `chirp` | `playTone(Waveform.SQUARE, ...)` | |
| `purr` | `playTone(Waveform.SINE, ...)` | |
| `hiss` | `playTone(Waveform.NOISE, ...)` | |
| `vibrato` | `playTone(SINE, ...)` + LFO stays in lib | LFO is a language feature |
| `sample` | `playSample` | KP falls back to hiss if asset missing |
| `scurry` | `setCursor` | |
| `groom` | `clearScreen` | |
| `sniff_env` | `envLookup` | KP injects world state via this |
| `nap` | `now()` | lib computes elapsed; host is the clock |
| `tempo` | (lib-only) | No host call needed; sets interpreter policy |

Everything else (`set`, `sniff`, `scratch`, `jump`, `purr_at`, `hiss_at`, `bat`, `swat`, `nine_lives`, `crouch`, `spring`, `def_asset`/`end_asset`, `knead`, `tumble`, `zoomies`, `def`) is pure interpreter work. No host call.

---

## 3. Target repo shape

```
catoscript/
├── AGENTS.md                       # language-first doc, no KP context
├── README.md                       # "write a .cato, run it" in 30 seconds
├── build.gradle.kts                # kotlin("jvm") + kotlinx.serialization
├── settings.gradle.kts
├── gradle/                         # committed Gradle wrapper
├── src/main/kotlin/com/catoscript/
│   ├── lexer/                      # tokens, line splitter (handles def_asset heredocs)
│   ├── parser/                     # text → AST (recursive descent; see §5)
│   ├── ast/                        # sealed Expr, Stmt, SourcePos
│   ├── interpreter/                # interpreter loop, ScriptContext, IP, instruction budget
│   ├── runtime/                    # CatoHost, NullHost, Waveform
│   ├── commands/                   # built-in stdlib (meow, set, sniff, jump, …)
│   ├── analyzer/                   # CatoScriptAnalyzer (moved from KP's CatoDE)
│   └── errors/                     # CatoScriptError, categories, Levenshtein suggestions
├── src/test/kotlin/com/catoscript/ # one test file per slice, fast, no host
├── samples/                        # .cato files; golden scripts library lives here too
└── tools/
    └── repl/                       # CLI REPL app (no Compose; uses NullHost or a TTY host)
```

**Publishing:** `./gradlew publishToMavenLocal` → `com.catoscript:catoscript:<version>` (the fat jar; KP pulls via `mavenLocal()` in `settings.gradle.kts`).

**Distribution:** `./gradlew shadowDistZip` → `build/distributions/catoscript-shadow-<version>.zip`. The zip contains three files: `bin/cato` (bash launcher), `bin/cato.bat` (Windows launcher), and `lib/catoscript-<version>.jar` (the same fat jar). End users extract the zip, add `bin/` to `PATH`, run `cato run <file.cato>`. The launchers are the shadow plugin's auto-generated `CreateStartScripts` output, renamed from `catoscript` to `cato`. The application plugin's main distribution is disabled because it expands every runtime dep into its own lib/*.jar — wrong shape for a fat-jar install.

**Direct jar:** `./gradlew shadowJar` → `build/libs/catoscript-<version>.jar` (2.5 MB fat jar with `Main-Class: com.catoscript.cli.RunScriptKt` in the manifest). Runs via `java -jar build/libs/catoscript-<version>.jar <file.cato>`. No classpath needed.

The application plugin's `jar` and `startScripts` tasks are disabled so the regular library jar (KP-consumable, no `Main-Class`) and its classpath-launcher scripts don't clobber the shadow jar's output at `build/libs/`. The `main` distribution's tasks (`distZip`, `distTar`, `installDist`, `assembleDist`) are disabled for the same reason.

---

## 4. What moves · what stays

### Out of KernelPanic-Kotlin-Port (eventually)

- All `com.kp.cato.*` source under `:cato-kotlin` — the candidate set for the new repo.
- `CatoScriptAnalyzer` (currently in `:desktop/catoDE/`).
- Error reporting with line numbers, `category` enum, and `suggestions` (Levenshtein).
- The Golden Scripts Library — moves to `catoscript/samples/` with a CLI loader (`cato run samples/03_modern/bouncing_face.cato`).

### Stays in KernelPanic-Kotlin-Port (always KP-specific)

- `:desktop` (Compose window, 5-pane grid, themed shell, onboarding).
- `KernelPanicHost : CatoHost` adapter in `desktop/src/main/kotlin/com/kp/desktop/KernelPanicHost.kt`.
- The 67-command canned-tools registry (whoami, ls, cat, cd, rm, chmod, …). These are **terminal shell** commands, not catoscript language primitives. They live behind `CommandSpec` in `:desktop`.
- VFS service, progression, save migration, cosmetics, themes.
- All Compose UI files.

### The namespace question

The interim `:cato-kotlin` keeps `package com.kp.cato.*`. The standalone repo uses `package com.catoscript.*`. When the standalone repo lands its first commit, it does a single rename pass over the moved source. Until then, do not preemptively rename — drift between the two namespaces costs more than the interim awkwardness.

---

## 5. Language improvements unlocked by extraction

These become possible once catoscript is its own repo. None are required for the extraction itself; they are the *reason* extraction is worth doing.

### 5.1 Real parser

Today's parser is "a simple line-by-line tokenizer" (`AGENT_catoscript.md` §4). It splits on spaces, respects quotes and brackets, and that's it. Replace with recursive descent producing a typed AST:

```kotlin
sealed interface Stmt {
    data class Set(val var: String, val expr: Expr) : Stmt
    data class Meow(val expr: Expr) : Stmt
    data class Jump(val label: String) : Stmt
    data class Label(val name: String) : Stmt
    // …
}

sealed interface Expr {
    data class Str(val value: String) : Expr
    data class VarRef(val name: String) : Expr
    data class Number(val value: Double) : Expr
    data class Binary(val op: Op, val left: Expr, val right: Expr) : Expr
    // …
}
```

Behavior-preserving. Every existing test still passes. Unlocks proper error messages with `SourcePos(line, column)`.

### 5.2 Configurable interpreter policy

The 100-instruction-per-frame budget becomes a `data class InterpreterPolicy(val maxStepsPerTick: Int = 100, val maxTotalSteps: Long = 1_000_000, val seed: Long? = null)`. Tests can pin the seed; KP keeps the existing budget as the default.

### 5.3 Generic env source

`sniff_env` becomes `env("KEY")` and `set_env` for symmetry. The host decides what the env *is*. KP injects game state. A teaching REPL injects a small mock. A future web playground injects URL params.

### 5.4 Standalone CLI REPL

`tools/repl/` is a pure-stdlib JVM app:

```bash
$ cato
catoscript> set $x 10
catoscript> meow "x is $x"
x is 10
catoscript> :exit
```

This is the **proof of independence** — a binary that runs catoscript with zero game knowledge.

### 5.5 `cato fmt` (formatter)

A deterministic formatter that rewrites scripts to a canonical layout. Standalone tool, no host. KP's CatoDE calls it on save.

### 5.6 Levenshtein suggestions, properly

Move the suggestion engine from the CatoDE two-pass analyzer into `com.catoscript.analyzer`. KP's notification pane displays the result; the standalone REPL prints them inline.

### 5.7 Debug-mode single-stepper

A `Stepper` interface lets a host run one instruction at a time, inspect the AST node being evaluated, and the `ScriptContext` before/after. KP's debug overlay (F2) uses it. The CLI REPL exposes it as `:step`.

### 5.8 LSP-shaped API (optional, later)

A long-term target: a language server that any editor can plug into. Not required for v0.1.0.

### 5.9 The "improvements" decision (added 2026-07-16)

> **Frame, not features.** catoscript is not "Kotlin for cats." It's a small DSL with a stdlib, opt-in types, and a host seam. The cat vocabulary is load-bearing — it's why the player bothers to learn the language. Don't lose the read-out-loud quality of scripts.

**The marketing line:**

> catoscript is the smallest language that can drive the terminal. Small enough to read out loud. Powerful enough that you don't have to write a `strlen` from scratch.

**The technical line:**

> catoscript is a flat-namespace, label-based DSL with a stdlib, opt-in types, and a host seam. It is not a general-purpose language. The `CatoHost` SPI is the boundary.

**The three improvements worth doing (in priority order):**

1. **A real `std.*` standard library.** Commands are flat globals today; the player re-implements string length, list iteration, basic math from scratch. Promote a small set to a `std.*` namespace without changing syntax:

    ```catoscript
    set $len std.str.length("meow")           # was: a label-soup loop
    set $parts std.str.split("a,b,c", ",")
    set $n std.list.length($parts)
    set $r std.math.random(1, 100)
    ```

    Why: collapses dozens of common patterns into one line. The language stays small at the surface (one import line) but grows underneath. **No grammar change required** — just a namespace lookup. `std` is a convention prefix, like Java's `java.util` or Python's built-ins.

2. **`for` over lists (the one missing control structure).**

    ```catoscript
    for $x in $items
      meow "got $x"
    end_for
    ```

    Why: the language has lists but no way to iterate them, which is weird. This is the *only* new control structure. Labels and `jump` still exist; `for` is sugar that desugars to label-based dispatch. Ten lines in the parser.

3. **Opt-in types via `let`.**

    ```catoscript
    set $x 10                # still works, untyped (default)
    let $name: str = "mochi"
    let $count: num = 0
    let $items: list<str> = []
    ```

    Why: optional. Scripts without `let` work exactly like today. Scripts with `let` get autocomplete, better errors, and the analyzer can catch "you tried to add a string to a number" before runtime. **This is the LSP payoff.** `set` stays; `let` is sugar for `set` + an analyzer annotation. Both ship; neither breaks the other.

**Why this set, and not others:**

- Every item on this list **collapses something the player currently has to write**. None of them add a new concept for the player to learn *without* removing one.
- Each ships in its own commit behind its own flag (`std.enable`, `for.enable`, `let.enable`) until it stabilizes. No big-bang rewrite of the parser.
- None of them require a host change. They're pure language work.

**See §10 for the explicit "no" list** — the things that look like good ideas and aren't.

---

## 6. Migration order

Each step is a commit. Each commit ships green. Each commit has a checkbox here and a corresponding lesson slice in `docs/KOTLIN_PORT_PLAN.md` §17.

### Phase A · Stand up the new repo (no KP changes yet)

- [x] Create `catoscript/` repo with empty Gradle Kotlin/JVM project
- [x] Add `AGENTS.md` (language-first), `README.md` (30-second quickstart)
- [x] Configure `publishToMavenLocal`, version `0.1.0-LOCAL`
- [ ] ~~Add CI: `./gradlew build` + `./gradlew test` on push~~ *(skipped — single contributor, local `./gradlew test` before push is enough; revisit if outside contributors land)*
- [x] First publish: `com.catoscript:catoscript:0.1.0-LOCAL` (empty library, package only)

> Phase A shipped the CatoHost SPI alongside the empty library, which is technically Phase C work. The reasoning: an empty jar is a hollow publish, and NullHost with its tests is the smallest possible proof that the SPI is real. Phase C's checkbox for "add CatoHost and NullHost" can be checked off when KP actually routes commands through it (the dependency direction the other way around).

### Phase B · Move the engine verbatim (behavior-preserving)

- [x] Move `lexer/` from `:cato-kotlin` to `catoscript`, package rename `com.kp.cato.lexer` → `com.catoscript.lexer`
- [x] Move `parser/` (line-splitter version) → `com.catoscript.parser`
- [ ] Move `ast/` (sealed Expr/Stmt) → `com.catoscript.ast` *(no source exists in KP yet — sealed Expr/Stmt land when the language commands move, see Phase B note)*
- [x] Move `interpreter/` (loop, ScriptContext, IP) → `com.catoscript.interpreter` *(ScriptContext, ThreadHandle, LastResult moved; interpreter loop does not exist in KP yet — lands as new work when language commands ship)*
- [ ] Move `commands/` (meow, set, sniff, jump, …) → `com.catoscript.commands` *(KP's `commands/` package is the 67-command canned-tools shell registry, which stays in KP per §4 — language commands like `meow`/`set`/`sniff`/`jump` are not implemented in KP and need to be written fresh here)*
- [x] Move all tests with their files; ensure `./gradlew test` is green *(19 tests across 6 classes: NullHostTest, TokenTest, ParserTest, ScriptContextTest, ThreadHandleTest, LastResultTest)*
- [x] Bump version to `0.2.0-LOCAL`, publish

> **Phase B scope correction.** The devplan listed five subpackages to move but only three had source in KP. What moved: `Token.kt` (lexer), `Parser.kt` (parser), `ScriptContext.kt` + `ThreadHandle.kt` + `LastResult.kt` (interpreter data carriers). What didn't exist in KP and needed to be written fresh: the interpreter loop (it lives where the language commands execute), the sealed `Expr`/`Stmt` AST (replacing the flat `Token` shape), and the language command implementations themselves (`meow`, `set`, `sniff`, `jump`, etc.).

> **That fresh work landed as Phase B.5** (commit `44cbdb4`) before Phase D, so the parser, AST, and interpreter loop exist without waiting for KP-side consumer work. The old flat `Token.kt` and line-splitter `Parser.kt` were deleted in the same commit; the new `RealParser.kt` is the only parser. Phase C's `meow`→`host.print` routing landed in B.5 too. What's still open in Phase C: the audio (`chirp`/`purr`/`hiss`/`vibrato`/`sample`), screen (`scurry`/`groom`), and env (`sniff_env` / future `env`) commands — they need both parser branches and interpreter branches written, with the corresponding `CatoHost` methods lit up.

### Phase C · Introduce the host SPI

- [x] Add `com.catoscript.runtime.CatoHost` interface and `NullHost` (per §2)
- [x] Refactor `Interpreter` constructor to take `host: CatoHost`
- [x] Route `meow` through `host.print` *(shipped in Phase B.5, commit `44cbdb4`)*
- [ ] Route `chirp`, `purr`, `hiss`, `vibrato`, `sample`, `scurry`, `groom` through `host.*` *(these language commands don't exist as primitives yet — they need to be written alongside the AST. The `CatoHost` methods are declared and `NullHost` provides defaults.)*
- [ ] Route `sniff_env` (or its §5.3 successor `env`) through `host.envLookup`
- [x] Add `NullHost` to test fixtures; tests stay green *(tests use a `RecordingHost` test fixture that captures `print` calls; `NullHost` is still the no-op default)*
- [x] Bump to `0.3.0-LOCAL`, publish

> Phase C is partially landed as part of the minimum viable loop slice. The interpreter takes CatoHost, and `meow` routes through `host.print`. The other CatoHost methods (audio, cursor, env, CLI, FS) are declared but unused — they'll light up as the corresponding language commands ship. A `ConsoleHost` was added for the CLI smoke-test path; it's a thin stdout implementation that the future `tools/repl/` CLI will replace.

> CatoHost + NullHost shipped early as part of Phase A (see note there). The remaining Phase C checkboxes depend on Phase B landing first — there's no Interpreter to refactor until the engine moves over. `meow` routing landed as part of Phase B.5 alongside the interpreter loop.

### Phase D · KP consumes the library

- [ ] Add `mavenLocal()` repository to KP `settings.gradle.kts`
- [ ] Add `com.catoscript:catoscript:0.3.0-LOCAL` dependency to `:desktop`
- [ ] Create `desktop/src/main/kotlin/com/kp/desktop/KernelPanicHost.kt` implementing `CatoHost`
- [ ] Delete KP's `:cato-kotlin` module entirely
- [ ] Update AGENTS.md §3 (test command) and §4 (module layout) to drop `:cato-kotlin`
- [ ] Run `./gradlew :desktop:run`; window opens; `whoami` works; a `.cato` script runs through the new path
- [ ] KP's `:cato-kotlin:test` is no longer a thing — tests live in the standalone repo

### Phase E · Real parser + AST (the fun begins)

- [x] Add recursive-descent parser to `com.catoscript.parser` *(shipped as `RealParser.kt` in Phase B.5, commit `44cbdb4`)*
- [x] Define `SourcePos(line, column)` and thread it through `Expr` / `Stmt` *(shipped: every `Stmt` and `Expr` carries `pos: SourcePos`; `ParseError` carries one too)*
- [x] All existing tests stay green; new tests cover error positions *(RealParserTest, InterpreterTest shipped alongside the parser; full suite `./gradlew test` is green)*
- [ ] Bump to `0.4.0-LOCAL`, publish *(the parser landed under the existing `0.3.0-LOCAL` bump; Phase E's own bump happens when its KP-side click-to-line work lands)*
- [ ] (KP side) CatoDE editor gains click-to-line on error toasts

> **Phase E actually landed as Phase B.5.** Commit `44cbdb4` ("Phase B.5: AST, recursive-descent parser, interpreter loop, host wiring") shipped the recursive-descent parser, the sealed `Expr`/`Stmt`/`CompareOp`/`StrPart` AST, `SourcePos` threaded through every node, and the interpreter loop that walks them — all the §5.1 / Phase E work. The phase-name drift ("B.5" vs "E") is documented because the devplan said E was after D, and D is KP-side work; running B.5 before D let the parser land without a downstream consumer. The Phase E bump to `0.4.0-LOCAL` is parked until the KP-side click-to-line work makes the error positions user-visible.

### Phase F · Standalone CLI REPL ships

- [ ] Create `tools/repl/` Gradle subproject (kotlin("jvm") application)
- [ ] Implement `ReplHost : CatoHost` using ANSI escape codes for cursor/clear
- [x] Ship `cato` launcher script *(shipped via the shadow distribution's `startShadowScripts` task with `applicationName = "cato"`. Two launchers: `bin/cato` (bash, Mark-as-Executable in install dir) and `bin/cato.bat` (Windows). Both auto-discover the fat jar at `../lib/catoscript-<version>.jar`. See §3 "Distribution" note.)*
- [x] README documents the 30-second quickstart *(README §2 has three paths: install via the zip, run via `java -jar`, and the dev launcher)*
- [ ] Bump to `0.5.0-LOCAL`, publish

> **Phase F partially shipped ahead of REPL.** The launcher scripts and the install distribution (`shadowDistZip`) shipped in commit `6c3ecf4` because the fat jar was the prerequisite for any user-facing install path. The `tools/repl/` subproject and the ANSI `ReplHost` are still pending — they need Phase G's analyzer / formatter / stepper to land first for `:tutorial` and `:step` to be meaningful. The Phase F bump to `0.5.0-LOCAL` happens when the REPL itself ships.

### Phase G · Move the analyzer + formatter + stepper

- [ ] Move `CatoScriptAnalyzer` from KP `:desktop/catoDE/` → `catoscript/analyzer/`
- [ ] Add `cato fmt` to `tools/repl/` (calls `analyzer.format()`)
- [ ] Add `Stepper` interface to `com.catoscript.interpreter`
- [ ] Implement `Stepper` in tests; expose `:step` in REPL
- [ ] Bump to `0.6.0-LOCAL`, publish
- [ ] (KP side) CatoDE editor calls the library analyzer; debug overlay (F2) uses the library stepper

### Phase I · Editor support (VS Code now, IntelliJ later)

- [x] Stand up `editor/` as a standalone VS Code extension module (TextMate grammar + theme + language config + manifest)
- [x] Mirror the Kernel Panic CatoDE palette (cyan / pink / yellow / purple / green / orange hex values from `cato/components/Editor.tsx`)
- [x] Cover the full KP keyword vocabulary (`cato/data/catoKeywords.ts`) so unimplemented commands still light up as a forward-looking spec
- [x] Document install: `cd editor && npx --yes @vscode/vsce package && code --install-extension catoscript-*.vsix`
- [x] Add `*.vsix` to `.gitignore` (build artifact, regenerated from sources)
- [ ] Decide whether IntelliJ support is worth the SDK lift, or whether VS Code stays the only editor
- [ ] If yes: ship `editor-intellij/` as a separate Gradle module with the IntelliJ Platform Plugin (see `docs/intellij-plugin-plan.md` for the plan)
- [ ] If no: document VS Code as the supported editor in README

> Phase I shipped VS Code support early (alongside Phase C) because the language is now usable end to end and an editor is the natural next tool. The IntelliJ piece is deferred until Tiers 9-11 ship and there's enough catoscript code in the wild that an IntelliJ user might actually want to edit it. The plan lives in `docs/intellij-plugin-plan.md` (gitignored personal notes) so the work doesn't get lost.

### Phase H · Real publishing (after API stabilizes)

- [ ] Pin `catoscript` version in KP `gradle.properties` (`catoscript.version=0.6.0`)
- [ ] Decide publishing target (Maven Central? Private GitHub Packages? JitPack?)
- [ ] Set up signing keys; configure `publishing { publications { mavenJava { … } } }`
- [ ] Drop `mavenLocal()` from KP `settings.gradle.kts`
- [ ] Cut `1.0.0` of `catoscript`; KP bumps its pin

---

## 7. Open questions

- [ ] **Namespace strategy during Phase B.** Do we land one PR per package, or one PR with all packages renamed? One PR is safer (single green); multiple PRs are easier to review. Default: one PR per phase, single commit at the end.
- [ ] **Sample program canonical home.** Golden Scripts Library — does it stay in KP as game content, or move to catoscript as language content? Lean: language content. KP imports it as a resource.
- [ ] **LSP-shaped API timing.** Phase I, II, or later? No commitment. Revisit when someone actually wants it.
- [ ] **TS original.** Do we keep KP's reference to the TS source (`KernelPanic-and-CatoScript-DE-Master/cato/`) forever, or does it become a Phase 0 reference only? Lean: forever. The TS file is the language's behavioral reference for any ambiguity.

---

## 8. Versioning rules

- `0.x.y-LOCAL` during the port. Bumps are explicit commits.
- After Phase H, SemVer: breaking changes bump `major`; new language features bump `minor`; internal fixes bump `patch`.
- KP's `catoscript.version` is in `gradle.properties`. Bumps are their own commits with a clear message: `bump catoscript 0.3.0 → 0.4.0 (real parser)`.
- The library's own version is in `catoscript/gradle.properties`. Bumps happen at the end of each phase.

---

## 9. What "done" looks like

The extraction is complete when:

- `catoscript` is a standalone Kotlin/JVM library on its own git timeline
- KP consumes it via a versioned Maven coordinate
- The CLI REPL runs any `.cato` script without touching KP code
- A second consumer (a teaching tool, a web playground, anything) can adopt the library in under an hour
- The 67-command canned-tools registry stays game-specific and doesn't leak into the library
- Kernel Panic's lessons focus on game-specific code; catoscript improvements live in the catoscript repo and arrive via dependency bumps

---

## 10. Out of scope (deliberate)

> This section is policy, not a backlog. Each item below was considered and rejected. Adding any of them requires an amendment to this section first.

The temptation: "catoscript should be a *real* language, so it should have…" — fill in the blank with familiar Kotlin/Python/Rust features. Most of those features are wrong for catoscript. Here's why.

### What catoscript will never be

- **Turing-complete-in-practice for the player.** The instruction budget per tick is a feature. It forces scripts to be small and inspectable. Removing it lets a player write a brute-forcer that freezes the host. The cap stays.
- **A general-purpose language.** No networking, no filesystem, no process spawning, no native interop. The host decides what crosses that boundary via `CatoHost`. A "send this over the wire" command would be a host method, not a language primitive.
- **A frontend for itself.** Scripts don't render UI. They emit commands that *some* host renders. KP renders them in a Compose terminal; the web playground renders them in a `<canvas>`; the CLI renders them in ANSI. The language doesn't know and doesn't need to.
- **A versioned-migration target forever.** Scripts are throwaway automation in the player's deck. Breaking changes between minor versions are fine if the migration is mechanical (`cato fmt` does it). No long-term compat promises beyond the major version.

### What catoscript will never add

These are the "looks like a good idea, isn't" list. Each one was considered and rejected for a specific reason.

| Feature | Why we say no |
|---|---|
| **User-defined functions with closures** | Adds real complexity to the parser + interpreter (capture analysis, scope rules). The label + `jump` system is enough for the player's mental model. The player doesn't write closures; they write scripts. |
| **Extension functions** | Cat-themed Kotlin sugar (e.g. `"meow".shout()`) that no player will discover without docs. Pure ergonomic cost, zero capability gain. |
| **Lambdas / higher-order functions** | `map`, `filter`, `reduce` — the player can `for` over a list. Lambdas add a syntax rule, a scoping rule, and a closure rule for no real win. |
| **Classes / objects / inheritance** | The game state is the host's job. The language doesn't model state, it *drives* state. Modeling state in the language duplicates the host's job and creates two sources of truth. |
| **Coroutines / async / `await`** | The interpreter loop *is* the cooperative scheduler. The language has nothing to await — every command runs synchronously inside the host's frame. Adding `async` would let the player write code that the host can't reason about step-budgets for. |
| **Operator overloading** | Cat-themed arithmetic (`+` that does string concat, `*` that does repetition) — clever once, confusing forever. Players should see `std.str.concat(a, b)` not `"a" + b` doing magic. |
| **Module system / packages** | One file = one script. `include` is the only seam needed. A package system without a real ecosystem is just naming ceremony. |
| **Macros / compile-time evaluation** | The player is writing runtime scripts, not building a language. Macros add a meta-language the player would have to learn to read other people's scripts. |
| **A bytecode compiler / AOT to JVM classes** | Worth doing eventually for performance, but not now. Premature optimization. Re-evaluate when someone has a real script that hits the step budget. |
| **Generic type parameters / variance** | Opt-in types are for *the analyzer*, not for runtime dispatch. `list<str>` is sugar for "the analyzer should treat this as a list of strings." No JVM-style generics, no variance rules, no type-class hierarchy. |
| **Pattern matching beyond `sniff`** | `sniff ==` covers 90% of what players do. Full pattern matching is a parser + analyzer project for marginal benefit. Revisit if the player base grows. |
| **String interpolation with code execution** | `${"$x + 1"}` style interpolation with embedded expressions — adds an expression grammar inside strings. Just use `knead` and concatenation. Players can read the current form. |

### The rule

> **Add a feature only if it collapses something the player currently has to write. Never add a feature for parity with another language.**

This is the only test. "Kotlin has it" is not a reason. "Python has it" is not a reason. "The player has to write the same 12 lines in every script" *is* a reason.

The three approved improvements in §5.9 (stdlib, `for`, opt-in `let`) all clear this bar. Everything in the table above doesn't.

### How to amend this section

If someone proposes a new feature later, the workflow is:

1. Open a doc PR adding the feature to a "Proposed" subsection of this section, with a "why it clears the bar" argument.
2. The proposal must name the **specific scripts that get shorter** because of the feature. "It would be cleaner" doesn't count; "this 30-line script becomes 5 lines" does.
3. Two-week waiting period. If nobody in that time posts a counter-example showing the script can already be short without the feature, the proposal moves to §5.9. If somebody shows it can, the proposal dies.

This is bureaucracy on purpose. Languages die from feature creep, not from missing features.

---

## 11. Pedagogical positioning (added 2026-07-16)

> **Concepts before keywords.** catoscript is not just a scripting language. It's a *literacy-first DSL*: a language where every programming concept has a one-sentence English shape in cat vocabulary. The player learns the concept by reading the code out loud, before they ever think of it as code.

### The expanded marketing line

> catoscript is the smallest language that can drive the terminal. It teaches programming concepts by giving every concept a one-sentence English shape in cat vocabulary. **Concepts before keywords. Read it out loud, then run it.**

### The two pulls (and how we resolve them)

The language lives under two competing pressures:

1. **"Small enough to read out loud"** → flat namespace, one concept per line, no scoping rules, no imports, no parentheses-required ceremony
2. **"Teaches real stuff and logic"** → variables, types, conditionals, loops, functions, scope, recursion, data structures

Naively, you can't have both. As you add the second list, the first one dies. Python's `f"hello {name.upper()}"` is "real stuff" but it isn't read-out-loud anymore.

The resolution: **make the real stuff read out loud too.** The cat vocabulary isn't decoration — it's the *form* the abstraction takes. If we pick the right words, every concept lands as a sentence a player could say. The player's brain doesn't switch modes between *reading English* and *reading code*.

### The eight tiers

The canonical learning order. Each tier is *one idea*, *one sentence*, *one concept*. New players encounter tiers in this order. Each tier unlocks the next.

**Tier 1 · First script (cause-and-effect)**

```catoscript
meow "hello"
meow "world"
```

Concept taught: *a program is a list of things to do, top to bottom.* No variables. No logic. The player has *written a program*.

**Tier 2 · State**

```catoscript
set $name "mochi"
meow "hello, $name"
```

Concept taught: *a variable is a labeled box.* `$name` is the box; `"mochi"` is what's in it. Still no logic. The player can now remember things.

**Tier 3 · Decision**

```catoscript
set $hp 5
sniff $hp < 1
purr_at :DEAD
meow "still alive"
jump :END
:DEAD
meow "game over"
:END
```

Concept taught: *a conditional is a yes/no question with two paths.* The `sniff`/`purr_at`/`hiss_at` vocabulary makes `if/else` a literal question-and-answer, not a syntax rule with braces. The player learns `if/else` without learning the word "if/else."

**Tier 4 · Repetition**

```catoscript
set $count 0
:LOOP
  scratch $count + 1
  meow "tick $count"
  sniff $count < 5
  purr_at :LOOP
```

Concept taught: *a loop is a label you can jump back to.* The `for` from §5.9 lands later as the easy version; the loop-with-labels is the *real* version. Both teach the same thing — "do this again" — at different abstraction levels. Players who want to understand *how* `for` works, can. Players who don't, use `for`.

**Tier 5 · Decomposition**

```catoscript
:DRINK $beverage
  meow "slurp $beverage"
  jump :end

jump :DRINK "milk"
jump :DRINK "tea"
```

Concept taught: *a function is a labeled snippet with inputs.* Calling it is `jump :NAME args`. Returning is `jump :end`. No `def`, no `return`, no `()`. The player has *learned functions* without learning the word "function."

**Tier 6 · Lists**

```catoscript
set $toys ["ball", "mouse", "yarn"]
for $toy in $toys
  meow "$toy!"
end_for
```

Concept taught: *a list is a row of boxes; `for` walks the row one box at a time.* Maps to iteration, arrays, the concept of "many things."

**Tier 7 · State you remember**

```catoscript
bury $score 100      # write to disk
dig $score            # read from disk
meow "high score: $score"
```

Concept taught: *persistence* — variables don't just disappear when the script ends. The player has *learned about state*. (`bury`/`dig` are placeholder names shown here to illustrate how a real concept gets a cat word — they are not currently in the language spec.)

**Tier 8 · Real types (opt-in, §5.9)**

```catoscript
let $name: str = "mochi"
let $lives: num = 9
let $toys: list<str> = []
```

Concept taught: *types are promises about what's in the box.* The player opts in when they're ready. Untyped `set` keeps working.

### The principle behind every tier

For every concept we add, we ask:

> **Is there a one-sentence English version of this, in cat vocabulary, that a player could say out loud and have the program do what they meant?**

- If yes → ship it.
- If no → it's too clever; back off.

`for $x in $items` passes the test. `items.map(x => x.length())` doesn't. Same capability, different readability. We pick the readable one.

This is the **positive** of §10's rule. §10 says *"add a feature only if it collapses something the player has to write."* §11 says *"and only if the result reads out loud."* Both tests must pass.

### Tradition we belong to

This kind of language has a name and a lineage: **literacy-first DSL**, sometimes called a **pedagogical language** or **concept-first language**. The pattern is always the same:

1. Strip syntax to the minimum that can express the concept
2. Use vocabulary that maps 1:1 to how a human would describe the program
3. Each new feature is a *new concept*, not a *new keyword*

Predecessors and cousins: Logo (turtle graphics), Scratch (block syntax), BASIC (line-numbered minimalism), HyperCard (English-shaped scripting). catoscript is in this lineage — explicitly.

### Our specific position

catoscript is **not** Logo or Scratch. We are not for kids. We are not a teaching tool for children.

catoscript **is** for the hacker who doesn't want to learn a language to script a game. The player has probably touched Python or JavaScript. They have *no interest* in learning another language. They want to script the terminal *now*, and if reading a script teaches them what a `for` loop is along the way, that's a bonus — not the point.

The pedagogical positioning is a *consequence* of the design discipline, not the goal. The goal is "small enough to drive the terminal." The teaching happens because every concept has a one-sentence English shape.

### How this constrains §5 and §10

- §5 (improvements): every addition must enable a tier or extend an existing one. If it doesn't teach a concept the player would otherwise have to learn elsewhere, it's out.
- §10 (out of scope): the "no" list gets a new column — *"reads out loud?"* — and every "no" row must also fail this test. Closures don't read out loud. Operator overloading doesn't read out loud. Lambdas don't read out loud.
- §6 (migration order): each phase has a *"which tier does this unlock?"* annotation. A phase that doesn't unlock a tier or improve tier ergonomics is suspect.

### Teaching artifacts that come for free

If we land this positioning, the language ships with:

- A **golden scripts library** organized by tier (`samples/01_first_script/`, `samples/02_state/`, …). Each script opens with a one-sentence comment: *"this teaches: variables."* Players learn by reading scripts in tier order.
- A **`:tutorial` REPL command** that walks the player through tiers interactively. The CLI REPL from §6 Phase F ships with this.
- **Tier badges in the LSP** — the editor can show "this script uses Tier 1–4 concepts" so the player knows what they know.
- A **docs site** (mdoc / dokka) where each command's page has a *"teaches:"* tag listing which tier(s) it belongs to.

These are *not* features to build first. They're features that become trivial once the positioning is right. Build the language, the artifacts fall out.

### What "done" looks like for §11

The pedagogical positioning is complete when:

- A new player can read a Tier-1 script out loud and predict what it does, with zero prior coding experience
- The eleven tiers cover everything a player needs to script the terminal; there's no "you'll need to learn X first" gap
- Every command in the spec has a *"teaches:"* tag listing its tier(s)
- The `cato fmt` formatter is idempotent on a script the player wrote by hand — i.e., it doesn't fight the way a human naturally writes
- The CLI REPL's `:tutorial` walks through tiers 1–11 in order
- A second consumer (a teaching tool, a web playground, a textbook) can adopt the language *as a teaching tool* without modification

---

## 12. CLI tools and UI stdlib (added 2026-07-16)

> **catoscript ships with `std.cli` and `std.ui` namespaces that turn the language into a CLI + interactive script platform.** The UI primitives are named after *intent*, not *widgets*, and read out loud. Stdlib functions are implemented as small catoscript scripts themselves, so the stdlib is dogfooded documentation.

### The position

The language has the pieces to be a CLI tool language already — `meow` writes to stdout, `set` and `sniff` cover logic, `for` covers iteration. The gap is *system* I/O (args, stdin, exit codes) and *interactive* I/O (prompts, menus, modals). Both are stdlib work, not language work. Neither changes the grammar.

**The trap** is shipping a UI framework with widget-named primitives (`box`, `text`, `button`). That turns catoscript into "Python with cat words" — the player has to learn an API before they can read a script. We don't ship that.

**The fix** is naming primitives after what the player is *doing*, not what the screen *looks like*. The host decides layout; the language names intent.

### The `CatoHost` extension (small)

Add four methods to the SPI. Optional: hosts that don't want CLI support leave them as no-ops.

```kotlin
interface CatoHost {
    // existing (per §2)...
    fun args(): List<String>           // command-line args (empty list if not a CLI)
    fun readLine(prompt: String?): String?  // stdin line, null on EOF
    fun exit(code: Int)                // terminate the script
    fun printErr(line: String)         // stderr
}
```

KP's `KernelPanicHost` implements these as no-ops or simple stubs (the game's terminal isn't a CLI host). The CLI REPL's `ReplHost` implements them properly. Future hosts pick.

### The `std.cli` namespace

```catoscript
# In std.cli
std.cli.args()               # → ["--name", "mochi"]
std.cli.flag("verbose")      # → true / false (parses --flag / --no-flag)
std.cli.usage("usage: cato-run <file> [args]")
std.cli.exit(0)
std.cli.exit_fail("oops")    # printErr + exit(1) in one
std.cli.confirm_flag(args, "verbose")   # helper: std.cli.flag(std.cli.args(), "verbose")
```

**Tier 9 example — a real CLI tool, readable out loud:**

```catoscript
# A "cat file" clone, written in catoscript
:MAIN
  set $args std.cli.args()
  sniff std.list.length($args) < 1
  purr_at :USAGE
  set $path std.list.first($args)

  set $lines std.fs.read($path)
  sniff $lines == null
  hiss_at :NOT_FOUND

  for $line in $lines
    meow $line
  end_for
  std.cli.exit(0)

:USAGE
  std.cli.usage("usage: cat <file>")
  std.cli.exit(2)

:NOT_FOUND
  std.cli.exit_fail("cat: $path: No such file")
```

**Tier 9 · CLI tools.** *A CLI tool is a function with inputs from the system (args, stdin) and outputs to the system (stdout, exit code).* The script is the function body. `std.cli.args()` is the parameter list. `meow` and `std.cli.exit` are the return path.

### The `std.ui` namespace (the read-out-loud UI)

Seven primitives. That's the whole library.

| Cat word | What the player is doing | What the host draws (varies) |
|---|---|---|
| `ask "prompt" $var` | Asking the player for input | A prompt + text input |
| `choose "prompt" ["a", "b", "c"] $pick` | Asking the player to pick one | A prompt + selectable list |
| `confirm "prompt"` | Asking yes/no | A prompt + y/n |
| `show "title" "body"` | Showing information | A modal / banner / toast |
| `progress $done $total` | Showing progress | A progress bar (or spinner, or text, host's choice) |
| `pause` | Waiting for the player to press Enter | A "press Enter to continue" line |
| `menu :MAIN_MENU` | Showing a full-screen menu | A fullscreen list of options |

The player writes a settings screen:

```catoscript
:SETTINGS
  show "Settings" "Tweak your terminal experience"
  choose "Theme?" ["ocean", "midnight", "paper"] $theme
  ask "Volume (0-100)" $volume
  confirm "Save and restart?"
  purr_at :SAVE_AND_RESTART
  meow "Settings discarded."
  jump :end

:SAVE_AND_RESTART
  bury $theme
  bury $volume
  meow "Saved. Restarting..."
  std.cli.exit(0)
```

That reads out loud. Player wrote a settings screen.

**Tier 10 · Talking to the player.** *A program can ask, show, and confirm. The host decides what that looks like.* The player learns that interactive I/O is just more `meow` plus a few primitives that take input instead of produce output.

**Tier 11 · Menus and flows.** *A UI is a sequence of asks and shows, with jumps between them.* The script is a state machine; the host is the renderer. This is the moment the player realizes "wait, the entire app is just a script with jumps."

### Implementation rule: the stdlib is dogfooded

Every `std.*` function must be implementable as a small catoscript script itself. Example — `std.cli.exit_fail` could be:

```catoscript
:STD_CLI_EXIT_FAIL $msg
  std.cli.print_err($msg)    # writes to stderr
  std.cli.exit(1)            # leaves with error code
  jump :end
```

The interpreter resolves `std.cli.exit_fail` to the `:STD_CLI_EXIT_FAIL` label in the stdlib. The player can `cat std/cli/exit_fail.cato` and read the implementation. **The stdlib is the longest comment in the language.**

This rule:
- Forces the stdlib to read out loud (it's written in catoscript)
- Gives the player real scripts to read past Tier 8
- Catches bad stdlib design early — if a function can't be written cleanly in catoscript, it's the wrong abstraction

### What stays out (deliberate)

Even with `std.cli` and `std.ui`, the §10 list still holds:

- **No widget primitives.** No `box`, `text`, `button`, `image`, `canvas`. The seven `std.ui` words cover everything a CLI/terminal needs.
- **No positioning.** No `at(x, y)`, no `size(w, h)`. The host chooses layout; the script names intent.
- **No event handlers.** No `on_click`, no `on_key`, no callbacks. The script is sequential. Input is read synchronously by `ask`/`choose`/`confirm`/`menu`.
- **No reactivity.** No "re-render when $x changes." The script is a one-shot program; the host renders each command as it executes.
- **No widgets in the language itself.** A future `std.ui.widget.*` namespace could expose widgets, but only if a player has actually asked for one. §10's amendment process applies.

These "no"s are what keep the read-out-loud test passing. Every `std.ui` primitive is a *verb* the player would use in English.

### The full eleven-tier ladder (revised)

Tier 1 · First script (cause-and-effect)
Tier 2 · State (variables)
Tier 3 · Decision (sniff + purr_at / hiss_at)
Tier 4 · Repetition (labels + jump)
Tier 5 · Decomposition (jump :LABEL args as functions)
Tier 6 · Lists (set $xs [...]; for $x in $xs)
Tier 7 · State you remember (persistence)
Tier 8 · Real types (opt-in let)
**Tier 9 · CLI tools (std.cli: args, exit, stderr)**
**Tier 10 · Talking to the player (std.ui: ask, show, confirm)**
**Tier 11 · Menus and flows (state-machine scripts as UIs)**

### How this interacts with §5, §6, §10, §11

- **§5 (improvements):** Tiers 9–11 are implemented as stdlib additions (§5.x). No grammar change. Feature-flagged behind `cli.enable` / `ui.enable` during rollout.
- **§6 (migration order):** Add a Phase F.5 — *ship `std.cli` and `std.ui` with the CLI REPL*. The REPL's `:tutorial` covers Tiers 9–11 after Tiers 1–8.
- **§10 (out of scope):** Add the read-out-loud column to the "no" table. Every existing "no" (closures, lambdas, classes, async, operator overloading) fails the test. UI widgets would too — that's why they're in this section's "stays out" list.
- **§11 (pedagogical positioning):** Update the tier ladder from eight to eleven tiers. The "what done looks like" checklist grows from "tiers 1–8" to "tiers 1–11."

### What "done" looks like for §12

The CLI + UI stdlib is complete when:

- A player can write a working `cat`-style CLI tool in catoscript with `std.cli.args()`, `std.cli.exit()`, and `std.fs.read()`
- A player can write an interactive settings flow with `ask`, `choose`, `confirm`, `show`
- Every `std.cli.*` and `std.ui.*` function is also a real catoscript script in `samples/std/`, readable by a Tier-3 player
- The CLI REPL's `:tutorial` covers Tiers 9–11
- The `CatoHost` extension in §12 ships, with `KernelPanicHost` documenting that it implements the four methods as no-ops (KP is not a CLI host)
- Tiers 9–11 appear in the docs site with examples, and every primitive has a *"teaches:"* tag
- A second consumer (a CLI tool author, a textbook, a teaching tool) can write a working interactive script in catoscript without touching host code

### A note on the CLI REPL itself

The CLI REPL (§6 Phase F) becomes the *proof of independence for the full stdlib*, not just the language. When the REPL ships with `std.cli` and `std.ui`, a player can:

```bash
$ cato
catoscript> :tutorial
# walks Tier 1 through Tier 11
catoscript> :load samples/std/cli/cat.cato
catoscript> :run -- --help
# runs a CLI tool written in catoscript
```

That's the moment catoscript is *clearly* not just a game scripting language. It's a CLI tool language that happens to ship inside a game.

---

## 13. Implementation discipline (added 2026-07-16)

> **Simple is not the absence of advanced. Simple is the absence of unnecessary.** The recursive-descent parser is *simple* because it's a 600-line grammar that does one job. Bytecode compilation is *advanced* because it does five jobs, none of which the player needs. Pick simple. Pick correct. Don't pick "more advanced."

### The principle

There's a specific failure mode that kills small language projects:

> 1. Start the parser. It's a clean recursive-descent grammar.
> 2. Decide "while we're at it, let's add closures." Now the grammar needs a closure-conversion pass.
> 3. Decide "and async." Now the interpreter needs a state machine, not a loop.
> 4. Decide "and types." Now we need a type checker.
> 5. Two years later, you have a half-finished Rust.

Every "advanced" feature pulls in *two more* "advanced" features that it depends on. The recursion bottoms out only when someone stops, or runs out of time, or ships something the player can't actually use.

The §10 rule is the guardrail against this for *language features*. This section is the guardrail against it for *implementation features*. Same spirit, different surface.

### The four real implementation gaps

Stripped to actual needs — the parser and interpreter work that's worth doing, all of it behavior-preserving, all of it collapsing something real:

| Gap | Why it matters | Already in devplan? |
|---|---|---|
| **Real parser, typed AST, line/column positions** | Better errors, LSP hover/jump, formatter, stepper — and the line-splitter today has bugs (nested quotes, escape sequences, `$var` interpolation re-parsed at runtime) | ✅ §5.1, Phase E |
| **Configurable `InterpreterPolicy`** (step budget, max total steps, seed) | Testability, deterministic replays, future sandboxing | ✅ §5.2 |
| **Generic `env()` host call** (replaces KP-specific `sniff_env`) | Language portability — the host decides what env is | ✅ §5.3 |
| **`Stepper` interface** | LSP debug features, `:step` in REPL, KP's debug overlay | ✅ §5.7 |

That's the list. Four things. Each ships in its own commit. Each clears §10's bar (collapses something real). None are "more advanced" — they're *correct*.

### The traps, named explicitly

Each item below is a real engineering project, six-plus months of focused work each, none of which change what the player can build or how the read-out-loud test passes. They add *machinery*, not *capability*.

| Trap | One-line rejection |
|---|---|
| **Bytecode compilation** (parse → AST → custom bytecode → interp) | A `cat`-clone runs in milliseconds. The 100-instr/tick budget is a feature, not a bottleneck. Solving a perf problem that doesn't exist. |
| **AOT to JVM `.class` files** | The moment you compile to JVM bytecode you've shipped a JVM language. That's Kotlin, not catoscript. Player scripts don't run often enough for this to matter. |
| **Register-based VM with optimizations** (stack vs. register, inline caching, branch prediction) | None of this matters at script scale (hundreds of instructions, not millions). |
| **SSA-form IR for optimization** | catoscript doesn't *have* an optimization problem. This is research dressed as engineering. |
| **Type inference at the language level** (Hindley-Milner, bidirectional typing) | §5.9 says opt-in `let` annotations. The analyzer reads the annotation; no inference needed. Inference is a research project for marginal analyzer benefit. |
| **Closure conversion** (the analysis pass that turns lambdas-with-free-vars into closures-with-captured-env) | Closures are in §10's "no" list. The pass is only needed if we add closures. We won't. |
| **Async/await IR transformation** (state machines for `suspend`) | Async is in §10's "no" list. The interpreter loop *is* the cooperative scheduler. There's nothing to await. |
| **JIT compilation** (HotSpot-style runtime profiling + native codegen) | catoscript doesn't ship a JIT. JVM does. Use the JVM. |
| **Tail-call optimization** (so recursive functions don't blow the stack) | The script runs in a step budget, not a stack-depth budget. TCO solves a problem the interpreter doesn't have. |
| **Constant folding / dead-code elimination** | The analyzer can warn about unused `$vars`; runtime folding doesn't matter at script scale. |

If a future proposal adds any of these, the workflow is the §10 amendment process: open a doc PR, name the **specific scripts that get faster or smaller**, two-week wait, decide. None of the above clears the bar today. None of them should clear it without an actual measured bottleneck.

### What "advanced" *does* mean here

The list above (real parser, policy, env, stepper) is not boring — it's the difference between a prototype and a tool. A real parser with typed AST nodes and source positions is *correct* in a way the line-splitter isn't. A `Stepper` interface is what makes the LSP debug features possible. These are advanced *because they earn it*, not because they sound impressive.

The test:

> **Is this solving a problem the player has, or a problem the implementer is interested in?**

If "player has" — ship it.
If "implementer is interested in" — write it down, don't build it. Maybe someday. Probably never.

### How this interacts with §5, §6, §10, §11, §12

- **§5 (improvements):** The "real parser, policy, env, stepper" four are listed in §5.1, §5.2, §5.3, §5.7. Each is its own §5.x subsection. Anything new that wants to land in §5 must name which of §13's four gaps it covers — or it doesn't belong in §5.
- **§6 (migration order):** The four gaps each get their own phase in the migration. No phase is "improve the parser"; the phase name is "real parser, typed AST, line/column positions" (specific). Drift in phase naming is the first sign of scope creep.
- **§10 (out of scope):** The implementation traps (bytecode, AOT, register VM, SSA, inference, closure conversion, async IR, JIT, TCO, folding) get their own subsection in §10's "what catoscript will never add" table. The "why" column mirrors this section.
- **§11 (pedagogical positioning):** The implementation discipline *enables* the read-out-loud test. A real parser produces AST nodes that name concepts (`Label`, `Jump`, `VarRef`); the LSP can hover and teach. A line-splitter produces strings.
- **§12 (CLI + UI stdlib):** The implementation discipline keeps the stdlib small. Seven `std.ui` primitives, no widget layer underneath them. The host renders; the stdlib names intent. Adding an `std.ui.widget.*` namespace would be the same "advanced" trap, in stdlib clothing.

### The corollary rule

> **The engineering quality of catoscript comes from doing the four things above correctly, not from adding more layers between source and execution.**

A 600-line recursive-descent parser that produces a typed AST is *higher quality* than a 6000-line bytecode VM. The first one does its job and stops. The second one does five jobs, none of them well, and ships six months late.

Pick the line where complexity earns its keep. Draw it. Don't cross it on a "while we're at it."

### What "done" looks like for §13

The implementation discipline holds when:

- A new contributor proposing "let's add X" gets asked "which of §13's four gaps does X close?" before getting asked "how big is the patch?"
- A new contributor proposing "let's add bytecode compilation" gets pointed to this section's table and the conversation ends
- The four gaps above each ship in their own commit, each with a passing test, each in their own phase of §6
- The parser/interpreter source size (excluding tests) stays under ~3000 lines combined — a soft cap that catches creep early
- Every "advanced" feature suggestion gets the §10 amendment process, not a code review
- Five years from now, the implementation is recognizably the same shape as today — a parser, an AST, an interpreter loop, a host seam, a stdlib. Nothing got added that wasn't on the four-item list.

---

## 14. Capability surface (added 2026-07-16)

> **The four checks.** A new capability lands only if it passes all four: §5 (it's an improvement, not a new keyword), §10 (it collapses something the player has to write), §11 (there's a one-sentence English version in cat vocabulary), §13 (the implementation fits the four-gap list or is pure stdlib work). Most proposals fail at (3) or (4). That's the design working.

### The four approved capabilities

Each item below is a new stdlib namespace that lands in its own commit, behind its own feature flag (`time.enable`, `fs.enable`, `test.enable`, `json.enable`), and ships with a `:tutorial` entry covering its tier.

**Tier 12 · Other people's code — `std.time` and `std.random`**

The language already has `nap` (sleep) and `zoomies` (random int). Promote them to stdlib with companions that collapse the date-format / dice-rolling / shuffle loops every player writes.

```catoscript
set $now std.time.now()              # unix millis, via host.now()
set $iso std.time.iso($now)          # → "2026-07-16T14:30:00Z"
set $year std.time.year($now)        # → 2026
set $after std.time.add($now, "1h")  # → $now + 3600000
set $die std.random.dice("3d6")      # → 11
set $pick std.random.pick($toys)     # → random element
set $shuffled std.random.shuffle($toys)
```

**Passes all four checks:**
- §5: stdlib addition, no grammar change.
- §10: collapses the manual date-format / dice-rolling / shuffle loops every player writes.
- §11: `std.random.dice("3d6")` reads as English; "roll three six-sided dice" is the literal human description.
- §13: `host.now()` is already in `CatoHost` per §2. `std.random.*` is pure in-process.

**Concept taught:** *a function in a namespace can come from somewhere else.* The player learns "stdlib exists" — a foundational concept for any language journey.

---

**Tier 13 · The world outside the script — `std.fs` and `std.path`**

The `cat` example in §12 already calls `std.fs.read`. Land the real thing. Every CLI tool needs file I/O; without it, players reinvent `open()`/`close()` patterns in every script.

```catoscript
set $text std.fs.read("~/.config/cato/settings.cato")
set $lines std.fs.read_lines("log.txt")
std.fs.write("out.txt", $lines)
std.fs.append("log.txt", $line)
sniff std.fs.exists("~/.cato/init.cato")
purr_at :RUN_INIT

set $parts std.path.split("/usr/local/bin/cato")   # → ["usr", "local", "bin", "cato"]
set $dir std.path.folder("/usr/local/bin/cato")    # → "/usr/local/bin"  (was: dirname — folder reads better)
set $name std.path.basename("/usr/local/bin/cato") # → "cato"
set $ext std.path.extname("config.cato")           # → ".cato"
```

**Passes all four checks:**
- §5: stdlib addition.
- §10: every CLI tool needs file I/O; without it, players reinvent `open()`/`close()` patterns in every script.
- §11: `std.fs.read("file")` reads out loud. `std.path.folder` is borderline — `dirname` was rejected as too jargony; `folder` reads better. Ship `folder` first; if a player asks for `dirname` later, it's an alias.
- §13: host decides what filesystem is (CLI REPL: real disk; KP: VFS; web playground: virtual FS). New `CatoHost` methods:

    ```kotlin
    interface CatoHost {
        // existing (per §2 + §12)...
        fun readFile(path: String): String?
        fun writeFile(path: String, content: String): Boolean
        fun fileExists(path: String): Boolean
    }
    ```

    All three are no-ops in `NullHost`. `KernelPanicHost` returns "filesystem is virtual; use `env()`" or delegates to the VFS. The CLI REPL's `ReplHost` uses real disk. Future hosts pick.

**Concept taught:** *the world outside the script has structure (files, folders, paths) and your script can read and write it.* This is the moment catoscript becomes a *tooling* language, not just a game scripting one.

---

**Tier 14 · Testing what you built — `std.test`**

Without built-in test support, players write bespoke assertion harnesses in every script. With it, *checking your work* becomes part of writing code, not a separate chore. This is huge — most teaching languages never land this lesson.

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

:FAIL_starts_empty
  meow "FAIL: list should start empty"
  jump :end

:FAIL_adds_one
  meow "FAIL: push should add one item"
  jump :end
```

Run with `cato test script.cato`. The CLI REPL gets `:test` that runs all labels prefixed `:test_`. Each `:test_*` block is a separate sub-script — failures don't stop the others.

**Passes all four checks:**
- §5: stdlib + a small runner.
- §10: every non-trivial script gets a test; without stdlib support, players write bespoke assertion harnesses.
- §11: `cato test script.cato` reads as English; `:test_NAME` is a label the player already knows.
- §13: pure stdlib; the runner is a tiny interpreter-level loop that walks `:test_*` labels and reports pass/fail. No new host methods.

**Concept taught:** *a test is a function that fails loudly when its claim is wrong. Running tests is part of writing code, not a separate chore.*

---

**Tier 15 · Talking to other programs — `std.json`**

JSON is the universal data interchange. Without a parser, the player can't talk to anything modern — every script that wants config files re-implements a key=value parser. With it, the player learns serialization, the idea that data can travel between programs, and (by extension) that *their script can talk to other programs*.

```catoscript
set $text std.fs.read("config.json")
set $cfg std.json.parse($text)
set $theme $cfg.theme                          # → "ocean"
set $cfg std.json.set($cfg, "lives", 9)
set $out std.json.stringify($cfg)
std.fs.write("config.json", $out)
```

**Passes all four checks:**
- §5: stdlib addition.
- §10: JSON is the universal data interchange; without it, the player can't talk to anything modern.
- §11: `std.json.parse(text)` reads out loud.
- §13: pure stdlib, no host additions; JSON is in-process.

**Concept taught:** *structured data has a text form, and the script can read and write that form. Players learn serialization, the idea that data can travel between programs, and that their script can cooperate with other programs.*

### The six rejected proposals

These are real things players might ask for. They look tempting but fail the four checks. Naming them now so they don't sneak in later.

**❌ 5. Async / `await` commands** (e.g. `await fetch(url)`)

Fails §10, §13. The interpreter loop is the cooperative scheduler; there's nothing to await that the host doesn't already provide synchronously. Network I/O is a host method, not a language feature.

**Where it lands instead:** `CatoHost.fetchUrl(url: String): String?`. `std.web.fetch` is a thin wrapper. The language doesn't change. CLI REPL implements real `fetch`; KP's host returns "fetch not available."

**❌ 6. User-defined functions (`def`)** (e.g. `def greet $name meow "hi $name"`)

Fails §11. `def` doesn't read as English. The label system already covers this — `jump :GREET "mochi"` is the call, `:GREET $name ... jump :end` is the body. Same capability, fewer rules, no new syntax.

**Where it lands instead:** the §11 Tier 5 pattern stays. `def` doesn't ship.

**❌ 7. Classes / "data with methods"** (e.g. `class Cat $name $lives`)

Fails §10, §11. The host models the world; the language drives it. Two sources of truth = bugs. `class Cat` is a modeling primitive, not a driving primitive.

**Where it lands instead:** `CatoHost` env lookup returns whatever data shape the host provides. In KP, `env("cats")` returns a list of cat-shaped data (host decides the shape; language treats it as opaque values). The player iterates with `for`.

**❌ 8. Modules / `import`** (e.g. `import "lib.cato"`)

Already covered. `AGENT_catoscript.md` §4: *"Handles `include` by injecting the target script's lines and wrapping them in a skip-label to prevent immediate execution."* One seam, no package system needed. `import` would add a module path syntax, a namespace prefix, and a resolution algorithm. None clear §11.

**Where it lands instead:** §12 names `include` more readably (e.g. `std.script.load("lib.cato", $lib)`). One mechanism, no ceremony.

**❌ 9. Regex** (e.g. `std.str.match($text, "^foo")`)

Fails §11. Regex isn't English. `std.str.starts_with` / `std.str.contains` / `std.str.split` cover 95% of what the player reaches for regex to do. The 5% that needs regex can be wrapped in a host method.

**Where it lands instead:** `std.str.*` gets more verbs — `starts_with`, `ends_with`, `contains`, `count`, `index_of`, `replace`, `trim`, `pad_left`, `pad_right`. Each reads out loud. Regex stays a host concern, opt-in per host.

**❌ 10. `match` / pattern matching** (e.g. `match $x case 1: ... case 2: ...`)

Fails §11. `match`/`case` are syntax for "switch on value," which the player already does with `sniff` + labels. Same concept, two syntaxes. Adding `match` teaches the same thing twice.

**Where it lands instead:** `std.case.dispatch $x { 1: :CASE_1, 2: :CASE_2 } $default` *might* clear §11 someday — but only after seeing 100 player scripts use the label pattern first. The label pattern is enough for now.

### The full fifteen-tier ladder

Pulling §5, §11, §12, and §14 together:

```
Tier 1   First script                  cause-and-effect
Tier 2   State                         variables
Tier 3   Decision                      sniff + purr_at / hiss_at
Tier 4   Repetition                    labels + jump
Tier 5   Decomposition                 jump :LABEL args as functions
Tier 6   Lists                         set $xs [...]; for $x in $xs
Tier 7   State you remember            persistence (bury / dig)
Tier 8   Real types                    opt-in let
Tier 9   CLI tools                     std.cli: args, exit, stderr
Tier 10  Talking to the player         std.ui: ask, show, confirm
Tier 11  Menus and flows               state-machine scripts as UIs
Tier 12  Other people's code           std.time, std.random
Tier 13  The world outside the script  std.fs, std.path
Tier 14  Testing what you built        std.test, cato test
Tier 15  Talking to other programs     std.json, structured data
```

**Every concept a player needs to script the terminal, automate the simulation, write CLI tools, build interactive apps, talk to the filesystem, test what they built, and cooperate with other programs.** No "you'll need to learn X first" gap.

### The `CatoHost` extension list (consolidated)

Across §2, §12, and §14, the full SPI:

```kotlin
package com.catoscript.runtime

enum class Waveform { SQUARE, SINE, NOISE, SAMPLE }

interface CatoHost {
    // §2 — terminal + audio + clock + env
    fun print(line: String)
    fun playTone(freq: Double, durationMs: Int, waveform: Waveform)
    fun playSample(id: String)
    fun setCursor(x: Int, y: Int)
    fun clearScreen()
    fun envLookup(key: String): String?
    fun now(): Long

    // §12 — CLI + interactive (optional; no-op for non-CLI hosts)
    fun args(): List<String>
    fun readLine(prompt: String?): String?
    fun exit(code: Int)
    fun printErr(line: String)

    // §14 — filesystem (optional; no-op for non-FS hosts)
    fun readFile(path: String): String?
    fun writeFile(path: String, content: String): Boolean
    fun fileExists(path: String): Boolean
}

object NullHost : CatoHost {
    // all methods no-op or sensible default
}
```

**Per-host implementation policy:**

| Host | terminal | audio | clock | env | CLI | FS |
|---|---|---|---|---|---|---|
| `NullHost` (lib tests) | no-op | no-op | `System.currentTimeMillis()` | `null` | `[]` / EOF | `null` / false |
| `ReplHost` (CLI REPL) | ANSI stdout | none | real | env-vars | real | real disk |
| `KernelPanicHost` (KP) | Compose `Text` | `playTone` via audio engine | real | game state | no-op | VFS delegate |
| `WebHost` (playground, future) | `<canvas>` | WebAudio | `Date.now()` | URL params | none | virtual FS |

Every method is optional in the sense that the host picks what's meaningful. No host implements everything.

### What this section locks in

- **The grammar is closed.** No new keywords ship after §14. New capabilities land in stdlib.
- **The four-check process is policy.** Every proposal goes through it. If it doesn't pass all four, it doesn't ship.
- **The four-item implementation list (§13) is closed.** New stdlib namespaces don't add parser or interpreter work; they're scripts in `samples/std/` plus a registry entry.
- **The host seam is the only extension point.** New system capabilities (network, audio, filesystem, env) are host methods, not language features.
- **The tier ladder is complete.** A new player going Tier 1 → 15 learns: cause-and-effect → state → decisions → loops → functions → lists → persistence → types → CLI → interactive UI → state-machine UIs → libraries → filesystem → testing → interop. That's a programming curriculum, hidden inside a cat vocabulary.

### What "done" looks like for §14

The capability surface is complete when:

- The four stdlib namespaces (time, fs, test, json) ship behind their feature flags and pass tests
- The CLI REPL's `:tutorial` walks all 15 tiers
- Every approved capability has at least one real-world example in `samples/` that's a Tier-3-readable catoscript script
- Every rejected proposal (async, `def`, classes, modules, regex, `match`) is documented in this section with its rejection reason and where the need lands instead
- The `CatoHost` extension list above ships, with `NullHost` providing sensible defaults for all 14 methods
- A second consumer (a CLI tool author, a textbook, a teaching tool) can adopt the language and find every common need covered by a stdlib namespace, with no language changes required
- Five years from now, the tier ladder is recognizably the same 15 tiers — no tiers added, none removed. New stdlib namespaces land inside existing tiers.