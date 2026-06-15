package eu.tintera.background.tasks.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface AppDispatchers {
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
    val io: CoroutineDispatcher
}

internal expect fun platformIoDispatcher(): CoroutineDispatcher

internal class AppDispatchersImpl : AppDispatchers {
    override val main = Dispatchers.Main
    override val default = Dispatchers.Default
    override val io = platformIoDispatcher()
}