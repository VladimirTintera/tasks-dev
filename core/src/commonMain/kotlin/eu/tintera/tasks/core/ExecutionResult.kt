package eu.tintera.tasks.core

import eu.tintera.tasks.TaskResult

sealed interface ExecutionResult {
    data class Finished(val result: TaskResult<ByteArray>) : ExecutionResult
    data object Yielded : ExecutionResult
    data object Canceled: ExecutionResult
}