package eu.tintera.background.tasks.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual val AppDispatchers.io: kotlinx.coroutines.CoroutineDispatcher get() = Dispatchers.IO