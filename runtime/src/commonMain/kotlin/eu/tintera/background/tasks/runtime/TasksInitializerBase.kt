package eu.tintera.background.tasks.runtime

import eu.tintera.background.guard.ExecutionEnvironmentConfig
import eu.tintera.background.tasks.TaskLifecycleObserver
import eu.tintera.background.tasks.TaskManager
import eu.tintera.background.tasks.TaskManagerConfiguration
import eu.tintera.background.tasks.Tasks
import eu.tintera.background.tasks.core.guard.guardInit
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.module

abstract class TasksInitializerBase {

    internal abstract fun module(config: TaskManagerConfiguration): Module

    internal open fun KoinApplication.customInitialization(config: TaskManagerConfiguration) {}

    internal fun create(
        config: TaskManagerConfiguration,
        taskLifecycleObservers: List<TaskLifecycleObserver> = emptyList()
    ): TaskManager {

        TaskManagerBootstrapper.initialize(
            taskLifecycleObservers = taskLifecycleObservers
        ) {
            customInitialization(config)
            modules(
                module {
                    guardInit(
                        executionEnvironment = config.executionEnvironment,
                        config = ExecutionEnvironmentConfig(
                            releaseDebounce = config.executionContextReleaseDebounce
                        )
                    )
                },
                module(config)
            )
        }

        return Tasks.taskManager
    }
}