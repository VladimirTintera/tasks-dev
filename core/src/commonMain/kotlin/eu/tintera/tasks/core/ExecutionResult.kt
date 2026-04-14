package eu.tintera.tasks.core

import eu.tintera.tasks.TaskResult

sealed interface ExecutionResult {
    data class Finished(val result: TaskResult) : ExecutionResult
    data object Yielded : ExecutionResult
}