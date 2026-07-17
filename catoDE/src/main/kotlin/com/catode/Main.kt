package com.catode

import androidx.compose.ui.window.singleWindowApplication
import androidx.compose.material.MaterialTheme

fun main() = singleWindowApplication(
    title = "catoDE",
) {
    MaterialTheme {
        EditorPane()
    }
}