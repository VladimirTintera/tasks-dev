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

inline fun <reified T : LegacyTaskHandler> TaskManager.register(
    noinline factory: () -> T
) = register(
    identifier = T::class.fullName,
    factory = factory,
)