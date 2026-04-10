package eu.tintera.tasks.core

import kotlinx.coroutines.CancellationException

internal class CapabilityLostException: CancellationException("Required execution capability was lost during execution")