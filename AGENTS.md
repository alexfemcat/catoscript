# AGENTS.md — catoscript (standalone)

> The catoscript language as a standalone Kotlin/JVM library. This repo's job is the standalone language, originally created as part of a game named Kernel Panic. A CLI REPL, web playground, or teaching tool can consume it. The extraction plan, scope decisions, positioning, and tier ladder live in `catoscript-devplan.md` at the root of this repo.

**Naming:** `catoscript` is one word, camelcase. Not `cato-script`, not `KatoScript`, not `CATOScript`. The package root is `com.catoscript`. When source is migrated from `KernelPanic-Kotlin-Port`, all `com.kp.cato.*` namespaces rename to `com.catoscript.*` in a single commit (see `catoscript-devplan.md` §6 Phase B).

---

## 1. Project Overview

> **License:** MIT. Anyone can use, modify, and redistribute catoscript for any purpose; the copyright notice must travel with the code. See `LICENSE` at the repo root.

**catoscript is a Kotlin/JVM library that ships the catoscript language — a small, literacy-first scripting DSL.** It is the language that lives inside Kernel Panic's `catoDE` editor, but the language itself is independent. The extraction plan (`catoscript-devplan.md` §1) names why: improvements fight the game, the surface leaks, and reuse is impossible while the language lives inside a game repo.

The devplan is the source of truth for *what catoscript is*. The headline:

> **catoscript is the smallest language that can drive the terminal. It teaches programming concepts by giving every concept a one-sentence English shape in cat vocabulary. Concepts before keywords. Read it out loud, then run it.** (`catoscript-devplan.md` §11)

The language's design is locked to four documents in `catoscript-devplan.md`:

- **§5** — language improvements (stdlib, `for`-over-lists, opt-in `let` for types)
- **§10** — out of scope (the "no" list: closures, classes, async, operator overloading, modules, macros, bytecode, etc.)
- **§11** — pedagogical positioning (the read-out-loud test, the eleven tiers)
- **§13** — implementation discipline (the four-item list of real implementation gaps: real parser, `InterpreterPolicy`, generic `env()`, `Stepper`)

A new capability lands only if it passes all four checks (§5 approves, §10 doesn't reject, §11 reads out loud, §13 fits the four-gap list or is pure stdlib). The full process is in `catoscript-devplan.md` §14.

### The implementation snapshot

**`catoscript-reference.md` is what catoscript does today.** The devplan describes design intent — what catoscript *is intended* to become. The reference describes implementation truth — every shipped command, expression, AST node, host method, error message, and known pitfall, verified by reading every source file. AI assistants writing catoscript code should consult the reference first; the devplan is the design spec, the reference is the implementation doc, the source is the ground truth. Where the reference and the devplan disagree on runtime behavior, the reference wins.

**Tech Stack:**

- Kotlin 2.2.20, Gradle 9.6.1, JDK 21 toolchain (JDK 25 acceptable on host)
- JUnit 5 + `kotlin.test`
- `kotlinx.serialization` for AST persistence (used by `cato fmt`, the analyzer, the stepper)
- No Compose. No webview. No browser. The library is JVM-only; hosts render.

**Distribution:**

- `./gradlew publishToMavenLocal` → `com.catoscript:catoscript:0.x.y-LOCAL`
- Future: real publishing target decided at `catoscript-devplan.md` §6 Phase H
- Kernel Panic pulls from `mavenLocal()` until then; KP's `gradle.properties` pins the version

**Working Directory:** The `.git` repo lives at this directory's root.

---

## 2. Critical Commands

```bash
# Run the language test suite (from the repo root)
./gradlew test                       # macOS / Linux
./gradlew test                       # Windows (Finnish layout: ./gradlew works on Windows too)

# Force re-run (don't trust the cached "up-to-date" status)
./gradlew test --rerun-tasks

# Typecheck via Gradle compile
./gradlew compileKotlin

# Run the CLI REPL (lands in Phase F per devplan §6)
./gradlew :tools:repl:run            # explicit form, always works
./gradlew run                        # shorthand from repo root, when only one subproject defines :run

# Run the catoDE IDE (Compose Desktop; Phase 1)
./gradlew :catoDE:run                # launches the IDE window
./gradlew :catoDE:shadowJar          # produces build/libs/catoDE-0.1.0-LOCAL.jar

# Build everything
./gradlew build

# Publish to mavenLocal for KP and catoDE to consume
./gradlew publishToMavenLocal
```

> **Do not write `.\gradlew.bat` in instructions.** The Finnish keyboard layout has no normal backslash key (it's an AltGr combination on the `"` key). `./gradlew` works identically on Windows, macOS, and Linux. Always `./gradlew`.

---

## 3. Module Layout

```
CatoScript-Standalone/
├── AGENTS.md                         # this file
├── AI_POLICY.md                      # how AI tools are used in this repo
├── catoscript-reference.md           # implementation snapshot — what catoscript does today (for AI assistants)
├── catoscript-devplan.md             # source of truth for what catoscript is / isn't
├── README.md                         # 30-second quickstart ("write a .cato, run it")
├── settings.gradle.kts
├── build.gradle.kts                  # root build, plugins
├── gradle.properties                 # JVM args, version pin (catoscript.version)
├── gradle/                           # committed Gradle wrapper
├── gradlew / gradlew.bat             # wrapper scripts
├── docs/                             # additional docs (tattoo docs, language spec)
├── src/main/kotlin/com/catoscript/
│   ├── lexer/                        # tokens, line splitter (handles def_asset heredocs)
│   ├── parser/                       # text → AST (recursive descent; devplan §5.1, §13)
│   ├── ast/                          # sealed Expr, Stmt, SourcePos
│   ├── interpreter/                  # loop, ScriptContext, IP, instruction budget
│   ├── runtime/                      # CatoHost, NullHost, Waveform
│   ├── commands/                     # built-in stdlib (meow, set, sniff, jump, …)
│   ├── analyzer/                     # CatoScriptAnalyzer (two-pass; moved from KP's CatoDE)
│   └── errors/                       # CatoScriptError, categories, Levenshtein suggestions
├── src/test/kotlin/com/catoscript/   # one test file per slice; uses NullHost
├── samples/                          # .cato files; golden scripts organized by tier
├── editor/                           # VS Code TextMate grammar (.vsix)
├── catoDE/                           # standalone Compose Desktop IDE (devplan Phase 1)
└── tools/
    └── repl/                         # CLI REPL app (devplan §6 Phase F); stdlib host
```

> **The in-repo source under `cato-kotlin/` in `KernelPanic-Kotlin-Port` is the candidate set to move here.** When Phase B of the devplan lands, source files rename `package com.kp.cato.*` → `package com.catoscript.*` in a single commit. Until that commit lands, do not preemptively rename — drift between the two namespaces costs more than the interim awkwardness. (`catoscript-devplan.md` §4.)

---

## 4. The four-document rule

This repo's design lives in `catoscript-devplan.md`. Specifically:

| Decision | Devplan section |
|---|---|
| Why catoscript is its own repo | §1 (Why extract) |
| What the host seam looks like (`CatoHost` SPI, command → method mapping) | §2 (The seam) |
| What moves out of KP / what stays in KP | §4 (What moves · what stays) |
| What language improvements are worth doing | §5 (Language improvements) |
| The order to build the standalone repo in | §6 (Migration order — phases A through H) |
| How versions get bumped (`0.x.y-LOCAL` during the port, SemVer after) | §8 (Versioning rules) |
| What "extraction done" means | §9 (What "done" looks like) |
| The deliberate "no" list (closures, classes, async, etc.) | §10 (Out of scope) |
| The pedagogical positioning + tier ladder | §11 (Pedagogical positioning) |
| CLI tools + UI stdlib (`std.cli`, `std.ui`) | §12 (CLI tools and UI stdlib) |
| Implementation discipline (the four-item gap list) | §13 (Implementation discipline) |
| The capability surface (`std.time`, `std.fs`, `std.test`, `std.json`) | §14 (Capability surface) |
| What catoscript actually does today (every shipped command, AST shape, error message, host method) | `catoscript-reference.md` |

If any other document in this repo contradicts `catoscript-devplan.md`, the devplan wins for design intent; `catoscript-reference.md` wins for runtime behavior. When the two disagree on what currently runs, the reference wins; when they disagree on what should be added, the devplan wins.

---

## 5. CRITICAL CODING RULES

1. **Package declaration must match folder structure.** A file at `src/main/kotlin/com/catoscript/parser/Parser.kt` MUST start with `package com.catoscript.parser`. Folder and package always match.

2. **`Stmt` / `Expr` / `Token` style sealed types are the language's category pattern.** Every sealed class/interface represents "one of N fixed things." The compiler enforces exhaustiveness on `when`. Use this pattern for AST nodes, tokens, error categories, and the analyzer's diagnostic kinds.

3. **`data class` requires at least one constructor parameter.** Members with no data use `object` (singletons, exactly one of them) or `data object` (singletons with auto-generated `toString`). Never write `data class Empty : Foo()`.

4. **`val` over `var` by default.** Mutable state goes in `MutableList` / `MutableMap`, not in the class field. Class fields are read-only `val`s. Interpreter state (the `MutableMap<String, Value>` for variables) is the one exception — and even there, prefer immutable snapshots where possible.

5. **`tests/` mirrors `main/`.** Test file lives at the same package path under `src/test/...` as the file under test. Tests use `kotlin.test` (`@Test`, `assertEquals`, `assertIs`, etc.). Run via `./gradlew test`.

6. **No coroutines in pure-functional slices.** The lexer, parser, AST, types, analyzer, and value objects do NOT use `suspend`. Coroutines are reserved for the interpreter loop's step-budget interaction with the host, and the REPL's I/O. Engine purity is a §13 requirement, not a UI convenience.

7. **Every change ships with a passing test in the same commit.** Run `./gradlew test --rerun-tasks` before committing. Never commit a red build.

8. **The TS original is the reference, but only for behavior.** Read `KernelPanic-and-CatoScript-DE-Master/cato/AGENT_catoscript.md` for the language spec, and `game/AGENT/CATOSCRIPT.md` for engine integration. The devplan's §10 / §11 / §13 are the *Kotlin port's* rules; the TS file is the *language's* reference.

9. **Update the plan, then the code, then the test.** A new primitive first needs a checkbox in the relevant phase in `catoscript-devplan.md` §6, then the code, then the test. Same commit.

10. **The dash rule.** No ASCII dashes (`-`) in any Kotlin source comment that surfaces in docs or player-facing output. Comments live in Chinese-finger-trap territory: easy to break, silent failures. Use middle dots `·`, slashes `/`, or rewrite the sentence.

11. **Read-out-loud test for new primitives.** Every new `std.*` function name and every new command must clear §11 of the devplan: *is there a one-sentence English version of this, in cat vocabulary, that a player could say out loud and have the program do what they meant?* If not, the name is wrong. Find a better word before shipping.

12. **Handwcrafted code only.** Per `AI_POLICY.md`.

---

## 6. Subagents

This repo has one project-scoped subagent: **docs-doctor** (`.claude/agents/docs-doctor.md`). It is the docs maintainer for `catoscript-reference.md`, `catoscript-devplan.md`, and this file.

**Invoke it explicitly** with `deploy docs-doctor` or `@docs-doctor`, optionally naming what just shipped (e.g. "we just landed B.6", "[is] is now real"). It will diff the docs against `src/main/` and `src/test/` and patch the drift.

**Invoke it implicitly** when a feature lands: a new file in `src/main/`, a previously-failing test that now passes, a phase checkbox ticked in the devplan. The agent reads the source on its own and updates the docs without being asked.

**Authority order the agent follows** (top wins): Kotlin source → `catoscript-reference.md` → `catoscript-devplan.md` → `AGENTS.md`. When docs disagree with source, source wins and the doc is patched. The agent does not edit Kotlin — it is docs-only. If you ask it to implement a feature, it will refuse and surface that to you.

---

## 7. Engine reference docs (read these before porting a slice)

These documents describe the engine or the language spec that the Kotlin port preserves. Every lesson in this repo should pull up the matching file and read it before starting the slice.

| Slice to land | Read this reference first |
|---|---|
| catoscript language spec | `KernelPanic-and-CatoScript-DE-Master/cato/AGENT_catoscript.md` — the language's behavioral source of truth |
| catoscript engine integration | `KernelPanic-and-CatoScript-DE-Master/game/AGENT/CATOSCRIPT.md` — how the TS engine wires parser + interpreter + host together |
| CatoDE two-pass analyzer (Phase G) | `KernelPanic-and-CatoScript-DE-Master/cato/components/CatoDE.tsx` (lines 515–740) — the reference two-pass analyzer with Levenshtein suggestions |

The behavioral reference is the TS original. The design reference is `catoscript-devplan.md`. When in doubt about what a port slice should *do*, the TS file wins on behavior; the devplan wins on Kotlin-side design.

---

## 8. What Lives Elsewhere

- **Original TS code, scenarios, tests:** `\Misc\KernelPanic-and-CatoScript-DE-Master\`
- **Full Kernel Panic AGENTS.md context:** `\Misc\KernelPanic-Kotlin-Port\AGENTS.md`

---

## 9. The standing rule

> **catoscript is the smallest language that can drive the terminal. Read it out loud, then run it. The grammar is closed. The implementation discipline (§13) is closed. New capabilities land in stdlib, not in the language. New stdlib names must clear §11.**

If a proposal fails the four checks in `catoscript-devplan.md` §14, it doesn't ship. If a §10 item is proposed, the §10 amendment process applies. If a "while we're at it" feature is suggested, this section is the answer.

This is bureaucracy on purpose. Languages die from feature creep, not from missing features.