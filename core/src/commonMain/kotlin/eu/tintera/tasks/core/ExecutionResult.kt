package eu.tintera.tasks.core

sealed interface ExecutionResult {
    data class EvaluatorResult(
        val evaluatorResult: TaskEvaluatorResult<Any>,
    ) : ExecutionResult

    data object Yielded : ExecutionResult
    data object Canceled : ExecutionResult
}