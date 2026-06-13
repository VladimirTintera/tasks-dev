package eu.tintera.background.tasks

import eu.tintera.background.tasks.compat.LegacyTaskHandler

fun Registry.register(
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