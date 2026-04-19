package eu.tintera.tasks.legacy

import eu.tintera.tasks.TaskResult

fun TaskResult.Companion. success(outputData: Data = Data.EMPTY) = TaskResult.success(outputData)

typealias LegacyTaskResult = TaskResult<Data>