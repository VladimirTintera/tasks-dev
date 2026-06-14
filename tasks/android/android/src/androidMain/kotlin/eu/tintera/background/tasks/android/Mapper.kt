package eu.tintera.background.tasks.android

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import eu.tintera.background.tasks.ExistingTaskPolicy
import eu.tintera.background.tasks.State

internal fun ExistingTaskPolicy.toWorkPolicy() = when (this) {
    ExistingTaskPolicy.Keep -> ExistingWorkPolicy.KEEP
    ExistingTaskPolicy.Append -> ExistingWorkPolicy.APPEND_OR_REPLACE
    ExistingTaskPolicy.Replace -> ExistingWorkPolicy.REPLACE
}

internal fun WorkInfo.State.toState() = when (this) {
    WorkInfo.State.BLOCKED -> State.Blocked
    WorkInfo.State.ENQUEUED -> State.Enqueued
    WorkInfo.State.RUNNING -> State.Running
    WorkInfo.State.SUCCEEDED -> State.Succeeded
    WorkInfo.State.CANCELLED -> State.Cancelled
    WorkInfo.State.FAILED -> State.Failed
}

internal fun State.toWorkState() = when (this) {
    State.Enqueued -> WorkInfo.State.ENQUEUED
    State.Blocked -> WorkInfo.State.BLOCKED
    State.Running -> WorkInfo.State.RUNNING
    State.Cancelled -> WorkInfo.State.CANCELLED
    State.Succeeded -> WorkInfo.State.SUCCEEDED
    State.Failed -> WorkInfo.State.FAILED
}