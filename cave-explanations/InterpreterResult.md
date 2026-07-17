# InterpreterResult

> Cave-cat explanation of InterpreterResult — the three ways the doer cat can come home. Pure analogy, no code tokens.

---

## The Setup

The doer cat goes into the cave with a stack of stones. It comes home later. There are exactly **three ways** it can come home:

1. **It finished the stack.** All stones picked up, finger fell off the bottom. Normal ending.
2. **It got tired.** Hit the step counter ceiling. Came home early.
3. **It growled.** Real failure — unknown signpost, type mismatch, purr with no prior sniff. Came home with a complaint.

That's the whole report. Three doors out of the cave.

---

## The Three Doors

### Door 1: Finished

The cat walks down the stack until the finger falls off the bottom. No complaints. Report is just "done."

In code: `Completed`. Nothing else attached. No message, no count. The cat did its job.

### Door 2: Tired

The cat hit the max-stones ceiling. Came home early. Report is "I got tired at stone N."

In code: `BudgetExceeded(stepsConsumed = N)`. The count is attached so a debugger or test can say "the script ran for 1,000,000 stones and gave up."

### Door 3: Growled

The cat hit a real problem. Unknown signpost, type mismatch, missing name-tag, purr with no prior nose-memory. Came home with a complaint.

In code: `RuntimeError(message = "...", stepsConsumed = N)`. The message is human-readable — meant for the CLI or the game's error pane. The count is attached for the same reason as Door 2.

---

## Why Three Doors (Not One)

If there was only one door, the caller would have to guess: "did the script finish? did it run out of budget? did it crash?" That's three different responses (success message, retry, show error). Guessing is a bug.

Three doors = three unambiguous outcomes. The caller switches on the shape and handles each one cleanly.

---

## Why the Count Is Attached to Doors 2 and 3

A debugger or stepper wants to say "you got to step 4,237 before things went wrong." Without the count, the debugger is blind — it just knows *that* something went wrong, not *where*. The count is a small piece of telemetry that costs nothing to attach and saves real debugging time.

Door 1 doesn't need a count because the count is "all of them" by definition.

---

## Picture

```
   doer cat walks stones
          |
          v
   +---------------+
   |  pick stone   |--+
   |  (counter++)  |  |
   +---------------+  |
          |           |
          v           |
   +---------------+  |
   | counter < max?|  |  no --> Door 2: Tired
   +---------------+  |
          | yes       |
          v           |
   +---------------+  |
   | stone shape?  |  |
   +---------------+  |
          |           |
   +--+--+--+--+-----+--+
   |  |  |  |  |        |
   meow set ... jump    |
   ...                  |
   |  |  |  |  |        |
   +--+--+--+--+--------+
          |
          v
   growls? --yes--> Door 3: Growled (with message + count)
          |
          no
          |
          v
   finger off bottom? --no--> loop back to "pick stone"
          |
          yes
          |
          v
   Door 1: Finished
```

---

## Trace

Script with a typo:

```
set $hp 5
meow $name
```

1. Set "hp" → 5. Pile: `{ hp: 5 }`. Counter: 1.
2. Meow stone → look at Name-lookup("name"). No tag named "name". Cat growls.

Cat comes home through Door 3: `RuntimeError("undefined variable: name", stepsConsumed = 2)`.

Caller sees Door 3, prints the message, exits.

---

## One Line

Three doors out of the cave: finished, tired, or growled. Each has its own shape so the caller knows what to do.
