/**
 * catoscript-web: HTTP host and `std.web.*` namespace for catoscript.
 *
 * This module ships a [com.catoscript.runtime.CatoHost] implementation that
 * registers HTTP routes, dispatches incoming requests to user-defined baskets,
 * and serves the basket's `meow` output as the response body. The catoscript-
 * facing surface (`std.web.route`, `std.web.start`, `std.web.respond`,
 * `std.web.body`) lives in `samples/std/web/` of the root project, dogfooded
 * per the devplan §9 stdlib rule.
 *
 * The actual host interface and the SPI live in `catoscript-core` (the root
 * project's `com.catoscript.runtime` package). This module is the impl.
 *
 * See `catoscript-web/docs/devplan.md` for the module-level design and
 * roadmap. The root `catoscript-devplan.md` covers the language and core
 * library only.
 */
package com.catoscript.web
