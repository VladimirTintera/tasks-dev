package eu.tintera.tasks.android

import androidx.work.WorkInfo
import eu.tintera.tasks.State

internal fun WorkInfo.State.toState() = when (this) {
    WorkInfo.State.BLOCKED -> State.Blocked
    WorkInfo.State.ENQUEUED -> State.Enqueued
    WorkInfo.State.RUNNING -> State.Running
    WorkInfo.State.SUCCEEDED -> State.Succeeded
    WorkInfo.State.CANCELLED -> State.Cancelled
    WorkInfo.State.FAILED -> State.Failed
}