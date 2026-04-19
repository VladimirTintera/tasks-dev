package eu.tintera.tasks.koin

import eu.tintera.tasks.TaskManager
import eu.tintera.tasks.getInstance
import org.koin.dsl.module

fun tasksKoinModule() = module {
    single { TaskManager.getInstance() }

    single(createdAtStart = true) {
        TasksRegistrations(
            koin = getKoin(),
            taskManager = get(),
            registrations = getAll()
        )
    }
}