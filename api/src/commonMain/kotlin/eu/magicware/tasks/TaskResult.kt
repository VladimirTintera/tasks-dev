package eu.tintera.tasks

sealed class TaskResult {
    data class Success(val outputData: Data) : TaskResult()
    data object Failure : TaskResult()
    data object Retry : TaskResult()

    companion object {
        fun success(outputData: Data = Data.EMPTY) = Success(outputData)
        fun retry() = Retry
        fun failure() = Failure
    }
}