package eu.tintera.tasks

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() {

    val app = koinApp()
    TasksInitializer.initialize(
        JvmTasksManagerConfiguration("ComposeApp")
    )
    app.koin.get<TokenObserver>().start()


    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Tasks",
        ) {
            App()
        }
    }
}