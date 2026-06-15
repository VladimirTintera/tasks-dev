package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.BackoffCriteria
import eu.tintera.background.tasks.State
import eu.tintera.background.tasks.core.data.Task
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
    processTime: Instant = Clock.System.now() + initialDelay,
    runAttemptCount: Int = 0,
    requiresDeviceIdle: Boolean = false,
    inputData: ByteArray? = ByteArray(0),
    outputData: ByteArray? = null,
    progressData: ByteArray? = null,
    version: Int = 1
): Task = Task(
    id = id,
    state = state,
    identifier = identifier,
    uniqueName = identifier,
    runAttemptCount = runAttemptCount,
    initialDelay = initialDelay,
    processTime = processTime,
    inputData = inputData,
    outputData = outputData,
    networkRequired = networkRequired,
    createdAt = Clock.System.now(),
    finishedAt = if (state.terminal()) Clock.System.now() else null,
    repeatInterval = null,
    backoffCriteria = defaultBackoffCriteria,
    progressData = progressData,
    retentionDelay = 24.hours,
    requiresDeviceIdle = requiresDeviceIdle,
    version = version
)