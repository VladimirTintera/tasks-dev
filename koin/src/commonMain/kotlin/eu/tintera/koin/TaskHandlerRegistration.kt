package eu.tintera.koin

import eu.tintera.tasks.Data
import eu.tintera.tasks.LegacyTaskHandler
import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.fullName
import eu.tintera.tasks.migrations.Migration
import eu.tintera.tasks.serialization.TaskDataSerializer
import eu.tintera.tasks.serialization.jsonSerializer
import org.koin.core.Koin
import kotlin.reflect.KClass


@PublishedApi
internal sealed interface TaskHandlerRegistration<Input: Any, Output: Any, Progress: Any, T : TaskHandler<Input, Output, Progress>> {
    val type: KClass<T>
    val koinFactory: (Koin) -> TaskHandler<Input, Output, Progress>

    class Legacy<T : LegacyTaskHandler>(
        override val type: KClass<T>,
        override val koinFactory: (Koin) -> T
    ) : TaskHandlerRegistration<Data, Data, Data, T>

    class Typed<Input: Any, Output: Any, Progress: Any, T : TaskHandler<Input, Output, Progress>>(
        override val type: KClass<T>,
        override val koinFactory: (Koin) -> TaskHandler<Input, Output, Progress>,
        val identifier: String,
        val currentVersion: Int,
        val inputSerializer: TaskDataSerializer<Input>,
        val outputSerializer: TaskDataSerializer<Output>,
        val progressSerializer: TaskDataSerializer<Progress>,
        val migrations: List<Migration>
    ) : TaskHandlerRegistration<Input, Output, Progress, T>
}

@PublishedApi
internal inline fun <reified Input: Any, reified Output: Any, reified Progress: Any, reified T : TaskHandler<Input, Output, Progress>> taskHandlerRegistration(
    identifier: String,
    currentVersion: Int,
    migrations: List<Migration> = emptyList(),
    inputSerializer: TaskDataSerializer<Input> = jsonSerializer<Input>(),
    outputSerializer: TaskDataSerializer<Output> = jsonSerializer<Output>(),
    progressSerializer: TaskDataSerializer<Progress> = jsonSerializer<Progress>(),
) = TaskHandlerRegistration.Typed(
    type = T::class,
    identifier = identifier.ifBlank { T::class.fullName },
    currentVersion = currentVersion,
    inputSerializer = inputSerializer,
    outputSerializer = outputSerializer,
    progressSerializer = progressSerializer,
    migrations = migrations,
    koinFactory = { koinInstance -> koinInstance.get<T>() },

)

@PublishedApi
internal inline fun <reified T : LegacyTaskHandler> legacyTaskHandlerRegistration() = TaskHandlerRegistration.Legacy(
    type = T::class,
    koinFactory = { koinInstance -> koinInstance.get<T>() },
)
