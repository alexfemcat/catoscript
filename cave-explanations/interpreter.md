# The Doer Cat

> Cave-cat explanation of how the catoscript interpreter works. Pure analogy, no code tokens.

---

## The Setup

You have a stack of stones (from the parser cave-cat). Now you hand the stack to a *second* cave-cat — the doer-cat. The doer-cat's job: pick up stones one by one and do what each stone says.

---

## The Doer-Cat's Pouch

The doer-cat has four things in its pouch:

1. **A finger pointing at the current stone.** Starts pointing at the top of the stack. After each stone, the finger moves down by one — unless a stone says "jump to the signpost named X," then the finger leaps straight there.

2. **A pile of name-tags.** Each name-tag has a name scratched on it and a thing tied to it. "hp" → 5. "name" → "mochi". When a set-stone says "put 5 on the tag named hp," the cat ties 5 to that tag.

3. **A nose-memory.** The last yes/no smell the cat smelled. Purr-stones and hiss-stones check the nose-memory before deciding whether to jump.

4. **A stone counter.** Every stone the cat picks up, the counter goes up by one. If the counter gets too big, the cat gets tired and stops.

---

## What the Doer-Cat Does on Each Stone

- **Blank stone, thought stone, signpost stone** → ignore, move finger down.
- **Meow stone** → look at the thing inside, turn it into a sentence, *yell it out loud*. Move finger down.
- **Set stone** → look at the value, tie it to the name-tag. Move finger down.
- **Sniff stone** → smell the question inside, write yes or no into the nose-memory. Move finger down.
- **Purr stone** → check nose-memory. If yes, *leap* the finger to the signpost. If no, move finger down.
- **Hiss stone** → same as purr, but flip the answer (jump if no, move down if yes).
- **Jump stone** → always leap the finger to the signpost; growl if that name belongs to a basket.
- **Basket stone** → leave its stored stones alone and move finger down.
- **Call stone** → find the basket, check the gift count and nesting limit, save the current place and name-tags, fill the basket's tags, and walk its stones. Restore the saved pouch on return.
- **Return stone** → restore the most recently saved place and name-tags; growl if there is no active basket call.

If the cat leaps to a signpost that doesn't exist, the cat growls. If a purr stone comes without a prior sniff, the cat growls.

When the finger falls off the bottom of the stack, the cat is done.

---

## Picture

```
   stack of stones (top to bottom)
        |
        v
   +-----------+
   |  finger   |  <-- ip, points at current stone
   +-----------+
        |
        v
   +-----------+
   |  read     |  <-- when(stmt)
   |  stone    |
   |  shape    |
   +-----------+
        |
        +---> meow     -> yell, finger++
        +---> set      -> tie value to name-tag, finger++
        +---> sniff    -> smell, write to nose-memory, finger++
        +---> purr_at  -> if nose-memory yes, finger = signpost;
        |              else finger++
        +---> hiss_at  -> if nose-memory no, finger = signpost;
        |              else finger++
        +---> jump     -> finger = signpost
        +---> label/blank/thought -> finger++
        |
        v
   +-----------+
   |  counter  |  <-- stepsConsumed++; if too big, cat stops
   +-----------+
        |
        v
   +-----------+
   |  nose-    |  <-- lastSniff (read by purr_at, hiss_at)
   |  memory   |
   +-----------+
        |
        v
   +-----------+
   |  name-tag |  <-- MutableMap<String, Value>
   |  pile     |
   +-----------+
```

---

## Trace with `samples/3.cato`

Stack: [set $hp 5, sniff $hp > 10, purr_at :DEAD, meow "still alive", jump :END, :DEAD, meow "game over", :END]

1. Finger at stone 0: set. Tie "5" to tag "hp". Finger → 1.
2. Stone 1: sniff. Smell "is 5 > 10?" → no. Nose-memory = no. Finger → 2.
3. Stone 2: purr :DEAD. Nose-memory is no → don't jump. Finger → 3.
4. Stone 3: meow "still alive". Yell "still alive". Finger → 4.
5. Stone 4: jump :END. Finger leaps to signpost END.
6. Stone 7 (:END is at index 7). Signpost, ignore. Finger → 8.
7. Finger falls off bottom. Done.

Output: "still alive". Counter: 6 stones picked up.

---

## Bonus: Two Cave-Cats in a Row

```
bark with claw-marks
        |
        v
   PARSER CAT         <-- scribble in, stack of stones out
        |
        v
   stack of stones
        |
        v
   DOER CAT           <-- stones in, yelps + side-effects out
        |
        v
   output + side-effects
```

Parser cat doesn't *do* anything. Doer cat doesn't *read* the bark. They hand off a clean stack between them. Two cats, one job each.
