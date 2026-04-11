package eu.tintera.tasks.core.preconditions

import kotlinx.coroutines.CancellationException

internal class PreconditionLostException: CancellationException("Required execution capability was lost during execution")