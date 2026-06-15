package eu.tintera.background.tasks.core

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

internal class TaskDispatcher(
    private val taskProcessor: TaskProcessor,
    private val repository: TaskDispatcherRepository,
    private val scope: ApplicationScope,
    private val dispatchers: AppDispatchers,
) {
    private val runningJobs = MutableStateFlow<Map<Uuid, Job>>(emptyMap())
    private val jobFinishedEvent = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_LATEST
    )

    private fun tasks() = combine(
        repository.dispatchableTasks(runningStates).distinctUntilChanged(),
        jobFinishedEvent.onStart { emit(Unit) },
    ) { tasks, _ -> tasks }

    init {
        scope.launch(dispatchers.io) {
            collectAndDispatchTasks()
        }
    }

    private suspend fun collectAndDispatchTasks() {
        tasks().collect { tasks ->

            val currentJobsMap = runningJobs.value

            val newTasks = tasks.filter { !currentJobsMap.containsKey(it.id) }

            if (newTasks.isNotEmpty()) {
                newTasks.forEach { task ->

                    val job = scope.launch(context = dispatchers.io, start = CoroutineStart.LAZY) {
                        taskProcessor.run(task.id)
                    }

                    runningJobs.update { it + (task.id to job) }

                    job.invokeOnCompletion {
                        runningJobs.update { it - task.id }
                        jobFinishedEvent.tryEmit(Unit)
                    }

                    job.start()
                }
            }
        }
    }

    companion object {
        private const val TAG = "TaskDispatcher"
    }
}
