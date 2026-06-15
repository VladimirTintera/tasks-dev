package eu.tintera.background.tasks.core.fakes

import eu.tintera.background.tasks.core.TaskProcessor
import kotlinx.coroutines.awaitCancellation
import kotlin.uuid.Uuid

internal class FakeTaskProcessor : TaskProcessor {
    val currentlyRunningIds = mutableSetOf<Uuid>()

    override suspend fun run(id: Uuid) {
        currentlyRunningIds.add(id)
        try {
            awaitCancellation()
        } finally {
            currentlyRunningIds.remove(id)
        }
    }
}