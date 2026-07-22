package com.catoscript.runtime

/**
 * Web host SPI extension. Hosts that handle HTTP requests implement this
 * on top of [CatoHost]. The web module ships the reference implementation.
 *
 * The web host's job:
 *  - registerRoute() collects routes during script execution
 *  - serve() blocks, dispatches incoming requests to the matching basket
 *  - currentRequest() exposes the in-flight request to the basket
 *  - respond() sends the basket's output back as the HTTP response
 */
interface WebHost : CatoHost {
    fun registerRoute(method: String, path: String, basketName: String)
    fun serve(port: Int)
    fun currentRequest(): Request
    fun respond(status: Int, body: String)
}
