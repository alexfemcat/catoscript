package com.catoscript.runtime

/**
 * The HTTP request currently being handled by a [WebHost]. Passed to the
 * basket registered for the matching route.
 *
 * @property method  the HTTP method (GET, POST, PUT, DELETE, …) in upper case
 * @property path    the request path, exactly as received (e.g. "/hello")
 * @property headers the request headers, lower-cased keys (e.g. "content-type")
 * @property body    the raw request body, decoded as a string. Empty for GET.
 */
data class Request(
    val method: String,
    val path: String,
    val headers: Map<String, String>,
    val body: String,
)