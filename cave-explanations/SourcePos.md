# SourcePos

> Cave-cat explanation of SourcePos — the line-and-column stamp on every stone. Pure analogy, no code tokens.

---

## The Setup

The parser cat hands the doer cat a stack of stones. When the doer cat growls ("unknown signpost!"), the caller wants to know *where in the bark* the bad stone came from. Was it line 5? Line 12? Column 3?

Without a stamp on each stone, the only answer is "somewhere." That's useless for debugging.

---

## What the Stamp Looks Like

Every stone the parser cat carves gets a small **clay tablet** tied to it. The tablet has two scratches:

- **Line number** — which claw-mark line the stone came from. (Lines start at 1.)
- **Column number** — which scratch within the line the stone started at. (Columns start at 1.)

That's it. Two numbers. Tied to every stone.

---

## Why Two Numbers and Not One

Line alone tells you which strip of bark. But some stones (like `meow`) have a value inside them — the value stone also has a stamp. If a growling happens *inside* the value (e.g. comparing two numbers where one is a name-lookup that doesn't exist), you want to know both:

- Which strip of bark did this stone come from?
- Which scratch on that strip?

Two numbers = "I know exactly where this went wrong."

---

## What the Stamp Is Used For

Three things:

1. **Error messages.** When the doer cat growls, the message says "at line 7, col 3." The user opens the file, jumps to line 7, scans to col 3, and sees the problem.

2. **Future debugger / stepper.** When a stepper walks the script one stone at a time, it can show "you're on stone at line 7, col 3" — the user can map stone positions back to file positions.

3. **Formatters / linters.** A formatter that wants to re-print the script with new spacing needs to know where each stone started so it can preserve comments and whitespace faithfully.

For all three, the stamp is the bridge between "stone in the stack" and "scratch on the bark."

---

## What's NOT on the Stamp

- **No file name.** Right now, every script is one file. If we ever load multiple files (one script `include`ing another), the stamp would need a file name too. Not yet.
- **No end position.** The stamp is where the stone *starts*, not where it *ends*. End positions are useful for some tools but not yet needed.
- **No offset within a multi-line string.** For now, each stone is one line. Multi-line strings would need their own stamp scheme. Not yet.

---

## Picture

```
   bark with claw-marks                    stack of stones
   +-----------------+                     +-----------------+
   | 1: set $hp 5    | ---parser carves--->| Set: stamp(1,1) |
   +-----------------+                     +-----------------+
   | 2: sniff $hp>10 |                     | Sniff: stamp(2,1)|
   +-----------------+                     +-----------------+
   | 3: purr_at :DEAD|                     | PurrAt: stamp(3,1)
   +-----------------+                     +-----------------+
   | 4: meow "alive" |                     | Meow: stamp(4,1) |
   +-----------------+                     |   inner Str:     |
                                          |   stamp(4,6)     |
                                          +-----------------+

   SourcePos
   +-----------------+
   | line: Int       |  <-- which strip of bark (1-based)
   | col: Int        |  <-- which scratch on the strip (1-based)
   +-----------------+
```

---

## Trace

Script:
```
set $hp 5
meow $name
```

Parser cat stamps each stone:

- Stone 0 (Set): stamp = line 1, col 1.
- Stone 1 (Meow): stamp = line 2, col 1. The inner Str stone (the literal "still alive") gets stamp = line 2, col 6 — the scratch right after `meow `.

If the doer cat growls on stone 1 because `$name` doesn't exist, the error says:

```
undefined variable: name at line 2, col 6
```

User opens the file, jumps to line 2, scans to col 6, sees `$name`. Knows exactly where to look.

---

## One Line

A two-number clay tablet (line, column) tied to every stone, so growls can say "right here" instead of "somewhere."
