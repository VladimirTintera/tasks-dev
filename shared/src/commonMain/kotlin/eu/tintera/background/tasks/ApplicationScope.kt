package eu.tintera.background.tasks

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

class ApplicationScope(
    private val context: CoroutineContext
) : CoroutineScope by CoroutineScope(context)