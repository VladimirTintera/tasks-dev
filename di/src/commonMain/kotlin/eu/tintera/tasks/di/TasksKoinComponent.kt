package eu.tintera.tasks.di

import org.koin.core.Koin
import org.koin.core.component.KoinComponent

interface TasksKoinComponent : KoinComponent {
    override fun getKoin(): Koin = TasksKoinContext.koinApp.koin
}