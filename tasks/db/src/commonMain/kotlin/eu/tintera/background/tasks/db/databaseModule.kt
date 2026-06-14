package eu.tintera.background.tasks.db

import eu.tintera.background.tasks.core.data.Repository
import eu.tintera.background.tasks.core.data.TransactionRunner
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
    factory { get<TasksDatabase>().dispatchableTaskDao() }
    factory { get<TasksDatabase>().cleanableTaskDao() }
    factory { get<TasksDatabase>().schedulableTaskDao() }
    factory { get<TasksDatabase>().orphanTaskDao() }
    factory { get<TasksDatabase>().parentConstraintDao() }
    factory { get<TasksDatabase>().taskResultDao() }

    factoryOf(::DatabaseFactory)

    factoryOf(::RepositoryImpl) bind Repository::class
    single { get<DatabaseFactory>().create() }
    singleOf(::DatabaseTransactionRunner) bind TransactionRunner::class




}

internal expect fun Module.platformDb()