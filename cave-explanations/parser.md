# The Parser Cat

> Cave-cat explanation of how the catoscript parser works. Pure analogy, no code tokens.

---

## The Setup

You have a piece of bark with claw-marks on it. The claw-marks spell out instructions. But the bark is one long scribble — words squished together, no spaces in the right places, numbers mixed with names.

You hand the bark to a cave-cat. The cave-cat's job: turn the scribble into a stack of clean little instruction-stones, one stone per claw-mark line. Each stone has a *shape* — like "this is a meow stone" or "this is a set stone" or "this is a signpost stone."

---

## How the Cave-Cat Does It

1. **Snap the bark into strips.** One strip per claw-mark line.

2. **Look at the first scratch on each strip.**
   - Strip is blank → drop it. (No stone for nothing.)
   - Strip starts with a hash-mark → it's a *thought stone*. Keep the words after the hash.
   - Strip starts with a colon → it's a *signpost stone*. The word after the colon is the signpost's name.
   - Anything else → read the first word. That word tells you what kind of stone to carve.

3. **For each stone kind, the cat has one rule.**
   - A print-mark → carve a yelling stone and put the following value inside.
   - A remember-mark → expect a name-tag, then a value after a gap.
   - A smell-mark → carve a smelling stone with the following question.
   - A conditional leap-mark → expect a signpost name.
   - A plain leap-mark → expect a signpost name and any gift values.
   - A basket opening → collect its name, empty name-tags, and all enclosed stones until the closing mark.
   - A named basket followed by a gift circle → carve a basket-call stone.
   - A return-mark → carve a return stone.
   - An include-mark → fetch another bark, carve its stones here, and fence them so the main walk skips them. Missing bark and circular fetching make the cat growl.

4. **If the bark doesn't match any rule, the cat growls.** That's the error stone. ("You wrote 'mew' but there's no such stone. Did you mean 'meow'?")

5. **At the end, hand over the stack of stones.** The cat can also press that whole stack into a tidy, shareable clay record. The interpreter later picks up stones one by one and does what each says.

---

## Picture

```
bark with claw-marks
        |
        v
  +-----------+
  |   snap    |   <-- source.lines()
  | into strips|
  +-----------+
        |
        v
  +-----------+
  |  read     |   <-- parseLine: blank / # / : / keyword
  |  first    |
  | scratch   |
  +-----------+
        |
        v
  +-----------+
  |  pick     |   <-- parseKeyword: meow / set / sniff / purr_at / ...
  |  the      |
  |  stone    |
  |  shape    |
  +-----------+
        |
        v
  +-----------+
  |  carve    |   <-- Stmt.Meow(...), Stmt.Set(...), etc.
  |  value    |
  |  inside   |
  +-----------+
        |
        v
   stack of stones = Program
```

---

## Trace with `samples/3.cato`

Bark has 8 strips. Cat walks down them:

1. `set $hp 5` → first scratch "set" → carve a set-stone, inside: name "hp", value 5.
2. `sniff $hp > 10` → first scratch "sniff" → carve a sniff-stone, inside: question (5 > 10).
3. `purr_at :DEAD` → carve a purr-stone, signpost "DEAD".
4. `meow "still alive"` → carve a meow-stone, value "still alive".
5. `jump :END` → carve a jump-stone, signpost "END".
6. `:DEAD` → signpost stone, name "DEAD".
7. `meow "game over"` → meow-stone, value "game over".
8. `:END` → signpost stone, name "END".

Hand the stack to the interpreter. The interpreter does what each stone says.

---

## One Line

Scribble in, stack of stones out.
