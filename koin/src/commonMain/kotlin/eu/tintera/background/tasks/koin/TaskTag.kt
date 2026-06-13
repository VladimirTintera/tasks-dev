package eu.tintera.background.tasks.koin

import eu.tintera.background.tasks.Tag
import eu.tintera.background.tasks.Tasks
import eu.tintera.background.tasks.serialization.TagSerializer
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import kotlin.reflect.KClass


@PublishedApi
internal class TagRegistration<T : Tag>(
    val identifier: String,
    val serializer: TagSerializer<T>,
    val type: KClass<T>
) {
    init {
        println("Registering tag with identifier: $identifier")
        Tasks.registry.registerTag(
            identifier = identifier,
            type = type,
            serializer = serializer
        )
    }
}

inline fun <reified T : Tag> Module.taskTag(
    identifier: String,
    serializer: TagSerializer<T>
) {
    println("Trying to register tag with identifier: $identifier")
    single(createdAtStart = true) {
        named<T>()
        TagRegistration(
            identifier = identifier,
            serializer = serializer,
            type = T::class
        )
    }
}