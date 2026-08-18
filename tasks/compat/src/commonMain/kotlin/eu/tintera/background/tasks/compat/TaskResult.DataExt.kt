package eu.tintera.background.tasks.compat

import eu.tintera.background.tasks.TaskResult

/** Success with an untyped payload; mirrors `TaskResult.success()` for [Data]. */
fun TaskResult.Companion.success(outputData: Data = Data.EMPTY) = TaskResult.success(outputData)

/** The result a [DataTaskHandler] returns. */
typealias DataTaskResult = TaskResult<Data>
