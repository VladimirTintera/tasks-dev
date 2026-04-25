package eu.tintera.tasks.db

import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.TransactionRunner
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val databaseModule = module {
    platformDb()
    factory { get<TasksDatabase>().taskDao() }
    factory { get<TasksDatabase>().taskParentTaskDao() }
    factory { get<TasksDatabase>().taskTagDao() }
    factory { get<TasksDatabase>().taskProgressDataDao() }
    factory { get<TasksDatabase>().taskDataDao() }
    factory { get<TasksDatabase>().taskProcessorDao() }

    factoryOf(::DatabaseFactory)

    factoryOf(::DatabaseRepository) bind Repository::class
    single { get<DatabaseFactory>().create() }
    singleOf(::DatabaseTransactionRunner) bind TransactionRunner::class

    factoryOf(::TaskScopeRepository) bind eu.tintera.tasks.core.data.TaskScopeRepository::class
    factoryOf(::TaskEvaluatorRepository) bind eu.tintera.tasks.core.data.TaskEvaluatorRepository::class
    factoryOf(::TaskProcessorRepository) bind eu.tintera.tasks.core.data.TaskProcessorRepository::class
}

internal expect fun Module.platformDb()