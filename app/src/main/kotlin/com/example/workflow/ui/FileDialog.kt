package com.example.workflow.ui

import androidx.compose.ui.awt.ComposeWindow
import java.awt.FileDialog
import java.io.File

fun openFileDialog(window: ComposeWindow): File? {
    val dialog = FileDialog(window, "Select Workflow File", FileDialog.LOAD)
    dialog.file = "*.workflow.kts"
    dialog.isVisible = true
    val file = dialog.file
    val directory = dialog.directory
    return if (file != null && directory != null) {
        File(directory, file)
    } else {
        null
    }
}
