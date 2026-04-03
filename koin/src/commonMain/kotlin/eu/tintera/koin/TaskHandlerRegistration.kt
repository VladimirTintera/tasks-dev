package eu.tintera.koin

import eu.tintera.tasks.TaskHandler
import kotlin.reflect.KClass

@PublishedApi
internal class TaskHandlerRegistration<T : TaskHandler>(
    val type: KClass<out T>,
)

@PublishedApi
internal inline fun <reified T : TaskHandler> taskHandlerRegistration() =
    TaskHandlerRegistration(T::class)
