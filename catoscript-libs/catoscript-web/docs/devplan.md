# catoscript-web · dev plan

> **Scope.** This is the web module's own devplan. The root `catoscript-devplan.md` covers the **core** language and library (`com.catoscript.*` — parser, interpreter, stdlib, host SPI). Anything that is a host concern, a stdlib surface, or a JVM-side implementation that does work the language shouldn't do lives in a sibling module under `catoscript-libs/`, with its own devplan here.

**The convention going forward:** one Gradle subproject per concern, one `docs/devplan.md` per subproject, one `catoscript-<name>.version` line in `gradle.properties` per subproject. The root devplan stays scoped to core. New modules never bloat the root; they add a subdirectory under `catoscript-libs/` and a `docs/devplan.md` inside it.

---

## Status

| What                                                                                | State                       | Version       |
|-------------------------------------------------------------------------------------|-----------------------------|---------------|
| Empty module scaffold (build.gradle.kts, package, settings include, version pin)     | ✅ shipped (`0.1.0-LOCAL`)  | `0.1.0-LOCAL` |
| `WebHost : CatoHost` interface (in `catoscript-core`, this module's consumer)       | ✅ shipped                  | —             |
| Embedded HTTP server (JDK `com.sun.net.httpserver.HttpServer` for v0, ktor optional)| ⏳ pending                  | —             |
| `WebHost` impl: route registry, request-to-basket dispatcher, response capture      | ⏳ pending                  | —             |
| `samples/std/web/{route,start,respond,body}.cato` (dogfooded stdlib surface)        | ⏳ pending                  | —             |
| One worked sample: `samples/web/hello_api.cato` (the "done" check)                  | ⏳ pending                  | —             |
| `Interpreter` integration: serve() blocks, dispatches per-request with step budget  | ⏳ pending                  | —             |

**Current shipped version of this module:** `0.1.0-LOCAL` (the empty scaffold). First bump lands when the `WebHost` impl and one worked sample are both green.

---

## 1. Why this module

HTTP is a host concern. The language doesn't know about sockets or status codes; the host does. `CatoHost` (§2 of the root devplan) is the seam; this module is one of the things on the other side of it.

Why not put `WebHost` in core? Because core should stay small, pure, and free of HTTP-shaped dependencies. Core depends on nothing HTTP; `catoscript-web` depends on core. The dep direction is one-way, and the core's published jar stays free of ktor / `com.sun.net.httpserver` / anything else web-shaped.

Why a separate module instead of a separate repo? See `AGENTS.md` §3 — multi-module beats multi-repo for solo dev. Each module here can be `git mv`'d to its own repo later if it ever gets an audience. The escape hatch is mechanical, not a redesign.

---

## 2. The catoscript-facing surface (read-out-loud, fixed grammar)

No new keywords. The whole `std.web.*` namespace is function calls. From the design sketch:

```catoscript
std.web.route("GET",  "/hello",    greet)
std.web.route("POST", "/api/cats", add_cat)
std.web.start(8080)

basket greet $path
  meow "hello from $path"
  return
end_basket

basket add_cat $path
  set $cat std.web.json_body()
  std.web.respond(201, "ok")
  return
end_basket
```

Reads out loud: *route a GET to /hello using greet, start on 8080.* Every primitive is `std.web.*` — the grammar stays closed (root devplan §10). The bracket family and basket syntax are the only things the player learns, and they already learned those for Tier 5/6.

**Trade-off: blocking `std.web.start`.** Today the interpreter runs to completion in one tick. A web host flips that — the script *is* the long-running process. The new execution mode lives entirely in this module's serve loop; core's interpreter loop is unchanged.

---

## 3. The lib-side surface (dense Kotlin, not read-out-loud)

This is where the feature-creep lives. Convention: the lib code in this module does NOT have to read out loud. Naming can be concise, idiomatic, dense. The only constraint is that the catoscript-facing surface above is readable.

**Architecture (mirrors the root devplan §2 seam pattern):**

```
catoscript script
    │
    ├── std.web.route(method, path, basket_name)  ← parsed as Stmt.Call
    │     └── std_web_route basket (in samples/std/web/route.cato)
    │           └── web.register_route(...)        ← this module's host method
    │
    ├── std.web.start(port)
    │     └── web.serve(port)                      ← blocks; script is the server
    │
    └── (request arrives) → WebHost dispatcher
                              ├── match route → basket name
                              ├── bind $path = path, run interpreter(basketCall)
                              ├── capture meow output
                              └── web.respond(status, body)
```

**The `WebHost` interface belongs in core** (per the seam pattern — SPIs live in core, impls live in extensions). The first version declares:

```kotlin
interface WebHost : CatoHost {
    fun registerRoute(method: String, path: String, basketName: String)
    fun serve(port: Int)
    fun currentRequest(): Request
    fun respond(status: Int, body: String)
}
```

`Request` is a data class: `(method: String, path: String, headers: Map<String, String>, body: String)`. It comes through the interpreter as a value the basket can read via `std.web.request()` or by binding `$path` directly (the path-only fast path is the read-out-loud shape).

---

## 4. Phased work (in order)

Each phase = one commit, each commit ships green, each commit has a checkbox here.

### Phase A · Empty module (this commit) · ✅ shipped (`0.1.0-LOCAL`)

- [x] `catoscript-libs/catoscript-web/build.gradle.kts` mirrors `:catoDE`'s shape: kotlin/jvm, `project(":")` dep, JUnit 5, `maven-publish` to `mavenLocal()`
- [x] `catoscript-libs/catoscript-web/src/main/kotlin/com/catoscript/web/package-info.kt` — KDoc explaining what this module is
- [x] `settings.gradle.kts` includes `:catoscript-libs:catoscript-web`
- [x] `gradle.properties` pins `catoscript-web.version=0.1.0-LOCAL`
- [x] `./gradlew :catoscript-libs:catoscript-web:build` is green
- [x] `./gradlew :catoscript-libs:catoscript-web:publishToMavenLocal` publishes `com.catoscript:catoscript-web:0.1.0-LOCAL`
- [x] This devplan (the file you're reading)

### Phase B · `WebHost` interface (lives in core, declared here) · ✅ shipped

- [x] Add `WebHost : CatoHost` to `com.catoscript.runtime` in core
- [x] Add `Request` data class to `com.catoscript.runtime`
- [x] Document in `catoscript-reference.md` §8 (`CatoHost` SPI table) that web is now a known extension
- [x] No bump to this module's version (the change is in core)

### Phase C · `WebHost` impl + dispatcher · ⏳ pending

- [ ] `WebHostImpl` class in `com.catoscript.web` (dense Kotlin, no read-out-loud constraint)
- [ ] Embedded server choice: `com.sun.net.httpserver.HttpServer` for v0 (zero new dep), ktor optional later
- [ ] Route registry, exact-match path matching for v0, pattern matching later
- [ ] Per-request interpreter invocation: build a synthetic `Program` that calls the basket, capture meow output
- [ ] One JUnit 5 test: spin up the host on a random port, hit it with `HttpClient`, assert body
- [ ] Bump to `0.2.0-LOCAL`

### Phase D · `std.web.*` stdlib (dogfooded) · ⏳ pending

- [ ] `samples/std/web/route.cato` — registers a route via `web.register_route`
- [ ] `samples/std/web/start.cato` — calls `web.serve`
- [ ] `samples/std/web/respond.cato` — calls `web.respond`
- [ ] `samples/std/web/body.cato` — returns `web.current_request().body`
- [ ] Each file is the implementation AND the documentation
- [ ] Bump to `0.3.0-LOCAL` once all four land

### Phase E · Worked sample · ⏳ pending

- [ ] `samples/web/hello_api.cato` — the 5-line script from §2, end to end
- [ ] README §2.5 "Your first web API" walks through it (cross-link from root README)
- [ ] Bump to `0.4.0-LOCAL` (the "done" check for this module's v0)

---

## 5. Out of scope (deliberate, root devplan §10 applies)

- WebSockets — ktor-only, lands in a later bump if a real player script needs them
- Middleware / decorators / async handlers — the read-out-loud test fails; the player can `if`-chain in the basket if they need middleware
- Path patterns with `:id` syntax in v0 — exact-match only, see §6
- TLS / HTTPS — host concern, not language. v0 is plain HTTP. A future host impl can add it.
- Templates / view rendering — `meow` is the only output. If a player needs a templating system, they write it in a basket.
- Any new catoscript keyword — root devplan §10 forbids them. If the surface needs a new word, it goes through §5 / §10 / §11 / §13 there.

---

## 6. Open questions

- [ ] **Path patterns.** v0 does exact-match (`/hello` matches `/hello`, not `/hello/world`). When does `:id`-style routing become a player need? Lean: only when a real script hits the wall. §10 amendment process applies.
- [ ] **Request shape.** String-in/string-out (path body, body out via `meow`) vs map-in/map-out. v0 is string-in/string-out. §2 of the core devplan says "the host decides what env is" — web is the same pattern at a different scale.
- [ ] **Where does `cato web` (the CLI) live?** Three options: (1) a new `catoscript-web-cli` subproject under `catoscript-libs/`, (2) lift out of the root CLI into this module, (3) add a subcommand to the existing root `cato run` / `cato compile` via a `web.enable` feature flag. (3) is the smallest lift. (1) is the right answer if web grows beyond a single subcommand.
- [ ] **Module dependency direction.** v0 is one-way: `catoscript-web` depends on `catoscript-core`, never the other way. If web grows features that other modules want, lift them into core or into a shared `catoscript-web-common` subproject. Do NOT add `catoscript-core -> catoscript-web` cycles.
