package eu.tintera.tasks.core

import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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

    private fun tasks() = combine(
        repository.tasksByState(runningStates).distinctUntilChanged(),
        runningJobs
    ) { tasks, jobs -> Pair(tasks, jobs) }

    private fun Task.executionKey() = ExecutionKey(id)

    init {

        // Init blok necháváme čistý a logiku přesouváme do privátních funkcí
        scope.launch(dispatchers.io) {
            collectAndDispatchTasks()
        }
    }

    private suspend fun collectAndDispatchTasks() {
        tasks().collect { (tasks, currentJobsMap) ->

            val currentExecutionKeys = tasks.map { it.executionKey() }.toSet()

            // 2. Projdeme aktuální joby a zrušíme ty, co už nejsou validní
            currentJobsMap.forEach { (key, job) ->
                if (key !in currentExecutionKeys) {
                    job.cancel()
                    // Z mapy to zde MAZAT NEMUSÍME.
                    // job.cancel() vyvolá invokeOnCompletion, který si to smaže sám!
                }
            }

            // 3. Najdeme tasky, pro které ještě nemáme Job
            val newTasks = tasks.filter { !currentJobsMap.containsKey(it.executionKey()) }

            if (newTasks.isNotEmpty()) {
                newTasks.forEach { task ->
                    val key = task.executionKey()

                    // 1. Vytvoříme Job, ale zatím ho NEspustíme
                    val job = scope.launch(context = dispatchers.io, start = CoroutineStart.LAZY) {
                        taskProcessor.run(task)
                    }

                    // 2. Nejdřív ho bezpečně zaregistrujeme do map a trackerů
                    runningJobs.update { it + (key to job) }
                    activeTaskTracker.track(task.id)

                    // 3. Zaregistrujeme úklid
                    job.invokeOnCompletion {
                        runningJobs.update { it - key }
                        activeTaskTracker.untrack(task.id)
                    }

                    // 4. Až teď ho bezpečně odstartujeme!
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
