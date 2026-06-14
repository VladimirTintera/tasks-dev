package eu.tintera.background.tasks

import eu.tintera.background.guard.ExecutionContextProvider
import eu.tintera.background.guard.use
import eu.tintera.background.tasks.runtime.Resolver
import org.koin.core.component.get


suspend fun TaskManager.executeFromIosBackgroundEvent(
    block: suspend TaskManager.() -> Unit
) = Resolver.get<ExecutionContextProvider>().acquire().use {
    block()
}
