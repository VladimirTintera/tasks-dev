package eu.tintera.background.tasks.core.fakes

import eu.tintera.background.tasks.TaskHandler
import eu.tintera.background.tasks.TaskResult
import eu.tintera.background.tasks.core.TaskRegistry
import eu.tintera.background.tasks.core.serialization.UnitSerializer
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
            inputSerializer = UnitSerializer,
            outputSerializer = UnitSerializer,
            progressSerializer = UnitSerializer,
            migrations = emptyList()
        )
    )
}