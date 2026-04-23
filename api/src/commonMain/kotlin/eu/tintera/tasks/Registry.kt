package eu.tintera.tasks

import eu.tintera.tasks.migrations.Migration
import eu.tintera.tasks.serialization.Serializer

interface Registry {
    fun <Input : Any, Output : Any, Progress : Any> register(
        registration: TaskRegistration<Input, Output, Progress>,
    )
}

inline fun <reified T : TaskHandler<I, O, P>, reified I : Any, reified O : Any, reified P : Any> Registry.register(
    identifier: String,
    currentVersion: Int = 1,
    inputSerializer: Serializer<I>,
    outputSerializer: Serializer<O>,
    progressSerializer: Serializer<P>,
    migrations: List<Migration> = emptyList(),
    noinline factory: () -> T
) = register(
    TaskRegistration(
        identifier = identifier,
        currentVersion = currentVersion,
        factory = factory,
        inputSerializer = inputSerializer,
        outputSerializer = outputSerializer,
        progressSerializer = progressSerializer,
        migrations = migrations
    )
)

inline fun <reified T : TaskHandler<I, O, P>, reified I : Any, reified O : Any, reified P : Any> Registry.register(
    currentVersion: Int = 1,
    inputSerializer: Serializer<I>,
    outputSerializer: Serializer<O>,
    progressSerializer: Serializer<P>,
    migrations: List<Migration> = emptyList(),
    noinline factory: () -> T
) = register(
    identifier = T::class.fullName,
    currentVersion = currentVersion,
    factory = factory,
    inputSerializer = inputSerializer,
    outputSerializer = outputSerializer,
    progressSerializer = progressSerializer,
    migrations = migrations,
)