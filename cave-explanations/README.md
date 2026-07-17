# Cave Explanations

> **AI-generated.** These docs were written by an AI assistant in cave-cat mode. They are *learning aids*, not authoritative spec. If something here disagrees with the code or `AGENTS.md` / `catoscript-devplan.md`, the code and the devplan win.

> Pure-analogy (cave-cat mode) explanations of the catoscript runtime pieces. No code tokens in the analogies — the goal is the *picture*, not the syntax. The bridge back to code happens after the picture lands.

## Index

| File | What it explains |
|---|---|
| [`parser.md`](parser.md) | The parser cat. Bark with claw-marks in, stack of stones out. |
| [`interpreter.md`](interpreter.md) | The doer cat. Stack of stones in, yells and side-effects out. |
| [`AST.md`](AST.md) | The stone catalog. Instruction stones (do) and value stones (be), plus the three pouch shapes. |
| [`variables-and-sniff.md`](variables-and-sniff.md) | The doer cat's two memories: the name-tag pile (long-lived) and the nose-memory (one-shot yes/no). |
| [`CatoHost.md`](CatoHost.md) | The leash. The named pokes from the script to the outside world; whoever holds the other end decides what each poke does. |
| [`InterpreterPolicy.md`](InterpreterPolicy.md) | The cat's tiredness setting. Three knobs: per-nap budget, total budget, seed for later. |
| [`InterpreterResult.md`](InterpreterResult.md) | The three doors out of the cave: finished, tired, or growled. |
| [`label-map.md`](label-map.md) | The signpost book. Built once before the walk; jump stones open it and leap. |
| [`SourcePos.md`](SourcePos.md) | The clay tablet on every stone. Line + column, so growls say "right here" not "somewhere." |

## How to read these

Pick the file that matches the piece you're trying to understand. Read the analogy, then the picture, then the trace with `samples/3.cato`. The trace is the bridge from the cave back to the real code — same idea, real names.

If a piece is missing, ask for it. The pack grows on demand.
