package eu.tintera.tasks.di

import eu.tintera.tasks.InternalTasksApi
import org.koin.core.Koin
import org.koin.core.component.KoinComponent

@InternalTasksApi
interface TasksKoinComponent : KoinComponent {
    override fun getKoin(): Koin = TasksKoinContext.koinApp.koin
}