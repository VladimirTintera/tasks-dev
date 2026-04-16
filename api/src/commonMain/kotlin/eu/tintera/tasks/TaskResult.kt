package eu.tintera.tasks

sealed class TaskResult<out Output> {
    data class Success<Output>(val outputData: Output) : TaskResult<Output>()
    data object Failure : TaskResult<Nothing>()
    data object Retry : TaskResult<Nothing>()

    companion object {
        fun <T> success(outputData: T) = Success(outputData)
        fun success(outputData: Data = Data.EMPTY) = success<Data>(outputData)
        fun retry() = Retry
        fun failure() = Failure
    }
}

typealias LegacyTaskResult = TaskResult<Data>