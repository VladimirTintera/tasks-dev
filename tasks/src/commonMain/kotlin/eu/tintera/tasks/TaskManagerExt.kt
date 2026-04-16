package eu.tintera.tasks

import eu.tintera.tasks.core.seriaization.DataSerializer
import eu.tintera.tasks.koin.Resolver
import org.koin.core.component.get

fun TaskManager.Companion.getInstance(): TaskManager = with(Resolver) {
    get<TaskManager>()
}

@Deprecated("Use typed registration instead")
fun TaskManager.register(
    identifier: String,
    factory: () -> LegacyTaskHandler
) {
    register(
        identifier = identifier,
        currentVersion = 1,
        factory = factory,
        inputSerializer = DataSerializer,
        outputSerializer = DataSerializer,
        progressSerializer = DataSerializer
    )
}