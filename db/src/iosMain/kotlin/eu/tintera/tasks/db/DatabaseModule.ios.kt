package eu.tintera.tasks.db

import org.koin.core.module.Module

actual fun Module.platformDb() {
    single { getDatabase() }
}