# Variables and Sniff

> Cave-cat explanation of the doer cat's two pieces of memory: the name-tag pile and the nose-memory. Pure analogy, no code tokens.

---

## The Setup

The doer cat has four things in its pouch (see `interpreter.md`). This doc zooms in on two of them — the **name-tag pile** and the **nose-memory** — because they're the cat's memory, and memory is where the script's state lives.

---

## The Name-Tag Pile

Imagine a pile of small leather tags, each with a name scratched on it and a thing tied to the bottom.

- "hp" → tied to a number pouch: 5.
- "name" → tied to a sentence pouch: "mochi".
- "alive" → tied to a yes/no pouch: yes.

When the doer cat reads a **set stone**, it adds (or replaces) a tag. The name on the tag comes from the stone. The thing tied to it comes from looking at the value-stone inside.

When the doer cat reads a **name-lookup stone**, it finds the tag with that name and grabs the thing tied to it. If there's no such tag, the cat growls.

That's the whole name-tag pile. Tags in, tags out. Scratch a name, get a thing.

---

## The Nose-Memory

Imagine a tiny pouch under the cat's nose that holds the last yes/no smell the cat smelled.

- Cat reads a **sniff stone** → smells the question inside → writes yes or no into the nose-pouch (overwriting whatever was there).
- Cat reads a **purr stone** → sniffs the nose-pouch. If yes, jump. If no, walk past.
- Cat reads a **hiss stone** → sniffs the nose-pouch. If no, jump. If yes, walk past.

That's it. One slot. Last yes/no wins.

---

## Why Two Kinds of Memory

The name-tag pile is **long-lived during an ordinary walk**. Tags stay until something replaces them. When the cat enters a basket, it saves a copy of the caller's pile, fills the basket's parameter tags, and restores the saved pile when the basket returns.

The nose-memory is **one-shot**. It's the bridge between a sniff and the next purr or hiss. After the purr/hiss uses it, the memory is "stale" — but the script is supposed to sniff again before the next purr/hiss, so it doesn't matter.

If you tried to use the name-tag pile for the yes/no bridge, every set-stone would overwrite it, and purr/hiss would get confused. If you tried to use the nose-memory for variables, every sniff would overwrite it, and you'd lose your data. Two memories, two jobs.

---

## The Order Rule

There's a strict rule: **a purr or hiss stone must be preceded by a sniff stone, with no set-stone in between that would change the answer.**

In practice, catoscript's parser doesn't enforce this — it just lets the doer cat growl at runtime if you try `purr_at :FOO` with no prior sniff. But the *idiomatic* shape is:

```
sniff <question>
purr_at :YES_BRANCH
hiss_at :NO_BRANCH
```

The yes/no is fresh. The nose-memory reflects *that* question, not an old one.

---

## Picture

```
   doer cat's pouch
   +-----------------------+
   |                       |
   |  +---------------+    |
   |  |  name-tag     |    |
   |  |  pile         |    |  <-- long-lived, tags in/out
   |  |               |    |
   |  |  hp -> 5      |    |
   |  |  name -> mochi|    |
   |  |  alive -> yes |    |
   |  +---------------+    |
   |                       |
   |  +---------------+    |
   |  |  nose-memory  |    |  <-- one-shot, last yes/no
   |  |               |    |
   |  |     no        |    |
   |  +---------------+    |
   |                       |
   |  +---------------+    |
   |  |  finger       |    |  <-- which stone we're on
   |  +---------------+    |
   |                       |
   |  +---------------+    |
   |  |  step counter |    |  <-- how many stones we've done
   |  +---------------+    |
   +-----------------------+
```

---

## Trace with `samples/3.cato`

1. Set stone → add tag "hp" → tie Number(5) to it. Pile: `{ hp: 5 }`. Nose: empty.
2. Sniff stone → look at Compare(GT, hp, 10) → 5 > 10 → no. Nose: `no`. Pile: `{ hp: 5 }`.
3. Purr stone → sniff nose → no → don't jump. Pile: `{ hp: 5 }`. Nose: `no`.
4. Meow stone → look at Sentence → "still alive". Yell it. Pile unchanged.
5. Jump stone → leap to Label("END").
6. Label stone → ignore. Pile unchanged.
7. Done.

At the end: pile = `{ hp: 5 }`, nose = `no` (stale, never reused).

---

## One Line

Long-lived name-tag pile for variables, one-shot nose-memory for the last yes/no. Two memories, two jobs.
