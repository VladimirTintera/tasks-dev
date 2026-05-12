package eu.tintera.tasks

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import eu.tintera.tasks.koin.taskManagerBootstrapper
import org.koin.dsl.module

@OptIn(ExperimentalComposeUiApi::class)
fun main() {

    val app = koinApp {
        printLogger()
        modules(
            module {

                single {
                    TaskManagerConfiguration()
                }

                taskManagerBootstrapper {
                    println("Bootstrapped")
                    koin.loadModules(listOf(logModule), createEagerInstances = true)
                }
            }
        )
    }

    app.createEagerInstances()

    ComposeViewport {
        App()
    }
}