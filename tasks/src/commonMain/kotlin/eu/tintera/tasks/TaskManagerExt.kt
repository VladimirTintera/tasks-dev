package eu.tintera.tasks

import eu.tintera.tasks.koin.Resolver
import org.koin.core.component.get

fun TaskManager.Companion.getInstance(): TaskManager = with(Resolver) {
    get<TaskManager>()
}