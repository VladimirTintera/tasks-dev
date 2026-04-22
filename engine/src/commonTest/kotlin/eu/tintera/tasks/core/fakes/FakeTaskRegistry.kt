package eu.tintera.tasks.core.fakes

import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.core.TaskRegistry
import eu.tintera.tasks.core.serialization.UnitTaskDataSerializer
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes

fun fakeTaskRegistry()  = TaskRegistry().apply {
    register(
        "fakeTask", TaskRegistry.TaskRegistration(
            currentVersion = 1,
            factory = {
                TaskHandler {
                    delay(10.minutes) // 10 minut virtuálního času
                    TaskResult.success(Unit)
                }
            },
            inputSerializer = UnitTaskDataSerializer,
            outputSerializer = UnitTaskDataSerializer,
            progressSerializer = UnitTaskDataSerializer,
            migrations = emptyList()
        )
    )
}