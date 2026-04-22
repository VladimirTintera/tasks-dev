package eu.tintera.tasks.koin

import org.koin.dsl.module

fun tasksKoinModule() = module {
    single(createdAtStart = true) {
        TasksRegistrations(
            koin = getKoin(),
            registrations = getAll()
        )
    }
}