# InterpreterPolicy

> Cave-cat explanation of InterpreterPolicy and the step counter. Pure analogy, no code tokens.

---

## The Setup

The doer cat picks up stones one by one. But what if the cat picks up a stone that says "jump back to here," and the stone right before that says "jump back to here," and so on forever? The cat would pick up stones until the sun burns out.

The cat needs a **tiredness setting**. After a certain number of stones, the cat gets tired and stops. That's the whole job of `InterpreterPolicy`.

---

## The Four Knobs

Four knobs on the tiredness setting:

1. **Max stones per nap** — how many stones the cat picks up before pausing for a breath. In the game, this is the per-frame budget (the interpreter loop is also a cooperative scheduler). In the CLI, there's no frame, so this knob is just sitting there for future use.

2. **Max stones total** — how many stones the cat will pick up across the *whole* script run, no matter what. Hit this and the cat stops, period.

3. **Max basket depth** — how many baskets may be nested inside baskets. If calls keep nesting past this mark, the cat growls instead of filling the cave with an endless tower of baskets.

4. **Seed** — a number for when the script needs randomness later. Nothing reads this knob yet. It's just sitting there so the shape is in place.

That's the whole policy. Four knobs.

---

## Why a Tiredness Setting

Three reasons:

1. **Infinite loops.** A script with a `jump :LOOP` and no way out would run forever. The tiredness setting stops it.
2. **Games need breathing room.** In Kernel Panic, the interpreter runs inside a frame loop. If the script eats the whole frame, the game freezes. The per-nap knob is how much script time the game is willing to spend per frame.
3. **Tests need determinism.** A test wants to know "this script finishes in N stones, or it errors out cleanly." Without a cap, a buggy script could hang the test runner.

The tiredness setting is a guardrail. It's not the interesting part of the interpreter — the stone-walking is. It's the thing that prevents the stone-walking from going wrong in obvious ways.

---

## What Happens When the Cat Gets Tired

The cat doesn't growl. It comes home with a special report: "I got tired at stone N." That's `BudgetExceeded`. The caller (the CLI, the game, the test) decides what to do — usually print a friendly error.

This is different from a real error (unknown label, type mismatch), which is a growl.

---

## Picture

```
   doer cat's pouch
   +-----------------------+
   |                       |
   |  +---------------+    |
   |  |  step counter |    |  <-- ticks up per stone
   |  +---------------+    |
   |          |            |
   |          v            |
   |  counter >= maxTotal? |
   |     |          |      |
   |    yes         no     |
   |     |          |      |
   |     v          v      |
   |   STOP     pick up    |
   |            next stone |
   |                       |
   +-----------------------+

   InterpreterPolicy
   +-----------------------+
   |  maxStepsPerTick: 100 |  <-- per-nap budget
   |  maxTotalSteps: 1e6   |  <-- total budget
   |  max basket depth: 64 |  <-- nested-basket guard
   |  seed: empty          |  <-- for later
   +-----------------------+
```

---

## Trace

Imagine a script that's just:

```
:LOOP
jump :LOOP
```

InterpreterPolicy says `maxTotalSteps = 1_000_000`.

The cat picks up `:LOOP` (a label — counter ticks up, finger moves down). Picks up `jump :LOOP` (counter ticks up, finger leaps back). Repeats. Counter hits 1,000,000. Cat comes home with `BudgetExceeded(stepsConsumed = 1_000_000)`.

Same script, but policy says `maxTotalSteps = 10`. Cat comes home after 10 stones.

---

## Why Four Knobs and Not One

- **Total-stone ceiling** is the hard ceiling. Always on. Always there.
- **Per-nap ceiling** is for leash-holders that care about frames, like games. The terminal does not care. Keeping it as a knob means the same doer cat can serve both.
- **Basket-depth ceiling** prevents endless nesting even when the total-stone ceiling is large.
- **Seed** is a placeholder. It's there so that when randomness lands, the tiredness setting already has the right shape.

---

## One Line

Four knobs: per-nap budget, total budget, basket depth, and a seed for later. The cat gets tired or growls before a runaway walk fills the cave.
