package eu.tintera.tasks

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

inline fun <reified T : LegacyTaskHandler> Registry.register(
    noinline factory: () -> T
) = register(
    identifier = T::class.fullName,
    factory = factory,
)