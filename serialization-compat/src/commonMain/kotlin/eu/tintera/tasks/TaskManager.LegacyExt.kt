package eu.tintera.tasks

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