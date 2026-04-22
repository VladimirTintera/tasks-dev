package eu.tintera.tasks.core

import androidx.work.WorkInfo

internal fun WorkInfo.State.toState() = when (this) {
    WorkInfo.State.BLOCKED -> eu.tintera.tasks.State.Blocked
    WorkInfo.State.ENQUEUED -> eu.tintera.tasks.State.Enqueued
    WorkInfo.State.RUNNING -> eu.tintera.tasks.State.Running
    WorkInfo.State.SUCCEEDED -> eu.tintera.tasks.State.Succeeded
    WorkInfo.State.CANCELLED -> eu.tintera.tasks.State.Cancelled
    WorkInfo.State.FAILED -> eu.tintera.tasks.State.Failed
}