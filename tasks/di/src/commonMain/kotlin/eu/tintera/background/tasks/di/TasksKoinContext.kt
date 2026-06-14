package eu.tintera.background.tasks.di

import eu.tintera.background.tasks.InternalTasksApi
import org.koin.core.KoinApplication

@InternalTasksApi
object TasksKoinContext {
    lateinit var koinApp: KoinApplication
}