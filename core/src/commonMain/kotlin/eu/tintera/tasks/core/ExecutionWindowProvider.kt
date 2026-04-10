package eu.tintera.tasks.core

import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.flow.Flow

enum class ExecutionWindo {
    SHORT,
    LONG
}

interface ExecutionWindowProvider {
    fun capabilities(): Flow<Set<ExecutionWindo>>
}


interface TaskPrecondition {
    fun isValid(task: Task) : Flow<Boolean>
}

