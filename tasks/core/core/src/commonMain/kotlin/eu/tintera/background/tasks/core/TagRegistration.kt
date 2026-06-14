package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.Tag
import eu.tintera.background.tasks.serialization.TagSerializer

class TagRegistration<T: Tag>(
    val identifier: String,
    val serializer: TagSerializer<T>
)