package eu.tintera.background.tasks.core.constraints

import kotlinx.coroutines.CancellationException

internal class ConstraintLostException : CancellationException("Required execution capability was lost during execution")