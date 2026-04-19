package eu.tintera.tasks.koin

import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.fullName
import eu.tintera.tasks.migrations.Migration
import eu.tintera.tasks.serialization.TaskDataSerializer
import org.koin.core.Koin
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import kotlin.reflect.KClass


class TaskHandlerRegistration<Input : Any, Output : Any, Progress : Any, T : TaskHandler<Input, Output, Progress>>(
    val type: KClass<T>,
    val koinFactory: (Koin) -> TaskHandler<Input, Output, Progress>,
    val identifier: String,
    val currentVersion: Int,
    val inputSerializer: TaskDataSerializer<Input>,
    val outputSerializer: TaskDataSerializer<Output>,
    val progressSerializer: TaskDataSerializer<Progress>,
    val migrations: List<Migration>
)

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>> Module.taskHandlerRegistration(
    currentVersion: Int,
    identifier: String,
    migrations: List<Migration>,
    inputSerializer: TaskDataSerializer<Input>,
    outputSerializer: TaskDataSerializer<Output>,
    progressSerializer: TaskDataSerializer<Progress>,
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
            koinFactory = { koinInstance -> koinInstance.get<R>() },
            type = R::class
        )
    }
}
