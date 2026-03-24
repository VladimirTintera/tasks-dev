package eu.tintera.tasks.core

import eu.tintera.tasks.core.data.Repository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.uuid.Uuid

internal class TaskDispatcher(
    private val taskProcessor: TaskProcessor,
    private val repository: Repository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun tasks() = repository.tasksByState(runningStates).distinctUntilChanged()

    init {
        scope.launch {

            val runningJobs = mutableMapOf<Uuid, Job>()

            tasks().distinctUntilChanged().collect { tasks ->

                val currentTaskIds = tasks.map { it.id }.toSet()

                // 1. Úklid: Odstraň z mapy joby tasků, které už nejsou v seznamu z DB
                val iterator = runningJobs.iterator()
                while (iterator.hasNext()) {
                    val (id, job) = iterator.next()
                    if (id !in currentTaskIds) {
                        iterator.remove()
                    }
                }

                // 2. Spouštění: Spusť jen ty, které nemáme v mapě
                val newTasks = tasks.filter { !runningJobs.containsKey(it.id) }

                if (newTasks.isNotEmpty()) {

                    newTasks.forEach { task ->
                        val job = scope.launch {
                            taskProcessor.run(task)
                        }
                        runningJobs[task.id] = job
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "TaskDispatcher"
    }
}