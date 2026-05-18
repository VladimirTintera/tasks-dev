package eu.tintera.tasks

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@OptIn(ExperimentalComposeUiApi::class)
fun main() {

    val app = koinApp {
        printLogger()
        modules(webModule)
    }

    app.createEagerInstances()

    ComposeViewport {
        App()
    }
}