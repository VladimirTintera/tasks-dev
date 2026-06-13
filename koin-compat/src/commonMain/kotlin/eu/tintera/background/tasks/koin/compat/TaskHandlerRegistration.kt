package eu.tintera.background.tasks.koin.compat

import eu.tintera.background.tasks.TaskHandler
import eu.tintera.background.tasks.compat.Data
import eu.tintera.background.tasks.koin.taskRegistration
import eu.tintera.background.tasks.legacySerializer
import eu.tintera.background.tasks.migrations.Migration
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf


inline fun <reified R : TaskHandler<Data, Data, Data>> Module.taskHandlerOf(
    crossinline constructor: () -> R,
    identifier: String,
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
) = taskRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    migrations = migrations,
) {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler<Data, Data, Data>, reified T1> Module.taskHandlerOf(
    crossinline constructor: (T1) -> R,
    identifier: String,
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
) = taskRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler<Data, Data, Data>, reified T1, reified T2> Module.taskHandlerOf(
    crossinline constructor: (T1, T2) -> R,
    identifier: String,
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
) = taskRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler<Data, Data, Data>, reified T1, reified T2, reified T3> Module.taskHandlerOf(
    crossinline constructor: (T1, T2, T3) -> R,
    identifier: String,
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
) = taskRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler<Data, Data, Data>, reified T1, reified T2, reified T3, reified T4> Module.taskHandlerOf(
    crossinline constructor: (T1, T2, T3, T4) -> R,
    identifier: String,
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
) = taskRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler<Data, Data, Data>, reified T1, reified T2, reified T3, reified T4, reified T5> Module.taskHandlerOf(
    crossinline constructor: (T1, T2, T3, T4, T5) -> R,
    identifier: String,
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
) = taskRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler<Data, Data, Data>, reified T1, reified T2, reified T3, reified T4, reified T5, reified T6> Module.taskHandlerOf(
    crossinline constructor: (T1, T2, T3, T4, T5, T6) -> R,
    identifier: String,
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
) = taskRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified R : TaskHandler<Data, Data, Data>> Module.taskRegistration(
    identifier: String,
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
    noinline definition: Module.() -> KoinDefinition<R>
) = taskRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    inputSerializer = legacySerializer(),
    outputSerializer = legacySerializer(),
    progressSerializer = legacySerializer(),
    migrations = migrations,
    definition = definition
)

