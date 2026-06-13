package eu.tintera.background.tasks.koin

import eu.tintera.background.tasks.TaskHandler
import eu.tintera.background.tasks.TaskRegistration
import eu.tintera.background.tasks.migrations.Migration
import eu.tintera.background.tasks.serialization.Serializer
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
            identifier = identifier,
            migrations = migrations,
            inputSerializer = inputSerializer,
            outputSerializer = outputSerializer,
            progressSerializer = progressSerializer,
            factory = { get<R>() },
            type = R::class
        )
    }

    single(named<R>(), createdAtStart = true) {
        TaskRegistrationInstaller(get(named<R>()))
    }
}
