package eu.tintera.tasks.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface AppDispatchers {
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
}

expect val AppDispatchers.io : CoroutineDispatcher

internal class AppDispatchersImpl : AppDispatchers {
    override val main = Dispatchers.Main
    override val default = Dispatchers.Default
}