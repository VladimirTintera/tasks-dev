package eu.tintera.koin

import eu.tintera.tasks.Data
import eu.tintera.tasks.LegacyTaskHandler
import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.migrations.Migration
import eu.tintera.tasks.serialization.TaskDataSerializer
import eu.tintera.tasks.serialization.jsonSerializer
import kotlin.reflect.KClass


@PublishedApi
internal sealed interface TaskHandlerRegistration<Input, Output, Progress, T : TaskHandler<Input, Output, Progress>> {
    val type: KClass<T>

    class Legacy<T : LegacyTaskHandler>(
        override val type: KClass<T>
    ) : TaskHandlerRegistration<Data, Data, Data, T>

    class Typed<Input, Output, Progress, T : TaskHandler<Input, Output, Progress>>(
        override val type: KClass<T>,
        val identifier: String,
        val currentVersion: Int,
        val inputSerializer: TaskDataSerializer<Input>,
        val outputSerializer: TaskDataSerializer<Output>,
        val progressSerializer: TaskDataSerializer<Progress>,
        val migrations: List<Migration>
    ) : TaskHandlerRegistration<Input, Output, Progress, T>
}

@PublishedApi
internal inline fun <reified Input, reified Output, reified Progress, reified T : TaskHandler<Input, Output, Progress>> taskHandlerRegistration(
    identifier: String,
    currentVersion: Int,
    migrations: List<Migration> = emptyList(),
    inputSerializer: TaskDataSerializer<Input> = jsonSerializer<Input>(),
    outputSerializer: TaskDataSerializer<Output> = jsonSerializer<Output>(),
    progressSerializer: TaskDataSerializer<Progress> = jsonSerializer<Progress>(),
) = TaskHandlerRegistration.Typed(
    type = T::class,
    identifier = identifier,
    currentVersion = currentVersion,
    inputSerializer = inputSerializer,
    outputSerializer = outputSerializer,
    progressSerializer = progressSerializer,
    migrations = migrations

)

@PublishedApi
internal inline fun <reified T : LegacyTaskHandler> legacyTaskHandlerRegistration() = TaskHandlerRegistration.Legacy(
    type = T::class,
)
