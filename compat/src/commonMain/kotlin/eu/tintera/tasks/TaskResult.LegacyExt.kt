package eu.tintera.tasks

fun TaskResult.Companion. success(outputData: Data = Data.EMPTY) = TaskResult.success(outputData)

typealias LegacyTaskResult = TaskResult<Data>