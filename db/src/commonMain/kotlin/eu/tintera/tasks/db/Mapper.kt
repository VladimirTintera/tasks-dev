package eu.tintera.tasks.db

import eu.tintera.tasks.BackoffCriteria
import eu.tintera.tasks.BackoffPolicy
import eu.tintera.tasks.State
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
    progressData = progressData?.takeIf { it.isNotEmpty() },
    retentionDelay = retentionDelay,
    requiresDeviceIdle = requiresDeviceIdle,
    version = version
)

fun StateDb.toTaskState() = when (this) {
    StateDb.Enqueued -> State.Enqueued
    StateDb.Running -> State.Running
    StateDb.Succeeded -> State.Succeeded
    StateDb.Failed -> State.Failed
    StateDb.Cancelled -> State.Cancelled
    StateDb.Blocked -> State.Blocked
}

fun State.toEntityState() = when (this) {
    State.Enqueued -> StateDb.Enqueued
    State.Failed -> StateDb.Failed
    State.Running -> StateDb.Running

    State.Succeeded -> StateDb.Succeeded
    State.Cancelled -> StateDb.Cancelled
    State.Blocked -> StateDb.Blocked
}

fun BackoffPolicyDb.toTaskBackoffPolicy() = when (this) {
    BackoffPolicyDb.Linear -> BackoffPolicy.Linear
    BackoffPolicyDb.Exponential -> BackoffPolicy.Exponential
}

fun BackoffCriteriaDb.toTaskBackoffCriteria() = BackoffCriteria(
    backoffPolicy = backoffPolicy.toTaskBackoffPolicy(),
    delay = delay
)

fun BackoffPolicy.toEntityBackoffPolicy() = when (this) {
    BackoffPolicy.Linear -> BackoffPolicyDb.Linear
    BackoffPolicy.Exponential -> BackoffPolicyDb.Exponential
}

fun BackoffCriteria.toEntityBackoffCriteria() = BackoffCriteriaDb(
    backoffPolicy = backoffPolicy.toEntityBackoffPolicy(),
    delay = delay
)