package eu.tintera.tasks.core

import eu.tintera.tasks.Tag
import eu.tintera.tasks.serialization.TagSerializer

class TagRegistration<T: Tag>(
    val identifier: String,
    val serializer: TagSerializer<T>
)