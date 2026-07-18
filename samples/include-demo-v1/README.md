# include-demo

What `include` does today:

1. Loads `library.cato` at parse time, inlining its statements wrapped
   in a skip-guard (a `jump :__include_skip_N` at the top and a matching
   `:__include_skip_N` label at the bottom).
2. The library's top-level code does NOT fire on load. That's the
   skip-guard's job.
3. The library's labels are reachable from the including script via
   `jump :LABEL`. Clean call/return semantics (`jump :end` as the
   return opcode; see `catoscript-reference.md` §5.3) shipped with
   Phase B.6 in `0.3.1-LOCAL`. Library bodies can now declare params
   and return to their caller the same way any other label does.
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
