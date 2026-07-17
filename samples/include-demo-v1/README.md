# include-demo

What `include` does today:

1. Loads `library.cato` at parse time, inlining its statements wrapped
   in a skip-guard (a `jump :__include_skip_N` at the top and a matching
   `:__include_skip_N` label at the bottom).
2. The library's top-level code does NOT fire on load. That's the
   skip-guard's job.
3. The library's labels are reachable from the including script via
   `jump :LABEL`. Clean call/return semantics land with Tier 5
   (devplan §6 Phase B.6). Until then, library code is "you can jump
   to a label, but you have to handle your own return."
4. Errors from inside the included file say which file the error came
   from.

Run:

```powershell
cato run main.cato
```

Expected output:

```
main: include skipped library top-level, now main runs
```

If you see `lib: top-level should NOT print`, the skip-guard broke.

See `docs/include-plan.md` for the full design.
