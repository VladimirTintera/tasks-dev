package eu.tintera.tasks

import eu.tintera.tasks.core.TaskProcessorConfig
import eu.tintera.tasks.core.locks.ExecutionContextConfig
import eu.tintera.tasks.koin.startTasksKoin
import org.koin.dsl.module

object TasksInitializer {
    fun initialize(
        taskManagerConfig: TaskManagerConfig = TaskManagerConfig()
    ) {
        startTasksKoin {
            modules(
                module {
                    single { taskManagerConfig }
                    single {
                        TaskProcessorConfig(
                            maxConcurrentTasks = taskManagerConfig.maxConcurrentTasks
                        )
                    }
                    single {
                        ExecutionContextConfig(
                            releaseDebounce = taskManagerConfig.executionContextReleaseDebounce
                        )
                    }
                }
            )
        }
    }
}