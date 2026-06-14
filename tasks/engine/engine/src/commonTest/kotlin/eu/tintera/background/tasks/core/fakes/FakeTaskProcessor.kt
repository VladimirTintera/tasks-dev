package eu.tintera.background.tasks.core.fakes

import eu.tintera.background.tasks.core.ExecutionKey
import eu.tintera.background.tasks.core.TaskProcessor
import eu.tintera.background.tasks.core.data.Task
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