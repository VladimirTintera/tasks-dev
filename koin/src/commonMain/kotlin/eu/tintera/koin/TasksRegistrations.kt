package eu.tintera.koin

import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.TaskManager
import eu.tintera.tasks.register
import org.koin.core.Koin
import org.koin.core.definition.Definition
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.dsl.bind

class TasksRegistrations(
    koin: Koin,
    taskManager: TaskManager,
    registrations: List<TaskHandlerRegistration<*>>
) {
    init {
        registrations.forEach { registration ->
            taskManager.register(registration.type) { koin.get(registration.type) }
        }
    }
}

inline fun <reified R : TaskHandler> Module.taskHandlerOf(
    crossinline constructor: () -> R
) = taskHandlerRegistration {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler, reified T1> Module.taskHandlerOf(
    crossinline constructor: (T1) -> R
) = taskHandlerRegistration {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler, reified T1, reified T2> Module.taskHandlerOf(
    crossinline constructor: (T1, T2) -> R
) = taskHandlerRegistration {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler, reified T1, reified T2, reified T3> Module.taskHandlerOf(
    crossinline constructor: (T1, T2, T3) -> R
) = taskHandlerRegistration {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler, reified T1, reified T2, reified T3, reified T4> Module.taskHandlerOf(
    crossinline constructor: (T1, T2, T3, T4) -> R
) = taskHandlerRegistration {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler, reified T1, reified T2, reified T3, reified T4, reified T5> Module.taskHandlerOf(
    crossinline constructor: (T1, T2, T3, T4, T5) -> R
) = taskHandlerRegistration {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler, reified T1, reified T2, reified T3, reified T4, reified T5, reified T6> Module.taskHandlerOf(
    crossinline constructor: (T1, T2, T3, T4, T5, T6) -> R
) = taskHandlerRegistration {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler> Module.taskHandler(
    qualifier: Qualifier? = null,
    noinline definition: Definition<R>,
) = taskHandlerRegistration {
    factory(qualifier, definition)
}

inline fun <reified R : TaskHandler> Module.taskHandlerRegistration(
    noinline definition: Module.() -> KoinDefinition<R>
) {
    definition()
    single(named<R>()) {
        taskHandlerRegistration<R>()
    } bind TaskHandlerRegistration::class
}
