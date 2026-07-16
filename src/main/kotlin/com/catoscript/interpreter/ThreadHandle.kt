package com.catoscript.interpreter

data class ThreadHandle(
    val id :String,
    val ip :String,
    val active: Boolean
)