package com.catode

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.catoscript.interpreter.InterpreterResult

@Composable
fun OutputPane(runResult: RunResult) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        for (outputLine in runResult.lines) {
            Text(outputLine)
        }
        when (val outcome = runResult.result) {
            is InterpreterResult.Completed -> Text("✓ done")
            is InterpreterResult.BudgetExceeded -> Text("budget exceeded at step ${outcome.stepsConsumed}")
            is InterpreterResult.RuntimeError -> Text("error ${outcome.message}")
            null -> {

                for (squiggle in runResult.errors) {
                    Text("parse error (line ${squiggle.line}): ${squiggle.message}")
                }
            }
        }
    }
}
