package eu.tintera.background.tasks

import eu.tintera.background.tasks.migrations.Migration

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any> Registry.register(
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
        inputSerializer = protobufSerializer<Input>(),
        outputSerializer = protobufSerializer<Output>(),
        progressSerializer = protobufSerializer<Progress>()
    )
}