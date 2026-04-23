package eu.tintera.tasks.koin

import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.TaskRegistration
import eu.tintera.tasks.fullName
import eu.tintera.tasks.migrations.Migration
import eu.tintera.tasks.serialization.Serializer
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.qualifier.named

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>> Module.taskRegistration(
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
        TaskRegistration(
            currentVersion = currentVersion,
            identifier = identifier.ifEmpty { R::class.fullName },
            migrations = migrations,
            inputSerializer = inputSerializer,
            outputSerializer = outputSerializer,
            progressSerializer = progressSerializer,
            factory = { get<R>() }
        )
    }
}
