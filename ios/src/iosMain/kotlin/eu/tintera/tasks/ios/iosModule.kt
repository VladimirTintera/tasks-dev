package eu.tintera.tasks.ios

import eu.tintera.guard.ExecutionContextObserver
import eu.tintera.guard.TokenProducer
import eu.tintera.tasks.core.AppStateObserver
import eu.tintera.tasks.core.NetworkState
import eu.tintera.tasks.core.engineModule
import eu.tintera.tasks.core.preconditions.TaskPrecondition
import eu.tintera.tasks.engine.db.engineDbModule
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import platform.posix.clock

fun iosModule(
    bgProcessingTaskIdentifier: String?,
    appRefreshTaskIdentifier: String?
) = module {
    includes(
        engineModule,
        engineDbModule
    )

    singleOf<NetworkState>(::IosNetworkState)

    bgProcessingTaskIdentifier?.also { identifier ->
        single(createdAtStart = true) {
            BgProcessingTaskManager(
                scope = get(),
                dispatchers = get(),
                taskIdentifier = identifier,
                repository = get(),
                appLifecycleObserver = get(),
                isAppRefreshTaskAllowed = appRefreshTaskIdentifier != null,
                clock = get(),
            )
        } binds arrayOf(TokenProducer::class, ExecutionContextObserver::class, TaskPrecondition::class)
    }

    appRefreshTaskIdentifier?.also { identifier ->
        single(createdAtStart = true) {
            AppRefreshTaskManager(
                scope = get(),
                dispatchers = get(),
                taskIdentifier = identifier,
                repository = get(),
                appLifecycleObserver = get(),
                clock = get(),
            )
        } binds arrayOf(TokenProducer::class, ExecutionContextObserver::class)
    }

    singleOf(::AppLifecycleObserver) {
        createdAtStart()
    } bind AppStateObserver::class
}