package eu.tintera.koin

import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.TaskManager
import eu.tintera.tasks.fullName
import eu.tintera.tasks.legacy.LegacyTaskHandler
import eu.tintera.tasks.migrations.Migration
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
            when (registration) {
                is TaskHandlerRegistration.Legacy -> {
                    taskManager.register(
                        identifier = registration.type.fullName,
                        factory = {
                            registration.koinFactory(koin)
                        }
                    )
                }

                is TaskHandlerRegistration.Typed -> {
                    registerTyped(taskManager, koin, registration)
                }
            }
        }
    }

    private fun <I : Any, O : Any, P : Any, T : TaskHandler<I, O, P>> registerTyped(
        taskManager: TaskManager,
        koin: Koin,
        registration: TaskHandlerRegistration.Typed<I, O, P, T>
    ) {
        taskManager.register(
            identifier = registration.identifier,
            currentVersion = registration.currentVersion,
            inputSerializer = registration.inputSerializer,   // Kompilátor vidí: TaskDataSerializer<I>
            outputSerializer = registration.outputSerializer, // Kompilátor vidí: TaskDataSerializer<O>
            progressSerializer = registration.progressSerializer,
            migrations = registration.migrations,
            factory = { registration.koinFactory(koin) }      // Kompilátor vidí: () -> TaskHandler<I, O, P>
        ) // VŠECHNO DOKONALE SEDÍ!
    }
}

inline fun <reified R : LegacyTaskHandler> Module.taskHandlerOf(
    crossinline constructor: () -> R,
) = legacyTaskHandlerRegistration {
    factoryOf(constructor)
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>> Module.taskHandlerOf(
    crossinline constructor: () -> R,
    identifier: String = "",
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
) = taskHandlerRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    migrations = migrations,
) {
    factoryOf(constructor)
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>, reified T1> Module.taskHandlerOf(
    crossinline constructor: (T1) -> R,
    identifier: String = "",
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
) = taskHandlerRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>, reified T1, reified T2> Module.taskHandlerOf(
    crossinline constructor: (T1, T2) -> R,
    identifier: String = "",
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
) = taskHandlerRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>, reified T1, reified T2, reified T3> Module.taskHandlerOf(
    crossinline constructor: (T1, T2, T3) -> R,
    identifier: String = "",
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
) = taskHandlerRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>, reified T1, reified T2, reified T3, reified T4> Module.taskHandlerOf(
    crossinline constructor: (T1, T2, T3, T4) -> R,
    identifier: String = "",
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
) = taskHandlerRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>, reified T1, reified T2, reified T3, reified T4, reified T5> Module.taskHandlerOf(
    crossinline constructor: (T1, T2, T3, T4, T5) -> R,
    identifier: String = "",
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
) = taskHandlerRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>, reified T1, reified T2, reified T3, reified T4, reified T5, reified T6> Module.taskHandlerOf(
    crossinline constructor: (T1, T2, T3, T4, T5, T6) -> R,
    identifier: String = "",
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
) = taskHandlerRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>> Module.taskHandler(
    identifier: String = "",
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
    qualifier: Qualifier? = null,
    noinline definition: Definition<R>,
) = taskHandlerRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    migrations = migrations
) {
    factory(qualifier, definition)
}

@Deprecated("Use typed registration instead")
inline fun <reified R : LegacyTaskHandler> Module.legacyTaskHandlerRegistration(
    noinline definition: Module.() -> KoinDefinition<R>
) {
    definition()
    single(named<R>()) {
        legacyTaskHandlerRegistration<R>()
    } bind TaskHandlerRegistration::class
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>> Module.taskHandlerRegistration(
    currentVersion: Int,
    identifier: String,
    migrations: List<Migration>,
    noinline definition: Module.() -> KoinDefinition<R>
) {
    definition()
    single(named<R>()) {
        taskHandlerRegistration<Input, Output, Progress, R>(
            currentVersion = currentVersion,
            identifier = identifier,
            migrations = migrations
        )
    } bind TaskHandlerRegistration::class
}
