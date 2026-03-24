package eu.tintera.tasks.koin

import androidx.work.WorkManager
import eu.tintera.tasks.core.WorkManagerCoreTaskManager
import eu.tintera.tasks.core.CoreTaskManager
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal actual fun platformModule(): Module = module {
    factory<WorkManager> { WorkManager.getInstance(get()) }
    factoryOf(::WorkManagerCoreTaskManager) bind CoreTaskManager::class
}