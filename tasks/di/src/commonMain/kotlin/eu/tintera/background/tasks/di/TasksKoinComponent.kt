package eu.tintera.background.tasks.di

import eu.tintera.background.tasks.InternalTasksApi
import org.koin.core.Koin
import org.koin.core.component.KoinComponent

@InternalTasksApi
interface TasksKoinComponent : KoinComponent {
    override fun getKoin(): Koin = TasksKoinContext.koinApp.koin
}