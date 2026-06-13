package eu.tintera.background.tasks.compat

import eu.tintera.background.tasks.TaskResult

fun TaskResult.Companion. success(outputData: Data = Data.EMPTY) = TaskResult.success(outputData)

typealias LegacyTaskResult = TaskResult<Data>