package com.catoscript.interpreter


data class ScriptContext(
    val variables: MutableMap<String, Any>,
    val labels: Map<String, Int>,
    val paused: Boolean? = null
)