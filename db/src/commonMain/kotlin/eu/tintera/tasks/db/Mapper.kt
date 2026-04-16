package eu.tintera.tasks.db

import eu.tintera.tasks.core.data.Task
import eu.tintera.tasks.db.entities.TaskEntity

internal fun TaskEntity.toTask() = Task(
    id = id,
    identifier = identifier,
    uniqueName = uniqueName,
    runAttemptCount = runAttemptCount,
    initialDelay = initialDelay,
    processTime = processTime,
    state = state.toTaskState(),
    inputData = inputData,
    outputData = outputData,
    networkRequired = networkRequired,
    createdAt = createdAt,
    finishedAt = finishedAt,
    repeatInterval = repeatInterval,
    backoffCriteria = backoffCriteria?.toTaskBackoffCriteria(),
    progressData = progressData,
    retentionDelay = retentionDelay,
    requiresDeviceIdle = requiresDeviceIdle,
    version = version
)

internal fun Task.toTaskEntity() = TaskEntity(
    id = id,
    identifier = identifier,
    uniqueName = uniqueName,
    runAttemptCount = runAttemptCount,
    initialDelay = initialDelay,
    processTime = processTime,
    state = state.toEntityState(),
    inputData = inputData,
    outputData = outputData,
    networkRequired = networkRequired,
    createdAt = createdAt,
    finishedAt = finishedAt,
    repeatInterval = repeatInterval,
    backoffCriteria = backoffCriteria?.toEntityBackoffCriteria(),
    progressData = progressData,
    retentionDelay = retentionDelay,
    requiresDeviceIdle = requiresDeviceIdle,
    version = version
)

internal fun State.toTaskState() = when (this) {
    State.Enqueued -> eu.tintera.tasks.State.Enqueued
    State.Running -> eu.tintera.tasks.State.Running
    State.Succeeded -> eu.tintera.tasks.State.Succeeded
    State.Failed -> eu.tintera.tasks.State.Failed
    State.Cancelled -> eu.tintera.tasks.State.Cancelled
    State.Blocked -> eu.tintera.tasks.State.Blocked
}

internal fun eu.tintera.tasks.State.toEntityState() = when (this) {
    eu.tintera.tasks.State.Enqueued -> State.Enqueued
    eu.tintera.tasks.State.Failed -> State.Failed
    eu.tintera.tasks.State.Running -> State.Running

    eu.tintera.tasks.State.Succeeded -> State.Succeeded
    eu.tintera.tasks.State.Cancelled -> State.Cancelled
    eu.tintera.tasks.State.Blocked -> State.Blocked
}

internal fun BackoffPolicy.toTaskBackoffPolicy() = when (this) {
    BackoffPolicy.Linear -> eu.tintera.tasks.BackoffPolicy.Linear
    BackoffPolicy.Exponential -> eu.tintera.tasks.BackoffPolicy.Exponential
}

internal fun BackoffCriteria.toTaskBackoffCriteria() = eu.tintera.tasks.BackoffCriteria(
    backoffPolicy = backoffPolicy.toTaskBackoffPolicy(),
    delay = delay
)

internal fun eu.tintera.tasks.BackoffPolicy.toEntityBackoffPolicy() = when (this) {
    eu.tintera.tasks.BackoffPolicy.Linear -> BackoffPolicy.Linear
    eu.tintera.tasks.BackoffPolicy.Exponential -> BackoffPolicy.Exponential
}

internal fun eu.tintera.tasks.BackoffCriteria.toEntityBackoffCriteria() = BackoffCriteria(
    backoffPolicy = backoffPolicy.toEntityBackoffPolicy(),
    delay = delay
)