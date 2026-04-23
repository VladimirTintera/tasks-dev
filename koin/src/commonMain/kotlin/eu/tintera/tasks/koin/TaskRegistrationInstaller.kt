package eu.tintera.tasks.koin

import eu.tintera.tasks.TaskRegistration
import eu.tintera.tasks.Tasks

@PublishedApi
internal class TaskRegistrationInstaller(
    registration: TaskRegistration<Any, Any, Any>
) {
    init {
        Tasks.registry.register(registration)
    }
}
