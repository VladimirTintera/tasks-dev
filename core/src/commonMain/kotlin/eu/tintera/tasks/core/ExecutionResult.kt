package eu.tintera.tasks.core

import eu.tintera.tasks.TaskResult

internal sealed interface ExecutionResult {
    data class Finished(val result: TaskResult) : ExecutionResult
    object Yielded : ExecutionResult
}