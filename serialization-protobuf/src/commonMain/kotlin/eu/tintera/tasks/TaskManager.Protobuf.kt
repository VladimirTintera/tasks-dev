package eu.tintera.tasks

import eu.tintera.tasks.migrations.Migration

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

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>> Registry.register(
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
    noinline factory: () -> TaskHandler<Input, Output, Progress>
) {
    register(
        identifier = R::class.fullName,
        currentVersion = currentVersion,
        migrations = migrations,
        factory = factory
    )
}