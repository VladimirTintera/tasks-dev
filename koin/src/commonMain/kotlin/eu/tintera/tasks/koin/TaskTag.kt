package eu.tintera.tasks.koin

import eu.tintera.tasks.Tag
import eu.tintera.tasks.Tasks
import eu.tintera.tasks.fullName
import eu.tintera.tasks.serialization.TagSerializer
import org.koin.core.module.Module

inline fun <reified T: Tag> Module.taskTag(
    identifier: String = T::class.fullName,
    serializer: TagSerializer<T>
) {
    single(createdAtStart = true) {
        object {
            init {
                Tasks.registry.registerTag<T>(
                    identifier = identifier,
                    type = T::class,
                    serializer = serializer
                )
            }
        }
    }
}