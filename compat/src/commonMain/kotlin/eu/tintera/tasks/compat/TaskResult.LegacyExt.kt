package eu.tintera.tasks.compat

import eu.tintera.tasks.TaskResult

fun TaskResult.Companion. success(outputData: Data = Data.EMPTY) = TaskResult.success(outputData)

typealias LegacyTaskResult = TaskResult<Data>