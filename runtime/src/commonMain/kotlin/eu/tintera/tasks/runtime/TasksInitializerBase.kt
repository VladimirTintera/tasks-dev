package eu.tintera.tasks.runtime

import eu.tintera.guard.ExecutionEnvironmentConfig
import eu.tintera.tasks.TaskLifecycleObserver
import eu.tintera.tasks.TaskManager
import eu.tintera.tasks.TaskManagerConfiguration
import eu.tintera.tasks.Tasks
import eu.tintera.tasks.core.guard.guardInit
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