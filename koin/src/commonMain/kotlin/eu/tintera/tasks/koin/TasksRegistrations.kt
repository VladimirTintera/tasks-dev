package eu.tintera.tasks.koin

import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.TaskManager
import eu.tintera.tasks.TaskManagerBootstrapper
import eu.tintera.tasks.migrations.Migration
import eu.tintera.tasks.serialization.Serializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.Koin
import org.koin.core.definition.Definition
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.Qualifier

internal class TasksRegistrations(
    koin: Koin,
    registrations: List<TaskHandlerRegistration<*, *, *, *>>
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    init {
        scope.launch {
            val taskManager = TaskManagerBootstrapper.taskManager.filterNotNull().first()
            registrations.forEach { register(taskManager, koin, it) }
        }
    }

    private fun <I : Any, O : Any, P : Any, T : TaskHandler<I, O, P>> register(
        taskManager: TaskManager,
        koin: Koin,
        registration: TaskHandlerRegistration<I, O, P, T>
    ) {
        taskManager.register(
            identifier = registration.identifier,
            currentVersion = registration.currentVersion,
            inputSerializer = registration.inputSerializer,
            outputSerializer = registration.outputSerializer,
            progressSerializer = registration.progressSerializer,
            migrations = registration.migrations,
            factory = { registration.koinFactory(koin) }
        )
    }
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>> Module.taskHandlerOf(
    crossinline constructor: () -> R,
    identifier: String = "",
    currentVersion: Int = 1,
    inputSerializer: Serializer<Input>,
    outputSerializer: Serializer<Output>,
    progressSerializer: Serializer<Progress>,
    migrations: List<Migration> = emptyList(),
) = taskHandlerRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    inputSerializer = inputSerializer,
    outputSerializer = outputSerializer,
    progressSerializer = progressSerializer,
    migrations = migrations,
) {
    factoryOf(constructor)
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>, reified T1> Module.taskHandlerOf(
    crossinline constructor: (T1) -> R,
    identifier: String = "",
    currentVersion: Int = 1,
    inputSerializer: Serializer<Input>,
    outputSerializer: Serializer<Output>,
    progressSerializer: Serializer<Progress>,
    migrations: List<Migration> = emptyList(),
) = taskHandlerRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    inputSerializer = inputSerializer,
    outputSerializer = outputSerializer,
    progressSerializer = progressSerializer,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>, reified T1, reified T2> Module.taskHandlerOf(
    crossinline constructor: (T1, T2) -> R,
    identifier: String = "",
    currentVersion: Int = 1,
    inputSerializer: Serializer<Input>,
    outputSerializer: Serializer<Output>,
    progressSerializer: Serializer<Progress>,
    migrations: List<Migration> = emptyList(),
) = taskHandlerRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    inputSerializer = inputSerializer,
    outputSerializer = outputSerializer,
    progressSerializer = progressSerializer,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>, reified T1, reified T2, reified T3> Module.taskHandlerOf(
    crossinline constructor: (T1, T2, T3) -> R,
    identifier: String = "",
    currentVersion: Int = 1,
    inputSerializer: Serializer<Input>,
    outputSerializer: Serializer<Output>,
    progressSerializer: Serializer<Progress>,
    migrations: List<Migration> = emptyList(),
) = taskHandlerRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    inputSerializer = inputSerializer,
    outputSerializer = outputSerializer,
    progressSerializer = progressSerializer,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>, reified T1, reified T2, reified T3, reified T4> Module.taskHandlerOf(
    crossinline constructor: (T1, T2, T3, T4) -> R,
    identifier: String = "",
    currentVersion: Int = 1,
    inputSerializer: Serializer<Input>,
    outputSerializer: Serializer<Output>,
    progressSerializer: Serializer<Progress>,
    migrations: List<Migration> = emptyList(),
) = taskHandlerRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    inputSerializer = inputSerializer,
    outputSerializer = outputSerializer,
    progressSerializer = progressSerializer,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>, reified T1, reified T2, reified T3, reified T4, reified T5> Module.taskHandlerOf(
    crossinline constructor: (T1, T2, T3, T4, T5) -> R,
    identifier: String = "",
    currentVersion: Int = 1,
    inputSerializer: Serializer<Input>,
    outputSerializer: Serializer<Output>,
    progressSerializer: Serializer<Progress>,
    migrations: List<Migration> = emptyList(),
) = taskHandlerRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    inputSerializer = inputSerializer,
    outputSerializer = outputSerializer,
    progressSerializer = progressSerializer,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>, reified T1, reified T2, reified T3, reified T4, reified T5, reified T6> Module.taskHandlerOf(
    crossinline constructor: (T1, T2, T3, T4, T5, T6) -> R,
    identifier: String = "",
    currentVersion: Int = 1,
    inputSerializer: Serializer<Input>,
    outputSerializer: Serializer<Output>,
    progressSerializer: Serializer<Progress>,
    migrations: List<Migration> = emptyList(),
) = taskHandlerRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    inputSerializer = inputSerializer,
    outputSerializer = outputSerializer,
    progressSerializer = progressSerializer,
    migrations = migrations
) {
    factoryOf(constructor)
}

inline fun <reified Input : Any, reified Output : Any, reified Progress : Any, reified R : TaskHandler<Input, Output, Progress>> Module.taskHandler(
    identifier: String = "",
    currentVersion: Int = 1,
    migrations: List<Migration> = emptyList(),
    qualifier: Qualifier? = null,
    inputSerializer: Serializer<Input>,
    outputSerializer: Serializer<Output>,
    progressSerializer: Serializer<Progress>,
    noinline definition: Definition<R>,
) = taskHandlerRegistration(
    identifier = identifier,
    currentVersion = currentVersion,
    inputSerializer = inputSerializer,
    outputSerializer = outputSerializer,
    progressSerializer = progressSerializer,
    migrations = migrations
) {
    factory(qualifier, definition)
}


