package eu.tintera.tasks.android

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import eu.tintera.tasks.ExistingTaskPolicy
import eu.tintera.tasks.State

internal fun ExistingTaskPolicy.toWorkPolicy() = when (this) {
    ExistingTaskPolicy.Keep -> ExistingWorkPolicy.KEEP
    ExistingTaskPolicy.Append -> ExistingWorkPolicy.APPEND_OR_REPLACE
    ExistingTaskPolicy.Replace -> ExistingWorkPolicy.REPLACE
}

internal fun WorkInfo.State.toState() = when (this) {
    WorkInfo.State.BLOCKED -> eu.tintera.tasks.State.Blocked
    WorkInfo.State.ENQUEUED -> eu.tintera.tasks.State.Enqueued
    WorkInfo.State.RUNNING -> eu.tintera.tasks.State.Running
    WorkInfo.State.SUCCEEDED -> eu.tintera.tasks.State.Succeeded
    WorkInfo.State.CANCELLED -> eu.tintera.tasks.State.Cancelled
    WorkInfo.State.FAILED -> State.Failed
}