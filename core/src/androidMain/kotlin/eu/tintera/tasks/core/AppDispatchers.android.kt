package eu.tintera.tasks.core

import kotlinx.coroutines.Dispatchers

actual val AppDispatchers.io: kotlinx.coroutines.CoroutineDispatcher get() = Dispatchers.IO