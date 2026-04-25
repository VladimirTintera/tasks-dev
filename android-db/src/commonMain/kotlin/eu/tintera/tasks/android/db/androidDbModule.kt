package eu.tintera.tasks.android.db

import eu.tintera.tasks.android.WorkManagerDatabaseCleanupServiceRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val androidDbModule = module {
    factoryOf(::WorkManagerDatabaseCleanupServiceRepositoryImpl) bind WorkManagerDatabaseCleanupServiceRepository::class
}