package eu.tintera.tasks

import co.touchlab.kermit.Logger
import kotlin.uuid.Uuid

class TasksObserver : TaskLifecycleObserver {
    private val logger = Logger.withTag("TasksObserver")
    override fun onStarted(id: Uuid) {
        logger.i { "Task $id started" }
    }

    override fun onCanceled(id: Uuid, reason: String?) {
        logger.i { "Task $id canceled: $reason" }
    }

    override fun onCompleted(id: Uuid, result: TaskResult<Any>) {
        logger.i { "Task $id completed success = $result" }
    }
}