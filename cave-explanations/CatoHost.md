# CatoHost

> Cave-cat explanation of CatoHost — the leash between the script and the outside world. Pure analogy, no code tokens.

---

## The Setup

The doer cat lives in a cave. The cave has walls. Inside the cave, the cat can read stones, tie name-tags, yell into the echo. But the cat can't reach *outside* the cave on its own.

The **leash** is the rope that lets the cat poke its nose out of the cave. Through the leash, the script can:

- Yell a line out into the world.
- Beep a sound.
- Move a cursor on a screen.
- Clear the screen.
- Look up an environment variable.
- Ask what time it is.
- Get command-line arguments.
- Read a line from the user.
- Leave the cave (exit).
- Yell an error line.
- Read a file.
- Write a file.
- Check if a file exists.

Every one of these is a poke through the leash. The cat doesn't do the work itself — it tugs the leash, and whatever's on the other end does the work.

---

## Why a Leash

The cat can't know what's on the other end of the leash. Sometimes it's a terminal. Sometimes it's a game engine. Sometimes it's a fake cave (for tests) where every poke gets ignored or returns a default.

The leash is a **list of named pokes**. Whoever holds the other end decides what each poke does. The cat just tugs.

This means the same script — same stones, same doer cat — can run in a game, in a terminal, in a test, or in a web browser. The leash is the only thing that changes.

---

## The Three Leash-Holders

- **Null leash** (for tests) — every poke does nothing or returns a default. No yelling, no beeping, no reading. The cat can run the script without anything actually happening.
- **Console leash** (for the CLI REPL) — yelling goes to the screen. Reading reads from the keyboard. Files are real files on disk. Time is real time.
- **Game leash** (for Kernel Panic) — yelling goes into the game world. Beeping plays through the audio engine. Files come from the game's virtual filesystem. The clock is the game's clock.

Three leashes. Same rope. Different ends.

---

## Picture

```
   inside the cave                    outside the cave
   +-------------+                    +-----------------+
   |             |                    |                 |
   |  doer cat   |                    |   terminal /    |
   |             |                    |   game / test   |
   |  +-------+  |     LEASH          |                 |
   |  | tug   |----[rope of named pokes]----> yell       |
   |  | tug   |----[                ]----> beep        |
   |  | tug   |----[                ]----> read file   |
   |  | tug   |----[                ]----> etc.        |
   |  +-------+  |                    |                 |
   |             |                    |                 |
   +-------------+                    +-----------------+
```

---

## What the Leash Is NOT

- The leash is **not** part of the language. The parser cat doesn't see it. The doer cat only sees it as a thing to tug.
- The leash is **not** how scripts talk to each other. One script can't poke another script's leash.
- The leash is **not** optional in the sense of "you can omit it" — every script run has *some* leash. The Null leash counts.

---

## Why This Matters for Reading Code

When you read the interpreter and you see `host.print(...)` or `host.readFile(...)`, that's a tug on the leash. To know what it actually does, you have to know which leash is plugged in. Reading the interpreter alone won't tell you — you also need to read whichever host implementation is being used.

That's the load-bearing piece: **the interpreter is leash-agnostic. The host is leash-specific.** Both files together tell the full story. Either one alone tells half of it.

---

## One Line

The leash is a named list of pokes from the script to the outside world; whoever holds the other end decides what each poke does.
