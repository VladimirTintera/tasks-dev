package eu.tintera.background.tasks

import eu.tintera.background.tasks.compat.DataTaskHandler

/** Registers a handler working with the untyped [eu.tintera.background.tasks.compat.Data] payload. */
fun Registry.register(
    identifier: String,
    factory: () -> DataTaskHandler
) {
    register(
        identifier = identifier,
        currentVersion = 1,
        factory = factory,
        inputSerializer = dataSerializer(),
        outputSerializer = dataSerializer(),
        progressSerializer = dataSerializer()
    )
}
