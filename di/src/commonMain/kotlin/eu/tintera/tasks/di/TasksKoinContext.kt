package eu.tintera.tasks.di

import eu.tintera.tasks.InternalTasksApi
import org.koin.core.KoinApplication

@InternalTasksApi
object TasksKoinContext {
    lateinit var koinApp: KoinApplication
}