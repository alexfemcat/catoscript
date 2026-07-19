# Local install: bumping and testing catoscript

How to refresh your local Maven cache + the `cato` launcher so you can run
the latest engine against `.cato` scripts. Applies whenever you cut a new
`catoscript.version` (every phase ship is its own bump — see
`catoscript-devplan.md` §8).

## Where the version lives

Single source of truth: **`gradle.properties`** → `catoscript.version=<X.Y.Z-LOCAL>`.

`build.gradle.kts:18` reads it via `providers.gradleProperty(...)` and feeds it
to both the shadow jar (`./gradlew shadowJar`) and the `mavenJava` publication.
Nothing else in the build reads it directly — every other place is a manual
mirror that must be kept in sync.

## Files that mirror the version (bump ALL of these)

| File | What to change |
|---|---|
| `gradle.properties` | `catoscript.version=` — the pin itself |
| `cato.bat` | line 18: hardcoded `%USERPROFILE%\.m2\repository\com\catoscript\catoscript\<VER>\catoscript-<VER>.jar` |
| `cato.sh` | line 18: `VERSION="<VER>"` (the jar path is built from it on line 20) |
| `README.md` | install-path examples (Windows + Unix) and the `java -jar catoscript-<VER>.jar` line |
| `catoscript-reference.md` | append a row to the release history table (~line 1114) |
| `catoscript-devplan.md` | flip the matching `Bump to <VER>-LOCAL, publish` task from `[ ]` to `[x]` |

> **Known gotcha.** `cato.sh` has been observed stale (still pinned to the
> previous version while `gradle.properties` had moved on). Always diff the
> three pinned files together when bumping. A future cleanup is to have
> `cato.bat` / `cato.sh` parse `gradle.properties` directly — for now it's
> manual.

## Bump sequence (every phase ship)

1. **Pick the version.** Phase work → bump (per `catoscript-devplan.md` §8
   rules). Edit `gradle.properties` first.
2. **Mirror to the launchers.**
   - `cato.bat:18` — replace both occurrences of the version in the `JAR=`
     path string.
   - `cato.sh:18` — replace the `VERSION=` value.
3. **Mirror to the docs** (`README.md`, `catoscript-reference.md` release
   table, `catoscript-devplan.md` checkbox).
4. **Publish to your local Maven cache:**
   ```bash
   ./gradlew publishToMavenLocal
   ```
   This runs `shadowJar` first, then drops
   `com/catoscript/catoscript/<VER>/catoscript-<VER>.jar` into
   `~/.m2/repository/`. Old versions stay on disk (harmless — they're
   shadowed by the new coordinate).
5. **Sanity-check:** the error message in `cato.bat` / `cato.sh` will tell
   you if the launcher can't find the new jar (path mismatch means you
   skipped step 2).

## Run a script

After step 4, launch from the repo root or anywhere on `PATH`:

```bat
cato run samples\hello.cato
```

```bash
cato run samples/hello.cato
```

(`cato.bat` insists on the `run` keyword; `cato.sh` takes the file
directly. Both invoke `java -cp <jar> com.catoscript.cli.RunScriptKt <file>`.)

## When `./gradlew publishToMavenLocal` is the wrong tool

- **Producing an installable distribution** (zip + `bin/cato` + `bin/cato.bat`
  launcher scripts) — use `./gradlew installShadowDist` →
  `build/install/catoscript-shadow/`. What `README.md` §"Install" describes.
- **Producing just the fat jar** — `./gradlew shadowJar` →
  `build/libs/catoscript-<VER>.jar`. Runnable with
  `java -jar build/libs/catoscript-<VER>.jar <file.cato>`.
- **Publishing to a remote repo** — not wired up. `build.gradle.kts`
  declares only `mavenLocal()`; add a repository block before pushing to a
  shared host.

## Troubleshooting

| Symptom | Cause |
|---|---|
| `cato: artifact not found at ...catoscript-<OLD>.jar` | Launcher still pinned to the previous version — re-check `cato.bat:18` / `cato.sh:18`. |
| `cato: artifact not found at ...catoscript-<NEW>.jar` after `publishToMavenLocal` | Build cache from a prior version — run `./gradlew clean publishToMavenLocal`. |
| `ClassNotFoundException` on `kotlinx.serialization.*` | First-time publish on a fresh machine — `publishToMavenLocal` only puts the *project* artifact in `~/.m2`; the Kotlin stdlib + serialization jars come from the gradle build's resolved dependencies, which the launcher discovers by scanning `~/.m2/repository/org/jetbrains/...`. Run any gradle task once (`./gradlew test`) before the first `publishToMavenLocal` so the deps are resolved into the cache. |
| Launcher works but interpreter rejects syntax that should parse | Engine is older than the script — engine and devplan are out of sync. Re-run `publishToMavenLocal`. |
