package eu.tintera.koin

import eu.tintera.tasks.TaskHandler
import kotlin.reflect.KClass

@PublishedApi
internal class TaskHandlerRegistration<I, O, P, T : TaskHandler<out I, out O, out P>>(
    val type: KClass<T>,
    val currentVersion: Int,
)

@PublishedApi
internal inline fun <reified I, reified O, reified P, reified T : TaskHandler<I, O, P>> taskHandlerRegistration(
    currentVersion: Int
) = TaskHandlerRegistration(
    type = T::class,
    currentVersion = currentVersion
)
