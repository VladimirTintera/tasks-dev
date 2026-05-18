package eu.tintera.tasks

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() {

    koinApp {
        modules(jvmModule)
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Tasks",
        ) {
            App()
        }
    }
}