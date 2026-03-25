package eu.tintera.tasks.db

import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind

internal actual fun Module.platformDb() {
    singleOf(::JvmDatabaseBuilderFactory) bind DatabaseBuilderFactory::class
}