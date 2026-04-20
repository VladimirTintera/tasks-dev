package eu.tintera.tasks.core

import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

internal class TaskDispatcher(
    private val taskProcessor: TaskProcessor,
    private val repository: Repository,
    private val scope: ApplicationScope,
    private val dispatchers: AppDispatchers,
    private val activeTaskTracker: ActiveTaskTracker,
) {
    private val runningJobs = MutableStateFlow<Map<ExecutionKey, Job>>(emptyMap())
    private val jobFinishedEvent = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_LATEST
    )

    private fun tasks() = combine(
        repository.tasksByState(runningStates).distinctUntilChanged(),
        jobFinishedEvent.onStart { emit(Unit) },
    ) { tasks, _ -> tasks }

    private fun Task.executionKey() = ExecutionKey(id)

    init {

        // Init blok necháváme čistý a logiku přesouváme do privátních funkcí
        scope.launch(dispatchers.io) {
            collectAndDispatchTasks()
        }
    }

    private suspend fun collectAndDispatchTasks() {
        tasks().collect { tasks ->

            val currentExecutionKeys = tasks.map { it.executionKey() }.toSet()

            val currentJobsMap = runningJobs.value

            currentJobsMap.forEach { (key, job) ->
                if (key !in currentExecutionKeys) {
                    job.cancel()
                }
            }

            // 3. Najdeme tasky, pro které ještě nemáme Job
            val newTasks = tasks.filter { !currentJobsMap.containsKey(it.executionKey()) }

            if (newTasks.isNotEmpty()) {
                newTasks.forEach { task ->
                    val key = task.executionKey()

                    val job = scope.launch(context = dispatchers.io, start = CoroutineStart.LAZY) {
                        taskProcessor.run(task)
                    }

                    runningJobs.update { it + (key to job) }
                    activeTaskTracker.track(task.id)

                    job.invokeOnCompletion {
                        runningJobs.update { it - key }
                        activeTaskTracker.untrack(task.id)
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

internal data class ExecutionKey(
    val id: Uuid,
)
