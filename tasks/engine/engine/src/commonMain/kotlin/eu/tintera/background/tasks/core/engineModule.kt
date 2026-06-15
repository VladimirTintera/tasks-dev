package eu.tintera.background.tasks.core

import eu.tintera.background.guard.ExecutionContextObserver
import eu.tintera.background.tasks.TaskLifecycleObserver
import eu.tintera.background.tasks.TaskManager
import eu.tintera.background.tasks.core.db.coreDbModule
import eu.tintera.background.tasks.core.constraints.*
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val engineModule = module {

    includes(
        coreModule,
        coreDbModule
    )

    factoryOf(::TaskProcessorImpl) bind TaskProcessor::class
    singleOf(::TaskDispatcher) {
        createdAtStart()
    }

    singleOf(::InitialDelayConstraint) bind Constraint::class
    singleOf(::NetworkStateConstraint) bind Constraint::class
    singleOf(::ParentsConstraint) bind Constraint::class
    singleOf(::ProcessTimePrecondition) bind Constraint::class

    single {
        ConstraintController(constraints = getAll())
    }

    singleOf(::ActiveTaskTrackerImpl) binds arrayOf(ActiveTaskTracker::class, TaskLifecycleObserver::class)

    singleOf(::OrphanTaskSweeper) {
        createdAtStart()
    } bind ExecutionContextObserver::class


    factoryOf(::RepositoryCoreTaskManager) bind TaskManager::class
}