package eu.tintera.tasks.koin

import org.koin.core.Koin
import org.koin.core.component.KoinComponent

internal interface TasksKoinComponent : KoinComponent {
    override fun getKoin(): Koin = TasksKoinContext.koinApp.koin
}