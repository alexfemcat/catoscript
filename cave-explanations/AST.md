# AST

> Cave-cat explanation of AST, Stmt, Expr, and Value. Pure analogy, no code tokens.

---

## The Setup

The parser cat hands the doer cat a stack of stones. But the stones aren't all the same — there are *instruction* stones and *value* stones, and even the instruction stones come in different shapes. This doc is the catalog of stone shapes.

Two big families of stones:

1. **Instruction stones** — things the doer cat *does*. Print, jump, remember, sniff.
2. **Value stones** — things the doer cat *uses*. Numbers, sentences, name-lookups, yes/no questions.

A third thing lives in the same neighborhood: the **value in the cat's pouch**. The doer cat takes value stones, looks at them, and produces a pouch-value. The pouch-values are: a number, a sentence, or a yes/no.

---

## Instruction Stones (the ones the doer cat acts on)

Each instruction stone is one of these shapes:

- **Meow stone** — has a value-stone inside. Doer cat looks at the value-stone, turns it into a sentence, yells it.
- **Set stone** — has a name-scratch and a value-stone. Doer cat ties the value to the name-tag in its pile.
- **Sniff stone** — has a yes/no question inside. Doer cat smells the answer, writes yes or no into its nose-memory.
- **Purr stone** — has a signpost name. Doer cat checks nose-memory. If yes, jump. If no, walk past.
- **Hiss stone** — has a signpost name. Same as purr, but flip the answer.
- **Jump stone** — has a signpost name and may carry gift stones. Doer cat always jumps, no question asked; jumping to a basket is forbidden.
- **Basket stone** — has a name, a row of empty name-tags, and its own stack of instruction stones. The doer cat walks past the stored basket until it is called.
- **Call stone** — has a basket name and gift stones. Doer cat saves its place and name-tags, fills the basket's tags, and walks the basket.
- **Return stone** — ends the current basket walk and restores the saved place and name-tags.
- **Label stone** — has a signpost name and may carry empty name-tags. Doer cat ignores it. It's there for the signpost book.
- **Thought stone** — has some words. Doer cat ignores it.
- **Blank stone** — has nothing. Doer cat ignores it.

Plus one **bundle**: a list of stones, top to bottom. That's the whole script.

---

## Value Stones (the ones the doer cat looks at)

Each value stone is one of these shapes:

- **Sentence stone** — has a list of pieces inside. Each piece is either a plain chunk of words, or a name-tag to look up and stitch in. ("hello $name" → "hello " + (look up "name")).
- **Number stone** — has a number scratched on it.
- **Name-lookup stone** — has a name-scratch. Doer cat looks up that name in its tag pile.
- **Compare stone** — has two value stones and a "less than / equal / greater than" scratch. Doer cat looks at both values, decides yes or no.

---

## Pouch Values (what the doer cat produces when it looks at a value stone)

Three shapes, no more:

- **Number-pouch** — holds a number.
- **Sentence-pouch** — holds a sentence.
- **Yes/no pouch** — holds yes or no.

That's it. No decimals. No lists. No nulls. No baskets of stuff. Those come later when their features ship.

---

## Picture

```
+----------------------+
|      STONES          |
|                      |
|  Instruction stones  |  <-- the doer cat acts on these
|   - Meow             |
|   - Set              |
|   - Sniff            |
|   - PurrAt           |
|   - HissAt           |
|   - Jump             |
|   - Basket           |
|   - Call             |
|   - Return           |
|   - Label            |
|   - Comment          |
|   - Empty            |
|                      |
|  Value stones        |  <-- the doer cat looks at these
|   - Sentence         |
|   - Number           |
|   - Name-lookup      |
|   - Compare          |
+----------------------+

+----------------------+
|      POUCH VALUES    |  <-- what looking-at produces
|                      |
|   - Number pouch     |
|   - Sentence pouch   |
|   - Yes/no pouch     |
+----------------------+
```

---

## Why Two Families (and Not One)

If you mixed "do something" stones and "be something" stones in one bag, the doer cat would have to guess: "is this a thing I do, or a thing I look at?" Two bags = no guessing.

The split also means: any place where the doer cat expects a value, you can drop any value-stone in. Sentence-stone? Sure. Number-stone? Sure. "is 5 bigger than 10?" Compare stone that produces a yes/no? Sure. The shapes line up because the family is fixed.

---

## Trace with `samples/3.cato`

Stack of instruction stones:
- Set("hp", Number(5))
- Sniff(Compare(GT, Name-lookup("hp"), Number(10)))
- PurrAt("DEAD")
- Meow(Sentence(["still alive"]))
- Jump("END")
- Label("DEAD")
- Meow(Sentence(["game over"]))
- Label("END")

Doer cat walks down:
1. Set stone → tie Number-pouch(5) to tag "hp".
2. Sniff stone → look at Compare stone: Name-lookup("hp") gives Number(5), Number(10) gives Number(10), Compare(GT, 5, 10) → Yes/no-pouch(no). Write to nose-memory.
3. Purr stone → nose-memory says no → don't jump.
4. Meow stone → look at Sentence stone → Sentence-pouch("still alive") → yell "still alive".
5. Jump stone → leap to Label("END").
6. Label stone → ignore.
7. Finger falls off bottom. Done.

Output: "still alive".

---

## One Line

Two bags of stone shapes — instruction stones (do) and value stones (be) — plus three pouch shapes (what looking-at produces).
