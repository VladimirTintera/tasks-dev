package eu.tintera.tasks.core.fakes

import eu.tintera.tasks.core.ExecutionKey
import eu.tintera.tasks.core.TaskProcessor
import eu.tintera.tasks.core.data.Task
import kotlinx.coroutines.awaitCancellation

internal class FakeTaskProcessor : TaskProcessor{
    // Ukládáme si klíče právě běžících tasků
    val currentlyRunningKeys = mutableSetOf<ExecutionKey>()

    override suspend fun run(task: Task) {
        val key = ExecutionKey(task.id, task.processTime)
        currentlyRunningKeys.add(key)
        try {
            // awaitCancellation() simuluje task, který běží "donekonečna",
            // dokud ho někdo zvenku nezruší (což je přesně to, co chceme testovat)
            awaitCancellation()
        } finally {
            // Až Dispatcher zavolá job.cancel(), blok finally se vykoná
            currentlyRunningKeys.remove(key)
        }
    }
}