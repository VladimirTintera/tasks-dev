package eu.tintera.tasks.android

import androidx.work.WorkManager
import eu.tintera.tasks.TaskManager
import eu.tintera.tasks.core.cleanup.DatabaseCleanupService
import eu.tintera.tasks.core.coreModule
import eu.tintera.tasks.core.db.coreDbModule
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val androidModule = module {

    includes(
        coreModule,
        coreDbModule
    )

    factoryOf(::WorkManagerDatabaseCleanupService) bind DatabaseCleanupService::class

    factory<WorkManager> { WorkManager.getInstance(get()) }
    factoryOf(::WorkManagerTaskManager) bind TaskManager::class
    factoryOf(::WorkManagerRepositoryImpl) bind WorkManagerRepository::class
}