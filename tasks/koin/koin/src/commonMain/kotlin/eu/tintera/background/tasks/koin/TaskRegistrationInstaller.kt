package eu.tintera.background.tasks.koin

import eu.tintera.background.tasks.TaskRegistration
import eu.tintera.background.tasks.Tasks

@PublishedApi
internal class TaskRegistrationInstaller(
    registration: TaskRegistration<Any, Any, Any>
) {
    init {
        Tasks.registry.register(registration)
    }
}
