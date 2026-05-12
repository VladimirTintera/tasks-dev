package eu.tintera.tasks.db

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind

internal actual fun org.koin.core.module.Module.platformDb() {
    factoryOf(::WebDatabaseBuilderFactory) bind DatabaseBuilderFactory::class
}