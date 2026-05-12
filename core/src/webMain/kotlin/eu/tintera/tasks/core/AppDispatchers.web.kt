package eu.tintera.tasks.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val AppDispatchers.io: CoroutineDispatcher get() = Dispatchers.Default