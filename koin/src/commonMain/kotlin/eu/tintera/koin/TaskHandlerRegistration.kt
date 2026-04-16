package eu.tintera.koin

import eu.tintera.tasks.Data
import eu.tintera.tasks.LegacyTaskHandler
import eu.tintera.tasks.TaskHandler
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.KClass


@PublishedApi
internal sealed interface TaskHandlerRegistration<Input, Output, Progress, T : TaskHandler<Input, Output, Progress>> {
    val type: KClass<T>

    class Legacy<T: LegacyTaskHandler>(
        override val type: KClass<T>
    ) : TaskHandlerRegistration<Data, Data, Data, T>

    class Typed<Input, Output, Progress, T : TaskHandler<Input, Output, Progress>>(
        override val type: KClass<T>,
        val identifier: String,
        val currentVersion: Int,
        val inputSerializer: KSerializer<Input>,
        val outputSerializer: KSerializer<Output>,
        val progressSerializer: KSerializer<Progress>
    ) : TaskHandlerRegistration<Input, Output, Progress, T>
}

@PublishedApi
internal inline fun <reified Input, reified Output, reified Progress, reified T : TaskHandler<Input, Output, Progress>> taskHandlerRegistration(
    identifier: String,
    currentVersion: Int
) = TaskHandlerRegistration.Typed(
    type = T::class,
    identifier = identifier,
    currentVersion = currentVersion,
    inputSerializer = serializer<Input>(),
    outputSerializer = serializer<Output>(),
    progressSerializer = serializer<Progress>()
)
@PublishedApi
internal inline fun <reified T : LegacyTaskHandler> legacyTaskHandlerRegistration() = TaskHandlerRegistration.Legacy(
    type = T::class,
)
