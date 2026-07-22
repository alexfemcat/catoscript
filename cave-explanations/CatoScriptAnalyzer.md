# The Warning Cat

> Cave-cat explanation of the analyzer — the cat that inspects carved stones before the doer cat walks them. Pure analogy, no code tokens.

---

## The Setup

After the parser cat carves the stack, but before the doer cat starts walking, a warning cat can inspect the stones. This cat does not perform the instructions. It looks for mistakes that can be spotted while the stack is still sitting still.

First it makes two catalogs: one for every signpost, and one for every named basket of stones. Then it walks the stack and collects warning slips instead of stopping at the first problem.

---

## What the Warning Cat Checks

- A jump names a signpost that exists.
- A basket call names a basket that exists and brings the right number of gifts.
- A basket does not steal the name of an instruction.
- A name-tag is filled before a value stone tries to read it.
- Name-lookups hidden inside sentences are checked too.
- Questions are opened and both sides are inspected.
- Stones inside baskets receive the same inspection as stones in the main stack.

The warning cat starts with clean catalogs and an empty warning pile every time it receives a new stack.

---

## Picture

```
       stack of carved stones
                 |
                 v
       +-------------------+
       | build catalogs    |
       | signposts+baskets |
       +-------------------+
                 |
                 v
       +-------------------+
       | inspect every     |
       | stone and value   |
       +-------------------+
                 |
                 v
       pile of warning slips
       each with words + a clay tablet
```

---

## Trace

The warning cat sees a stone that fills the tag named health. Later it sees a sentence asking for the tag named player, but no earlier stone filled that tag. It adds one warning slip and ties the sentence's clay tablet to it.

Farther down, a basket call brings two gifts to a basket that expects one. The cat adds another slip. It keeps inspecting until the whole stack has been checked, then hands back the complete pile.

The doer cat has not moved a paw. These are warnings found by reading shapes and catalogs, not by running the script.

---

## One Line

The warning cat catalogs names, inspects every carved stone, and returns all detectable mistakes with their clay tablets.
