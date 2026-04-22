package eu.tintera.tasks

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    koinApp {  }
    TasksInitializer.initialize(
        defaultJvmTasksManagerConfiguration("ComposeApp")
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "Tasks",
    ) {
        App()
    }
}