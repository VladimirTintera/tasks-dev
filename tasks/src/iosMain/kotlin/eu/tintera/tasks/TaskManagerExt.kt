package eu.tintera.tasks

import eu.tintera.guard.ExecutionContextProvider
import eu.tintera.guard.use
import eu.tintera.tasks.koin.Resolver
import org.koin.core.component.get


suspend fun TaskManager.executeFromIosBackgroundEvent(
    block: suspend TaskManager.() -> Unit
) = Resolver.get<ExecutionContextProvider>().acquire().use {
    block()
}
