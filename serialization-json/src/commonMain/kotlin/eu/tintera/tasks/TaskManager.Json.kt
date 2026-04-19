package eu.tintera.tasks

import eu.tintera.tasks.migrations.Migration

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any> TaskManager.register(
    identifier: String,
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
    noinline factory: () -> TaskHandler<Input, Output, Progress>
) {
    register(
        identifier = identifier,
        currentVersion = currentVersion,
        migrations = migrations,
        factory = factory,
        inputSerializer = jsonSerializer<Input>(),
        outputSerializer = jsonSerializer<Output>(),
        progressSerializer = jsonSerializer<Progress>()
    )
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any> TaskManager.register(
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
    noinline factory: () -> TaskHandler<Input, Output, Progress>
) {
    register(
        currentVersion = currentVersion,
        migrations = migrations,
        factory = factory,
        inputSerializer = jsonSerializer<Input>(),
        outputSerializer = jsonSerializer<Output>(),
        progressSerializer = jsonSerializer<Progress>()
    )
}