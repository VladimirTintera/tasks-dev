package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.TaskLifecycleObserver
import eu.tintera.background.tasks.TaskResult
import kotlin.uuid.Uuid

class CompositeTaskLifecycleObserver(
    private val observers: List<TaskLifecycleObserver>
) : TaskLifecycleObserver {
    override fun onCanceled(id: Uuid, reason: String?) {
        observers.forEach { observer -> observer.onCanceled(id, reason) }
    }

    override fun onWaitingForPreconditions(id: Uuid) {
        observers.forEach { observer -> observer.onWaitingForPreconditions(id) }
    }

    override fun onPreconditionsSucceeded(id: Uuid) {
        observers.forEach { observer -> observer.onPreconditionsSucceeded(id) }
    }

    override fun onPreconditionsFailed(id: Uuid) {
        observers.forEach { observer -> observer.onPreconditionsFailed(id) }
    }

    override fun onCompleted(id: Uuid, result: TaskResult<Any>) {
        observers.forEach { observer -> observer.onCompleted(id, result) }
    }

    override fun onStarted(id: Uuid) {
        observers.forEach { observer -> observer.onStarted(id) }
    }
}