package eu.tintera.tasks

import eu.tintera.tasks.koin.Resolver
import eu.tintera.tasks.legacy.legacySerializer
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
        inputSerializer = legacySerializer(),
        outputSerializer = legacySerializer(),
        progressSerializer = legacySerializer()
    )
}