package eu.tintera.tasks.legacy

import eu.tintera.tasks.TaskHandler

@Deprecated("Use typed RegistrationHandler instead")
typealias LegacyTaskHandler = TaskHandler<Data, Data, Data>