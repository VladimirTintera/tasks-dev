package eu.tintera.tasks.core

import eu.tintera.guard.ExecutionContextObserver
import eu.tintera.tasks.TaskManager
import eu.tintera.tasks.core.db.coreDbModule
import eu.tintera.tasks.core.constraints.*
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
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

    singleOf(::ActiveTaskTrackerImpl) bind ActiveTaskTracker::class

    singleOf(::OrphanTaskSweeper) {
        createdAtStart()
    } bind ExecutionContextObserver::class


    factoryOf(::RepositoryCoreTaskManager) bind TaskManager::class
}