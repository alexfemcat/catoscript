# catoscript

> The smallest language that can drive the terminal.
> Small enough to read out loud. Powerful enough that you do not have to write a `strlen` from scratch.

catoscript is a literacy first scripting language: every programming idea has a one sentence English shape in cat vocabulary. Concepts before keywords. Read it out loud, then run it.

If you have dabbled in Python or Scratch and want a language that does not get in the way, this is for you. If you have never written a line of code, the section below will hold your hand through the first script.

---

## Quick install for contributors

```bash
./gradlew publishToMavenLocal
```

That puts `catoscript-<version>.jar` into `~/.m2/repository` so the
`cato` launcher at the repo root can run your scripts against the
freshly built library. Run it again after every change to Kotlin code
in this repo. See section 2.3 below for the full dev workflow.

---

## (1) catoscript in a nutshell

**If you have never coded before, read this paragraph first, otherwise skip to 1.1 below**

A program is a list of instructions written in a file. The computer reads the file from top to bottom and does each instruction in order, one after another, until it reaches the end. That is the whole model. Everything else, loops, variables, functions, is built on top of that one idea.

A `.cato` file is one of those files. The instructions are short English shaped words (most of them cat themed, because the language grew up inside a game). When you run a `.cato` file, the computer reads it and does what each line says.

### 1.1 Print two lines

```catoscript
meow "hello"
meow "world"
```

`meow` is the instruction "print this to the screen." The thing in quotes is the text to print. Run this file and the screen shows:

```
hello
world
```

Two lines, because the file had two instructions and the computer did them in order.

### 1.2 Remember something with a variable

```catoscript
set $name "mochi"
meow "hello, $name"
```

`set` puts a value into a labeled box. `$name` is the label on the box. `"mochi"` is what goes inside it. The `$` is how you say "this is a variable name" instead of a regular word. The second line prints the text `hello, ` followed by whatever is in the `$name` box, so the screen shows:

```
hello, mochi
```

Change `"mochi"` to your own name and run it again. That is editing a program.

### 1.3 Make a decision

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

`sniff` asks a yes/no question. Here it asks "is `$hp` less than 1?" `purr_at :DEAD` means "if yes, jump to the line labeled `:DEAD`." If the answer is no, the computer keeps going to the next line. `jump :END` always jumps. `:DEAD` and `:END` are named signposts in the file, like chapter titles you can jump to. With `$hp` set to 5, the screen shows `still alive`. Change `set $hp 5` to `set $hp 0` and run it again — now it prints `game over`.

This is what an `if/else` looks like in catoscript, just spelled out in English instead of nested braces.

### 1.4 Skip a block with a label

```catoscript
meow "before"
jump :SKIP
meow "never prints"
:SKIP
meow "after"
```

`:SKIP` is a label, a named signpost in the file. `jump :SKIP` moves the interpreter there in one step. Lines between the original position and the label are skipped. The screen shows:

```
before
after
```

A conditional branch uses `sniff` + `purr_at` together (you saw that in §1.3). The interpreter walks top to bottom and labels are the only places it can land besides the next line. A real loop — repeat until a condition flips — uses `sniff` + `purr_at :LOOP` in the same shape you saw in §1.3.

> **Note.** A `for ... in ...` loop and list literals (`[]`) are *planned* in the devplan §5.9 but **not yet shipped** in the current library. The label + `jump` pattern shown here is the only iteration shape that runs today.

### 1.5 You just learned the shipped surface

Those four scripts cover every shape that ships in the current library: print (`meow`), variable (`set`), decision (`sniff` + `purr_at` / `hiss_at`), and unconditional jump (`jump :LABEL`). The interpreter loops by going back to a label, conditionally via `sniff` + `purr_at`, unconditionally via `jump`. That is the whole engine.

Beyond them are the *planned* additions — `for` over lists, the `[]` bracket family, `()` for function calls, the `std.*` standard library, persistence (`bury` / `dig`), and the rest of the tier ladder. Each is locked into the devplan §5.9 / §14 and documented in §5 below. They are not in the current library; they will land in their own commits.

---

## 2. Try it in thirty seconds

Pick the path that fits how you want to use catoscript.

### 2.1 Install the distribution (the one-click path)

Download `catoscript-shadow-<version>.zip` from the GitHub Release (or build it locally with `./gradlew shadowDistZip`). The zip contains everything you need:

```
catoscript-shadow-<version>/
├── bin/
│   ├── cato        # bash launcher (Mac, Linux, WSL, git bash)
│   └── cato.bat    # Windows launcher
└── lib/
    └── catoscript-<version>.jar   # the fat jar (parser, interpreter, CLI, stdlib)
```

Unzip it anywhere and add the `bin/` folder to your `PATH`. That's the whole install.

**Windows (PowerShell or cmd):**

```powershell
Expand-Archive .\catoscript-shadow-0.3.0-LOCAL.zip -DestinationPath C:\Tools\
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\Tools\catoscript-shadow-0.3.0-LOCAL\bin", "User")
```

Close and reopen the terminal. From anywhere:

```powershell
cato run hello.cato
```

**Mac / Linux / git bash / WSL:**

```bash
unzip catoscript-shadow-0.3.0-LOCAL.zip -d ~/.local/
export PATH="$HOME/.local/catoscript-shadow-0.3.0-LOCAL/bin:$PATH"
```

From anywhere:

```bash
cato run hello.cato
```

Both `cato run <file>` and `cato <file>` work. The launcher discovers the fat jar next to itself; no classpath, no `~/.m2` lookup.

### 2.2 Just run a script (one jar, no install)

If you have a fat jar and a JDK, that's enough:

```bash
java -jar catoscript-0.3.0-LOCAL.jar hello.cato
```

The jar bundles the parser, interpreter, CLI, Kotlin stdlib, and `kotlinx-serialization-json`. One file, no classpath. Use this when you don't want to put `cato` on `PATH` — for CI, for one-off scripts, for sandboxed environments.

### 2.3 Build from source (the dev launcher)

For hacking on catoscript itself, the launcher at the repo root (`cato.bat` on Windows, `cato.sh` on Mac/Linux) reads the published fat jar from `~/.m2/repository`. Rebuild and re-publish whenever you change Kotlin:

```bash
./gradlew publishToMavenLocal
./cato.bat run samples\01_first_script\hello.cato   # Windows
./cato.sh run samples/01_first_script/hello.cato   # Mac/Linux
```

The CLI REPL at `./gradlew :tools:repl:run` lands later in Phase F (devplan §6). For now, `cato run <file>` is the CLI path.

The REPL reads:

```
catoscript> set $x 10
catoscript> meow "x is $x"
x is 10
catoscript> :exit
```

`:tutorial` walks the tier ladder interactively. `:load <file>` runs a `.cato` script. `:step` single steps one instruction.

---

## 3. The philosophy, in three lines

> **Yes, the words are cat themed.** That is on purpose. The language grew up inside a cat themed game and the vocabulary stuck because it works. You will get used to `meow`, `sniff`, `purr_at`, and `hiss_at` in about ten minutes. If you hate them, the language still works the same — the words just feel weirder.

1. **Read it out loud, then run it.** Every primitive has a one sentence English shape in cat vocabulary. `meow` prints. `sniff` checks a condition. `purr_at` jumps when true. `hiss_at` jumps when false. `jump` always jumps. If you cannot say it out loud and have the program do what you meant, the name is wrong.
2. **Concepts before keywords.** We do not add a keyword to teach a concept that can be said in English. `if/else` becomes `sniff` + `purr_at` / `hiss_at`. `while` becomes a label + `jump`. `function` becomes `jump :LABEL args... ... jump :end` (label parameters shipped in `0.3.1-LOCAL`). You learn the *concept* before you ever see the syntax.
3. **The grammar is closed. The stdlib grows.** New capabilities land as functions under `std.*`, never as new keywords. The grammar is the smallest thing that can express the tier ladder. The standard library is the longest comment in the language, and it is written in catoscript itself, so you can read it.

---

## 4. The tier ladder

If you keep going past the four scripts above, here is the whole journey, in order. Each tier unlocks the next. Pick a tier, build something, move on. You do not have to climb straight to the top.

The **status** column marks what is in the current library vs. what is locked into the devplan but not yet shipped. *Shipped* means the snippet on the right runs today. *Planned* means it is approved (per the four-check process in §7) but the language work has not landed yet.

| Tier | Status | What you can do after | The one line shape |
|---|---|---|---|
| 1 | shipped | Write a script that prints things in order | `meow "..."` |
| 2 | shipped | Remember values between lines | `set $name "..."` |
| 3 | shipped | Make the script branch on a yes/no question | `sniff ... purr_at ... hiss_at ...` |
| 4 | shipped | Loop a block of lines via a label and `jump` | `:LOOP ... jump :LOOP` |
| 5 | shipped | Bundle a reusable snippet with inputs (label params + call stack + `jump :end` return) | `jump :DRINK "milk"` |
| 6 | planned | Store many values in a row and walk them (`[]` lists + `for`/`[over]`) | `for $toy in $toys ... end_for` |
| 7 | planned | Save values to disk and load them next run | `bury $score ... dig $score` |
| 8 | planned | Promise a variable will hold a specific shape (opt-in `let`) | `let $count: num = 0` |
| 9 | planned | Build a command line tool that takes flags and files | `std.cli.args()`, `std.cli.exit(...)` |
| 10 | planned | Ask the user a question, show a message, confirm yes/no | `ask`, `show`, `confirm` |
| 11 | planned | Build a full screen menu out of asks and jumps | `menu :MAIN_MENU` |
| 12 | planned | Use clocks and randomness from the standard library | `std.time.now()`, `std.random.dice("3d6")` |
| 13 | planned | Read and write files on disk | `std.fs.read("log.txt")`, `std.fs.write(...)` |
| 14 | planned | Write tests for your own scripts | `cato test script.cato` |
| 15 | planned | Read and write JSON so your script can talk to other programs | `std.json.parse(...)`, `std.json.stringify(...)` |
| 16 | planned | Reach the network through the host (fetch a URL) | `std.web.fetch("https://...")` |

**Tiers 1 to 5 ship today. Tiers 6 to 16 are locked in the devplan and will land in their own commits, each behind its own feature flag.** A new player going Tier 1 → 16 learns: cause-and-effect → state → decisions → loops → functions → lists → persistence → types → CLI → interactive UI → state-machine UIs → libraries → filesystem → testing → interop → network-via-host. That is a programming curriculum, hidden inside a cat vocabulary. No "you will need to learn X first" gap — but most of the ladder is still ahead of the library.

---

## 5. The `[]` family and the `()` call

Beyond the keywords, catoscript has a small vocabulary of punctuation that makes list work and function calls read out loud. Each entry below is sugar for a `std.*` function. None are keywords. The grammar stays closed.

> **Status.** The bracket family and `()` call punctuation are defined in `catoscript-devplan.md` §5.9 and §14 but **not yet shipped** in the current library. They are documented here so the language shape is visible end to end. Each lands in its own commit once its tier work begins.

### `[]` lists · `[over]` walker

```catoscript
set $toys ["ball", "mouse", "yarn"]
for $toy in $toys          # equivalent to: $toys [over] $toy
  meow "$toy"
end_for
```

`[]` is a list literal (a row of boxes). `for ... in ...` desugars to `[over]` — the keyword form for readability, the bracket form for short scripts.

### The bracket vocabulary

Nine bracket operators, grouped by what they do.

**Declaration** — read like a sentence:

```catoscript
$state [is] loading        # same as: set $state "loading"
```

**Order** — arrange or randomize a list:

```catoscript
$names [sort]    $sorted       # alphabetical
$deck  [shuffle] $shuffled     # randomized
$names [reverse] $reversed     # backwards
```

**Selection** — pick one item:

```catoscript
$deck [first] $top_card
$deck [last]  $bottom_card
```

**Shape** — ask about or transform the whole list:

```catoscript
$list  [count] $n              # how many items
$parts [join "-"] $slug        # glue with a separator
```

**Test** — ask yes/no about the list:

```catoscript
$list   [empty?]         $is_empty    # nothing in it?
$toys   [contains? "yarn"] $has_yarn  # has this item?
```

Every bracket is sugar for a `std.list.*` or `std.random.*` function. The bracket is the readable form; the stdlib function is canonical. **The bracket family is closed after this round.** Further expansion goes through the §10 amendment process.

### `()` for function calls

A function is a label with inputs. `()` shows the slot:

```catoscript
:GREET $name                # declare a labeled snippet with a $name slot
  meow "hello, $name"
  jump :end

greet("mochi")              # call it
greet("mochi") greet("luna")
```

`:GREET($name)` is the template declaration. `greet("mochi")` is the call. The underlying mechanism (Tier 5 label parameters + call stack) shipped in `0.3.1-LOCAL`; the readable `()` sugar is still pending — it shows the player where the inputs go without forcing them to remember the colon-prefix convention.

---

## 6. The two pulls (and how we resolve them)

Two pressures push on this language, and the design is the answer to both.

1. **"Small enough to read out loud"** pulls toward short sentences, no nesting, no curly braces, no import statements.
2. **"Teaches real stuff"** pulls toward variables, types, loops, functions, files, tests — the things a working script actually needs.

Most languages pick one and sacrifice the other. Python's `f"hello {name.upper()}"` does real stuff but it does not read out loud. Scratch reads out loud but it tops out before you can write a real program.

catoscript's bet: **make the real stuff read out loud too.** That is why `if/else` becomes `sniff` and `purr_at`, why a loop is a label you jump back to, why a function is `jump :NAME args`. The cat vocabulary is not decoration. It is the form the abstraction takes. If the words are right, the player's brain does not have to switch modes between reading English and reading code.

You do not have to agree with the bet to use the language. But if you have ever stared at a tutorial and thought "why is this so much harder to say than to do," that is the bet trying to win.

---

## 7. The rules

These are not aspirations. They are gates. A proposal that fails any of them does not ship.

### 6.1 The four check process

Every new capability must pass all four:

| Check | Question |
|---|---|
| §5 improvement | Does it collapse something the player currently has to write? |
| §10 in scope | Is it absent from the deliberate "no" list? |
| §11 reads out loud | Is there a one sentence English version in cat vocabulary? |
| §13 implementation | Does it fit the four gap implementation list, or is it pure stdlib? |

If any check fails, the proposal dies or goes back to the drawing board. Most proposals fail at the third check. That is the design working.

### 6.2 The "no" list (deliberate, locked)

The temptation: "catoscript should be a *real* language, so it should have..." fill in the blank with familiar Kotlin/Python/Rust features. Most of those features are wrong for catoscript.

| Feature | Why we say no |
|---|---|
| Closures / user defined functions with capture | Adds real parser + interpreter complexity. Labels + `jump` cover the player's mental model. |
| Extension functions | Cat themed sugar no player discovers without docs. Pure ergonomic cost. |
| Lambdas / higher order functions | The player can `for` over a list. Lambdas add syntax, scoping, and closure rules for no real win. |
| Classes / inheritance | The host models state. The language drives state. Two sources of truth = bugs. |
| Coroutines / `async` / `await` | The interpreter loop *is* the cooperative scheduler. There is nothing to await. |
| Operator overloading | Clever once, confusing forever. Explicit function calls read; `a + b` doing magic does not. |
| Module system / packages | One file = one script. `include` is the only seam needed. |
| Macros / compile time evaluation | The player writes runtime scripts, not a language. Macros add a meta language to read other people's scripts. |
| Bytecode compiler / AOT to JVM classes | Worth doing eventually for performance, not now. Solve the perf problem when one exists. |
| Generic type parameters / variance | Opt in types are for the analyzer, not runtime dispatch. `list<str>` is sugar. |
| Pattern matching beyond `sniff` | `sniff ==` covers 90% of what players do. Full pattern matching is a parser project for marginal benefit. |
| String interpolation with code execution | Embedded expression grammar in strings. Stick to `$var` interpolation; no code in strings. |

**The rule.** Add a feature only if it collapses something the player currently has to write. Never add a feature for parity with another language.

### 6.3 The amendment process

If someone proposes adding one of these later, the workflow is:

1. Open a doc PR adding the feature to a "Proposed" subsection with a "why it clears the bar" argument.
2. The proposal must name the **specific scripts that get shorter** because of the feature. "It would be cleaner" does not count. "This 30 line script becomes 5 lines" does.
3. Two week waiting period. If nobody in that time posts a counter example showing the script can already be short without the feature, the proposal moves into §5.9. If somebody shows it can, the proposal dies.

This is bureaucracy on purpose. Languages die from feature creep, not from missing features.

---

## 8. The host seam (`CatoHost`)

Every command that needs to reach outside the script goes through one interface. The interpreter takes a host in its constructor. The library ships `NullHost` for tests and the CLI REPL. Kernel Panic ships `KernelPanicHost`. Future hosts pick.

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

    fun fetchUrl(url: String): String?
}

object NullHost : CatoHost {
    override fun print(line: String) {}
    override fun playTone(freq: Double, durationMs: Int, waveform: Waveform) {}
    // ... all methods no op or sensible default
}
```

**Per host implementation policy:**

| Host | terminal | audio | clock | env | CLI | FS | Network |
|---|---|---|---|---|---|---|---|
| `NullHost` (lib tests) | no op | no op | real | null | empty / EOF | null / false | null |
| `ReplHost` (CLI REPL) | ANSI stdout | none | real | env vars | real | real disk | `java.net.http` |
| `KernelPanicHost` (KP) | Compose text | audio engine | real | game state | no op | VFS | not available |
| `WebHost` (playground, future) | canvas | WebAudio | `Date.now()` | URL params | none | virtual FS | browser `fetch()` |

Every method is optional in the sense that the host picks what is meaningful. No host implements everything.

---

## 9. The stdlib is dogfooded

> **Status.** The standard library (`std.*`) is *planned* in the devplan §12 / §14 but **not yet shipped** in the current library. This section documents the rule the stdlib will follow once it lands.

Every `std.*` function must be implementable as a small catoscript script itself. Example — `std.cli.exit_fail` would be:

```catoscript
:STD_CLI_EXIT_FAIL $msg
  std.cli.print_err($msg)
  std.cli.exit(1)
  jump :end
```

The interpreter resolves `std.cli.exit_fail` to the `:STD_CLI_EXIT_FAIL` label in the stdlib. The player can `cat std/cli/exit_fail.cato` and read the implementation. **The stdlib is the longest comment in the language.**

This rule:

* Forces the stdlib to read out loud (it is written in catoscript).
* Gives the player real scripts to read past Tier 8.
* Catches bad stdlib design early. If a function cannot be written cleanly in catoscript, it is the wrong abstraction.

---

## 10. Implementation discipline

> Simple is not the absence of advanced. Simple is the absence of unnecessary.

There is a specific failure mode that kills small language projects:

1. Start the parser. It is a clean recursive descent grammar.
2. Decide "while we are at it, let us add closures." Now the grammar needs a closure conversion pass.
3. Decide "and async." Now the interpreter needs a state machine, not a loop.
4. Decide "and types." Now we need a type checker.
5. Two years later, you have a half finished Rust.

Every "advanced" feature pulls in two more "advanced" features that it depends on.

### The four real implementation gaps

The parser and interpreter work that is actually worth doing. All behavior preserving. All collapsing something real.

| Gap | Why it matters |
|---|---|
| Real parser, typed AST, line/column positions | Better errors, LSP hover/jump, formatter, stepper. The line splitter today has bugs (nested quotes, escape sequences, `$var` interpolation re parsed at runtime). |
| Configurable `InterpreterPolicy` (step budget, max total steps, seed) | Testability, deterministic replays, future sandboxing. |
| Generic `env()` host call (replaces KP specific `sniff_env`) | Language portability — the host decides what env is. |
| `Stepper` interface | LSP debug features, `:step` in REPL, KP debug overlay. |

That is the list. Four things. Each ships in its own commit. Each clears the bar. None are "more advanced." They are *correct*.

### The traps, named explicitly

| Trap | One line rejection |
|---|---|
| Bytecode compilation (parse to AST to custom bytecode to interp) | A `cat` clone runs in milliseconds. The 100 instr/tick budget is a feature, not a bottleneck. |
| AOT to JVM `.class` files | The moment you compile to JVM bytecode you have shipped a JVM language. That is Kotlin, not catoscript. |
| Register based VM with optimizations | None of this matters at script scale (hundreds of instructions, not millions). |
| SSA form IR for optimization | catoscript does not *have* an optimization problem. |
| Type inference at the language level (Hindley Milner, bidirectional) | Opt in `let` annotations. The analyzer reads the annotation. No inference needed. |
| Closure conversion | Closures are in the "no" list. The pass is only needed if we add closures. We will not. |
| Async / await IR transformation | Async is in the "no" list. Nothing to await. |
| JIT compilation (HotSpot style) | catoscript does not ship a JIT. JVM does. Use the JVM. |
| Tail call optimization | The script runs in a step budget, not a stack depth budget. TCO solves a problem the interpreter does not have. |
| Constant folding / dead code elimination | The analyzer can warn about unused `$vars`. Runtime folding does not matter at script scale. |

If a future proposal adds any of these, the workflow is the amendment process: name the **specific scripts that get faster or smaller**, two week wait, decide. None of the above clears the bar today. None should clear it without an actual measured bottleneck.

### The corollary rule

> The engineering quality of catoscript comes from doing the four things above correctly, not from adding more layers between source and execution.

A 600 line recursive descent parser that produces a typed AST is *higher quality* than a 6000 line bytecode VM. The first one does its job and stops. The second one does five jobs, none of them well, and ships six months late.

Pick the line where complexity earns its keep. Draw it. Do not cross it on a "while we are at it."

---

## 11. The seam with Kernel Panic

Kernel Panic grew catoscript. The language is leaving the building. The plan, in three lines:

* catoscript becomes a standalone Kotlin/JVM library published to `mavenLocal()`.
* Kernel Panic consumes it like any other dependency via `com.catoscript:catoscript:0.x.y-LOCAL`.
* KP's `:cato-kotlin` module is deleted. The 67 command canned tools registry stays in KP (it is a terminal shell layer, not a language primitive). All Compose UI stays in KP.

The migration is phased (A through H), each phase one commit, each commit green. Details live in `catoscript-devplan.md` §6.

The host seam is the boundary. `KernelPanicHost : CatoHost` lives in KP. `NullHost` lives in the lib. The CLI REPL's `ReplHost` lives in the lib. Future hosts (web, mobile, embedded) plug into the same seam.

---

## 12. The tradition

This kind of language has a name and a lineage: **literacy first DSL**, sometimes called a **pedagogical language** or **concept first language**. The pattern is always the same:

1. Strip syntax to the minimum that can express the concept.
2. Use vocabulary that maps 1:1 to how a human would describe the program.
3. Each new feature is a *new concept*, not a *new keyword*.

Predecessors and cousins: Logo (turtle graphics), Scratch (block syntax), BASIC (line numbered minimalism), HyperCard (English shaped scripting). catoscript is in this lineage, explicitly.

catoscript is **not** Logo or Scratch. We are not for kids. We are not a teaching tool for children.

catoscript **is** for the hacker who does not want to learn a language to script a game. The player has probably touched Python or JavaScript. They have *no interest* in learning another language. They want to script the terminal *now*, and if reading a script teaches them what a `for` loop is along the way, that is a bonus. Not the point.

The pedagogical positioning is a *consequence* of the design discipline, not the goal. The goal is "small enough to drive the terminal." The teaching happens because every concept has a one sentence English shape.

---

## 13. The naming rule

`catoscript` is one word, camelcase. Not `cato-script`. Not `KatoScript`. Not `CATOScript`. The package root is `com.catoscript`. Everything in the repo follows this rule.

In Kotlin source comments and player facing output, no ASCII dashes (`-`) appear alone. Use middle dots `·`, slashes `/`, colons `:`, or rewrite the sentence. Comments live in Chinese finger trap territory: easy to break, silent failures.

---

## 14. License

MIT. Anyone can use, modify, and redistribute catoscript for any purpose; the copyright notice must travel with the code. See `LICENSE` at the repo root.

---

## 15. Where to read more

* `AGENTS.md` — the language first project doc, no Kernel Panic context, daily driver.
* `catoscript-devplan.md` — the source of truth for what catoscript is and is not. §5 improvements, §10 out of scope, §11 pedagogical positioning, §12 CLI + UI stdlib, §13 implementation discipline, §14 capability surface (Tier 12 to 16 stdlib, including `std.web.fetch`, and the bracket family). If any other document contradicts the devplan, the devplan wins.
* `samples/` — golden scripts organized by tier.
* `tools/repl/` — the CLI REPL app.

The standing rule:

> catoscript is the smallest language that can drive the terminal. Read it out loud, then run it. The grammar is closed. The implementation discipline is closed. New capabilities land in stdlib, not in the language. New stdlib names must clear the read out loud test.

If a proposal fails the four checks, it does not ship. If a "while we are at it" feature is suggested, this README is the answer.