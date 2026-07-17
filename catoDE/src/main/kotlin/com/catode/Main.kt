package com.catode

import androidx.compose.ui.window.singleWindowApplication
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Modifier

fun main() = singleWindowApplication(
    title = "catoDE",
) {
    MaterialTheme {
        var editorText by remember { mutableStateOf("meow \"hello\"") }
        var runResult by remember { mutableStateOf<RunResult?>(null) }
        Column(modifier = Modifier.fillMaxSize()) {
            Button(onClick = { runResult = run(editorText) }) {
                Text("Run")
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                EditorPane(text = editorText, onTextChange = { newText -> editorText = newText })
            }
            val currentResult = runResult
            if (currentResult != null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    OutputPane(currentResult)
                }
            }
        }
    }
}