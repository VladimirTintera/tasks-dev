package eu.tintera.background.tasks

import kotlin.uuid.Uuid

interface TaskLifecycleObserver {
    fun onStarted(id: Uuid) {}
    fun onCompleted(id: Uuid, result: TaskResult<Any>) {}
    fun onCanceled(id: Uuid, reason: String? = null) {}


    fun onWaitingForPreconditions(id: Uuid) {}
    fun onPreconditionsSucceeded(id: Uuid) {}
    fun onPreconditionsFailed(id: Uuid) {}
}