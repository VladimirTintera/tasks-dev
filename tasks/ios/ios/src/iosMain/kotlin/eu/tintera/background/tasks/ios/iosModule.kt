package eu.tintera.background.tasks.ios

import eu.tintera.background.guard.ExecutionContextObserver
import eu.tintera.background.guard.TokenProducer
import eu.tintera.background.tasks.TaskLifecycleObserver
import eu.tintera.background.tasks.core.AppStateObserver
import eu.tintera.background.tasks.core.NetworkState
import eu.tintera.background.tasks.core.engineModule
import eu.tintera.background.tasks.core.constraints.Constraint
import eu.tintera.background.tasks.engine.db.engineDbModule
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

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
        } binds arrayOf(TokenProducer::class, ExecutionContextObserver::class, Constraint::class, TaskLifecycleObserver::class)
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
        } binds arrayOf(TokenProducer::class, ExecutionContextObserver::class, TaskLifecycleObserver::class)
    }

    singleOf(::AppLifecycleObserver) {
        createdAtStart()
    } bind AppStateObserver::class
}