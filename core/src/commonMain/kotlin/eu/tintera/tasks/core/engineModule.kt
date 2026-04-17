package eu.tintera.tasks.core

import eu.tintera.guard.ExecutionContextObserver
import eu.tintera.tasks.core.preconditions.InitialDelayTaskPrecondition
import eu.tintera.tasks.core.preconditions.NetworkStateTaskPrecondition
import eu.tintera.tasks.core.preconditions.TaskPreconditionController
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val engineModule = module {

    includes(coreModule)

    factoryOf(::TaskProcessorImpl) bind TaskProcessor::class
    singleOf(::TaskDispatcher) {
        createdAtStart()
    }

    singleOf(::InitialDelayTaskPrecondition) bind TaskPrecondition::class
    singleOf(::NetworkStateTaskPrecondition) bind TaskPrecondition::class
    single {
        TaskPreconditionController(preconditions = getAll())
    }

    singleOf(::ActiveTaskTrackerImpl) bind ActiveTaskTracker::class

    singleOf(::OrphanTaskSweeper) {
        createdAtStart()
    } bind ExecutionContextObserver::class


}