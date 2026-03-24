package eu.tintera.tasks.db

import eu.tintera.tasks.core.data.Repository
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val databaseModule = module {
    platformDb()
    factory { get<TasksDatabase>().taskDao() }
    factory { get<TasksDatabase>().taskParentTaskDao() }
    factory { get<TasksDatabase>().taskTagDao() }

    factoryOf(::DatabaseRepository) bind Repository::class
}

expect fun Module.platformDb()