package eu.tintera.background.tasks.core

import kotlinx.coroutines.Dispatchers

internal actual fun platformIoDispatcher(): kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO