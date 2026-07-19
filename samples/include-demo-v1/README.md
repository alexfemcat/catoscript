# include-demo

What `include` does today:

1. Loads `library.cato` at parse time, inlining its statements wrapped
   in a skip-guard (a `jump :__include_skip_N` at the top and a matching
   `:__include_skip_N` label at the bottom).
2. The library's top-level code does NOT fire on load. That's the
   skip-guard's job.
3. The library's labels are reachable from the including script via
   `jump :LABEL`. Library bodies can declare baskets and return to
   their caller via `return` (Phase B.8 in `0.3.2-LOCAL`). See
   `catoscript-reference.md` §5 for the current call/return shape.
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
