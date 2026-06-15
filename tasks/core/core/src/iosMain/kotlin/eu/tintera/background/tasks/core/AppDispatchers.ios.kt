package eu.tintera.background.tasks.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

internal actual fun platformIoDispatcher(): kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO