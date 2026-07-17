package com.catode

import androidx.compose.desktop.ui.dsl.singleWindowApplication
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable

fun main() = singleWindowApplication(
    title = "catoDE",
) {
    MaterialTheme {
        PlaceholderScreen()
    }
}

@Composable
private fun PlaceholderScreen() {
    Text("catoDE — Phase 0: build smoke test")
}
