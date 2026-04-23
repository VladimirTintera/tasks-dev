package eu.tintera.tasks.koin

import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.fullName
import eu.tintera.tasks.migrations.Migration
import eu.tintera.tasks.serialization.Serializer
import org.koin.core.Koin
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.qualifier.named


@PublishedApi
internal class TaskHandlerRegistration<Input : Any, Output : Any, Progress : Any, T : TaskHandler<Input, Output, Progress>>(
    val koinFactory: (Koin) -> TaskHandler<Input, Output, Progress>,
    val identifier: String,
    val currentVersion: Int,
    val inputSerializer: Serializer<Input>,
    val outputSerializer: Serializer<Output>,
    val progressSerializer: Serializer<Progress>,
    val migrations: List<Migration>
)

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>> Module.taskHandlerRegistration(
    currentVersion: Int,
    identifier: String,
    migrations: List<Migration>,
    inputSerializer: Serializer<Input>,
    outputSerializer: Serializer<Output>,
    progressSerializer: Serializer<Progress>,
    noinline definition: Module.() -> KoinDefinition<R>
) {
    definition()
    single(named<R>()) {
        TaskHandlerRegistration(
            currentVersion = currentVersion,
            identifier = identifier.ifEmpty { R::class.fullName },
            migrations = migrations,
            inputSerializer = inputSerializer,
            outputSerializer = outputSerializer,
            progressSerializer = progressSerializer,
            koinFactory = { koinInstance -> koinInstance.get<R>() }
        )
    }
}
