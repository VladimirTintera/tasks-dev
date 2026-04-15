package eu.tintera.koin

import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.TaskManager
import eu.tintera.tasks.fullName
import eu.tintera.tasks.register
import org.koin.core.Koin
import org.koin.core.definition.Definition
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.dsl.bind

internal class TasksRegistrations(
    koin: Koin,
    taskManager: TaskManager,
    registrations: List<TaskHandlerRegistration<*, *, *, *>>
) {
    init {
        registrations.forEach { registration ->
            taskManager.register(
                identifier = registration.type.fullName,
                currentVersion = registration.currentVersion,
            ) {
                koin.get<TaskHandler<*, *, *>>(registration.type)
            }
        }
    }
}

inline fun <reified R : TaskHandler<*, *, *>> Module.taskHandlerOf(
    currentVersion: Int = 1,
    crossinline constructor: () -> R
) = taskHandlerRegistration(currentVersion = currentVersion) {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler<*, *, *>, reified T1> Module.taskHandlerOf(
    currentVersion: Int = 1,
    crossinline constructor: (T1) -> R
) = taskHandlerRegistration(
    currentVersion = currentVersion
) {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler<*, *, *>, reified T1, reified T2> Module.taskHandlerOf(
    currentVersion: Int = 1,
    crossinline constructor: (T1, T2) -> R
) = taskHandlerRegistration(
    currentVersion = currentVersion
) {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler<*, *, *>, reified T1, reified T2, reified T3> Module.taskHandlerOf(
    currentVersion: Int = 1,
    crossinline constructor: (T1, T2, T3) -> R
) = taskHandlerRegistration(
    currentVersion = currentVersion
) {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler<*, *, *>, reified T1, reified T2, reified T3, reified T4> Module.taskHandlerOf(
    currentVersion: Int = 1,
    crossinline constructor: (T1, T2, T3, T4) -> R
) = taskHandlerRegistration(
    currentVersion = currentVersion
) {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler<*, *, *>, reified T1, reified T2, reified T3, reified T4, reified T5> Module.taskHandlerOf(
    currentVersion: Int = 1,
    crossinline constructor: (T1, T2, T3, T4, T5) -> R
) = taskHandlerRegistration(
    currentVersion = currentVersion
) {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler<*, *, *>, reified T1, reified T2, reified T3, reified T4, reified T5, reified T6> Module.taskHandlerOf(
    currentVersion: Int = 1,
    crossinline constructor: (T1, T2, T3, T4, T5, T6) -> R
) = taskHandlerRegistration(
    currentVersion = currentVersion
) {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler<*, *, *>> Module.taskHandler(
    currentVersion: Int = 1,
    qualifier: Qualifier? = null,
    noinline definition: Definition<R>,
) = taskHandlerRegistration(
    currentVersion = currentVersion
) {
    factory(qualifier, definition)
}

inline fun <reified R : TaskHandler<*, *, *>> Module.taskHandlerRegistration(
    currentVersion: Int,
    noinline definition: Module.() -> KoinDefinition<R>
) {
    definition()
    single(named<R>()) {
        taskHandlerRegistration<R>(currentVersion = currentVersion)
    } bind TaskHandlerRegistration::class
}
