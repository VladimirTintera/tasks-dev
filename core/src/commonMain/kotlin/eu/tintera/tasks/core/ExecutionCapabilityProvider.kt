package eu.tintera.tasks.core

import kotlinx.coroutines.flow.Flow

enum class ExecutionCapability {
    SHORT_LIVED,
    HEAVY_PROCESSING
}

interface ExecutionCapabilityProvider {
    fun capabilities(): Flow<Set<ExecutionCapability>>
}