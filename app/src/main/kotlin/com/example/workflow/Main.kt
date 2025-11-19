package com.example.workflow

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.workflow.ui.App

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Kotlin DSL Workflow Engine") {
        App(this.window)
    }
}
