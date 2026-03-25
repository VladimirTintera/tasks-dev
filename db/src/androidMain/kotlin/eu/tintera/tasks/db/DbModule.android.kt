package eu.tintera.tasks.db

import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind

internal actual fun Module.platformDb() {
    factoryOf(::AndroidDatabaseBuilderFactory) bind AndroidDatabaseBuilderFactory::class
}