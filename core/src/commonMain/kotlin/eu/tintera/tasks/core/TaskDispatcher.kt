package eu.tintera.tasks.core

import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class TaskDispatcher(
    private val taskProcessor: TaskProcessor,
    private val repository: Repository,
    private val scope: ApplicationScope,
    dispatchers: AppDispatchers
) {
    private fun tasks() = repository.tasksByState(runningStates).distinctUntilChanged()

    private fun Task.executionKey() = ExecutionKey(id, processTime)

    init {
        scope.launch(dispatchers.io) {

            val runningJobs = mutableMapOf<ExecutionKey, Job>()

            tasks().distinctUntilChanged().collect { tasks ->

                val currentExecutionKeys = tasks.map { it.executionKey() }.toSet()

                val iterator = runningJobs.iterator()
                while (iterator.hasNext()) {
                    val (key, job) = iterator.next()
                    if (key !in currentExecutionKeys) {
                        job.cancel()
                        iterator.remove()
                    }
                }

                val newTasks = tasks.filter { !runningJobs.containsKey(it.executionKey()) }

                if (newTasks.isNotEmpty()) {
                    newTasks.forEach { task ->
                        val job = scope.launch(dispatchers.io) {
                            taskProcessor.run(task)
                        }
                        runningJobs[task.executionKey()] = job
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "TaskDispatcher"
    }
}

internal data class ExecutionKey(
    val id: Uuid,
    val processTime: Instant
)