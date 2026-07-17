package com.catode

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EditorPane() {
    var editorText by remember { mutableStateOf("meow \"hello\"") }
    BasicTextField(
        value = editorText,
        onValueChange = { newText -> editorText = newText },
        modifier = Modifier.fillMaxSize().padding(12.dp)
    )
}