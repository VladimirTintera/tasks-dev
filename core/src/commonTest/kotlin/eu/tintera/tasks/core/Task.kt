package eu.tintera.tasks.core

import eu.tintera.tasks.BackoffCriteria
import eu.tintera.tasks.Data
import eu.tintera.tasks.State
import eu.tintera.tasks.core.data.Task
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlin.uuid.Uuid

fun createTask(
    identifier: String,
    state: State = State.Enqueued,
    id: Uuid = Uuid.random(),
    networkRequired: Boolean = false,
    initialDelay: Duration = Duration.ZERO,
    processTime: Instant = Clock.System.now(),
    runAttemptCount: Int = 0,
    requiresDeviceIdle: Boolean = false
): Task = Task(
    id = id,
    state = state,
    identifier = identifier,
    uniqueName = identifier,
    runAttemptCount = runAttemptCount,
    initialDelay = initialDelay,
    processTime = processTime,
    inputData = Data.EMPTY,
    outputData = Data.EMPTY,
    networkRequired = networkRequired,
    createdAt = Clock.System.now(),
    finishedAt = if (state.terminal()) Clock.System.now() else null,
    repeatInterval = null,
    backoffCriteria = BackoffCriteria.DEFAULT,
    progressData = null,
    retentionDelay = 24.hours,
    requiresDeviceIdle = requiresDeviceIdle
)