package eu.tintera.tasks.koin.json

import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.jsonSerializer
import eu.tintera.tasks.koin.taskRegistration
import eu.tintera.tasks.migrations.Migration
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf


inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>> Module.taskHandlerOf(
    crossinline constructor: () -> R,
    identifier: String = "",
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
) = taskRegistration(
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
) = taskRegistration(
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
) = taskRegistration(
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
) = taskRegistration(
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
) = taskRegistration(
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
) = taskRegistration(
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
) = taskRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>> Module.taskRegistration(
    identifier: String = "",
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
    noinline definition: Module.() -> KoinDefinition<R>
) = taskRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    inputSerializer = jsonSerializer<Input>(),
    outputSerializer = jsonSerializer<Output>(),
    progressSerializer = jsonSerializer<Progress>(),
    migrations = migrations,
    definition = definition
)

