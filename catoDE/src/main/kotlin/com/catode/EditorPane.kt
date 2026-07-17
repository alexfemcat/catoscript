package com.catode

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping

@Composable
fun EditorPane(text: String, onTextChange: (String) -> Unit) {
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier.fillMaxSize().padding(12.dp),
        visualTransformation = VisualTransformation { original ->
            TransformedText(highlight(original.text), OffsetMapping.Identity)
        }
    )
}
