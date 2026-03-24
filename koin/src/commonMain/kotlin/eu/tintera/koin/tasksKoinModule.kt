package eu.tintera.koin

import eu.tintera.tasks.TaskManager
import eu.tintera.tasks.getInstance
import org.koin.dsl.module

fun tasksKoinModule() = module {
    single { TaskManager.getInstance() }

    single(createdAtStart = true) {
        TasksRegistrations(getKoin(), get(), getAll())
    }
}