package eu.tintera.koin

import eu.tintera.tasks.TaskHandler
import kotlin.reflect.KClass

class TaskHandlerRegistration<T : TaskHandler>(
    val type: KClass<out T>,
)

inline fun <reified T : TaskHandler> taskHandlerRegistration() =
    TaskHandlerRegistration(T::class)
