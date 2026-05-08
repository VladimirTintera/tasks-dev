package eu.tintera.tasks

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import eu.tintera.tasks.koin.taskManagerBootstrapper
import org.koin.dsl.module

fun main() {

    koinApp {
        modules(
            module {
                single {
                    TaskManagerConfiguration("ComposeApp")
                }

                taskManagerBootstrapper {
                    koin.loadModules(listOf(logModule), createEagerInstances = true)
                }
            }
        )
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