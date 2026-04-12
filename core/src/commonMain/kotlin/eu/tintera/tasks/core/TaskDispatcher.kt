package eu.tintera.tasks.core

import eu.tintera.tasks.EventBus
import eu.tintera.tasks.State
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class TaskDispatcher(
    private val taskProcessor: TaskProcessor,
    private val repository: Repository,
    private val scope: ApplicationScope,
    private val dispatchers: AppDispatchers,
    private val activeTaskTracker: ActiveTaskTracker,
) {

    // 1. Přesunuto na úroveň třídy jako thread-safe StateFlow
    private val runningJobs = MutableStateFlow<Map<ExecutionKey, Job>>(emptyMap())

    private fun tasks() = repository.tasksByState(runningStates).distinctUntilChanged()
    private fun Task.executionKey() = ExecutionKey(id, processTime)

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

                    val job = scope.launch(dispatchers.io) {
                        taskProcessor.run(task)
                    }

                    // 4. Atomické přidání do mapy
                    runningJobs.update { it + (key to job) }
                    activeTaskTracker.track(task.id)

                    // 5. Atomické smazání z mapy po dokončení/zrušení
                    // (Tohle je ta krása StateFlow - nepotřebuje suspend!)
                    job.invokeOnCompletion {
                        runningJobs.update { it - key }
                        activeTaskTracker.untrack(task.id)
                    }
                }
            }
        }
    }

    private suspend fun recoverStuckTasks() {
        // 6. TADY VYUŽIJEME NÁŠ SMART SWEEP
        // Vytáhneme si všechny IDs tasků, které reálně běží.
        val activelyRunningIds = runningJobs.value.keys.map { it.id }.toSet()

        EventBus.send(TAG, "Running Smart Sweep. Excluding ${activelyRunningIds.size} active tasks.")

        repository.resetState(
            from = State.Running,
            to = State.Enqueued,
            excludedIds = activelyRunningIds
        )
    }

    companion object {
        private const val TAG = "TaskDispatcher"
    }
}

internal data class ExecutionKey(
    val id: Uuid,
    val processTime: Instant
)
