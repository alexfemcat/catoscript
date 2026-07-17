# Label Map

> Cave-cat explanation of the label map — how the doer cat knows where signposts are. Pure analogy, no code tokens.

---

## The Setup

The doer cat reads jump stones. A jump stone says "leap the finger to the signpost named DEAD." But the cat has to know *which stone* is the signpost named DEAD. Without a map, the cat would have to walk the whole stack every time, looking for it.

That's the job of the **signpost map**: before the cat picks up a single stone, build a small book that says "name → stone position."

---

## How the Map Is Built

Before the cat starts walking:

1. Walk the whole stack once, top to bottom.
2. Every time you see a **label stone**, write the name and the stone's position into the book.
3. If the same name appears twice, that's an error — two signposts with the same name. The cat growls before it even starts.

The book is a small leather-bound thing. Each page has a name scratched at the top and a stone position underneath. "DEAD" → 6. "END" → 8. "MAIN_MENU" → 12.

The book is built **once**. The cat carries it for the whole walk.

---

## How the Map Is Used

When the cat reads a **purr**, **hiss**, or **jump** stone:

1. Read the signpost name from the stone.
2. Open the book to that name.
3. If the name is there, set the finger to that stone's position.
4. If the name isn't there, the cat growls. ("unknown signpost").

That's it. No walking the stack. No searching. Open the book, look up, leap.

---

## Why Build It Up Front

Three reasons:

1. **Speed.** Looking up in a book is one step. Walking the stack is "however many stones there are." For a script with lots of jumps, the book is the difference between fast and slow.
2. **Errors caught early.** A duplicate signpost is a bug. Better to find it before the cat starts walking than in the middle of a 1000-stone run.
3. **Predictability.** The book is fixed for the whole walk. No "the signpost moved" surprises. The finger either leaps to where the book says, or growls.

---

## What Lives in the Book vs. on the Stack

- **In the book:** signpost names and their stone positions. Built once, never changes.
- **On the stack:** the actual stones, including the label stones themselves. The label stones are still there — the cat just walks past them at runtime. They're a no-op.

So the label stones are *double-bookkept*: they're on the stack (so the cat can walk past them) AND in the book (so jumps can find them). Two records of the same thing.

---

## Picture

```
   stack of stones                  signpost book
   +-----------+                    +-----------------+
   | 0: set    |                    |                 |
   +-----------+                    |  DEAD   ->  5   |
   | 1: sniff  |                    |  END    ->  7   |
   +-----------+                    |  LOOP   ->  4   |
   | 2: purr   |  ---book says--->  |                 |
   |    :DEAD  |    DEAD -> 5       +-----------------+
   +-----------+                    built once before
   | 3: meow   |                    the cat walks
   +-----------+
   | 4: jump   |  ---book says--->   END -> 7
   |    :END   |
   +-----------+
   | 5: :DEAD  |  <-- this stone
   +-----------+      (label, ignored at runtime,
   | 6: meow   |       but its position is in the book)
   +-----------+
   | 7: :END   |  <-- same here
   +-----------+
```

---

## Trace with `samples/3.cato`

Stack:
- 0: Set
- 1: Sniff
- 2: PurrAt("DEAD")
- 3: Meow
- 4: Jump("END")
- 5: Label("DEAD")
- 6: Meow
- 7: Label("END")

Build book:
- Walk to 5: see Label("DEAD") → book["DEAD"] = 5.
- Walk to 7: see Label("END") → book["END"] = 7.

Book: `{ DEAD: 5, END: 7 }`.

Walk:
- 0, 1, 2, 3 (Meow "still alive"), 4 (Jump "END" → look up book → finger = 7), 5 (Label "DEAD", ignored), 6 (Label "END", ignored), finger falls off bottom. Done.

---

## One Line

Build a name-to-position book from the label stones before walking. Jump stones open the book and leap to whatever position is written there.
