package eu.tintera.tasks

sealed interface TaskResult<out Output> {
    data class Success<Output>(val outputData: Output) : TaskResult<Output>
    data object Failure : TaskResult<Nothing>
    data object Retry : TaskResult<Nothing>

    companion object {
        fun <T> success(outputData: T) = Success(outputData)

        fun retry() = Retry
        fun failure() = Failure
    }
}
