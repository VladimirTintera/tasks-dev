package eu.tintera.tasks

import eu.tintera.tasks.migrations.Migration
import eu.tintera.tasks.serialization.Serializer
import eu.tintera.tasks.serialization.TagSerializer
import kotlin.reflect.KClass

interface Registry {
    fun <Input : Any, Output : Any, Progress : Any> register(
        registration: TaskRegistration<Input, Output, Progress>,
    )

    fun <T : Tag> registerTag(
        identifier: String,
        type: KClass<out T>,
        serializer: TagSerializer<T>
    )
}

inline fun <reified T : Tag> Registry.registerTag(
    serializer: TagSerializer<T>
) = registerTag(
    identifier = T::class.fullName,
    type = T::class,
    serializer = serializer
)

inline fun <reified T : TaskHandler<I, O, P>, reified I : Any, reified O : Any, reified P : Any> Registry.register(
    identifier: String = T::class.fullName,
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